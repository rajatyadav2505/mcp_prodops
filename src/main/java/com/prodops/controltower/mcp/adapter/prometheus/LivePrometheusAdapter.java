package com.prodops.controltower.mcp.adapter.prometheus;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import com.prodops.controltower.mcp.policy.GuardrailViolationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@Profile("live")
public class LivePrometheusAdapter implements MetricsPort {

  private final ProdOpsProperties properties;
  private final RestClient.Builder restClientBuilder;
  private final Clock clock;

  public LivePrometheusAdapter(
      ProdOpsProperties properties, RestClient.Builder restClientBuilder, Clock clock) {
    this.properties = properties;
    this.restClientBuilder = restClientBuilder;
    this.clock = clock;
  }

  @Override
  public List<MetricValue> goldenSignals(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      ServiceCatalogEntry catalogEntry) {
    Map<String, String> templates = queryTemplates(namespace, serviceOrWorkload, catalogEntry);
    Instant now = Instant.now(clock);
    return templates.entrySet().stream()
        .map(
            entry -> {
              PromqlExecutionResult result = instantQuery(cluster, entry.getValue(), now);
              double value =
                  result.series().stream()
                      .findFirst()
                      .flatMap(series -> series.points().stream().findFirst())
                      .map(MetricSeriesPoint::value)
                      .orElse(0.0d);
              return new MetricValue(
                  entry.getKey(),
                  "ratio",
                  Math.max(0.0d, Math.min(1.0d, value)),
                  value >= 0.8d ? "high" : value >= 0.5d ? "moderate" : "normal",
                  now,
                  entry.getValue(),
                  "Live Prometheus metric collected via configured golden-signal query.");
            })
        .toList();
  }

  @Override
  public PromqlExecutionResult instantQuery(String cluster, String query, Instant evaluationTime) {
    ClusterPrometheus config = clusterPrometheus(cluster);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("query", query);
    form.add("time", evaluationTime.toString());
    PrometheusResponse response =
        client(config)
            .post()
            .uri(config.baseUrl() + "/api/v1/query")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(PrometheusResponse.class);
    List<MetricSeries> series =
        Optional.ofNullable(response)
            .map(PrometheusResponse::data)
            .map(PrometheusData::result)
            .orElse(List.of())
            .stream()
            .limit(properties.guardrails().maxSeries())
            .map(this::toInstantSeries)
            .toList();
    boolean truncated =
        Optional.ofNullable(response)
                .map(PrometheusResponse::data)
                .map(PrometheusData::result)
                .orElse(List.of())
                .size()
            > properties.guardrails().maxSeries();
    return new PromqlExecutionResult(
        cluster,
        query,
        evaluationTime,
        series,
        truncated,
        truncated ? List.of("Series count truncated to configured maximum.") : List.of());
  }

  @Override
  public PromqlExecutionResult rangeQuery(
      String cluster, String query, Instant start, Instant end, Duration step) {
    if (step.isZero() || step.isNegative()) {
      throw new GuardrailViolationException("Range query step must be positive.");
    }
    ClusterPrometheus config = clusterPrometheus(cluster);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("query", query);
    form.add("start", start.toString());
    form.add("end", end.toString());
    form.add("step", String.valueOf(step.toSeconds()));
    PrometheusResponse response =
        client(config)
            .post()
            .uri(config.baseUrl() + "/api/v1/query_range")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(PrometheusResponse.class);
    List<MetricSeries> series =
        Optional.ofNullable(response)
            .map(PrometheusResponse::data)
            .map(PrometheusData::result)
            .orElse(List.of())
            .stream()
            .limit(properties.guardrails().maxSeries())
            .map(this::toRangeSeries)
            .toList();
    boolean truncated =
        Optional.ofNullable(response)
                .map(PrometheusResponse::data)
                .map(PrometheusData::result)
                .orElse(List.of())
                .size()
            > properties.guardrails().maxSeries();
    return new PromqlExecutionResult(
        cluster,
        query,
        end,
        series,
        truncated,
        truncated ? List.of("Series count truncated to configured maximum.") : List.of());
  }

  private Map<String, String> queryTemplates(
      String namespace, String workload, ServiceCatalogEntry catalogEntry) {
    Map<String, String> templates = new LinkedHashMap<>();
    if (catalogEntry != null
        && catalogEntry.promqlTemplates() != null
        && !catalogEntry.promqlTemplates().isEmpty()) {
      templates.put(
          "error_rate_ratio", catalogEntry.promqlTemplates().getOrDefault("error_rate", ""));
      templates.put(
          "latency_slo_ratio", catalogEntry.promqlTemplates().getOrDefault("latency", ""));
      templates.put("cpu_saturation_ratio", catalogEntry.promqlTemplates().getOrDefault("cpu", ""));
      templates.put(
          "memory_pressure_ratio", catalogEntry.promqlTemplates().getOrDefault("memory", ""));
      return templates;
    }
    templates.put(
        "error_rate_ratio",
        "clamp_max(sum(rate(http_server_requests_seconds_count{namespace=\""
            + namespace
            + "\",app=\""
            + workload
            + "\",status=~\"5..\"}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{namespace=\""
            + namespace
            + "\",app=\""
            + workload
            + "\"}[5m])), 0.001), 1)");
    templates.put(
        "latency_slo_ratio",
        "clamp_max(histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace=\""
            + namespace
            + "\",app=\""
            + workload
            + "\"}[5m])) by (le)) / 0.5, 1)");
    templates.put(
        "cpu_saturation_ratio",
        "clamp_max(sum(rate(container_cpu_usage_seconds_total{namespace=\""
            + namespace
            + "\",pod=~\""
            + workload
            + "-.*\",container!=\"POD\"}[5m])), 1)");
    templates.put(
        "memory_pressure_ratio",
        "clamp_max(sum(container_memory_working_set_bytes{namespace=\""
            + namespace
            + "\",pod=~\""
            + workload
            + "-.*\",container!=\"POD\"}) / clamp_min(sum(kube_pod_container_resource_limits{namespace=\""
            + namespace
            + "\",pod=~\""
            + workload
            + "-.*\",resource=\"memory\"}), 1), 1)");
    return templates;
  }

  private RestClient client(ClusterPrometheus config) {
    RestClient.Builder builder =
        restClientBuilder.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    if (!config.bearerToken().isBlank()) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.bearerToken());
    }
    return builder.build();
  }

  private ClusterPrometheus clusterPrometheus(String cluster) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(
            candidate ->
                new ClusterPrometheus(
                    candidate.prometheus().baseUrl(),
                    resolveSecret(candidate.prometheus().bearerTokenRef())))
        .orElseThrow(
            () -> new IllegalArgumentException("Cluster is not configured for Prometheus access."));
  }

  private String resolveSecret(String reference) {
    return reference == null || reference.isBlank() ? "" : System.getenv(reference);
  }

  private MetricSeries toInstantSeries(PrometheusResult result) {
    MetricSeriesPoint point =
        new MetricSeriesPoint(
            Instant.ofEpochSecond(((Number) result.value().getFirst()).longValue()),
            Double.parseDouble(String.valueOf(result.value().get(1))));
    return new MetricSeries("instant", "", "ratio", result.metric(), List.of(point));
  }

  private MetricSeries toRangeSeries(PrometheusResult result) {
    List<MetricSeriesPoint> points =
        Optional.ofNullable(result.values()).orElse(List.of()).stream()
            .map(
                value ->
                    new MetricSeriesPoint(
                        Instant.ofEpochSecond(((Number) value.getFirst()).longValue()),
                        Double.parseDouble(String.valueOf(value.get(1)))))
            .toList();
    return new MetricSeries("range", "", "ratio", result.metric(), points);
  }

  private record ClusterPrometheus(String baseUrl, String bearerToken) {}

  private record PrometheusResponse(String status, PrometheusData data) {}

  private record PrometheusData(String resultType, List<PrometheusResult> result) {}

  private record PrometheusResult(
      Map<String, String> metric, List<Object> value, List<List<Object>> values) {}
}
