package com.prodops.controltower.mcp.mcp.tools;

import com.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.prodops.controltower.mcp.domain.model.LogSearchResult;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.prodops.controltower.mcp.domain.model.TraceSearchResult;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.service.ObservabilityService;
import com.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.prodops.controltower.mcp.support.ArgumentMap;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ObservabilityTools {

  private final ObservabilityService observabilityService;
  private final McpInvocationSupport invocationSupport;

  public ObservabilityTools(
      ObservabilityService observabilityService, McpInvocationSupport invocationSupport) {
    this.observabilityService = observabilityService;
    this.invocationSupport = invocationSupport;
  }

  @McpTool(
      name = "run_promql_instant",
      description = "Safely execute a raw instant PromQL query with guardrails.")
  public PromqlExecutionResult runPromqlInstant(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "PromQL query") String query,
      @McpToolParam(description = "Optional RFC3339 time", required = false) String time) {
    return invocationSupport.invoke(
        "tool",
        "run_promql_instant",
        ArgumentMap.of("cluster", cluster, "query", query, "time", time),
        () ->
            observabilityService.runPromqlInstant(
                cluster,
                query,
                time == null || time.isBlank() ? Instant.now() : Instant.parse(time),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "run_promql_range",
      description = "Safely execute a raw range PromQL query with bounded range and step.")
  public PromqlExecutionResult runPromqlRange(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "PromQL query") String query,
      @McpToolParam(description = "Range start RFC3339") String start,
      @McpToolParam(description = "Range end RFC3339") String end,
      @McpToolParam(description = "Step duration, e.g. PT5M") String step) {
    return invocationSupport.invoke(
        "tool",
        "run_promql_range",
        ArgumentMap.of(
            "cluster", cluster, "query", query, "start", start, "end", end, "step", step),
        () ->
            observabilityService.runPromqlRange(
                cluster,
                query,
                Instant.parse(start),
                Instant.parse(end),
                Duration.parse(step),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "search_dashboards",
      description = "Search Grafana dashboards by query, tags, and folder.")
  public List<DashboardInfo> searchDashboards(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Search query") String query,
      @McpToolParam(description = "Optional tags", required = false) List<String> tags,
      @McpToolParam(description = "Optional folder", required = false) String folder) {
    return invocationSupport.invoke(
        "tool",
        "search_dashboards",
        ArgumentMap.of("cluster", cluster, "query", query, "tags", tags, "folder", folder),
        () ->
            observabilityService.searchDashboards(
                cluster, query, tags, folder, invocationSupport.identity()));
  }

  @McpTool(
      name = "get_dashboard_summary",
      description = "Get Grafana dashboard metadata, panels, variables, and direct links.")
  public DashboardInfo getDashboardSummary(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Dashboard UID") String dashboardUid) {
    return invocationSupport.invoke(
        "tool",
        "get_dashboard_summary",
        ArgumentMap.of("cluster", cluster, "dashboardUid", dashboardUid),
        () ->
            observabilityService.getDashboardSummary(
                cluster, dashboardUid, invocationSupport.identity()));
  }

  @McpTool(
      name = "search_kibana_logs",
      description =
          "Search Kibana logs by time range, namespace, service or workload, severity, text, trace id, request id, and version tags.")
  public LogSearchResult searchKibanaLogs(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @jakarta.validation.constraints.Min(1)
          int lookbackMinutes,
      @McpToolParam(description = "Optional severity", required = false) String severity,
      @McpToolParam(description = "Optional search text", required = false) String text,
      @McpToolParam(description = "Optional trace id", required = false) String traceId,
      @McpToolParam(description = "Optional request id", required = false) String requestId,
      @McpToolParam(description = "Optional version tag", required = false) String versionTag) {
    return invocationSupport.invoke(
        "tool",
        "search_kibana_logs",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes,
            "severity",
            severity,
            "text",
            text,
            "traceId",
            traceId,
            "requestId",
            requestId,
            "versionTag",
            versionTag),
        () ->
            observabilityService.searchKibanaLogs(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                severity,
                text,
                traceId,
                requestId,
                versionTag,
                invocationSupport.identity()));
  }

  @McpTool(
      name = "summarize_kibana_errors",
      description =
          "Summarize dominant Kibana error signatures, spikes, and novel failures around symptom onset.")
  public LogSearchResult summarizeKibanaErrors(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @jakarta.validation.constraints.Min(1)
          int lookbackMinutes,
      @McpToolParam(description = "Optional search text", required = false) String text,
      @McpToolParam(description = "Optional trace id", required = false) String traceId,
      @McpToolParam(description = "Optional request id", required = false) String requestId,
      @McpToolParam(description = "Optional version tag", required = false) String versionTag) {
    return invocationSupport.invoke(
        "tool",
        "summarize_kibana_errors",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes,
            "text",
            text,
            "traceId",
            traceId,
            "requestId",
            requestId,
            "versionTag",
            versionTag),
        () ->
            observabilityService.summarizeKibanaErrors(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                text,
                traceId,
                requestId,
                versionTag,
                invocationSupport.identity()));
  }

  @McpTool(
      name = "search_jaeger_traces",
      description =
          "Search Jaeger traces by service, operation, time range, error status, and trace id.")
  public TraceSearchResult searchJaegerTraces(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @jakarta.validation.constraints.Min(1)
          int lookbackMinutes,
      @McpToolParam(description = "Optional operation name", required = false) String operation,
      @McpToolParam(description = "Errors only") boolean errorsOnly,
      @McpToolParam(description = "Optional trace id", required = false) String traceId) {
    return invocationSupport.invoke(
        "tool",
        "search_jaeger_traces",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes,
            "operation",
            operation,
            "errorsOnly",
            errorsOnly,
            "traceId",
            traceId),
        () ->
            observabilityService.searchJaegerTraces(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                operation,
                errorsOnly,
                traceId,
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_jaeger_trace_summary",
      description =
          "Get a Jaeger trace summary including first failing span, latency hotspots, dependency edges, and deep link.")
  public TraceSummary getJaegerTraceSummary(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Trace id") String traceId) {
    return invocationSupport.invoke(
        "tool",
        "get_jaeger_trace_summary",
        ArgumentMap.of("cluster", cluster, "traceId", traceId),
        () ->
            observabilityService.getJaegerTraceSummary(
                cluster, traceId, invocationSupport.identity()));
  }
}
