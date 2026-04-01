package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ToilEstimationResult(
    String cluster,
    String namespace,
    String executiveSummary,
    String operatorSummary,
    List<ToilSummary> summaries,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record ToilSummary(
      String scope,
      String ownerTeam,
      int warningEvents,
      int restarts,
      int scalingSignals,
      double toilScore) {}
}
