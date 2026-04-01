package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record DailyRiskTrendResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    String trend,
    List<RiskTrendPoint> points,
    ConfidenceBreakdown confidenceBreakdown,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record RiskTrendPoint(Instant timestamp, double riskScore, RiskLevel riskLevel) {}
}
