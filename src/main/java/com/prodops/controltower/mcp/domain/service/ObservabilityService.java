package com.prodops.controltower.mcp.domain.service;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.correlation.LogSignatureAnalyzer;
import com.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.LogSearchQuery;
import com.prodops.controltower.mcp.domain.model.LogSearchResult;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.TraceSearchQuery;
import com.prodops.controltower.mcp.domain.model.TraceSearchResult;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.port.DashboardPort;
import com.prodops.controltower.mcp.domain.port.JaegerTracePort;
import com.prodops.controltower.mcp.domain.port.KibanaLogPort;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import com.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import com.prodops.controltower.mcp.policy.GuardrailViolationException;
import com.prodops.controltower.mcp.policy.ScopePolicy;
import com.prodops.controltower.mcp.redaction.RedactionService;
import com.prodops.controltower.mcp.support.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityService {

  private final MetricsPort metricsPort;
  private final DashboardPort dashboardPort;
  private final KibanaLogPort kibanaLogPort;
  private final JaegerTracePort jaegerTracePort;
  private final ServiceCatalogPort serviceCatalogPort;
  private final ScopePolicy scopePolicy;
  private final ProdOpsProperties properties;
  private final RedactionService redactionService;
  private final LogSignatureAnalyzer logSignatureAnalyzer;
  private final Clock clock;

  public ObservabilityService(
      MetricsPort metricsPort,
      DashboardPort dashboardPort,
      KibanaLogPort kibanaLogPort,
      JaegerTracePort jaegerTracePort,
      ServiceCatalogPort serviceCatalogPort,
      ScopePolicy scopePolicy,
      ProdOpsProperties properties,
      RedactionService redactionService,
      LogSignatureAnalyzer logSignatureAnalyzer,
      Clock clock) {
    this.metricsPort = metricsPort;
    this.dashboardPort = dashboardPort;
    this.kibanaLogPort = kibanaLogPort;
    this.jaegerTracePort = jaegerTracePort;
    this.serviceCatalogPort = serviceCatalogPort;
    this.scopePolicy = scopePolicy;
    this.properties = properties;
    this.redactionService = redactionService;
    this.logSignatureAnalyzer = logSignatureAnalyzer;
    this.clock = clock;
  }

  public PromqlExecutionResult runPromqlInstant(
      String cluster, String query, Instant time, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    if (!properties.guardrails().rawPromqlEnabled()) {
      throw new GuardrailViolationException("Raw PromQL execution is disabled by policy.");
    }
    return metricsPort.instantQuery(cluster, query, time);
  }

  public PromqlExecutionResult runPromqlRange(
      String cluster, String query, Instant start, Instant end, Duration step, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    if (!properties.guardrails().rawPromqlEnabled()) {
      throw new GuardrailViolationException("Raw PromQL execution is disabled by policy.");
    }
    scopePolicy.verifyRange(Duration.between(start, end));
    if (step.compareTo(properties.guardrails().minStep()) < 0) {
      throw new GuardrailViolationException("Requested step is below the configured minimum.");
    }
    return metricsPort.rangeQuery(cluster, query, start, end, step);
  }

  public List<DashboardInfo> searchDashboards(
      String cluster, String query, List<String> tags, String folder, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    return dashboardPort.search(
        cluster, query, tags, folder, properties.guardrails().maxDashboards());
  }

  public DashboardInfo getDashboardSummary(String cluster, String dashboardUid, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    return dashboardPort
        .getByUid(cluster, dashboardUid)
        .orElseThrow(
            () -> new NotFoundException("Dashboard UID is not available in the configured scope."));
  }

  public LogSearchResult searchKibanaLogs(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String severity,
      String text,
      String traceId,
      String requestId,
      String versionTag,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(lookback);
    Instant end = Instant.now(clock);
    Instant start = end.minus(lookback);
    return searchKibanaLogsWindow(
        cluster,
        namespace,
        serviceOrWorkload,
        start,
        end,
        severity,
        text,
        traceId,
        requestId,
        versionTag,
        identity);
  }

  public LogSearchResult summarizeKibanaErrors(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String text,
      String traceId,
      String requestId,
      String versionTag,
      String identity) {
    LogSearchResult searchResult =
        searchKibanaLogs(
            cluster,
            namespace,
            serviceOrWorkload,
            lookback,
            "ERROR",
            text,
            traceId,
            requestId,
            versionTag,
            identity);
    String dominant = logSignatureAnalyzer.dominantSignature(searchResult.topSignatures());
    return new LogSearchResult(
        searchResult.cluster(),
        searchResult.namespace(),
        searchResult.serviceOrWorkload(),
        "Top Kibana error signature for " + serviceOrWorkload + " is " + dominant + ".",
        "Error signatures were grouped deterministically from sanitized Kibana log events.",
        searchResult.totalHits(),
        searchResult.truncated(),
        searchResult.events(),
        searchResult.topSignatures(),
        searchResult.deepLinks(),
        searchResult.generatedAt(),
        searchResult.dataFreshness());
  }

  public TraceSearchResult searchJaegerTraces(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String operation,
      boolean errorsOnly,
      String traceId,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(lookback);
    Instant end = Instant.now(clock);
    Instant start = end.minus(lookback);
    return searchJaegerTracesWindow(
        cluster,
        namespace,
        serviceOrWorkload,
        start,
        end,
        operation,
        errorsOnly,
        traceId,
        identity);
  }

  public LogSearchResult searchKibanaLogsWindow(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Instant start,
      Instant end,
      String severity,
      String text,
      String traceId,
      String requestId,
      String versionTag,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyRange(Duration.between(start, end));
    int limit = properties.guardrails().maxLogHits();
    scopePolicy.verifyLogHits(limit);
    ServiceCatalogEntry catalogEntry = resolveCatalogEntry(cluster, namespace, serviceOrWorkload);
    List<LogEvent> events =
        kibanaLogPort.searchLogs(
            new LogSearchQuery(
                cluster,
                namespace,
                serviceOrWorkload,
                start,
                end,
                severity,
                text,
                traceId,
                requestId,
                versionTag,
                logDataView(cluster, catalogEntry),
                limit));
    List<LogEvent> sanitized = events.stream().map(this::sanitizeLogEvent).toList();
    Instant freshest =
        sanitized.stream().map(LogEvent::observedAt).max(Instant::compareTo).orElse(Instant.EPOCH);
    return new LogSearchResult(
        cluster,
        namespace,
        serviceOrWorkload,
        "Kibana log search returned "
            + sanitized.size()
            + " relevant events for "
            + serviceOrWorkload
            + ".",
        "Filters covered service scope, time range, and optional severity/text/correlation keys.",
        sanitized.size(),
        sanitized.size() >= limit,
        sanitized,
        logSignatureAnalyzer.summarize(sanitized, start, 5),
        sanitized.stream()
            .map(LogEvent::deepLink)
            .filter(link -> link != null)
            .distinct()
            .limit(5)
            .toList(),
        end,
        freshness(end, freshest));
  }

  public TraceSearchResult searchJaegerTracesWindow(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Instant start,
      Instant end,
      String operation,
      boolean errorsOnly,
      String traceId,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyRange(Duration.between(start, end));
    int limit = properties.guardrails().maxTraces();
    scopePolicy.verifyTraceLimit(limit);
    ServiceCatalogEntry catalogEntry = resolveCatalogEntry(cluster, namespace, serviceOrWorkload);
    List<TraceSummary> traces =
        jaegerTracePort.searchTraces(
            new TraceSearchQuery(
                cluster,
                namespace,
                jaegerServiceName(serviceOrWorkload, catalogEntry),
                operation,
                start,
                end,
                errorsOnly,
                traceId,
                limit,
                Map.of()));
    traces = traces.stream().map(this::sanitizeTraceSummary).toList();
    Instant freshest =
        traces.stream().map(TraceSummary::startTime).max(Instant::compareTo).orElse(Instant.EPOCH);
    return new TraceSearchResult(
        cluster,
        namespace,
        serviceOrWorkload,
        "Jaeger search returned " + traces.size() + " traces for " + serviceOrWorkload + ".",
        "Trace search filtered by service scope, time window, and optional operation/error flags.",
        traces.size(),
        traces.size() >= limit,
        traces,
        traces.stream()
            .map(TraceSummary::deepLink)
            .filter(link -> link != null)
            .distinct()
            .limit(5)
            .toList(),
        end,
        freshness(end, freshest));
  }

  public TraceSummary getJaegerTraceSummary(String cluster, String traceId, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    return jaegerTracePort
        .getTrace(cluster, traceId)
        .map(this::sanitizeTraceSummary)
        .orElseThrow(
            () -> new NotFoundException("Trace id is not available in the configured scope."));
  }

  private ServiceCatalogEntry resolveCatalogEntry(
      String cluster, String namespace, String serviceOrWorkload) {
    return serviceCatalogPort.listServices().stream()
        .filter(entry -> entry.cluster().equals(cluster))
        .filter(entry -> entry.namespace().equals(namespace))
        .filter(
            entry ->
                entry.serviceId().equals(serviceOrWorkload)
                    || entry.workloadName().equals(serviceOrWorkload))
        .findFirst()
        .orElse(null);
  }

  private String logDataView(String cluster, ServiceCatalogEntry catalogEntry) {
    if (catalogEntry != null
        && catalogEntry.kibanaDataView() != null
        && !catalogEntry.kibanaDataView().isBlank()) {
      return catalogEntry.kibanaDataView();
    }
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(candidate -> candidate.kibana().defaultDataView())
        .orElse("");
  }

  private String jaegerServiceName(String serviceOrWorkload, ServiceCatalogEntry catalogEntry) {
    if (catalogEntry != null
        && catalogEntry.traceServiceName() != null
        && !catalogEntry.traceServiceName().isBlank()) {
      return catalogEntry.traceServiceName();
    }
    return serviceOrWorkload;
  }

  private LogEvent sanitizeLogEvent(LogEvent event) {
    String sanitizedMessage = redactionService.redact(event.message());
    String signature =
        event.exceptionSignature() == null || event.exceptionSignature().isBlank()
            ? deriveSignature(sanitizedMessage)
            : redactionService.redact(event.exceptionSignature());
    return new LogEvent(
        event.cluster(),
        event.namespace(),
        event.serviceOrWorkload(),
        event.podName(),
        event.container(),
        event.severity(),
        event.message(),
        sanitizedMessage,
        signature,
        event.traceId(),
        event.requestId(),
        event.versionTag(),
        event.observedAt(),
        event.deepLink());
  }

  private String deriveSignature(String sanitizedMessage) {
    int separator = sanitizedMessage.indexOf(':');
    return separator > 0 ? sanitizedMessage.substring(0, separator) : sanitizedMessage;
  }

  private DataFreshness freshness(Instant generatedAt, Instant freshest) {
    return new DataFreshness(
        generatedAt,
        freshest,
        freshest.equals(Instant.EPOCH) ? Duration.ZERO : Duration.between(freshest, generatedAt),
        false);
  }

  private TraceSummary sanitizeTraceSummary(TraceSummary trace) {
    return new TraceSummary(
        trace.traceId(),
        trace.rootService(),
        trace.operation(),
        trace.startTime(),
        trace.duration(),
        trace.error(),
        trace.firstFailingService(),
        trace.firstFailingSpan(),
        trace.criticalPathDuration(),
        trace.errorSpans().stream().map(this::sanitizeSpanSummary).toList(),
        trace.latencyHotspots().stream().map(this::sanitizeSpanSummary).toList(),
        trace.dependencyEdges(),
        trace.deepLink(),
        trace.versionTag(),
        trace.podName());
  }

  private com.prodops.controltower.mcp.domain.model.TraceSpanSummary sanitizeSpanSummary(
      com.prodops.controltower.mcp.domain.model.TraceSpanSummary span) {
    Map<String, String> sanitizedTags =
        span.tags().entrySet().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> redactionService.redact(String.valueOf(entry.getValue()))));
    return new com.prodops.controltower.mcp.domain.model.TraceSpanSummary(
        span.spanId(),
        span.parentSpanId(),
        span.serviceName(),
        span.operation(),
        span.startTime(),
        span.duration(),
        span.error(),
        span.retry(),
        span.podName(),
        span.versionTag(),
        sanitizedTags);
  }
}
