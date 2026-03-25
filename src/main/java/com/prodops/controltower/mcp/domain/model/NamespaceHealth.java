package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record NamespaceHealth(
    String cluster,
    String namespace,
    int workloadCount,
    int unhealthyWorkloadCount,
    int restartCount,
    int warningEventCount,
    double riskScore,
    RiskLevel riskLevel,
    HealthVerdict verdict,
    List<String> topIssues,
    List<MetricValue> keyMetrics,
    List<EvidenceItem> evidence,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
