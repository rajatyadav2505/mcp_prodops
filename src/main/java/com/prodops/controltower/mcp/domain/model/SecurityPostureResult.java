package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record SecurityPostureResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int score,
    RiskLevel riskLevel,
    List<SecurityPostureFinding> findings,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record SecurityPostureFinding(
      String title, String description, String severity, boolean passing) {}
}
