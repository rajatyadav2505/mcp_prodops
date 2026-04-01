package com.prodops.controltower.mcp.mcp.tools;

import com.prodops.controltower.mcp.domain.model.AlertCorrelationResult;
import com.prodops.controltower.mcp.domain.model.AlertNoiseAnalysisResult;
import com.prodops.controltower.mcp.domain.model.BlastRadiusResult;
import com.prodops.controltower.mcp.domain.model.CanaryHealthResult;
import com.prodops.controltower.mcp.domain.model.CapacityForecastResult;
import com.prodops.controltower.mcp.domain.model.CascadingFailureResult;
import com.prodops.controltower.mcp.domain.model.ChangeAttributionResult;
import com.prodops.controltower.mcp.domain.model.ChangeCorrelationResult;
import com.prodops.controltower.mcp.domain.model.ChangeImpactComparison;
import com.prodops.controltower.mcp.domain.model.ClusterComparisonResult;
import com.prodops.controltower.mcp.domain.model.CoverageGapResult;
import com.prodops.controltower.mcp.domain.model.CrossClusterDriftResult;
import com.prodops.controltower.mcp.domain.model.DailyRiskTrendResult;
import com.prodops.controltower.mcp.domain.model.IncidentCorrelationResult;
import com.prodops.controltower.mcp.domain.model.IncidentTimelineResult;
import com.prodops.controltower.mcp.domain.model.ResourceWasteResult;
import com.prodops.controltower.mcp.domain.model.RightSizingResult;
import com.prodops.controltower.mcp.domain.model.RolloutHistoryResult;
import com.prodops.controltower.mcp.domain.model.RootCauseAnalysisResult;
import com.prodops.controltower.mcp.domain.model.ServiceDependencyMap;
import com.prodops.controltower.mcp.domain.model.SimilarIncidentResult;
import com.prodops.controltower.mcp.domain.model.SloBreachForecastResult;
import com.prodops.controltower.mcp.domain.model.SloStatusResult;
import com.prodops.controltower.mcp.domain.model.ToilEstimationResult;
import com.prodops.controltower.mcp.domain.service.IntelligenceService;
import com.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.prodops.controltower.mcp.support.ArgumentMap;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.List;
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

  @McpTool(
      name = "check_slo_status",
      description =
          "Check real-time SLO burn status, remaining error budget, and current burn windows for a service.")
  public SloStatusResult checkSloStatus(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "check_slo_status",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.checkSloStatus(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "slo_breach_forecast",
      description = "Project when the leading SLO will breach if the current burn trend continues.")
  public SloBreachForecastResult sloBreachForecast(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "slo_breach_forecast",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.sloBreachForecast(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "map_service_dependencies",
      description =
          "Infer upstream and downstream service dependencies from catalog mappings, ingress routing, and Jaeger edges.")
  public ServiceDependencyMap mapServiceDependencies(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "map_service_dependencies",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.mapServiceDependencies(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "detect_cascading_failure",
      description =
          "Check whether a focal unhealthy service is propagating degradation into downstream dependencies.")
  public CascadingFailureResult detectCascadingFailure(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "detect_cascading_failure",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.detectCascadingFailure(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "identify_resource_waste",
      description =
          "Compare workload resource requests against observed usage and flag over-provisioned workloads.")
  public ResourceWasteResult identifyResourceWaste(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Optional service ID or workload name", required = false)
          String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "identify_resource_waste",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes),
        () ->
            intelligenceService.identifyResourceWaste(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "right_sizing_recommendations",
      description =
          "Suggest workload resource requests from observed P95 usage without mutating live resources.")
  public RightSizingResult rightSizingRecommendations(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Optional service ID or workload name", required = false)
          String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "right_sizing_recommendations",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes),
        () ->
            intelligenceService.rightSizingRecommendations(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "compare_pre_post_deploy",
      description =
          "Compare golden signals, Kibana signatures, and Jaeger traces before and after a deploy or change anchor.")
  public ChangeImpactComparison comparePrePostDeploy(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes for each side of the comparison") @Min(1)
          int lookbackMinutes,
      @McpToolParam(description = "Optional commit SHA, PR id, or change id", required = false)
          String changeReference) {
    return invocationSupport.invoke(
        "tool",
        "compare_pre_post_deploy",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes,
            "changeReference",
            changeReference),
        () ->
            intelligenceService.comparePrePostDeploy(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                changeReference,
                invocationSupport.identity()));
  }

  @McpTool(
      name = "rollout_history",
      description =
          "List recent rollout revisions with timing, image changes, and correlated metric-shift notes.")
  public RolloutHistoryResult rolloutHistory(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "rollout_history",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.rolloutHistory(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "canary_health_check",
      description =
          "Compare canary and stable cohorts on readiness, restarts, log errors, and trace failures.")
  public CanaryHealthResult canaryHealthCheck(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "canary_health_check",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.canaryHealthCheck(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "alert_noise_analysis",
      description =
          "Quantify alert fatigue by deduplicating warning events and classifying noisy versus actionable categories.")
  public AlertNoiseAnalysisResult alertNoiseAnalysis(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "alert_noise_analysis",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.alertNoiseAnalysis(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "alert_correlation_groups",
      description =
          "Group related warning alerts by time proximity and involved objects to reduce incident noise.")
  public AlertCorrelationResult alertCorrelationGroups(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "alert_correlation_groups",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.alertCorrelationGroups(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "compare_clusters",
      description =
          "Compare cluster health, risk, workload counts, and optional version signals across multiple clusters.")
  public ClusterComparisonResult compareClusters(
      @McpToolParam(description = "Cluster names") List<String> clusters,
      @McpToolParam(description = "Optional namespace", required = false) String namespace,
      @McpToolParam(description = "Optional service ID or workload name", required = false)
          String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "compare_clusters",
        ArgumentMap.of(
            "clusters",
            clusters,
            "namespace",
            namespace,
            "serviceOrWorkload",
            serviceOrWorkload,
            "lookbackMinutes",
            lookbackMinutes),
        () ->
            intelligenceService.compareClusters(
                clusters,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "cross_cluster_drift",
      description =
          "Compare a workload across two clusters and surface drift in images, revisions, replicas, resources, and selected security fields.")
  public CrossClusterDriftResult crossClusterDrift(
      @McpToolParam(description = "Primary cluster") String clusterA,
      @McpToolParam(description = "Comparison cluster") String clusterB,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload) {
    return invocationSupport.invoke(
        "tool",
        "cross_cluster_drift",
        ArgumentMap.of(
            "clusterA", clusterA,
            "clusterB", clusterB,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload),
        () ->
            intelligenceService.crossClusterDrift(
                clusterA, clusterB, namespace, serviceOrWorkload, invocationSupport.identity()));
  }

  @McpTool(
      name = "daily_risk_trend",
      description =
          "Replay recent metric and warning-event windows through the deterministic risk model to show improving, degrading, or stable trend.")
  public DailyRiskTrendResult dailyRiskTrend(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "daily_risk_trend",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.dailyRiskTrend(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "incident_timeline_export",
      description =
          "Export a chronological evidence timeline across rollouts, changes, warning events, metrics, logs, and traces.")
  public IncidentTimelineResult incidentTimelineExport(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Service ID or workload name") String serviceOrWorkload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "incident_timeline_export",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "serviceOrWorkload", serviceOrWorkload,
            "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.incidentTimelineExport(
                cluster,
                namespace,
                serviceOrWorkload,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "toil_estimation",
      description =
          "Estimate operational toil from warning-event volume, restart pressure, and scaling-related signals.")
  public ToilEstimationResult toilEstimation(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Optional namespace", required = false) String namespace,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "toil_estimation",
        ArgumentMap.of(
            "cluster", cluster, "namespace", namespace, "lookbackMinutes", lookbackMinutes),
        () ->
            intelligenceService.toilEstimation(
                cluster,
                namespace,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }
}
