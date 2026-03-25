package com.prodops.controltower.mcp.domain.service;

import com.prodops.controltower.mcp.domain.model.ClusterInfo;
import com.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.HealthVerdict;
import com.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.NamespaceHealth;
import com.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.prodops.controltower.mcp.domain.model.PodDiagnostics;
import com.prodops.controltower.mcp.domain.model.PodInfo;
import com.prodops.controltower.mcp.domain.model.RiskLevel;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.prodops.controltower.mcp.domain.port.ClusterInventoryPort;
import com.prodops.controltower.mcp.domain.port.DashboardPort;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import com.prodops.controltower.mcp.domain.port.RiskWeightsPort;
import com.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import com.prodops.controltower.mcp.domain.scoring.RiskAssessment;
import com.prodops.controltower.mcp.domain.scoring.RiskScoreEngine;
import com.prodops.controltower.mcp.domain.scoring.RiskSignal;
import com.prodops.controltower.mcp.policy.ScopePolicy;
import com.prodops.controltower.mcp.redaction.RedactionService;
import com.prodops.controltower.mcp.support.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

  private final ClusterInventoryPort clusterInventoryPort;
  private final MetricsPort metricsPort;
  private final DashboardPort dashboardPort;
  private final ServiceCatalogPort serviceCatalogPort;
  private final RiskWeightsPort riskWeightsPort;
  private final RiskScoreEngine riskScoreEngine;
  private final ScopePolicy scopePolicy;
  private final RedactionService redactionService;
  private final Clock clock;

  public InventoryService(
      ClusterInventoryPort clusterInventoryPort,
      MetricsPort metricsPort,
      DashboardPort dashboardPort,
      ServiceCatalogPort serviceCatalogPort,
      RiskWeightsPort riskWeightsPort,
      RiskScoreEngine riskScoreEngine,
      ScopePolicy scopePolicy,
      RedactionService redactionService,
      Clock clock) {
    this.clusterInventoryPort = clusterInventoryPort;
    this.metricsPort = metricsPort;
    this.dashboardPort = dashboardPort;
    this.serviceCatalogPort = serviceCatalogPort;
    this.riskWeightsPort = riskWeightsPort;
    this.riskScoreEngine = riskScoreEngine;
    this.scopePolicy = scopePolicy;
    this.redactionService = redactionService;
    this.clock = clock;
  }

  public List<ClusterInfo> listClusters(String identity) {
    return clusterInventoryPort.listClusters().stream()
        .filter(cluster -> scopePolicy.authorizeCluster(cluster.name(), identity).allowed())
        .toList();
  }

  public List<NamespaceInfo> listNamespaces(
      String cluster, String team, boolean criticalOnly, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    return clusterInventoryPort.listNamespaces(cluster).stream()
        .filter(
            namespace ->
                scopePolicy.authorizeNamespace(cluster, namespace.name(), identity).allowed())
        .filter(namespace -> team == null || team.isBlank() || team.equals(namespace.ownerTeam()))
        .filter(namespace -> !criticalOnly || namespace.critical())
        .toList();
  }

  public List<WorkloadInfo> listWorkloads(
      String cluster, String namespace, WorkloadKind kind, String labelSelector, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    return clusterInventoryPort.listWorkloads(cluster, namespace, kind, labelSelector);
  }

  public NamespaceHealth getNamespaceHealth(
      String cluster, String namespace, Duration lookback, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(lookback);
    List<WorkloadInfo> workloads =
        clusterInventoryPort.listWorkloads(cluster, namespace, null, null);
    List<WorkloadHealth> workloadHealths =
        workloads.stream()
            .map(
                workload ->
                    buildWorkloadHealth(
                        cluster, namespace, workload.name(), workload.kind(), lookback))
            .toList();
    List<WarningEvent> warningEvents =
        clusterInventoryPort.listWarningEvents(cluster, namespace, null, lookback);
    double averageRisk =
        workloadHealths.stream().mapToDouble(WorkloadHealth::riskScore).average().orElse(0.0d);
    int restarts = workloadHealths.stream().mapToInt(WorkloadHealth::totalRestarts).sum();
    List<String> topIssues =
        warningEvents.stream()
            .collect(
                Collectors.groupingBy(
                    WarningEvent::reason, Collectors.summingInt(WarningEvent::count)))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(entry -> entry.getKey() + " x" + entry.getValue())
            .toList();
    List<MetricValue> keyMetrics =
        workloadHealths.stream()
            .flatMap(health -> health.coreMetrics().stream())
            .sorted(Comparator.comparingDouble(MetricValue::value).reversed())
            .limit(5)
            .toList();

    RiskLevel riskLevel = riskLevel(averageRisk);
    HealthVerdict verdict =
        workloadHealths.stream().anyMatch(health -> health.verdict() == HealthVerdict.UNHEALTHY)
            ? HealthVerdict.UNHEALTHY
            : averageRisk >= 50 ? HealthVerdict.DEGRADED : HealthVerdict.HEALTHY;
    Instant freshestSignal =
        freshest(
            warningEvents.stream()
                .map(WarningEvent::lastTimestamp)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH),
            keyMetrics.stream()
                .map(MetricValue::observedAt)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH));

    return new NamespaceHealth(
        cluster,
        namespace,
        workloads.size(),
        (int)
            workloadHealths.stream()
                .filter(health -> health.verdict() != HealthVerdict.HEALTHY)
                .count(),
        restarts,
        warningEvents.size(),
        averageRisk,
        riskLevel,
        verdict,
        topIssues,
        keyMetrics,
        List.of(),
        Instant.now(clock),
        new DataFreshness(
            Instant.now(clock),
            freshestSignal,
            Duration.between(freshestSignal, Instant.now(clock)),
            false));
  }

  public WorkloadHealth getWorkloadHealth(
      String cluster,
      String namespace,
      String workloadName,
      WorkloadKind workloadKind,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(lookback);
    return buildWorkloadHealth(cluster, namespace, workloadName, workloadKind, lookback);
  }

  public PodDiagnostics getPodDiagnostics(
      String cluster,
      String namespace,
      String podName,
      String container,
      boolean includeLogs,
      int tailLines,
      Duration lookback,
      String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(lookback);
    scopePolicy.verifyLogLines(tailLines);
    List<PodInfo> pods =
        clusterInventoryPort.listWorkloads(cluster, namespace, null, null).stream()
            .flatMap(
                workload ->
                    clusterInventoryPort.listPodsForWorkload(cluster, namespace, workload).stream())
            .toList();
    PodInfo pod =
        pods.stream()
            .filter(candidate -> candidate.name().equals(podName))
            .findFirst()
            .orElseThrow(
                () -> new NotFoundException("Pod not found in the configured namespace scope."));
    List<WarningEvent> warningEvents =
        clusterInventoryPort.listWarningEvents(cluster, namespace, podName, lookback);
    LogExcerpt logExcerpt = null;
    if (includeLogs) {
      logExcerpt =
          clusterInventoryPort
              .getPodLogs(
                  cluster,
                  namespace,
                  podName,
                  container == null ? "app" : container,
                  tailLines,
                  lookback)
              .map(
                  log ->
                      new LogExcerpt(
                          log.podName(),
                          log.container(),
                          log.collectedAt(),
                          redactionService.redactLines(
                              log.lines().stream().limit(tailLines).toList()),
                          log.truncated()))
              .orElse(null);
    }
    Instant freshestSignal =
        freshest(
            pod.createdAt(),
            warningEvents.stream()
                .map(WarningEvent::lastTimestamp)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH));
    return new PodDiagnostics(
        cluster,
        namespace,
        podName,
        pod.phase(),
        pod.containerStates(),
        pod.restartCount(),
        pod.lastTerminationReason(),
        warningEvents,
        pod.ownerReference(),
        logExcerpt,
        Instant.now(clock),
        new DataFreshness(
            Instant.now(clock),
            freshestSignal,
            Duration.between(freshestSignal, Instant.now(clock)),
            false));
  }

  public List<WarningEvent> getRecentWarningEvents(
      String cluster, String namespace, String workload, Duration since, String identity) {
    scopePolicy.assertAllowed(
        namespace == null
            ? scopePolicy.authorizeCluster(cluster, identity)
            : scopePolicy.authorizeNamespace(cluster, namespace, identity));
    scopePolicy.verifyLookback(since);
    return clusterInventoryPort.listWarningEvents(cluster, namespace, workload, since);
  }

  private WorkloadHealth buildWorkloadHealth(
      String cluster,
      String namespace,
      String workloadName,
      WorkloadKind workloadKind,
      Duration lookback) {
    WorkloadInfo workload =
        clusterInventoryPort
            .getWorkload(cluster, namespace, workloadName, workloadKind)
            .orElseThrow(() -> new NotFoundException("Workload not found in configured scope."));
    List<PodInfo> pods = clusterInventoryPort.listPodsForWorkload(cluster, namespace, workload);
    List<WarningEvent> warningEvents =
        clusterInventoryPort.listWarningEvents(cluster, namespace, workloadName, lookback);
    Optional<ServiceCatalogEntry> catalogEntry =
        serviceCatalogPort.findByWorkload(cluster, namespace, workloadName);
    List<MetricValue> coreMetrics =
        metricsPort.goldenSignals(
            cluster, namespace, workloadName, lookback, catalogEntry.orElse(null));
    List<DashboardInfo> dashboards =
        catalogEntry
            .map(
                entry ->
                    entry.dashboardUids().stream()
                        .map(uid -> dashboardPort.getByUid(cluster, uid))
                        .flatMap(Optional::stream)
                        .toList())
            .orElseGet(() -> dashboardPort.search(cluster, workloadName, List.of(), null, 3));

    int totalRestarts = pods.stream().mapToInt(PodInfo::restartCount).sum();
    double unavailableRatio = replicaGap(workload.desiredReplicas(), workload.readyReplicas());
    double errorRate = metricValue("error_rate_ratio", coreMetrics);
    double latency = metricValue("latency_slo_ratio", coreMetrics);
    double cpu = metricValue("cpu_saturation_ratio", coreMetrics);
    double memory = metricValue("memory_pressure_ratio", coreMetrics);
    double dependencyUncertainty = catalogEntry.isPresent() ? 0.15d : 0.45d;
    double noise = coreMetrics.isEmpty() ? Math.min(1.0d, warningEvents.size() / 10.0d) : 0.15d;
    double rolloutFreshnessMinutes =
        Duration.between(
                Optional.ofNullable(workload.updatedAt()).orElse(workload.createdAt()),
                Instant.now(clock))
            .toMinutes();

    RiskAssessment risk =
        riskScoreEngine.assess(
            new RiskSignal(
                totalRestarts,
                warningEvents.stream().mapToInt(WarningEvent::count).sum(),
                rolloutFreshnessMinutes,
                errorRate,
                latency,
                cpu,
                memory,
                unavailableRatio,
                dependencyUncertainty,
                noise),
            riskWeightsPort.getWeights());

    HealthVerdict verdict =
        unavailableRatio > 0.0d || totalRestarts > 0
            ? HealthVerdict.UNHEALTHY
            : risk.score() >= 50 ? HealthVerdict.DEGRADED : HealthVerdict.HEALTHY;

    Instant freshestSignal =
        freshest(
            Optional.ofNullable(workload.updatedAt()).orElse(workload.createdAt()),
            coreMetrics.stream()
                .map(MetricValue::observedAt)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH));

    return new WorkloadHealth(
        cluster,
        namespace,
        workloadName,
        workloadKind,
        workload.desiredReplicas(),
        workload.readyReplicas(),
        pods,
        totalRestarts,
        Duration.ofMinutes(Math.max(0, (long) rolloutFreshnessMinutes)),
        warningEvents,
        coreMetrics,
        dashboards,
        risk.score(),
        risk.level(),
        verdict,
        Instant.now(clock),
        new DataFreshness(
            Instant.now(clock),
            freshestSignal,
            Duration.between(freshestSignal, Instant.now(clock)),
            false));
  }

  private double metricValue(String metricName, List<MetricValue> metrics) {
    return metrics.stream()
        .filter(metric -> metric.name().equals(metricName))
        .mapToDouble(MetricValue::value)
        .findFirst()
        .orElse(0.0d);
  }

  private double replicaGap(Integer desiredReplicas, Integer readyReplicas) {
    if (desiredReplicas == null || desiredReplicas == 0 || readyReplicas == null) {
      return 0.0d;
    }
    return Math.max(0.0d, (desiredReplicas - readyReplicas) / (double) desiredReplicas);
  }

  private Instant freshest(Instant first, Instant second) {
    return first.isAfter(second) ? first : second;
  }

  private RiskLevel riskLevel(double score) {
    if (score >= riskWeightsPort.getWeights().criticalThreshold()) {
      return RiskLevel.CRITICAL;
    }
    if (score >= riskWeightsPort.getWeights().highThreshold()) {
      return RiskLevel.HIGH;
    }
    if (score >= riskWeightsPort.getWeights().moderateThreshold()) {
      return RiskLevel.MODERATE;
    }
    return RiskLevel.LOW;
  }
}
