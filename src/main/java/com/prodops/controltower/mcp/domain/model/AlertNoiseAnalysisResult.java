package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record AlertNoiseAnalysisResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int totalAlertVolume,
    int noisyAlertCount,
    int actionableAlertCount,
    List<AlertReasonSummary> reasons,
    ConfidenceBreakdown confidenceBreakdown,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record AlertReasonSummary(
      String reason,
      int occurrences,
      int deduplicatedObjects,
      boolean noisy,
      boolean actionable,
      String rationale) {}
}
