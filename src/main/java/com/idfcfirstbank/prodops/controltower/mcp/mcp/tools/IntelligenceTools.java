package com.idfcfirstbank.prodops.controltower.mcp.mcp.tools;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.BlastRadiusResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.CapacityForecastResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ChangeCorrelationResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.IncidentCorrelationResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.service.IntelligenceService;
import com.idfcfirstbank.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.idfcfirstbank.prodops.controltower.mcp.support.ArgumentMap;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class IntelligenceTools {

  private final IntelligenceService intelligenceService;
  private final McpInvocationSupport invocationSupport;

  public IntelligenceTools(
      IntelligenceService intelligenceService, McpInvocationSupport invocationSupport) {
    this.intelligenceService = intelligenceService;
    this.invocationSupport = invocationSupport;
  }

  @McpTool(
      name = "correlate_service_incident",
      description =
          "Flagship incident correlation across Kubernetes, Prometheus, and Grafana with executive and operator summaries.")
  public IncidentCorrelationResult correlateServiceIncident(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "correlate_service_incident",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.correlateServiceIncident(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "estimate_blast_radius",
      description =
          "Estimate blast radius across services, namespaces, and user-facing surfaces with explicit confidence.")
  public BlastRadiusResult estimateBlastRadius(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "estimate_blast_radius",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.estimateBlastRadius(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_change_correlation",
      description =
          "Correlate rollout timing and workload changes against metric inflection points with structured timeline.")
  public ChangeCorrelationResult getChangeCorrelation(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_change_correlation",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.getChangeCorrelation(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "forecast_capacity_risk",
      description =
          "Forecast near-term capacity risk using deterministic heuristics over Prometheus time series.")
  public CapacityForecastResult forecastCapacityRisk(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Optional namespace", required = false) String namespace,
      @McpToolParam(description = "Optional service ID or workload name", required = false)
          String serviceOrWorkload,
      @McpToolParam(description = "Resource name, e.g. cpu or memory") String resource,
      @McpToolParam(description = "Forecast horizon minutes") @Min(1) int horizonMinutes) {
    return invocationSupport.invoke(
        "tool",
        "forecast_capacity_risk",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "resource", resource,
            "horizonMinutes", horizonMinutes),
        () ->
            intelligenceService.forecastCapacityRisk(
                cluster,
                namespace,
                serviceOrWorkload,
                resource,
                Duration.ofMinutes(horizonMinutes),
                invocationSupport.identity()));
  }
}
