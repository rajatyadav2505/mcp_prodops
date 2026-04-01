package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record RightSizingResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<RightSizingRecommendation> recommendations,
    ConfidenceBreakdown confidenceBreakdown,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record RightSizingRecommendation(
      String workloadName,
      Double currentRequestedCpuCores,
      Double recommendedCpuCores,
      Double currentRequestedMemoryBytes,
      Double recommendedMemoryBytes,
      String rationale) {}
}
