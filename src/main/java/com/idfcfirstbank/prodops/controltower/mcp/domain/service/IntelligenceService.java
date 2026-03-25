package com.idfcfirstbank.prodops.controltower.mcp.domain.service;

import com.idfcfirstbank.prodops.controltower.mcp.domain.correlation.BlastRadiusEngine;
import com.idfcfirstbank.prodops.controltower.mcp.domain.correlation.CapacityForecastEngine;
import com.idfcfirstbank.prodops.controltower.mcp.domain.correlation.ChangeCorrelationEngine;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.BlastRadiusImpact;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.BlastRadiusResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.CapacityForecastResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ChangeCorrelationResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ChangeTimelineEntry;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DataFreshness;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DeepLink;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.EvidenceItem;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.EvidenceType;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.HealthVerdict;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.HpaInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.Hypothesis;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.IncidentCorrelationResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.MetricSeries;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.MetricValue;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ObjectReference;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.RiskLevel;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WarningEvent;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WorkloadHealth;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.ClusterInventoryPort;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.MetricsPort;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import com.idfcfirstbank.prodops.controltower.mcp.policy.ScopePolicy;
import com.idfcfirstbank.prodops.controltower.mcp.support.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IntelligenceService {

  private final InventoryService inventoryService;
  private final ClusterInventoryPort clusterInventoryPort;
  private final MetricsPort metricsPort;
  private final ServiceCatalogPort serviceCatalogPort;
  private final BlastRadiusEngine blastRadiusEngine;
  private final ChangeCorrelationEngine changeCorrelationEngine;
  private final CapacityForecastEngine capacityForecastEngine;
  private final ScopePolicy scopePolicy;
  private final Clock clock;

  public IntelligenceService(
      InventoryService inventoryService,
      ClusterInventoryPort clusterInventoryPort,
      MetricsPort metricsPort,
      ServiceCatalogPort serviceCatalogPort,
      BlastRadiusEngine blastRadiusEngine,
      ChangeCorrelationEngine changeCorrelationEngine,
      CapacityForecastEngine capacityForecastEngine,
      ScopePolicy scopePolicy,
      Clock clock) {
    this.inventoryService = inventoryService;
    this.clusterInventoryPort = clusterInventoryPort;
    this.metricsPort = metricsPort;
    this.serviceCatalogPort = serviceCatalogPort;
    this.blastRadiusEngine = blastRadiusEngine;
    this.changeCorrelationEngine = changeCorrelationEngine;
    this.capacityForecastEngine = capacityForecastEngine;
    this.scopePolicy = scopePolicy;
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
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    WorkloadInfo workload = resolveWorkload(cluster, namespace, serviceOrWorkload);
    WorkloadHealth workloadHealth =
        inventoryService.getWorkloadHealth(
            cluster, namespace, workload.name(), workload.kind(), lookback, identity);
    List<WarningEvent> warningEvents =
        inventoryService.getRecentWarningEvents(
            cluster, namespace, workload.name(), lookback, identity);
    Instant rolloutTime = Optional.ofNullable(workload.updatedAt()).orElse(workload.createdAt());
    List<ChangeTimelineEntry> timeline = new ArrayList<>();
    timeline.add(
        new ChangeTimelineEntry(
            rolloutTime,
            "rollout",
            "Most recent workload update or rollout marker.",
            "kubernetes"));
    warningEvents.forEach(
        event ->
            timeline.add(
                new ChangeTimelineEntry(
                    event.lastTimestamp(),
                    "warning-event",
                    event.reason() + ": " + event.message(),
                    "kubernetes")));

    ChangeCorrelationEngine.CorrelationAssessment assessment =
        changeCorrelationEngine.assess(rolloutTime, workloadHealth.coreMetrics(), timeline);
    List<EvidenceItem> evidence = evidenceFrom(workloadHealth, warningEvents);
    List<EvidenceItem> counterEvidence = counterEvidenceFrom(workloadHealth);

    return new ChangeCorrelationResult(
        cluster,
        namespace,
        serviceOrWorkload,
        "Latest change is assessed as "
            + assessment.causality().name().toLowerCase().replace('_', ' ')
            + " with "
            + Math.round(assessment.confidence() * 100)
            + "% confidence.",
        "Rollout timing was compared against warning events and degraded golden signals.",
        assessment.causality(),
        assessment.confidence(),
        workloadHealth.riskScore(),
        workloadHealth.riskLevel(),
        assessment.timeline(),
        evidence,
        counterEvidence,
        List.of(
            "No deployment-controller event stream beyond the configured lookback was available."),
        Instant.now(clock),
        workloadHealth.dataFreshness());
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

  private List<DeepLink> dashboardLinks(List<DashboardInfo> dashboards) {
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

  private double metric(String name, List<MetricValue> metrics) {
    return metrics.stream()
        .filter(metric -> metric.name().equals(name))
        .mapToDouble(MetricValue::value)
        .findFirst()
        .orElse(0.0d);
  }
}
