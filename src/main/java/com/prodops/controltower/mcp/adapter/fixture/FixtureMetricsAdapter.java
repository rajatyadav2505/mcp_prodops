package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.adapter.fixture.FixtureScenarioLoader.FixtureMetricBundle;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureMetricsAdapter implements MetricsPort {

  private final FixtureScenarioLoader loader;

  public FixtureMetricsAdapter(FixtureScenarioLoader loader) {
    this.loader = loader;
  }

  @Override
  public List<com.prodops.controltower.mcp.domain.model.MetricValue> goldenSignals(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      java.time.Duration lookback,
      ServiceCatalogEntry catalogEntry) {
    return bundleFor(cluster, namespace, serviceOrWorkload)
        .map(FixtureMetricBundle::goldenSignals)
        .orElse(List.of());
  }

  @Override
  public PromqlExecutionResult instantQuery(String cluster, String query, Instant evaluationTime) {
    List<com.prodops.controltower.mcp.domain.model.MetricSeries> series =
        loader.loadRepository().metrics().stream()
            .filter(bundle -> bundle.cluster().equals(cluster))
            .findFirst()
            .map(bundle -> matchingSeries(bundle, query))
            .orElse(List.of());
    return new PromqlExecutionResult(
        cluster,
        query,
        evaluationTime,
        series,
        false,
        List.of("Fixture-mode raw PromQL uses deterministic scenario mappings."));
  }

  @Override
  public PromqlExecutionResult rangeQuery(
      String cluster, String query, Instant start, Instant end, java.time.Duration step) {
    List<com.prodops.controltower.mcp.domain.model.MetricSeries> series =
        loader.loadRepository().metrics().stream()
            .filter(bundle -> bundle.cluster().equals(cluster))
            .findFirst()
            .map(bundle -> matchingSeries(bundle, query))
            .orElse(List.of());
    return new PromqlExecutionResult(
        cluster,
        query,
        end,
        series,
        false,
        List.of("Fixture-mode range PromQL uses deterministic scenario mappings."));
  }

  private java.util.Optional<FixtureMetricBundle> bundleFor(
      String cluster, String namespace, String scope) {
    return loader.loadRepository().metrics().stream()
        .filter(bundle -> bundle.cluster().equals(cluster))
        .filter(bundle -> bundle.namespace().equals(namespace))
        .filter(bundle -> bundle.scope().equals(scope))
        .findFirst();
  }

  private List<com.prodops.controltower.mcp.domain.model.MetricSeries> matchingSeries(
      FixtureMetricBundle bundle, String query) {
    List<com.prodops.controltower.mcp.domain.model.MetricSeries> exactMatches =
        bundle.series().stream().filter(metric -> metric.query().equals(query)).toList();
    if (!exactMatches.isEmpty()) {
      return exactMatches;
    }
    String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
    List<com.prodops.controltower.mcp.domain.model.MetricSeries> hintedMatches =
        bundle.series().stream()
            .filter(
                metric ->
                    normalizedQuery.contains(metric.name().toLowerCase(Locale.ROOT))
                        || normalizedQuery.contains(metric.query().toLowerCase(Locale.ROOT))
                        || (normalizedQuery.contains("http_requests")
                            && metric.name().equals("error_rate_ratio"))
                        || (normalizedQuery.contains("histogram_quantile")
                            && metric.name().equals("latency_slo_ratio"))
                        || (normalizedQuery.contains("container_cpu")
                            && (metric.name().equals("cpu_usage_cores")
                                || metric.name().equals("cpu_saturation_ratio")))
                        || (normalizedQuery.contains("container_memory")
                            && (metric.name().equals("memory_working_set_bytes")
                                || metric.name().equals("memory_pressure_ratio"))))
            .toList();
    if (!hintedMatches.isEmpty()) {
      return hintedMatches;
    }
    return bundle.series();
  }
}
