package com.prodops.controltower.mcp.domain.service;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.correlation.AlertCorrelationEngine;
import com.prodops.controltower.mcp.domain.correlation.AlertNoiseAnalyzer;
import com.prodops.controltower.mcp.domain.correlation.BlastRadiusEngine;
import com.prodops.controltower.mcp.domain.correlation.CanaryHealthAnalyzer;
import com.prodops.controltower.mcp.domain.correlation.CapacityForecastEngine;
import com.prodops.controltower.mcp.domain.correlation.CausalEvidenceGraphBuilder;
import com.prodops.controltower.mcp.domain.correlation.ChangeAttributionEngine;
import com.prodops.controltower.mcp.domain.correlation.ChangeCorrelationEngine;
import com.prodops.controltower.mcp.domain.correlation.ChangeImpactComparator;
import com.prodops.controltower.mcp.domain.correlation.ClusterComparisonEngine;
import com.prodops.controltower.mcp.domain.correlation.CoverageGapAnalyzer;
import com.prodops.controltower.mcp.domain.correlation.DependencyTopologyEngine;
import com.prodops.controltower.mcp.domain.correlation.IncidentFingerprintEngine;
import com.prodops.controltower.mcp.domain.correlation.IncidentTimelineBuilder;
import com.prodops.controltower.mcp.domain.correlation.ResourceWasteAnalyzer;
import com.prodops.controltower.mcp.domain.correlation.RiskTrendEngine;
import com.prodops.controltower.mcp.domain.correlation.RolloutHistoryEngine;
import com.prodops.controltower.mcp.domain.correlation.RootCauseAnalysisEngine;
import com.prodops.controltower.mcp.domain.correlation.SloStatusEngine;
import com.prodops.controltower.mcp.domain.correlation.ToilEstimator;
import com.prodops.controltower.mcp.domain.model.AlertCorrelationResult;
import com.prodops.controltower.mcp.domain.model.AlertNoiseAnalysisResult;
import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery;
import com.prodops.controltower.mcp.domain.model.BlastRadiusImpact;
import com.prodops.controltower.mcp.domain.model.BlastRadiusResult;
import com.prodops.controltower.mcp.domain.model.CanaryHealthResult;
import com.prodops.controltower.mcp.domain.model.CapacityForecastResult;
import com.prodops.controltower.mcp.domain.model.CascadingFailureResult;
import com.prodops.controltower.mcp.domain.model.CausalEvidenceGraph;
import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.ChangeAttributionResult;
import com.prodops.controltower.mcp.domain.model.ChangeCausality;
import com.prodops.controltower.mcp.domain.model.ChangeCorrelationResult;
import com.prodops.controltower.mcp.domain.model.ChangeImpactComparison;
import com.prodops.controltower.mcp.domain.model.ChangeTimelineEntry;
import com.prodops.controltower.mcp.domain.model.ClusterComparisonResult;
import com.prodops.controltower.mcp.domain.model.CoverageGapResult;
import com.prodops.controltower.mcp.domain.model.CrossClusterDriftResult;
import com.prodops.controltower.mcp.domain.model.DailyRiskTrendResult;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.EvidenceItem;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.EvidenceType;
import com.prodops.controltower.mcp.domain.model.HealthVerdict;
import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.model.HpaInfo;
import com.prodops.controltower.mcp.domain.model.Hypothesis;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.IncidentCorrelationResult;
import com.prodops.controltower.mcp.domain.model.IncidentTimelineResult;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.LogSearchResult;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.ObjectReference;
import com.prodops.controltower.mcp.domain.model.ObservabilityCoverageGap;
import com.prodops.controltower.mcp.domain.model.ResourceWasteResult;
import com.prodops.controltower.mcp.domain.model.RightSizingResult;
import com.prodops.controltower.mcp.domain.model.RiskLevel;
import com.prodops.controltower.mcp.domain.model.RolloutHistoryResult;
import com.prodops.controltower.mcp.domain.model.RolloutRevision;
import com.prodops.controltower.mcp.domain.model.RootCauseAnalysisResult;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import com.prodops.controltower.mcp.domain.model.RootCauseDossier;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.ServiceDependencyMap;
import com.prodops.controltower.mcp.domain.model.SimilarIncidentMatch;
import com.prodops.controltower.mcp.domain.model.SimilarIncidentResult;
import com.prodops.controltower.mcp.domain.model.SloBreachForecastResult;
import com.prodops.controltower.mcp.domain.model.SloStatusResult;
import com.prodops.controltower.mcp.domain.model.ToilEstimationResult;
import com.prodops.controltower.mcp.domain.model.TraceSearchResult;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.port.BitbucketPort;
import com.prodops.controltower.mcp.domain.port.ClusterInventoryPort;
import com.prodops.controltower.mcp.domain.port.IncidentHistoryPort;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import com.prodops.controltower.mcp.domain.port.RiskWeightsPort;
import com.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import com.prodops.controltower.mcp.policy.ScopePolicy;
import com.prodops.controltower.mcp.support.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IntelligenceService {

  private final InventoryService inventoryService;
  private final ObservabilityService observabilityService;
  private final ClusterInventoryPort clusterInventoryPort;
  private final MetricsPort metricsPort;
  private final ServiceCatalogPort serviceCatalogPort;
  private final RiskWeightsPort riskWeightsPort;
  private final BitbucketPort bitbucketPort;
  private final IncidentHistoryPort incidentHistoryPort;
  private final BlastRadiusEngine blastRadiusEngine;
  private final ChangeCorrelationEngine changeCorrelationEngine;
  private final CapacityForecastEngine capacityForecastEngine;
  private final ChangeAttributionEngine changeAttributionEngine;
  private final RootCauseAnalysisEngine rootCauseAnalysisEngine;
  private final CausalEvidenceGraphBuilder causalEvidenceGraphBuilder;
  private final ChangeImpactComparator changeImpactComparator;
  private final IncidentFingerprintEngine incidentFingerprintEngine;
  private final CoverageGapAnalyzer coverageGapAnalyzer;
  private final SloStatusEngine sloStatusEngine;
  private final DependencyTopologyEngine dependencyTopologyEngine;
  private final RolloutHistoryEngine rolloutHistoryEngine;
  private final AlertNoiseAnalyzer alertNoiseAnalyzer;
  private final AlertCorrelationEngine alertCorrelationEngine;
  private final ResourceWasteAnalyzer resourceWasteAnalyzer;
  private final CanaryHealthAnalyzer canaryHealthAnalyzer;
  private final ClusterComparisonEngine clusterComparisonEngine;
  private final RiskTrendEngine riskTrendEngine;
  private final IncidentTimelineBuilder incidentTimelineBuilder;
  private final ToilEstimator toilEstimator;
  private final ScopePolicy scopePolicy;
  private final ProdOpsProperties properties;
  private final Clock clock;

  public IntelligenceService(
      InventoryService inventoryService,
      ObservabilityService observabilityService,
      ClusterInventoryPort clusterInventoryPort,
      MetricsPort metricsPort,
      ServiceCatalogPort serviceCatalogPort,
      RiskWeightsPort riskWeightsPort,
      BitbucketPort bitbucketPort,
      IncidentHistoryPort incidentHistoryPort,
      BlastRadiusEngine blastRadiusEngine,
      ChangeCorrelationEngine changeCorrelationEngine,
      CapacityForecastEngine capacityForecastEngine,
      ChangeAttributionEngine changeAttributionEngine,
      RootCauseAnalysisEngine rootCauseAnalysisEngine,
      CausalEvidenceGraphBuilder causalEvidenceGraphBuilder,
      ChangeImpactComparator changeImpactComparator,
      IncidentFingerprintEngine incidentFingerprintEngine,
      CoverageGapAnalyzer coverageGapAnalyzer,
      SloStatusEngine sloStatusEngine,
      DependencyTopologyEngine dependencyTopologyEngine,
      RolloutHistoryEngine rolloutHistoryEngine,
      AlertNoiseAnalyzer alertNoiseAnalyzer,
      AlertCorrelationEngine alertCorrelationEngine,
      ResourceWasteAnalyzer resourceWasteAnalyzer,
      CanaryHealthAnalyzer canaryHealthAnalyzer,
      ClusterComparisonEngine clusterComparisonEngine,
      RiskTrendEngine riskTrendEngine,
      IncidentTimelineBuilder incidentTimelineBuilder,
      ToilEstimator toilEstimator,
      ScopePolicy scopePolicy,
      ProdOpsProperties properties,
      Clock clock) {
    this.inventoryService = inventoryService;
    this.observabilityService = observabilityService;
    this.clusterInventoryPort = clusterInventoryPort;
    this.metricsPort = metricsPort;
    this.serviceCatalogPort = serviceCatalogPort;
    this.riskWeightsPort = riskWeightsPort;
    this.bitbucketPort = bitbucketPort;
    this.incidentHistoryPort = incidentHistoryPort;
    this.blastRadiusEngine = blastRadiusEngine;
    this.changeCorrelationEngine = changeCorrelationEngine;
    this.capacityForecastEngine = capacityForecastEngine;
    this.changeAttributionEngine = changeAttributionEngine;
    this.rootCauseAnalysisEngine = rootCauseAnalysisEngine;
    this.causalEvidenceGraphBuilder = causalEvidenceGraphBuilder;
    this.changeImpactComparator = changeImpactComparator;
    this.incidentFingerprintEngine = incidentFingerprintEngine;
    this.coverageGapAnalyzer = coverageGapAnalyzer;
    this.sloStatusEngine = sloStatusEngine;
    this.dependencyTopologyEngine = dependencyTopologyEngine;
    this.rolloutHistoryEngine = rolloutHistoryEngine;
    this.alertNoiseAnalyzer = alertNoiseAnalyzer;
    this.alertCorrelationEngine = alertCorrelationEngine;
    this.resourceWasteAnalyzer = resourceWasteAnalyzer;
    this.canaryHealthAnalyzer = canaryHealthAnalyzer;
    this.clusterComparisonEngine = clusterComparisonEngine;
    this.riskTrendEngine = riskTrendEngine;
    this.incidentTimelineBuilder = incidentTimelineBuilder;
    this.toilEstimator = toilEstimator;
    this.scopePolicy = scopePolicy;
    this.properties = properties;
    this.clock = clock;
  }

  public IncidentCorrelationResult correlateServiceIncident(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    WorkloadInfo workload = resolveWorkload(cluster, namespace, serviceOrWorkload);
    WorkloadHealth workloadHealth =
        inventoryService.getWorkloadHealth(
            cluster, namespace, workload.name(), workload.kind(), lookback, identity);
    List<WarningEvent> warningEvents =
        inventoryService.getRecentWarningEvents(
            cluster, namespace, workload.name(), lookback, identity);
    Optional<HpaInfo> hpa = clusterInventoryPort.getHpa(cluster, namespace, workload.name());
    Hypothesis primary = primaryHypothesis(workloadHealth, hpa.orElse(null));
    List<Hypothesis> alternatives = alternativeHypotheses(workloadHealth, hpa.orElse(null));
    List<EvidenceItem> evidence = evidenceFrom(workloadHealth, warningEvents);
    List<EvidenceItem> counterevidence = counterEvidenceFrom(workloadHealth);
    List<DeepLink> deepLinks = dashboardLinks(workloadHealth.linkedDashboards());
    List<String> limitations =
        limitationsFor(
            workloadHealth, serviceCatalogPort.findByWorkload(cluster, namespace, workload.name()));
    double confidence =
        Math.min(0.93d, 0.45d + (evidence.size() * 0.05d) - (limitations.size() * 0.03d));

    String operatorSummary =
        primary.statement()
            + " Evidence includes "
            + evidence.size()
            + " cross-plane signals spanning Kubernetes, Prometheus, and Grafana.";
    String executiveSummary =
        "Operational risk for "
            + workload.name()
            + " in "
            + namespace
            + " is "
            + workloadHealth.riskLevel()
            + " with "
            + Math.round(confidence * 100)
            + "% confidence; the dominant signal is "
            + primary.statement().toLowerCase()
            + ".";

    return new IncidentCorrelationResult(
        cluster,
        namespace,
        serviceOrWorkload,
        executiveSummary,
        operatorSummary,
        workloadHealth.riskScore(),
        workloadHealth.riskLevel(),
        confidence,
        primary,
        alternatives,
        counterevidence,
        evidence,
        deepLinks,
        List.of(
            "Check whether the most recent deployment revision matches the observed inflection point.",
            "Compare request path latency against dependency saturation indicators.",
            "Confirm whether customer-facing ingress traffic is concentrated on the unhealthy pods."),
        limitations,
        Instant.now(clock),
        workloadHealth.dataFreshness());
  }

  public BlastRadiusResult estimateBlastRadius(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    WorkloadInfo workload = resolveWorkload(cluster, namespace, serviceOrWorkload);
    WorkloadHealth workloadHealth =
        inventoryService.getWorkloadHealth(
            cluster, namespace, workload.name(), workload.kind(), lookback, identity);
    List<BlastRadiusImpact> impacts =
        blastRadiusEngine.estimate(
            namespace,
            workload.name(),
            clusterInventoryPort.listServices(cluster, namespace),
            clusterInventoryPort.listIngresses(cluster, namespace),
            clusterInventoryPort
                .getHpa(cluster, namespace, workload.name())
                .map(HpaInfo::scaleConstrained)
                .orElse(false));
    List<EvidenceItem> evidence =
        evidenceFrom(
            workloadHealth,
            inventoryService.getRecentWarningEvents(
                cluster, namespace, workload.name(), lookback, identity));
    List<EvidenceItem> counterEvidence =
        workloadHealth.verdict() == HealthVerdict.HEALTHY
            ? List.of(
                new EvidenceItem(
                    "counter-healthy",
                    EvidenceSource.KUBERNETES,
                    EvidenceType.TOPOLOGY,
                    new ObjectReference(
                        cluster, namespace, workload.kind().name(), workload.name()),
                    "Workload remains healthy",
                    "Blast-radius certainty is reduced because the workload is not currently in a hard unhealthy state.",
                    null,
                    null,
                    null,
                    Instant.now(clock),
                    null,
                    null,
                    0.58d))
            : List.of();
    double confidence =
        Math.min(
            0.88d,
            0.4d
                + impacts.stream()
                    .mapToDouble(BlastRadiusImpact::confidence)
                    .average()
                    .orElse(0.3d));
    return new BlastRadiusResult(
        cluster,
        namespace,
        serviceOrWorkload,
        "The likely blast radius is concentrated on "
            + impacts.size()
            + " operational surfaces, led by "
            + impacts.getFirst().surface()
            + ".",
        "Blast radius was inferred from Services, Ingress backends, and scaling constraints without overclaiming certainty.",
        workloadHealth.riskScore(),
        workloadHealth.riskLevel(),
        confidence,
        impacts,
        evidence,
        counterEvidence,
        dashboardLinks(workloadHealth.linkedDashboards()),
        List.of(
            "Dependency topology is inferred from Kubernetes selectors and ingress references only."),
        Instant.now(clock),
        workloadHealth.dataFreshness());
  }

  public ChangeCorrelationResult getChangeCorrelation(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    List<ChangeTimelineEntry> timelineEntries = new ArrayList<>();
    Instant rolloutTime =
        Optional.ofNullable(context.workload().updatedAt()).orElse(context.workload().createdAt());
    timelineEntries.add(
        new ChangeTimelineEntry(
            rolloutTime,
            "rollout",
            "Most recent workload update or rollout marker.",
            "kubernetes"));
    context
        .warningEvents()
        .forEach(
            event ->
                timelineEntries.add(
                    new ChangeTimelineEntry(
                        event.lastTimestamp(),
                        "warning-event",
                        event.reason() + ": " + event.message(),
                        "kubernetes")));
    context.bitbucketChanges().stream()
        .limit(3)
        .forEach(
            change ->
                timelineEntries.add(
                    new ChangeTimelineEntry(
                        change.mergedAt() == null ? change.committedAt() : change.mergedAt(),
                        "bitbucket-change",
                        change.title(),
                        "bitbucket")));
    LogSearchResult logSearch =
        observabilityService.summarizeKibanaErrors(
            cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity);
    logSearch.topSignatures().stream()
        .limit(3)
        .forEach(
            signature ->
                timelineEntries.add(
                    new ChangeTimelineEntry(
                        signature.firstSeen(), "log-signature", signature.signature(), "kibana")));
    TraceSearchResult traceSearch =
        observabilityService.searchJaegerTraces(
            cluster, namespace, serviceOrWorkload, lookback, null, true, null, identity);
    traceSearch.traces().stream()
        .limit(3)
        .forEach(
            trace ->
                timelineEntries.add(
                    new ChangeTimelineEntry(
                        trace.startTime(),
                        "trace-failure",
                        trace.traceId() + " " + trace.firstFailingService(),
                        "jaeger")));
    List<ChangeTimelineEntry> timeline =
        timelineEntries.stream()
            .sorted(Comparator.comparing(ChangeTimelineEntry::timestamp))
            .toList();

    ChangeCorrelationEngine.CorrelationAssessment assessment =
        changeCorrelationEngine.assess(
            rolloutTime, context.workloadHealth().coreMetrics(), timeline);
    ChangeAttributionEngine.ChangeAssessment changeAssessment =
        changeAttributionEngine.assess(
            context,
            assessment.timeline().stream()
                .map(ChangeTimelineEntry::timestamp)
                .findFirst()
                .orElse(rolloutTime),
            logSearch.topSignatures(),
            riskWeightsPort.getWeights());

    ChangeCausality causality =
        mapCausality(
            changeAssessment.candidates().stream()
                .findFirst()
                .map(RootCauseCandidate::causationClass)
                .orElse(null),
            assessment.causality());
    double confidence = Math.max(assessment.confidence(), changeAssessment.confidence());

    return new ChangeCorrelationResult(
        cluster,
        namespace,
        serviceOrWorkload,
        "Latest change is assessed as "
            + causality.name().toLowerCase().replace('_', ' ')
            + " with "
            + Math.round(confidence * 100)
            + "% confidence.",
        "Rollout timing was compared against Bitbucket changes, Kibana signatures, Jaeger traces, and degraded golden signals.",
        causality,
        confidence,
        context.workloadHealth().riskScore(),
        context.workloadHealth().riskLevel(),
        timeline,
        extendedEvidenceFrom(context, logSearch.topSignatures()),
        counterEvidenceFrom(context.workloadHealth()),
        limitationsForContext(context),
        Instant.now(clock),
        context.dataFreshness());
  }

  public CapacityForecastResult forecastCapacityRisk(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      String resource,
      Duration horizon,
      String identity) {
    scopePolicy.assertAllowed(
        namespace == null
            ? scopePolicy.authorizeCluster(cluster, identity)
            : scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(horizon);

    String scope =
        serviceOrWorkload == null || serviceOrWorkload.isBlank() ? namespace : serviceOrWorkload;
    String query = "forecast_" + resource + "_" + scope;
    List<MetricSeries> series =
        metricsPort
            .rangeQuery(
                cluster,
                query,
                Instant.now(clock).minus(Duration.ofHours(1)),
                Instant.now(clock),
                Duration.ofMinutes(5))
            .series();
    CapacityForecastEngine.Forecast forecast = capacityForecastEngine.forecast(series, horizon);
    double ratio = Math.max(0.0d, Math.min(1.0d, forecast.forecastValue()));
    RiskLevel riskLevel =
        ratio >= 0.85d
            ? RiskLevel.CRITICAL
            : ratio >= 0.7d ? RiskLevel.HIGH : ratio >= 0.5d ? RiskLevel.MODERATE : RiskLevel.LOW;
    List<EvidenceItem> evidence =
        List.of(
            new EvidenceItem(
                "forecast-" + resource,
                EvidenceSource.PROMETHEUS,
                EvidenceType.METRIC,
                new ObjectReference(cluster, namespace, "Scope", scope),
                "Capacity forecast",
                forecast.rationale(),
                resource,
                query,
                null,
                Instant.now(clock),
                forecast.forecastValue(),
                null,
                forecast.confidence()));
    return new CapacityForecastResult(
        cluster,
        scope,
        resource,
        horizon,
        "Forecasted "
            + resource
            + " pressure for "
            + scope
            + " is "
            + riskLevel
            + " over the next "
            + horizon.toMinutes()
            + " minutes.",
        "Forecast uses deterministic slope-based heuristics, preferring PromQL-shaped series where available.",
        Math.round(ratio * 100.0d),
        riskLevel,
        forecast.confidence(),
        forecast.currentValue(),
        forecast.forecastValue(),
        "1.0 normalized saturation ratio",
        evidence,
        List.of(),
        series.isEmpty()
            ? List.of("No range series was available for the requested scope.")
            : List.of(),
        Instant.now(clock),
        new DataFreshness(Instant.now(clock), Instant.now(clock), Duration.ZERO, false));
  }

  public SloStatusResult checkSloStatus(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    Instant end = Instant.now(clock);
    List<MetricSeries> series =
        recentMetricSeries(
            cluster, namespace, context.workload().name(), context.catalogEntry(), end, lookback);
    SloStatusEngine.Analysis analysis =
        sloStatusEngine.analyze(
            context.catalogEntry() == null ? List.of() : context.catalogEntry().sloTargets(),
            context.workloadHealth().coreMetrics(),
            seriesByMetric(series));
    SloStatusResult.SloBudgetStatus leading = analysis.leadingRisk();
    return new SloStatusResult(
        cluster,
        namespace,
        serviceOrWorkload,
        leading == null
            ? "No curated SLO targets were available for the selected service."
            : "Closest SLO risk is "
                + leading.name()
                + " with "
                + Math.round(leading.remainingBudgetPercent())
                + "% budget remaining.",
        leading == null
            ? "SLO status requires curated service-catalog targets."
            : "SLO status used current golden signals and recent Prometheus trends to estimate burn and remaining budget.",
        analysis.budgets(),
        leading,
        analysis.confidenceBreakdown(),
        directLinks(
            context,
            observabilityService.summarizeKibanaErrors(
                cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity),
            null),
        combineUnknowns(analysis.limitations(), List.of()),
        Instant.now(clock),
        context.dataFreshness());
  }

  public SloBreachForecastResult sloBreachForecast(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    SloStatusResult status =
        checkSloStatus(cluster, namespace, serviceOrWorkload, lookback, identity);
    SloStatusEngine.Forecast forecast =
        sloStatusEngine.forecast(
            status.leadingRisk(), Instant.now(clock), status.confidenceBreakdown());
    List<EvidenceItem> evidence =
        status.leadingRisk() == null
            ? List.of()
            : List.of(
                new EvidenceItem(
                    "slo-forecast-" + status.leadingRisk().name(),
                    EvidenceSource.PROMETHEUS,
                    EvidenceType.METRIC,
                    new ObjectReference(cluster, namespace, "Service", serviceOrWorkload),
                    status.leadingRisk().name(),
                    "Forecasted using current SLO ratio and recent trend slope.",
                    status.leadingRisk().metricName(),
                    null,
                    null,
                    Instant.now(clock),
                    status.leadingRisk().currentRatio(),
                    null,
                    status.confidenceBreakdown().overallConfidence()));
    return new SloBreachForecastResult(
        cluster,
        namespace,
        serviceOrWorkload,
        status.leadingRisk() == null
            ? "No SLO forecast could be produced because no curated SLO target matched the scope."
            : forecast.timeToBreach() == null
                ? "At the current trend, no near-term SLO breach is projected."
                : "At the current burn rate, "
                    + serviceOrWorkload
                    + " will breach "
                    + status.leadingRisk().name()
                    + " in "
                    + forecast.timeToBreach().toMinutes()
                    + " minutes.",
        status.leadingRisk() == null
            ? "SLO forecasting requires a matching curated SLO and recent metric trend."
            : "Forecast projected the leading SLO breach point from the recent ratio slope without overstating certainty.",
        status.leadingRisk() == null ? null : status.leadingRisk().name(),
        status.leadingRisk() == null ? 0.0d : status.leadingRisk().currentRatio(),
        status.leadingRisk() == null ? 0.0d : status.leadingRisk().remainingBudgetPercent(),
        forecast.timeToBreach(),
        forecast.projectedBreachAt(),
        forecast.confidenceBreakdown(),
        evidence,
        status.limitations(),
        Instant.now(clock),
        status.dataFreshness());
  }

  public ServiceDependencyMap mapServiceDependencies(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    Map<String, WorkloadHealth> healthByService =
        relatedHealthByService(cluster, namespace, context, lookback, identity);
    DependencyTopologyEngine.Topology topology =
        dependencyTopologyEngine.build(
            cluster,
            namespace,
            serviceOrWorkload,
            context.catalogEntry(),
            serviceCatalogPort.listServices().stream()
                .filter(entry -> entry.cluster().equals(cluster))
                .toList(),
            clusterInventoryPort.listServices(cluster, namespace),
            clusterInventoryPort.listIngresses(cluster, namespace),
            context.traceSummaries(),
            healthByService,
            properties.guardrails().maxTopologyNodes());
    return new ServiceDependencyMap(
        cluster,
        namespace,
        serviceOrWorkload,
        "Dependency map identified "
            + topology.nodes().size()
            + " related services and surfaces around "
            + serviceOrWorkload
            + ".",
        "Topology combined curated dependencies, ingress routes, and Jaeger dependency edges.",
        topology.nodes(),
        topology.edges(),
        topology.confidence(),
        context.traceSummaries().stream()
            .map(TraceSummary::deepLink)
            .filter(link -> link != null)
            .distinct()
            .toList(),
        topology.limitations(),
        Instant.now(clock),
        context.dataFreshness());
  }

  public CascadingFailureResult detectCascadingFailure(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    Map<String, WorkloadHealth> healthByService =
        relatedHealthByService(cluster, namespace, context, lookback, identity);
    DependencyTopologyEngine.Topology topology =
        dependencyTopologyEngine.build(
            cluster,
            namespace,
            serviceOrWorkload,
            context.catalogEntry(),
            serviceCatalogPort.listServices().stream()
                .filter(entry -> entry.cluster().equals(cluster))
                .toList(),
            clusterInventoryPort.listServices(cluster, namespace),
            clusterInventoryPort.listIngresses(cluster, namespace),
            context.traceSummaries(),
            healthByService,
            properties.guardrails().maxTopologyNodes());
    String targetId =
        context.catalogEntry() == null ? serviceOrWorkload : context.catalogEntry().serviceId();
    DependencyTopologyEngine.CascadeAssessment assessment =
        dependencyTopologyEngine.detect(
            targetId, context.workloadHealth(), topology, healthByService);
    return new CascadingFailureResult(
        cluster,
        namespace,
        serviceOrWorkload,
        assessment.cascading()
            ? "The focal service appears to be cascading failure into downstream dependents."
            : "No strong cascading-failure pattern was detected for downstream dependents.",
        "Cascade detection looked for unhealthy downstream services that depend on the focal workload and degraded after it.",
        assessment.cascading(),
        assessment.impacts(),
        new com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown(
            topology.confidence(),
            0.42d,
            0.09d,
            topology.limitations().size() * 0.04d,
            List.of(
                new com.prodops.controltower.mcp.domain.model.ConfidenceFactor(
                    "topology map",
                    0.2d,
                    "Dependencies were mapped deterministically before cascade scoring."),
                new com.prodops.controltower.mcp.domain.model.ConfidenceFactor(
                    "downstream health",
                    assessment.impacts().isEmpty() ? 0.0d : 0.18d,
                    "Dependent workload health influenced the final cascade judgment.")),
            "Cascade confidence is bounded when downstream services lack workload mappings."),
        topology.limitations(),
        Instant.now(clock),
        context.dataFreshness());
  }

  public ResourceWasteResult identifyResourceWaste(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    List<WorkloadInfo> workloads = scopedWorkloads(cluster, namespace, serviceOrWorkload);
    Map<String, ResourceWasteAnalyzer.UsageSnapshot> usageSnapshots =
        usageSnapshots(cluster, namespace, workloads, lookback);
    List<ResourceWasteResult.ResourceWasteFinding> findings =
        resourceWasteAnalyzer.findWaste(
            workloads, usageSnapshots, properties.guardrails().maxChangeCandidates());
    return new ResourceWasteResult(
        cluster,
        namespace,
        serviceOrWorkload,
        findings.isEmpty()
            ? "No strongly over-provisioned workloads were detected in the selected scope."
            : "Top over-provisioned workload is " + findings.getFirst().workloadName() + ".",
        "Waste analysis compared workload resource requests against observed CPU and memory usage over the selected window.",
        findings,
        resourceConfidence(workloads, usageSnapshots),
        workloads.stream().anyMatch(workload -> !workload.hasResourceRequests())
            ? List.of("Some workloads did not declare resource requests and could not be scored.")
            : List.of(),
        Instant.now(clock),
        freshnessFromWorkloads(workloads));
  }

  public RightSizingResult rightSizingRecommendations(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    List<WorkloadInfo> workloads = scopedWorkloads(cluster, namespace, serviceOrWorkload);
    Map<String, ResourceWasteAnalyzer.UsageSnapshot> usageSnapshots =
        usageSnapshots(cluster, namespace, workloads, lookback);
    List<RightSizingResult.RightSizingRecommendation> recommendations =
        resourceWasteAnalyzer.rightSize(
            workloads, usageSnapshots, properties.guardrails().maxChangeCandidates());
    return new RightSizingResult(
        cluster,
        namespace,
        serviceOrWorkload,
        recommendations.isEmpty()
            ? "No right-sizing recommendations were produced for the selected scope."
            : "Right-sizing recommendations were produced for "
                + recommendations.size()
                + " workloads.",
        "Recommendations target approximately 130% of observed P95 usage while preserving deterministic guardrails.",
        recommendations,
        resourceConfidence(workloads, usageSnapshots),
        workloads.stream().anyMatch(workload -> !workload.hasResourceRequests())
            ? List.of(
                "Some workloads did not declare resource requests and could not be recommended.")
            : List.of(),
        Instant.now(clock),
        freshnessFromWorkloads(workloads));
  }

  public ChangeImpactComparison comparePrePostDeploy(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String changeReference,
      String identity) {
    return compareChangeImpact(
        cluster, namespace, serviceOrWorkload, lookback, changeReference, identity);
  }

  public RolloutHistoryResult rolloutHistory(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    List<RolloutRevision> revisions =
        clusterInventoryPort.listRolloutRevisions(
            cluster, namespace, context.workload().name(), context.workload().kind());
    List<MetricSeries> series =
        recentMetricSeries(
            cluster,
            namespace,
            context.workload().name(),
            context.catalogEntry(),
            Instant.now(clock),
            lookback);
    List<String> notes = rolloutHistoryEngine.correlate(revisions, series);
    return new RolloutHistoryResult(
        cluster,
        namespace,
        serviceOrWorkload,
        revisions.isEmpty()
            ? "No rollout revisions were available for the selected workload."
            : "Found " + revisions.size() + " rollout revisions for " + serviceOrWorkload + ".",
        "Rollout history combined Kubernetes revision lineage with nearby metric shifts and recent Bitbucket changes.",
        revisions,
        notes,
        context.bitbucketChanges().stream()
            .flatMap(change -> change.deepLinks().stream())
            .distinct()
            .toList(),
        Instant.now(clock),
        context.dataFreshness());
  }

  public CanaryHealthResult canaryHealthCheck(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    CanaryHealthAnalyzer.Analysis analysis =
        canaryHealthAnalyzer.analyze(
            context.workloadHealth(), context.logEvents(), context.traceSummaries());
    return new CanaryHealthResult(
        cluster,
        namespace,
        serviceOrWorkload,
        !analysis.canaryDetected()
            ? "No canary cohort could be detected for the selected workload."
            : analysis.canary().healthScore() >= analysis.stable().healthScore()
                ? "Canary is performing at least as well as the stable cohort."
                : "Canary is underperforming relative to the stable cohort.",
        "Canary health compared pod readiness, restarts, error logs, and trace failures between canary and stable cohorts.",
        analysis.canaryDetected(),
        analysis.canary(),
        analysis.stable(),
        analysis.confidenceBreakdown(),
        analysis.canaryDetected()
            ? List.of()
            : List.of("No canary-specific pod labels or naming markers were found."),
        Instant.now(clock),
        context.dataFreshness());
  }

  public AlertNoiseAnalysisResult alertNoiseAnalysis(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    AlertNoiseAnalyzer.Analysis analysis =
        alertNoiseAnalyzer.analyze(context.warningEvents(), context.workloadHealth());
    return new AlertNoiseAnalysisResult(
        cluster,
        namespace,
        serviceOrWorkload,
        analysis.noisyAlertCount() == 0
            ? "Current warning alerts are mostly actionable rather than noisy."
            : analysis.noisyAlertCount() + " alert categories appear noisy in the selected window.",
        "Alert noise analysis deduplicated warning events by reason and object, then checked workload corroboration.",
        context.warningEvents().stream().mapToInt(WarningEvent::count).sum(),
        analysis.noisyAlertCount(),
        analysis.actionableAlertCount(),
        analysis.reasons(),
        analysis.confidenceBreakdown(),
        context.warningEvents().isEmpty()
            ? List.of("No warning events were present in the selected window.")
            : List.of(),
        Instant.now(clock),
        context.dataFreshness());
  }

  public AlertCorrelationResult alertCorrelationGroups(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    List<AlertCorrelationResult.AlertCorrelationGroup> groups =
        alertCorrelationEngine.group(
            context.warningEvents(), properties.guardrails().maxAlertGroups());
    return new AlertCorrelationResult(
        cluster,
        namespace,
        serviceOrWorkload,
        groups.isEmpty()
            ? "No warning alerts were available to group."
            : context.warningEvents().size()
                + " warning alerts collapsed into "
                + groups.size()
                + " correlation groups.",
        "Alert groups were formed by time proximity and shared involved objects or reasons.",
        context.warningEvents().stream().mapToInt(WarningEvent::count).sum(),
        groups.size(),
        groups,
        Instant.now(clock),
        context.dataFreshness());
  }

  public ClusterComparisonResult compareClusters(
      List<String> clusters,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    scopePolicy.verifyClusterComparisonLimit(clusters.size());
    List<ClusterComparisonResult.ClusterHealthComparison> comparisons = new ArrayList<>();
    for (String cluster : clusters) {
      scopePolicy.assertAllowed(
          namespace == null || namespace.isBlank()
              ? scopePolicy.authorizeCluster(cluster, identity)
              : scopePolicy.authorizeNamespace(cluster, namespace, identity));
      if (namespace != null
          && !namespace.isBlank()
          && serviceOrWorkload != null
          && !serviceOrWorkload.isBlank()) {
        WorkloadInfo workload = resolveWorkload(cluster, namespace, serviceOrWorkload);
        WorkloadHealth health =
            inventoryService.getWorkloadHealth(
                cluster, namespace, workload.name(), workload.kind(), lookback, identity);
        comparisons.add(
            new ClusterComparisonResult.ClusterHealthComparison(
                cluster,
                namespace + "/" + serviceOrWorkload,
                health.verdict(),
                health.riskLevel(),
                health.riskScore(),
                1,
                health.verdict() == HealthVerdict.HEALTHY ? 0 : 1,
                versionTag(workload, resolveCatalogEntry(cluster, namespace, serviceOrWorkload))));
      } else if (namespace != null && !namespace.isBlank()) {
        var namespaceHealth =
            inventoryService.getNamespaceHealth(cluster, namespace, lookback, identity);
        comparisons.add(
            new ClusterComparisonResult.ClusterHealthComparison(
                cluster,
                namespace,
                namespaceHealth.verdict(),
                namespaceHealth.riskLevel(),
                namespaceHealth.riskScore(),
                namespaceHealth.workloadCount(),
                namespaceHealth.unhealthyWorkloadCount(),
                null));
      } else {
        List<com.prodops.controltower.mcp.domain.model.NamespaceInfo> namespaces =
            inventoryService.listNamespaces(cluster, null, false, identity);
        int workloadCount = 0;
        int unhealthy = 0;
        double riskScore = 0.0d;
        HealthVerdict verdict = HealthVerdict.HEALTHY;
        RiskLevel riskLevel = RiskLevel.LOW;
        for (com.prodops.controltower.mcp.domain.model.NamespaceInfo item : namespaces) {
          var health =
              inventoryService.getNamespaceHealth(cluster, item.name(), lookback, identity);
          workloadCount += health.workloadCount();
          unhealthy += health.unhealthyWorkloadCount();
          riskScore += health.riskScore();
          if (riskSeverity(health.riskLevel()) > riskSeverity(riskLevel)) {
            riskLevel = health.riskLevel();
          }
          if (health.verdict() == HealthVerdict.UNHEALTHY) {
            verdict = HealthVerdict.UNHEALTHY;
          } else if (verdict == HealthVerdict.HEALTHY
              && health.verdict() == HealthVerdict.DEGRADED) {
            verdict = HealthVerdict.DEGRADED;
          }
        }
        comparisons.add(
            new ClusterComparisonResult.ClusterHealthComparison(
                cluster,
                "cluster",
                verdict,
                riskLevel,
                namespaces.isEmpty() ? 0.0d : riskScore / namespaces.size(),
                workloadCount,
                unhealthy,
                null));
      }
    }
    ClusterComparisonEngine.Comparison comparison = clusterComparisonEngine.compare(comparisons);
    return new ClusterComparisonResult(
        comparisons.isEmpty()
            ? "No cluster comparison could be produced."
            : "Compared "
                + comparisons.size()
                + " clusters for "
                + (namespace == null || namespace.isBlank() ? "cluster-wide health" : namespace)
                + ".",
        "Cluster comparison aligned health, risk, and version signals across the requested scopes.",
        comparisons,
        comparison.differences(),
        comparison.confidenceBreakdown(),
        Instant.now(clock),
        new DataFreshness(Instant.now(clock), Instant.now(clock), Duration.ZERO, false));
  }

  public CrossClusterDriftResult crossClusterDrift(
      String clusterA,
      String clusterB,
      String namespace,
      String serviceOrWorkload,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(clusterA, namespace, identity));
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(clusterB, namespace, identity));
    WorkloadInfo left = resolveWorkload(clusterA, namespace, serviceOrWorkload);
    WorkloadInfo right = resolveWorkload(clusterB, namespace, serviceOrWorkload);
    List<CrossClusterDriftResult.DriftItem> driftItems = clusterComparisonEngine.drift(left, right);
    return new CrossClusterDriftResult(
        clusterA,
        clusterB,
        namespace,
        serviceOrWorkload,
        driftItems.isEmpty()
            ? "No material workload-spec drift was detected between the selected clusters."
            : "Detected "
                + driftItems.size()
                + " workload drift items across the selected clusters.",
        "Drift detection compared image, revision, replicas, resource requests, and selected security fields.",
        driftItems,
        List.of(),
        Instant.now(clock),
        new DataFreshness(
            Instant.now(clock),
            freshest(left.updatedAt(), right.updatedAt()),
            Duration.ZERO,
            false));
  }

  public DailyRiskTrendResult dailyRiskTrend(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    List<MetricSeries> series =
        recentMetricSeries(
            cluster,
            namespace,
            context.workload().name(),
            context.catalogEntry(),
            Instant.now(clock),
            lookback);
    RiskTrendEngine.Analysis analysis =
        riskTrendEngine.analyze(
            series,
            context.workloadHealth().totalRestarts(),
            context.warningEvents().stream().mapToInt(WarningEvent::count).sum(),
            context.workloadHealth().rolloutAge().toMinutes(),
            replicaGap(context.workload().desiredReplicas(), context.workload().readyReplicas()),
            context.catalogEntry() == null ? 0.45d : 0.15d,
            context.logEvents().isEmpty() ? 0.3d : 0.15d,
            riskWeightsPort.getWeights());
    return new DailyRiskTrendResult(
        cluster,
        namespace,
        serviceOrWorkload,
        "Risk trend for " + serviceOrWorkload + " is " + analysis.trend() + ".",
        "Risk trend replayed recent metric windows and event density through the deterministic risk model.",
        analysis.trend(),
        analysis.points(),
        new com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown(
            analysis.points().size() > 2 ? 0.76d : 0.48d,
            0.42d,
            0.08d,
            analysis.points().size() > 2 ? 0.08d : 0.22d,
            List.of(
                new com.prodops.controltower.mcp.domain.model.ConfidenceFactor(
                    "metric replay",
                    series.isEmpty() ? 0.0d : 0.22d,
                    "Risk points were reconstructed from Prometheus series."),
                new com.prodops.controltower.mcp.domain.model.ConfidenceFactor(
                    "event density",
                    context.warningEvents().isEmpty() ? 0.0d : 0.14d,
                    "Warning-event density contributed to each point.")),
            "Trend confidence is strongest when multiple Prometheus points were available."),
        series.isEmpty()
            ? List.of("No recent metric series were available for trend replay.")
            : List.of(),
        Instant.now(clock),
        context.dataFreshness());
  }

  public IncidentTimelineResult incidentTimelineExport(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    LogSearchResult logSearch =
        observabilityService.summarizeKibanaErrors(
            cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity);
    List<RolloutRevision> revisions =
        clusterInventoryPort.listRolloutRevisions(
            cluster, namespace, context.workload().name(), context.workload().kind());
    List<MetricSeries> series =
        recentMetricSeries(
            cluster,
            namespace,
            context.workload().name(),
            context.catalogEntry(),
            Instant.now(clock),
            lookback);
    List<IncidentTimelineResult.IncidentTimelineEntry> entries =
        incidentTimelineBuilder.build(
            revisions,
            context.warningEvents(),
            context.bitbucketChanges(),
            series,
            logSearch.topSignatures(),
            context.traceSummaries(),
            properties.guardrails().maxTimelineEntries());
    return new IncidentTimelineResult(
        cluster,
        namespace,
        serviceOrWorkload,
        entries.isEmpty()
            ? "No timeline entries were available for the selected incident window."
            : "Constructed an incident timeline with " + entries.size() + " evidence entries.",
        "Timeline export merged rollouts, changes, warnings, metrics, logs, and traces in chronological order.",
        entries,
        directLinks(context, logSearch, null),
        entries.isEmpty()
            ? List.of("No evidence sources were populated in the selected window.")
            : List.of(),
        Instant.now(clock),
        context.dataFreshness());
  }

  public ToilEstimationResult toilEstimation(
      String cluster, String namespace, Duration lookback, String identity) {
    scopePolicy.assertAllowed(
        namespace == null || namespace.isBlank()
            ? scopePolicy.authorizeCluster(cluster, identity)
            : scopePolicy.authorizeNamespace(cluster, namespace, identity));
    List<com.prodops.controltower.mcp.domain.model.NamespaceInfo> namespaces =
        namespace == null || namespace.isBlank()
            ? inventoryService.listNamespaces(cluster, null, false, identity)
            : List.of(
                new com.prodops.controltower.mcp.domain.model.NamespaceInfo(
                    cluster, namespace, Map.of(), "unknown", "standard", false));
    List<WorkloadHealth> workloadHealths = new ArrayList<>();
    List<WarningEvent> warningEvents = new ArrayList<>();
    for (com.prodops.controltower.mcp.domain.model.NamespaceInfo item : namespaces) {
      for (WorkloadInfo workload :
          clusterInventoryPort.listWorkloads(cluster, item.name(), null, null)) {
        workloadHealths.add(
            inventoryService.getWorkloadHealth(
                cluster, item.name(), workload.name(), workload.kind(), lookback, identity));
      }
      warningEvents.addAll(
          inventoryService.getRecentWarningEvents(cluster, item.name(), null, lookback, identity));
    }
    List<ToilEstimationResult.ToilSummary> summaries =
        toilEstimator.estimate(workloadHealths, warningEvents);
    return new ToilEstimationResult(
        cluster,
        namespace,
        summaries.isEmpty()
            ? "No operational toil signals were detected for the selected scope."
            : "Top toil burden is currently " + summaries.getFirst().scope() + ".",
        "Toil estimation combined warning-event volume, restart pressure, and scaling-related signals.",
        summaries,
        Instant.now(clock),
        new DataFreshness(Instant.now(clock), Instant.now(clock), Duration.ZERO, false));
  }

  public RootCauseAnalysisResult getRootCauseAnalysis(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    LogSearchResult logSearch =
        observabilityService.summarizeKibanaErrors(
            cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity);
    Instant symptomOnset =
        rootCauseAnalysisEngine.inferSymptomOnset(context, logSearch.topSignatures());
    ChangeAttributionEngine.ChangeAssessment changeAssessment =
        changeAttributionEngine.assess(
            context, symptomOnset, logSearch.topSignatures(), riskWeightsPort.getWeights());
    List<ObservabilityCoverageGap> coverageGaps =
        coverageGapAnalyzer.analyze(context, symptomOnset);
    RootCauseAnalysisEngine.Analysis analysis =
        rootCauseAnalysisEngine.assess(
            context,
            logSearch.topSignatures(),
            changeAssessment,
            coverageGaps,
            riskWeightsPort.getWeights());
    CausalEvidenceGraph evidenceGraph =
        causalEvidenceGraphBuilder.build(
            context,
            analysis.symptomOnset(),
            logSearch.topSignatures(),
            combineCandidates(analysis.primarySuspect(), analysis.alternateSuspects()),
            properties.guardrails().maxEvidenceNodes());
    List<DeepLink> directLinks = directLinks(context, logSearch, null);
    RootCauseDossier dossier =
        new RootCauseDossier(
            analysis.primarySuspect() == null
                ? "Evidence is too sparse to establish a primary suspect."
                : analysis.primarySuspect().whyLeadingSuspect(),
            analysis.primarySuspect() == null
                ? "Confidence is low because cross-plane evidence is incomplete."
                : "Primary suspect is "
                    + analysis.primarySuspect().entity()
                    + " with "
                    + Math.round(analysis.confidence() * 100)
                    + "% confidence.",
            analysis.symptomOnset(),
            analysis.primarySuspect(),
            analysis.alternateSuspects(),
            analysis.primarySuspect() == null ? null : analysis.primarySuspect().offendingChange(),
            analysis.primarySuspect() == null
                ? List.of()
                : analysis.primarySuspect().impactedDependencies(),
            causalChainSummary(analysis.primarySuspect()),
            analysis.confidenceBreakdown(),
            context.dataFreshness(),
            limitationsForContext(context),
            directLinks);
    return new RootCauseAnalysisResult(
        cluster,
        namespace,
        serviceOrWorkload,
        dossier.executiveSummary(),
        dossier.operatorSummary(),
        analysis.symptomOnset(),
        analysis.primarySuspect(),
        analysis.alternateSuspects(),
        analysis.primarySuspect() == null ? null : analysis.primarySuspect().offendingChange(),
        analysis.primarySuspect() == null
            ? List.of()
            : analysis.primarySuspect().impactedDependencies(),
        evidenceGraph,
        analysis.confidenceBreakdown(),
        analysis.confidence(),
        analysis.supportingEvidence(),
        analysis.weakeningEvidence(),
        analysis.unknowns(),
        coverageGaps,
        dossier,
        directLinks,
        limitationsForContext(context),
        Instant.now(clock),
        context.dataFreshness());
  }

  public ChangeAttributionResult getChangeRegressionAttribution(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    LogSearchResult logSearch =
        observabilityService.summarizeKibanaErrors(
            cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity);
    Instant symptomOnset =
        rootCauseAnalysisEngine.inferSymptomOnset(context, logSearch.topSignatures());
    ChangeAttributionEngine.ChangeAssessment assessment =
        changeAttributionEngine.assess(
            context, symptomOnset, logSearch.topSignatures(), riskWeightsPort.getWeights());
    RootCauseCandidate leading = assessment.candidates().stream().findFirst().orElse(null);
    BitbucketChange primaryChange = leading == null ? null : leading.offendingChange();
    List<BitbucketChange> alternates =
        assessment.candidates().stream()
            .skip(1)
            .map(RootCauseCandidate::offendingChange)
            .filter(change -> change != null)
            .limit(3)
            .toList();
    List<ObservabilityCoverageGap> coverageGaps =
        coverageGapAnalyzer.analyze(context, symptomOnset);
    return new ChangeAttributionResult(
        cluster,
        namespace,
        serviceOrWorkload,
        primaryChange == null
            ? "No recent Bitbucket change could be promoted beyond insufficient evidence."
            : "Leading Bitbucket suspect is "
                + primaryChange.title()
                + " with "
                + Math.round(assessment.confidence() * 100)
                + "% confidence.",
        primaryChange == null
            ? "Bitbucket data was available, but the evidence does not support a strong causal claim."
            : leading.whyLeadingSuspect(),
        primaryChange,
        alternates,
        leading == null ? CausationClass.INSUFFICIENT_EVIDENCE : leading.causationClass(),
        leading == null
            ? "No candidate achieved enough cross-plane support."
            : leading.whyLeadingSuspect(),
        extendedEvidenceFrom(context, logSearch.topSignatures()),
        counterEvidenceFrom(context.workloadHealth()),
        assessment.confidenceBreakdown(),
        assessment.confidence(),
        combineUnknowns(assessment.unknowns(), coverageGaps),
        directLinks(context, logSearch, null),
        Instant.now(clock),
        context.dataFreshness());
  }

  public ChangeImpactComparison compareChangeImpact(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String changeReference,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    BitbucketChange anchorChange =
        context.bitbucketChanges().stream()
            .filter(
                change ->
                    changeReference == null
                        || changeReference.isBlank()
                        || change.commitSha().startsWith(changeReference)
                        || change.changeId().equals(changeReference)
                        || (change.pullRequest() != null
                            && change.pullRequest().pullRequestId().equals(changeReference)))
            .findFirst()
            .orElse(context.bitbucketChanges().stream().findFirst().orElse(null));
    Instant anchor =
        anchorChange == null
            ? Optional.ofNullable(context.workload().updatedAt())
                .orElse(context.workload().createdAt())
            : anchorChange.mergedAt() == null
                ? anchorChange.committedAt()
                : anchorChange.mergedAt();
    List<MetricSeries> metricSeries =
        comparisonMetricSeries(
            cluster,
            namespace,
            context.workload().name(),
            context.catalogEntry(),
            anchor,
            lookback);
    LogSearchResult beforeLogs =
        observabilityService.searchKibanaLogsWindow(
            cluster,
            namespace,
            serviceOrWorkload,
            anchor.minus(lookback),
            anchor,
            "ERROR",
            null,
            null,
            null,
            null,
            identity);
    LogSearchResult afterLogs =
        observabilityService.searchKibanaLogsWindow(
            cluster,
            namespace,
            serviceOrWorkload,
            anchor,
            anchor.plus(lookback),
            "ERROR",
            null,
            null,
            null,
            null,
            identity);
    TraceSearchResult beforeTraces =
        observabilityService.searchJaegerTracesWindow(
            cluster,
            namespace,
            serviceOrWorkload,
            anchor.minus(lookback),
            anchor,
            null,
            true,
            null,
            identity);
    TraceSearchResult afterTraces =
        observabilityService.searchJaegerTracesWindow(
            cluster,
            namespace,
            serviceOrWorkload,
            anchor,
            anchor.plus(lookback),
            null,
            true,
            null,
            identity);
    List<EvidenceItem> evidence = extendedEvidenceFrom(context, afterLogs.topSignatures());
    return changeImpactComparator.compare(
        cluster,
        namespace,
        serviceOrWorkload,
        anchorChange == null ? "inferred-rollout-window" : anchorChange.commitSha(),
        anchor,
        lookback,
        metricSeries,
        beforeLogs.topSignatures(),
        afterLogs.topSignatures(),
        beforeTraces.traces(),
        afterTraces.traces(),
        evidence,
        context.dataFreshness(),
        Instant.now(clock));
  }

  public SimilarIncidentResult findSimilarIncidents(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    LogSearchResult logSearch =
        observabilityService.summarizeKibanaErrors(
            cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity);
    Instant symptomOnset =
        rootCauseAnalysisEngine.inferSymptomOnset(context, logSearch.topSignatures());
    ChangeAttributionEngine.ChangeAssessment changeAssessment =
        changeAttributionEngine.assess(
            context, symptomOnset, logSearch.topSignatures(), riskWeightsPort.getWeights());
    List<ObservabilityCoverageGap> coverageGaps =
        coverageGapAnalyzer.analyze(context, symptomOnset);
    RootCauseAnalysisEngine.Analysis analysis =
        rootCauseAnalysisEngine.assess(
            context,
            logSearch.topSignatures(),
            changeAssessment,
            coverageGaps,
            riskWeightsPort.getWeights());
    List<SimilarIncidentMatch> matches =
        incidentFingerprintEngine.match(
            incidentFingerprintEngine.fingerprint(
                context, logSearch.topSignatures(), analysis.primarySuspect()),
            context.historicalIncidents(),
            properties.guardrails().maxSimilarIncidents());
    return new SimilarIncidentResult(
        cluster,
        namespace,
        serviceOrWorkload,
        matches.isEmpty()
            ? "No sufficiently similar incidents were found in the deterministic history set."
            : "Top similar incident is "
                + matches.getFirst().title()
                + " with "
                + Math.round(matches.getFirst().similarity() * 100)
                + "% similarity.",
        "Incident fingerprinting compared onset metrics, top log signatures, trace failures, workload state, and dependency path.",
        matches,
        Instant.now(clock),
        context.dataFreshness());
  }

  public CoverageGapResult getObservabilityCoverageGaps(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    IncidentContext context =
        buildIncidentContext(cluster, namespace, serviceOrWorkload, lookback, identity);
    LogSearchResult logSearch =
        observabilityService.summarizeKibanaErrors(
            cluster, namespace, serviceOrWorkload, lookback, null, null, null, null, identity);
    List<ObservabilityCoverageGap> gaps =
        coverageGapAnalyzer.analyze(
            context, rootCauseAnalysisEngine.inferSymptomOnset(context, logSearch.topSignatures()));
    return new CoverageGapResult(
        cluster,
        namespace,
        serviceOrWorkload,
        gaps.isEmpty()
            ? "No material observability coverage gaps were detected for this analysis window."
            : "Detected "
                + gaps.size()
                + " coverage gaps reducing attribution confidence, led by "
                + gaps.getFirst().type().name()
                + ".",
        gaps.isEmpty()
            ? "Version tags, traces, repo mappings, and log fields are sufficient for deterministic RCA."
            : "Coverage gaps highlight why confidence is bounded and which read-only evidence dimensions need improvement.",
        gaps,
        Instant.now(clock),
        context.dataFreshness());
  }

  private IncidentContext buildIncidentContext(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    WorkloadInfo workload = resolveWorkload(cluster, namespace, serviceOrWorkload);
    WorkloadHealth workloadHealth =
        inventoryService.getWorkloadHealth(
            cluster, namespace, workload.name(), workload.kind(), lookback, identity);
    List<WarningEvent> warningEvents =
        inventoryService.getRecentWarningEvents(
            cluster, namespace, workload.name(), lookback, identity);
    ServiceCatalogEntry catalogEntry = resolveCatalogEntry(cluster, namespace, serviceOrWorkload);
    LogSearchResult logSearch =
        observabilityService.searchKibanaLogs(
            cluster,
            namespace,
            serviceOrWorkload,
            lookback,
            null,
            null,
            null,
            null,
            null,
            identity);
    TraceSearchResult traceSearch =
        observabilityService.searchJaegerTraces(
            cluster, namespace, serviceOrWorkload, lookback, null, true, null, identity);
    Instant now = Instant.now(clock);
    List<BitbucketChange> changes =
        bitbucketPort.listChanges(
            new BitbucketChangeQuery(
                cluster,
                catalogEntry == null ? workload.name() : catalogEntry.serviceId(),
                bitbucketWorkspace(cluster, catalogEntry),
                bitbucketRepoSlug(cluster, catalogEntry),
                null,
                now.minus(lookback),
                now,
                properties.guardrails().maxChangeCandidates()));
    List<HistoricalIncident> historicalIncidents =
        incidentHistoryPort.listHistoricalIncidents(
            cluster, namespace, serviceOrWorkload, properties.guardrails().maxSimilarIncidents());
    return new IncidentContext(
        cluster,
        namespace,
        serviceOrWorkload,
        workload,
        workloadHealth,
        warningEvents,
        catalogEntry,
        changes,
        logSearch.events(),
        traceSearch.traces(),
        historicalIncidents,
        now,
        combinedFreshness(workloadHealth, logSearch, traceSearch, changes));
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

  private WorkloadInfo resolveWorkload(String cluster, String namespace, String serviceOrWorkload) {
    return serviceCatalogPort.listServices().stream()
        .filter(entry -> entry.cluster().equals(cluster))
        .filter(entry -> entry.namespace().equals(namespace))
        .filter(
            entry ->
                entry.serviceId().equals(serviceOrWorkload)
                    || entry.workloadName().equals(serviceOrWorkload))
        .findFirst()
        .flatMap(
            entry ->
                clusterInventoryPort.getWorkload(
                    cluster, namespace, entry.workloadName(), entry.workloadKind()))
        .or(
            () ->
                clusterInventoryPort.listWorkloads(cluster, namespace, null, null).stream()
                    .filter(workload -> workload.name().equals(serviceOrWorkload))
                    .findFirst())
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No workload or catalog mapping found for service or workload."));
  }

  private DataFreshness combinedFreshness(
      WorkloadHealth workloadHealth,
      LogSearchResult logSearch,
      TraceSearchResult traceSearch,
      List<BitbucketChange> changes) {
    Instant generatedAt = Instant.now(clock);
    Instant freshest =
        List.of(
                workloadHealth.dataFreshness().sourceObservedAt(),
                logSearch.dataFreshness().sourceObservedAt(),
                traceSearch.dataFreshness().sourceObservedAt(),
                changes.stream()
                    .map(
                        change ->
                            change.mergedAt() == null ? change.committedAt() : change.mergedAt())
                    .max(Instant::compareTo)
                    .orElse(Instant.EPOCH))
            .stream()
            .max(Instant::compareTo)
            .orElse(Instant.EPOCH);
    return new DataFreshness(
        generatedAt,
        freshest,
        freshest.equals(Instant.EPOCH) ? Duration.ZERO : Duration.between(freshest, generatedAt),
        false);
  }

  private List<EvidenceItem> extendedEvidenceFrom(
      IncidentContext context, List<LogErrorSignature> signatures) {
    List<EvidenceItem> evidence =
        new ArrayList<>(evidenceFrom(context.workloadHealth(), context.warningEvents()));
    signatures.stream()
        .limit(3)
        .forEach(
            signature ->
                evidence.add(
                    new EvidenceItem(
                        "kibana-" + signature.signature(),
                        EvidenceSource.KIBANA,
                        EvidenceType.LOG_SIGNATURE,
                        new ObjectReference(
                            context.cluster(),
                            context.namespace(),
                            "Service",
                            context.serviceOrWorkload()),
                        signature.signature(),
                        signature.example(),
                        null,
                        null,
                        null,
                        signature.lastSeen(),
                        (double) signature.count(),
                        signature.deepLink(),
                        signature.confidence())));
    context.traceSummaries().stream()
        .limit(3)
        .forEach(
            trace ->
                evidence.add(
                    new EvidenceItem(
                        "jaeger-" + trace.traceId(),
                        EvidenceSource.JAEGER,
                        EvidenceType.TRACE,
                        new ObjectReference(
                            context.cluster(), context.namespace(), "Trace", trace.traceId()),
                        trace.traceId(),
                        trace.firstFailingService() == null
                            ? trace.operation()
                            : trace.firstFailingService() + "/" + trace.firstFailingSpan(),
                        null,
                        null,
                        null,
                        trace.startTime(),
                        (double) trace.duration().toMillis(),
                        trace.deepLink(),
                        trace.error() ? 0.85d : 0.62d)));
    context.bitbucketChanges().stream()
        .limit(3)
        .forEach(
            change ->
                evidence.add(
                    new EvidenceItem(
                        "bitbucket-" + change.changeId(),
                        EvidenceSource.BITBUCKET,
                        EvidenceType.CHANGE,
                        new ObjectReference(
                            context.cluster(),
                            context.namespace(),
                            "Repository",
                            change.repoSlug()),
                        change.title(),
                        change.commitSha(),
                        null,
                        null,
                        null,
                        change.mergedAt() == null ? change.committedAt() : change.mergedAt(),
                        null,
                        change.deepLinks().stream().findFirst().orElse(null),
                        0.78d)));
    return evidence;
  }

  private List<EvidenceItem> evidenceFrom(
      WorkloadHealth workloadHealth, List<WarningEvent> warningEvents) {
    List<EvidenceItem> evidence = new ArrayList<>();
    warningEvents.stream()
        .limit(3)
        .forEach(
            event ->
                evidence.add(
                    new EvidenceItem(
                        "event-" + event.reason(),
                        EvidenceSource.KUBERNETES,
                        EvidenceType.EVENT,
                        new ObjectReference(
                            event.cluster(),
                            event.namespace(),
                            event.involvedKind(),
                            event.involvedName()),
                        event.reason(),
                        event.message(),
                        null,
                        null,
                        null,
                        event.lastTimestamp(),
                        (double) event.count(),
                        null,
                        0.76d)));
    workloadHealth
        .coreMetrics()
        .forEach(
            metric ->
                evidence.add(
                    new EvidenceItem(
                        "metric-" + metric.name(),
                        EvidenceSource.PROMETHEUS,
                        EvidenceType.METRIC,
                        new ObjectReference(
                            workloadHealth.cluster(),
                            workloadHealth.namespace(),
                            workloadHealth.workloadKind().name(),
                            workloadHealth.workloadName()),
                        metric.name(),
                        metric.explanation(),
                        metric.name(),
                        metric.query(),
                        null,
                        metric.observedAt(),
                        metric.value(),
                        null,
                        0.82d)));
    workloadHealth.linkedDashboards().stream()
        .limit(2)
        .forEach(
            dashboard ->
                evidence.add(
                    new EvidenceItem(
                        "dashboard-" + dashboard.uid(),
                        EvidenceSource.GRAFANA,
                        EvidenceType.DASHBOARD,
                        new ObjectReference(
                            workloadHealth.cluster(),
                            workloadHealth.namespace(),
                            "Dashboard",
                            dashboard.uid()),
                        dashboard.title(),
                        "Relevant Grafana dashboard for workload evidence review.",
                        null,
                        null,
                        dashboard.url(),
                        Instant.now(clock),
                        null,
                        new DeepLink(
                            dashboard.title(),
                            EvidenceSource.GRAFANA,
                            dashboard.url(),
                            "Grafana dashboard"),
                        0.66d)));
    return evidence;
  }

  private List<EvidenceItem> counterEvidenceFrom(WorkloadHealth workloadHealth) {
    List<EvidenceItem> counter = new ArrayList<>();
    if (metric("memory_pressure_ratio", workloadHealth.coreMetrics()) < 0.4d) {
      counter.add(
          new EvidenceItem(
              "counter-memory-stable",
              EvidenceSource.PROMETHEUS,
              EvidenceType.METRIC,
              new ObjectReference(
                  workloadHealth.cluster(),
                  workloadHealth.namespace(),
                  workloadHealth.workloadKind().name(),
                  workloadHealth.workloadName()),
              "Memory pressure remains below risk threshold",
              "Memory pressure does not currently support a memory exhaustion hypothesis.",
              "memory_pressure_ratio",
              null,
              null,
              Instant.now(clock),
              metric("memory_pressure_ratio", workloadHealth.coreMetrics()),
              null,
              0.64d));
    }
    return counter;
  }

  private Hypothesis primaryHypothesis(WorkloadHealth workloadHealth, HpaInfo hpa) {
    double errorRate = metric("error_rate_ratio", workloadHealth.coreMetrics());
    double latency = metric("latency_slo_ratio", workloadHealth.coreMetrics());
    double cpu = metric("cpu_saturation_ratio", workloadHealth.coreMetrics());
    if (workloadHealth.rolloutAge().toMinutes() <= 60 && errorRate >= 0.6d) {
      return new Hypothesis(
          "Recent rollout regression is the primary incident driver.",
          0.84d,
          "Error-rate degradation aligned with a fresh rollout window and unhealthy workload state.",
          List.of("metric-error-rate", "event-rollout"),
          List.of("counter-stable-cpu"));
    }
    if (latency >= 0.7d && cpu >= 0.7d && hpa != null && hpa.scaleConstrained()) {
      return new Hypothesis(
          "Capacity saturation is the dominant driver.",
          0.81d,
          "Latency and CPU saturation increased while scaling remained constrained.",
          List.of("metric-latency", "metric-cpu", "hpa-constrained"),
          List.of("counter-no-restarts"));
    }
    return new Hypothesis(
        "Warning-event concentration is the strongest available explanation.",
        0.61d,
        "Cross-plane signals are partial, so the conclusion is intentionally conservative.",
        List.of("event-warning-volume"),
        List.of());
  }

  private List<Hypothesis> alternativeHypotheses(WorkloadHealth workloadHealth, HpaInfo hpa) {
    return List.of(
        new Hypothesis(
            "A dependent service may be introducing latency or error amplification.",
            0.48d,
            "Dependency certainty is limited without full service graph instrumentation.",
            List.of("metric-latency"),
            List.of("counter-direct-restarts")),
        new Hypothesis(
            "Operational alert noise may be inflating perceived urgency.",
            workloadHealth.warningEvents().size() > 5 ? 0.44d : 0.21d,
            "High event volume with mixed customer impact is a known support-system pattern.",
            List.of("event-warning-volume"),
            List.of("metric-error-rate")));
  }

  private List<DeepLink> dashboardLinks(
      List<com.prodops.controltower.mcp.domain.model.DashboardInfo> dashboards) {
    return dashboards.stream()
        .map(
            dashboard ->
                new DeepLink(
                    dashboard.title(),
                    EvidenceSource.GRAFANA,
                    dashboard.url(),
                    "Grafana dashboard"))
        .toList();
  }

  private List<String> limitationsFor(
      WorkloadHealth workloadHealth, Optional<ServiceCatalogEntry> catalogEntry) {
    List<String> limitations = new ArrayList<>();
    if (catalogEntry.isEmpty()) {
      limitations.add(
          "Curated service catalog mapping was unavailable, so dashboard and SLO discovery was heuristic.");
    }
    if (workloadHealth.coreMetrics().isEmpty()) {
      limitations.add("Prometheus golden signals were missing for the selected workload.");
    }
    if (workloadHealth.linkedDashboards().isEmpty()) {
      limitations.add("No Grafana dashboard evidence was found for the selected workload.");
    }
    return limitations;
  }

  private List<String> limitationsForContext(IncidentContext context) {
    List<String> limitations =
        new ArrayList<>(
            limitationsFor(context.workloadHealth(), Optional.ofNullable(context.catalogEntry())));
    if (context.bitbucketChanges().isEmpty()) {
      limitations.add("No Bitbucket changes were found inside the configured incident window.");
    }
    if (context.logEvents().isEmpty()) {
      limitations.add("No Kibana log events were found in the configured scope.");
    }
    if (context.traceSummaries().isEmpty()) {
      limitations.add("No Jaeger traces were found for the configured service scope.");
    }
    return limitations;
  }

  private List<DeepLink> directLinks(
      IncidentContext context,
      LogSearchResult logSearch,
      ChangeAttributionResult changeAttributionResult) {
    List<DeepLink> links = new ArrayList<>();
    context.workloadHealth().linkedDashboards().stream()
        .map(
            dashboard ->
                new DeepLink(
                    dashboard.title(),
                    EvidenceSource.GRAFANA,
                    dashboard.url(),
                    "Grafana dashboard"))
        .forEach(links::add);
    logSearch.deepLinks().forEach(links::add);
    context.traceSummaries().stream()
        .map(TraceSummary::deepLink)
        .filter(link -> link != null)
        .forEach(links::add);
    context.bitbucketChanges().stream()
        .flatMap(change -> change.deepLinks().stream())
        .forEach(links::add);
    if (changeAttributionResult != null) {
      changeAttributionResult.directLinks().forEach(links::add);
    }
    return links.stream().distinct().toList();
  }

  private String bitbucketWorkspace(String cluster, ServiceCatalogEntry catalogEntry) {
    if (catalogEntry != null
        && catalogEntry.repoWorkspace() != null
        && !catalogEntry.repoWorkspace().isBlank()) {
      return catalogEntry.repoWorkspace();
    }
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(candidate -> candidate.bitbucket().workspace())
        .orElse("");
  }

  private String bitbucketRepoSlug(String cluster, ServiceCatalogEntry catalogEntry) {
    if (catalogEntry != null
        && catalogEntry.repoSlug() != null
        && !catalogEntry.repoSlug().isBlank()) {
      return catalogEntry.repoSlug();
    }
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(candidate -> candidate.bitbucket().repoSlug())
        .orElse("");
  }

  private ChangeCausality mapCausality(CausationClass causationClass, ChangeCausality fallback) {
    if (causationClass == null) {
      return fallback;
    }
    return switch (causationClass) {
      case LIKELY_ROOT_CAUSE, LIKELY_CONTRIBUTING_FACTOR -> ChangeCausality.LIKELY_CAUSAL;
      case CORRELATED_BUT_NOT_CAUSAL -> ChangeCausality.POSSIBLY_RELATED;
      case INSUFFICIENT_EVIDENCE -> ChangeCausality.UNLIKELY;
    };
  }

  private List<MetricSeries> comparisonMetricSeries(
      String cluster,
      String namespace,
      String workloadName,
      ServiceCatalogEntry catalogEntry,
      Instant anchor,
      Duration window) {
    Map<String, String> queries = comparisonQueries(namespace, workloadName, catalogEntry);
    List<MetricSeries> series = new ArrayList<>();
    queries.forEach(
        (metricName, query) -> {
          if (query != null && !query.isBlank()) {
            metricsPort
                .rangeQuery(
                    cluster,
                    query,
                    anchor.minus(window),
                    anchor.plus(window),
                    properties.guardrails().minStep())
                .series()
                .forEach(
                    item ->
                        series.add(
                            new MetricSeries(
                                metricName,
                                item.query(),
                                item.unit(),
                                item.labels(),
                                item.points())));
          }
        });
    return series;
  }

  private Map<String, String> comparisonQueries(
      String namespace, String workloadName, ServiceCatalogEntry catalogEntry) {
    Map<String, String> queries = new LinkedHashMap<>();
    if (catalogEntry != null
        && catalogEntry.promqlTemplates() != null
        && !catalogEntry.promqlTemplates().isEmpty()) {
      queries.put(
          "error_rate_ratio", catalogEntry.promqlTemplates().getOrDefault("error_rate", ""));
      queries.put("latency_slo_ratio", catalogEntry.promqlTemplates().getOrDefault("latency", ""));
      queries.put("cpu_saturation_ratio", catalogEntry.promqlTemplates().getOrDefault("cpu", ""));
      queries.put(
          "memory_pressure_ratio", catalogEntry.promqlTemplates().getOrDefault("memory", ""));
      return queries;
    }
    queries.put(
        "error_rate_ratio",
        "clamp_max(sum(rate(http_server_requests_seconds_count{namespace=\""
            + namespace
            + "\",app=\""
            + workloadName
            + "\",status=~\"5..\"}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{namespace=\""
            + namespace
            + "\",app=\""
            + workloadName
            + "\"}[5m])), 0.001), 1)");
    queries.put(
        "latency_slo_ratio",
        "clamp_max(histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace=\""
            + namespace
            + "\",app=\""
            + workloadName
            + "\"}[5m])) by (le)) / 0.5, 1)");
    queries.put(
        "cpu_saturation_ratio",
        "clamp_max(sum(rate(container_cpu_usage_seconds_total{namespace=\""
            + namespace
            + "\",pod=~\""
            + workloadName
            + "-.*\",container!=\"POD\"}[5m])), 1)");
    queries.put(
        "memory_pressure_ratio",
        "clamp_max(sum(container_memory_working_set_bytes{namespace=\""
            + namespace
            + "\",pod=~\""
            + workloadName
            + "-.*\",container!=\"POD\"}) / clamp_min(sum(kube_pod_container_resource_limits{namespace=\""
            + namespace
            + "\",pod=~\""
            + workloadName
            + "-.*\",resource=\"memory\"}), 1), 1)");
    return queries;
  }

  private String causalChainSummary(RootCauseCandidate primarySuspect) {
    if (primarySuspect == null) {
      return "Evidence is insufficient to produce a reliable causal chain.";
    }
    return primarySuspect.causeType().name().toLowerCase().replace('_', ' ')
        + " -> "
        + primarySuspect.entity()
        + " -> leading incident symptoms";
  }

  private List<RootCauseCandidate> combineCandidates(
      RootCauseCandidate primary, List<RootCauseCandidate> alternates) {
    List<RootCauseCandidate> candidates = new ArrayList<>();
    if (primary != null) {
      candidates.add(primary);
    }
    if (alternates != null) {
      candidates.addAll(alternates);
    }
    return candidates;
  }

  private List<String> combineUnknowns(
      List<String> baseUnknowns, List<ObservabilityCoverageGap> coverageGaps) {
    List<String> unknowns = new ArrayList<>(baseUnknowns);
    coverageGaps.stream().map(ObservabilityCoverageGap::summary).forEach(unknowns::add);
    return unknowns;
  }

  private List<MetricSeries> recentMetricSeries(
      String cluster,
      String namespace,
      String workloadName,
      ServiceCatalogEntry catalogEntry,
      Instant end,
      Duration window) {
    Map<String, String> queries = comparisonQueries(namespace, workloadName, catalogEntry);
    List<MetricSeries> series = new ArrayList<>();
    queries.forEach(
        (metricName, query) -> {
          if (query == null || query.isBlank()) {
            return;
          }
          metricsPort
              .rangeQuery(cluster, query, end.minus(window), end, properties.guardrails().minStep())
              .series()
              .forEach(
                  item ->
                      series.add(
                          new MetricSeries(
                              metricName,
                              item.query(),
                              item.unit(),
                              item.labels(),
                              item.points())));
        });
    return series;
  }

  private Map<String, List<MetricSeries>> seriesByMetric(List<MetricSeries> series) {
    return series.stream().collect(java.util.stream.Collectors.groupingBy(MetricSeries::name));
  }

  private Map<String, WorkloadHealth> relatedHealthByService(
      String cluster,
      String namespace,
      IncidentContext context,
      Duration lookback,
      String identity) {
    Map<String, WorkloadHealth> healthByService = new LinkedHashMap<>();
    String targetId =
        context.catalogEntry() == null
            ? context.serviceOrWorkload()
            : context.catalogEntry().serviceId();
    healthByService.put(targetId, context.workloadHealth());
    java.util.Set<String> relatedServiceIds = new java.util.LinkedHashSet<>();
    if (context.catalogEntry() != null) {
      relatedServiceIds.addAll(context.catalogEntry().dependencyServiceIds());
    }
    serviceCatalogPort.listServices().stream()
        .filter(entry -> entry.cluster().equals(cluster))
        .filter(entry -> entry.dependencyServiceIds().contains(targetId))
        .map(ServiceCatalogEntry::serviceId)
        .forEach(relatedServiceIds::add);
    context.traceSummaries().stream()
        .flatMap(trace -> trace.dependencyEdges().stream())
        .map(
            edge ->
                targetId.equals(edge.sourceService()) ? edge.targetService() : edge.sourceService())
        .filter(candidate -> candidate != null && !candidate.equals(targetId))
        .forEach(relatedServiceIds::add);
    for (String serviceId : relatedServiceIds) {
      ServiceCatalogEntry relatedEntry =
          serviceCatalogPort.listServices().stream()
              .filter(entry -> entry.cluster().equals(cluster))
              .filter(entry -> entry.namespace().equals(namespace))
              .filter(
                  entry ->
                      entry.serviceId().equals(serviceId)
                          || serviceId.equals(entry.traceServiceName())
                          || entry.workloadName().equals(serviceId))
              .findFirst()
              .orElse(null);
      if (relatedEntry == null) {
        continue;
      }
      try {
        healthByService.put(
            relatedEntry.serviceId(),
            inventoryService.getWorkloadHealth(
                cluster,
                namespace,
                relatedEntry.workloadName(),
                relatedEntry.workloadKind(),
                lookback,
                identity));
      } catch (RuntimeException ignored) {
        // Missing workload mappings should lower confidence, not abort the whole topology flow.
      }
    }
    return healthByService;
  }

  private List<WorkloadInfo> scopedWorkloads(
      String cluster, String namespace, String serviceOrWorkload) {
    if (serviceOrWorkload == null || serviceOrWorkload.isBlank()) {
      return clusterInventoryPort.listWorkloads(cluster, namespace, null, null);
    }
    return List.of(resolveWorkload(cluster, namespace, serviceOrWorkload));
  }

  private Map<String, ResourceWasteAnalyzer.UsageSnapshot> usageSnapshots(
      String cluster, String namespace, List<WorkloadInfo> workloads, Duration lookback) {
    Map<String, ResourceWasteAnalyzer.UsageSnapshot> usage = new LinkedHashMap<>();
    Instant end = Instant.now(clock);
    for (WorkloadInfo workload : workloads) {
      List<MetricSeries> cpuSeries =
          metricsPort
              .rangeQuery(
                  cluster,
                  cpuUsageQuery(namespace, workload.name()),
                  end.minus(lookback),
                  end,
                  properties.guardrails().minStep())
              .series();
      List<MetricSeries> memorySeries =
          metricsPort
              .rangeQuery(
                  cluster,
                  memoryUsageQuery(namespace, workload.name()),
                  end.minus(lookback),
                  end,
                  properties.guardrails().minStep())
              .series();
      double currentCpu = latestValue(cpuSeries);
      double currentMemory = latestValue(memorySeries);
      double p95Cpu = percentile(cpuSeries, 0.95d);
      double p95Memory = percentile(memorySeries, 0.95d);
      usage.put(
          workload.name(),
          new ResourceWasteAnalyzer.UsageSnapshot(currentCpu, currentMemory, p95Cpu, p95Memory));
    }
    return usage;
  }

  private com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown resourceConfidence(
      List<WorkloadInfo> workloads,
      Map<String, ResourceWasteAnalyzer.UsageSnapshot> usageSnapshots) {
    boolean allRequested = workloads.stream().allMatch(WorkloadInfo::hasResourceRequests);
    boolean allMeasured =
        workloads.stream().allMatch(workload -> usageSnapshots.containsKey(workload.name()));
    double uncertaintyPenalty = (allRequested ? 0.04d : 0.16d) + (allMeasured ? 0.04d : 0.16d);
    return new com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown(
        Math.max(0.38d, 0.86d - uncertaintyPenalty),
        0.48d,
        0.08d,
        uncertaintyPenalty,
        List.of(
            new com.prodops.controltower.mcp.domain.model.ConfidenceFactor(
                "resource requests",
                allRequested ? 0.2d : 0.08d,
                "Workload resource requests are required for waste and right-sizing analysis."),
            new com.prodops.controltower.mcp.domain.model.ConfidenceFactor(
                "usage series",
                allMeasured ? 0.2d : 0.08d,
                "Prometheus usage series are required for P95 usage estimation.")),
        "Resource guidance confidence is strongest when requests and usage series are both complete.");
  }

  private double latestValue(List<MetricSeries> series) {
    return series.stream()
        .flatMap(item -> item.points().stream())
        .max(Comparator.comparing(point -> point.timestamp()))
        .map(point -> point.value())
        .orElse(0.0d);
  }

  private double percentile(List<MetricSeries> series, double percentile) {
    List<Double> values =
        series.stream()
            .flatMap(item -> item.points().stream())
            .map(point -> point.value())
            .sorted()
            .toList();
    if (values.isEmpty()) {
      return 0.0d;
    }
    int index = Math.min(values.size() - 1, (int) Math.floor(percentile * (values.size() - 1)));
    return values.get(index);
  }

  private String cpuUsageQuery(String namespace, String workloadName) {
    return "sum(rate(container_cpu_usage_seconds_total{namespace=\""
        + namespace
        + "\",pod=~\""
        + workloadName
        + "-.*\",container!=\"POD\"}[5m]))";
  }

  private String memoryUsageQuery(String namespace, String workloadName) {
    return "sum(container_memory_working_set_bytes{namespace=\""
        + namespace
        + "\",pod=~\""
        + workloadName
        + "-.*\",container!=\"POD\"})";
  }

  private String versionTag(WorkloadInfo workload, ServiceCatalogEntry catalogEntry) {
    List<String> keys =
        catalogEntry == null
                || catalogEntry.versionLabelKeys() == null
                || catalogEntry.versionLabelKeys().isEmpty()
            ? List.of("app.kubernetes.io/version", "release", "image-tag")
            : catalogEntry.versionLabelKeys();
    return keys.stream()
        .map(key -> workload.labels().get(key))
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private double replicaGap(Integer desiredReplicas, Integer readyReplicas) {
    if (desiredReplicas == null || desiredReplicas == 0 || readyReplicas == null) {
      return 0.0d;
    }
    return Math.max(0.0d, (desiredReplicas - readyReplicas) / (double) desiredReplicas);
  }

  private DataFreshness freshnessFromWorkloads(List<WorkloadInfo> workloads) {
    Instant observedAt =
        workloads.stream()
            .map(workload -> Optional.ofNullable(workload.updatedAt()).orElse(workload.createdAt()))
            .filter(item -> item != null)
            .max(Comparator.naturalOrder())
            .orElse(Instant.EPOCH);
    Instant now = Instant.now(clock);
    return new DataFreshness(
        now,
        observedAt,
        observedAt.equals(Instant.EPOCH) ? Duration.ZERO : Duration.between(observedAt, now),
        false);
  }

  private int riskSeverity(RiskLevel riskLevel) {
    return switch (riskLevel) {
      case CRITICAL -> 4;
      case HIGH -> 3;
      case MODERATE -> 2;
      case LOW -> 1;
    };
  }

  private Instant freshest(Instant first, Instant second) {
    if (first == null) {
      return second == null ? Instant.EPOCH : second;
    }
    if (second == null) {
      return first;
    }
    return first.isAfter(second) ? first : second;
  }

  private double metric(String name, List<MetricValue> metrics) {
    return metrics.stream()
        .filter(metric -> metric.name().equals(name))
        .mapToDouble(MetricValue::value)
        .findFirst()
        .orElse(0.0d);
  }
}
