package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record CanaryHealthResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    boolean canaryDetected,
    CanaryCohortSummary canary,
    CanaryCohortSummary stable,
    ConfidenceBreakdown confidenceBreakdown,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record CanaryCohortSummary(
      String cohort,
      int podCount,
      int readyPods,
      int restarts,
      int errorLogCount,
      int traceErrorCount,
      double healthScore) {}
}
