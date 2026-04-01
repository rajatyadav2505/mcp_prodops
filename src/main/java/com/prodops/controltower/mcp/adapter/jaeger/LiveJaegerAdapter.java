package com.prodops.controltower.mcp.adapter.jaeger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.TraceDependencyEdge;
import com.prodops.controltower.mcp.domain.model.TraceSearchQuery;
import com.prodops.controltower.mcp.domain.model.TraceSpanSummary;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.port.JaegerTracePort;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("live")
public class LiveJaegerAdapter implements JaegerTracePort {

  private final ProdOpsProperties properties;
  private final RestClient.Builder restClientBuilder;

  public LiveJaegerAdapter(ProdOpsProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public List<TraceSummary> searchTraces(TraceSearchQuery query) {
    if (query.traceId() != null && !query.traceId().isBlank()) {
      return getTrace(query.cluster(), query.traceId()).stream().toList();
    }
    ClusterJaeger config = clusterJaeger(query.cluster());
    if (config.baseUrl().isBlank()) {
      return List.of();
    }
    TraceResponse response =
        client(config)
            .get()
            .uri(searchUri(config.baseUrl(), query))
            .retrieve()
            .body(TraceResponse.class);
    if (response == null || response.data() == null) {
      return List.of();
    }
    return response.data().stream().map(trace -> toTraceSummary(config.baseUrl(), trace)).toList();
  }

  @Override
  public Optional<TraceSummary> getTrace(String cluster, String traceId) {
    ClusterJaeger config = clusterJaeger(cluster);
    if (config.baseUrl().isBlank()) {
      return Optional.empty();
    }
    TraceResponse response =
        client(config)
            .get()
            .uri(config.baseUrl() + "/api/traces/" + traceId)
            .retrieve()
            .body(TraceResponse.class);
    if (response == null || response.data() == null || response.data().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(toTraceSummary(config.baseUrl(), response.data().getFirst()));
  }

  private TraceSummary toTraceSummary(String baseUrl, TraceData traceData) {
    Map<String, Process> processes =
        traceData.processes() == null ? Map.of() : traceData.processes();
    List<SpanEnvelope> spans =
        traceData.spans().stream()
            .map(span -> toSpanEnvelope(span, processes))
            .sorted(Comparator.comparing(SpanEnvelope::startTime))
            .toList();
    List<SpanEnvelope> errorSpans = spans.stream().filter(SpanEnvelope::error).toList();
    SpanEnvelope firstFailing = errorSpans.stream().findFirst().orElse(null);
    List<TraceSpanSummary> hotspots =
        spans.stream()
            .sorted(Comparator.comparing(SpanEnvelope::duration).reversed())
            .limit(3)
            .map(SpanEnvelope::summary)
            .toList();
    Map<String, List<SpanEnvelope>> childrenByParent =
        spans.stream()
            .filter(span -> span.parentSpanId() != null)
            .collect(Collectors.groupingBy(SpanEnvelope::parentSpanId));
    Duration criticalPath =
        spans.stream()
            .filter(span -> span.parentSpanId() == null)
            .map(span -> longestPath(span, childrenByParent))
            .max(Duration::compareTo)
            .orElse(Duration.ZERO);
    List<TraceDependencyEdge> dependencyEdges =
        spans.stream()
            .filter(span -> span.parentSpanId() != null)
            .map(
                span -> {
                  SpanEnvelope parent =
                      spans.stream()
                          .filter(candidate -> candidate.spanId().equals(span.parentSpanId()))
                          .findFirst()
                          .orElse(null);
                  if (parent == null || parent.serviceName().equals(span.serviceName())) {
                    return null;
                  }
                  return new TraceDependencyEdge(
                      parent.serviceName(), span.serviceName(), span.duration(), span.error());
                })
            .filter(edge -> edge != null)
            .distinct()
            .toList();
    String versionTag =
        spans.stream()
            .map(SpanEnvelope::versionTag)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    String podName =
        spans.stream()
            .map(SpanEnvelope::podName)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    return new TraceSummary(
        traceData.traceId(),
        spans.isEmpty() ? "" : spans.getFirst().serviceName(),
        spans.isEmpty() ? "" : spans.getFirst().operation(),
        spans.isEmpty() ? Instant.EPOCH : spans.getFirst().startTime(),
        spans.stream().map(SpanEnvelope::duration).max(Duration::compareTo).orElse(Duration.ZERO),
        !errorSpans.isEmpty(),
        firstFailing == null ? null : firstFailing.serviceName(),
        firstFailing == null ? null : firstFailing.operation(),
        criticalPath,
        errorSpans.stream().map(SpanEnvelope::summary).toList(),
        hotspots,
        dependencyEdges,
        new DeepLink(
            "Jaeger trace " + traceData.traceId(),
            EvidenceSource.JAEGER,
            baseUrl + "/trace/" + traceData.traceId(),
            "Trace view"),
        versionTag,
        podName);
  }

  private SpanEnvelope toSpanEnvelope(Span span, Map<String, Process> processes) {
    Process process = processes.get(span.processId());
    String serviceName = process == null ? "unknown" : process.serviceName();
    Map<String, String> tags = new HashMap<>();
    if (span.tags() != null) {
      span.tags().forEach(tag -> tags.put(tag.key(), String.valueOf(tag.value())));
    }
    boolean error =
        tags.entrySet().stream()
            .anyMatch(
                entry ->
                    "error".equals(entry.getKey()) && "true".equalsIgnoreCase(entry.getValue()));
    long retryCount =
        tags.entrySet().stream()
            .filter(entry -> entry.getKey().toLowerCase().contains("retry"))
            .count();
    String parentSpanId =
        span.references() == null || span.references().isEmpty()
            ? null
            : span.references().getFirst().spanId();
    TraceSpanSummary summary =
        new TraceSpanSummary(
            span.spanId(),
            parentSpanId,
            serviceName,
            span.operationName(),
            Instant.ofEpochMilli(span.startTime() / 1000),
            Duration.ofNanos(span.duration() * 1_000L),
            error,
            retryCount > 0,
            firstNonBlank(tags.get("k8s.pod"), tags.get("pod")),
            firstNonBlank(tags.get("service.version"), tags.get("version")),
            tags);
    return new SpanEnvelope(
        summary.spanId(),
        summary.parentSpanId(),
        summary.serviceName(),
        summary.operation(),
        summary.startTime(),
        summary.duration(),
        summary.error(),
        summary.retry(),
        summary.podName(),
        summary.versionTag(),
        summary);
  }

  private Duration longestPath(
      SpanEnvelope span, Map<String, List<SpanEnvelope>> childrenByParent) {
    Duration longestChild =
        childrenByParent.getOrDefault(span.spanId(), List.of()).stream()
            .map(child -> longestPath(child, childrenByParent))
            .max(Duration::compareTo)
            .orElse(Duration.ZERO);
    return span.duration().plus(longestChild);
  }

  private String searchUri(String baseUrl, TraceSearchQuery query) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/api/traces")
            .queryParam("service", query.serviceOrWorkload())
            .queryParam("start", query.start().toEpochMilli() * 1000)
            .queryParam("end", query.end().toEpochMilli() * 1000)
            .queryParam("lookback", "custom")
            .queryParam("limit", query.limit());
    if (query.operation() != null && !query.operation().isBlank()) {
      builder.queryParam("operation", query.operation());
    }
    if (query.errorsOnly()) {
      builder.queryParam("tags", "{\"error\":\"true\"}");
    }
    return builder.toUriString();
  }

  private RestClient client(ClusterJaeger config) {
    RestClient.Builder builder =
        restClientBuilder.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    if (!config.bearerToken().isBlank()) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.bearerToken());
    }
    return builder.build();
  }

  private ClusterJaeger clusterJaeger(String cluster) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(
            candidate ->
                new ClusterJaeger(
                    candidate.jaeger().baseUrl(),
                    resolveSecret(candidate.jaeger().bearerTokenRef())))
        .orElseThrow(
            () -> new IllegalArgumentException("Cluster is not configured for Jaeger access."));
  }

  private String resolveSecret(String reference) {
    return reference == null || reference.isBlank() ? "" : System.getenv(reference);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private record ClusterJaeger(String baseUrl, String bearerToken) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TraceResponse(List<TraceData> data) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TraceData(
      @JsonProperty("traceID") String traceId, List<Span> spans, Map<String, Process> processes) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Span(
      @JsonProperty("traceID") String traceId,
      @JsonProperty("spanID") String spanId,
      String operationName,
      long startTime,
      long duration,
      @JsonProperty("processID") String processId,
      List<Reference> references,
      List<Tag> tags) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Reference(@JsonProperty("spanID") String spanId) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Tag(String key, Object value) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Process(String serviceName) {}

  private record SpanEnvelope(
      String spanId,
      String parentSpanId,
      String serviceName,
      String operation,
      Instant startTime,
      Duration duration,
      boolean error,
      boolean retry,
      String podName,
      String versionTag,
      TraceSpanSummary summary) {}
}
