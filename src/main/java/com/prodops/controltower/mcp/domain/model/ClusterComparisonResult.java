package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ClusterComparisonResult(
    String executiveSummary,
    String operatorSummary,
    List<ClusterHealthComparison> clusters,
    List<String> differences,
    ConfidenceBreakdown confidenceBreakdown,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record ClusterHealthComparison(
      String cluster,
      String scope,
      HealthVerdict verdict,
      RiskLevel riskLevel,
      double riskScore,
      int workloadCount,
      int unhealthyWorkloadCount,
      String versionTag) {}
}
