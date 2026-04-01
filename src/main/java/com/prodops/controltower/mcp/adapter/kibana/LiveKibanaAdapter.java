package com.prodops.controltower.mcp.adapter.kibana;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.LogSearchQuery;
import com.prodops.controltower.mcp.domain.port.KibanaLogPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("live")
public class LiveKibanaAdapter implements KibanaLogPort {

  private final ProdOpsProperties properties;
  private final RestClient.Builder restClientBuilder;

  public LiveKibanaAdapter(ProdOpsProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public List<LogEvent> searchLogs(LogSearchQuery query) {
    ClusterKibana config = clusterKibana(query.cluster());
    String dataView =
        query.dataView() == null || query.dataView().isBlank()
            ? config.defaultDataView()
            : query.dataView();
    if (config.searchBaseUrl().isBlank() || dataView.isBlank()) {
      return List.of();
    }
    SearchResponse response =
        client(config)
            .post()
            .uri(config.searchBaseUrl() + "/" + dataView + "/_search")
            .contentType(MediaType.APPLICATION_JSON)
            .body(searchBody(query))
            .retrieve()
            .body(SearchResponse.class);
    if (response == null || response.hits() == null || response.hits().hits() == null) {
      return List.of();
    }
    List<LogEvent> events = new ArrayList<>();
    for (SearchHit hit : response.hits().hits()) {
      Map<String, Object> source = hit.source();
      String serviceName =
          firstNonBlank(
              stringAt(source, "service.name"),
              stringAt(source, "kubernetes.labels.app"),
              query.serviceOrWorkload());
      String namespace = firstNonBlank(stringAt(source, "kubernetes.namespace"), query.namespace());
      String podName = stringAt(source, "kubernetes.pod.name");
      String container = stringAt(source, "kubernetes.container.name");
      String severity =
          firstNonBlank(stringAt(source, "log.level"), stringAt(source, "severity"), "INFO");
      String message =
          firstNonBlank(stringAt(source, "message"), stringAt(source, "error.message"), "");
      String signature =
          firstNonBlank(stringAt(source, "error.type"), stringAt(source, "exception.class"), "");
      String traceId =
          firstNonBlank(stringAt(source, "trace.id"), stringAt(source, "labels.trace_id"));
      String requestId =
          firstNonBlank(stringAt(source, "labels.request_id"), stringAt(source, "http.request.id"));
      String versionTag =
          firstNonBlank(stringAt(source, "service.version"), stringAt(source, "labels.version"));
      Instant observedAt =
          Instant.parse(firstNonBlank(stringAt(source, "@timestamp"), query.end().toString()));
      events.add(
          new LogEvent(
              query.cluster(),
              namespace,
              serviceName,
              podName,
              container,
              severity,
              message,
              message,
              signature,
              traceId,
              requestId,
              versionTag,
              observedAt,
              discoverLink(config, query, serviceName)));
    }
    return events;
  }

  private Map<String, Object> searchBody(LogSearchQuery query) {
    List<Object> filters = new ArrayList<>();
    filters.add(
        Map.of("range", Map.of("@timestamp", Map.of("gte", query.start(), "lte", query.end()))));
    if (query.namespace() != null && !query.namespace().isBlank()) {
      filters.add(Map.of("term", Map.of("kubernetes.namespace", query.namespace())));
    }
    if (query.serviceOrWorkload() != null && !query.serviceOrWorkload().isBlank()) {
      filters.add(
          Map.of(
              "bool",
              Map.of(
                  "should",
                  List.of(
                      Map.of("term", Map.of("service.name", query.serviceOrWorkload())),
                      Map.of("term", Map.of("kubernetes.labels.app", query.serviceOrWorkload()))),
                  "minimum_should_match",
                  1)));
    }
    if (query.severity() != null && !query.severity().isBlank()) {
      filters.add(Map.of("term", Map.of("log.level", query.severity().toLowerCase())));
    }
    if (query.traceId() != null && !query.traceId().isBlank()) {
      filters.add(Map.of("term", Map.of("trace.id", query.traceId())));
    }
    if (query.requestId() != null && !query.requestId().isBlank()) {
      filters.add(Map.of("term", Map.of("labels.request_id", query.requestId())));
    }
    if (query.versionTag() != null && !query.versionTag().isBlank()) {
      filters.add(Map.of("term", Map.of("service.version", query.versionTag())));
    }
    List<Object> must = new ArrayList<>();
    if (query.text() != null && !query.text().isBlank()) {
      must.add(Map.of("query_string", Map.of("query", query.text())));
    }
    return Map.of(
        "size",
        query.limit(),
        "sort",
        List.of(Map.of("@timestamp", Map.of("order", "desc"))),
        "query",
        Map.of("bool", Map.of("filter", filters, "must", must)));
  }

  private RestClient client(ClusterKibana config) {
    RestClient.Builder builder =
        restClientBuilder.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    if (!config.bearerToken().isBlank()) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.bearerToken());
    }
    return builder.build();
  }

  private ClusterKibana clusterKibana(String cluster) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(
            candidate ->
                new ClusterKibana(
                    candidate.kibana().baseUrl(),
                    resolveSecret(candidate.kibana().bearerTokenRef()),
                    candidate.kibana().defaultDataView(),
                    candidate.kibana().elasticsearchBaseUrl().isBlank()
                        ? candidate.kibana().baseUrl()
                        : candidate.kibana().elasticsearchBaseUrl(),
                    candidate.kibana().discoverPath()))
        .orElseThrow(
            () -> new IllegalArgumentException("Cluster is not configured for Kibana access."));
  }

  private DeepLink discoverLink(ClusterKibana config, LogSearchQuery query, String serviceName) {
    return new DeepLink(
        "Kibana Discover",
        EvidenceSource.KIBANA,
        config.baseUrl() + config.discoverPath(),
        "Discover view for " + serviceName + " between " + query.start() + " and " + query.end());
  }

  private String resolveSecret(String reference) {
    return reference == null || reference.isBlank() ? "" : System.getenv(reference);
  }

  @SuppressWarnings("unchecked")
  private String stringAt(Map<String, Object> source, String path) {
    Object current = source;
    for (String segment : path.split("\\.")) {
      if (!(current instanceof Map<?, ?> currentMap)) {
        return null;
      }
      current = ((Map<String, Object>) currentMap).get(segment);
      if (current == null) {
        return null;
      }
    }
    return String.valueOf(current);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
        return value;
      }
    }
    return null;
  }

  private record ClusterKibana(
      String baseUrl,
      String bearerToken,
      String defaultDataView,
      String searchBaseUrl,
      String discoverPath) {}

  private record SearchResponse(Hits hits) {}

  private record Hits(List<SearchHit> hits) {}

  private record SearchHit(Map<String, Object> _source) {
    Map<String, Object> source() {
      return _source;
    }
  }
}
