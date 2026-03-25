package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record WorkloadHealth(
    String cluster,
    String namespace,
    String workloadName,
    WorkloadKind workloadKind,
    Integer desiredReplicas,
    Integer readyReplicas,
    List<PodInfo> pods,
    int totalRestarts,
    Duration rolloutAge,
    List<WarningEvent> warningEvents,
    List<MetricValue> coreMetrics,
    List<DashboardInfo> linkedDashboards,
    double riskScore,
    RiskLevel riskLevel,
    HealthVerdict verdict,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
