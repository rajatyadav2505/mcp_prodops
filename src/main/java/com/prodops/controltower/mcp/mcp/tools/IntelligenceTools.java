package com.prodops.controltower.mcp.mcp.tools;

import com.prodops.controltower.mcp.domain.model.BlastRadiusResult;
import com.prodops.controltower.mcp.domain.model.CapacityForecastResult;
import com.prodops.controltower.mcp.domain.model.ChangeAttributionResult;
import com.prodops.controltower.mcp.domain.model.ChangeCorrelationResult;
import com.prodops.controltower.mcp.domain.model.ChangeImpactComparison;
import com.prodops.controltower.mcp.domain.model.CoverageGapResult;
import com.prodops.controltower.mcp.domain.model.IncidentCorrelationResult;
import com.prodops.controltower.mcp.domain.model.RootCauseAnalysisResult;
import com.prodops.controltower.mcp.domain.model.SimilarIncidentResult;
import com.prodops.controltower.mcp.domain.service.IntelligenceService;
import com.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.prodops.controltower.mcp.support.ArgumentMap;
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

  @McpTool(
      name = "get_root_cause_analysis",
      description =
          "Determine what broke, which change is the leading suspect, supporting and weakening evidence, confidence, and alternates.")
  public RootCauseAnalysisResult getRootCauseAnalysis(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_root_cause_analysis",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.getRootCauseAnalysis(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_change_regression_attribution",
      description =
          "Rank recent Bitbucket changes against workload, Kibana, and Jaeger evidence to assess likely causality.")
  public ChangeAttributionResult getChangeRegressionAttribution(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_change_regression_attribution",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.getChangeRegressionAttribution(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "compare_change_impact",
      description =
          "Compare pre-change and post-change metrics, Kibana signatures, and Jaeger traces around a commit, PR, or inferred rollout window.")
  public ChangeImpactComparison compareChangeImpact(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes for each side of the comparison") @Min(1)
          int lookbackMinutes,
      @McpToolParam(description = "Optional commit SHA, PR id, or change id", required = false)
          String changeReference) {
    return invocationSupport.invoke(
        "tool",
        "compare_change_impact",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes,
            "changeReference", changeReference),
        () ->
            intelligenceService.compareChangeImpact(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                changeReference,
                invocationSupport.identity()));
  }

  @McpTool(
      name = "find_similar_incidents",
      description =
          "Match the current incident fingerprint against deterministic historical incidents and explain similarity reasons.")
  public SimilarIncidentResult findSimilarIncidents(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "find_similar_incidents",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.findSimilarIncidents(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_observability_coverage_gaps",
      description =
          "Identify missing version tags, trace propagation, repo mappings, deployment metadata, and other confidence-limiting gaps.")
  public CoverageGapResult getObservabilityCoverageGaps(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_observability_coverage_gaps",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.getObservabilityCoverageGaps(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }
}
