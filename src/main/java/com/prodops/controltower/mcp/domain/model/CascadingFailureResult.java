package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record CascadingFailureResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    boolean cascading,
    List<CascadeImpact> impacts,
    ConfidenceBreakdown confidenceBreakdown,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record CascadeImpact(
      String serviceId,
      String direction,
      HealthVerdict verdict,
      RiskLevel riskLevel,
      double severity,
      String rationale,
      List<DeepLink> directLinks) {}
}
