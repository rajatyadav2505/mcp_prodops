package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SloStatusResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<SloBudgetStatus> budgets,
    SloBudgetStatus leadingRisk,
    ConfidenceBreakdown confidenceBreakdown,
    List<DeepLink> directLinks,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record BurnRateWindow(Duration window, double burnRate) {}

  public record SloBudgetStatus(
      String name,
      String objective,
      String threshold,
      String measurementWindow,
      String metricName,
      double currentRatio,
      double remainingBudgetPercent,
      List<BurnRateWindow> burnRates,
      Duration timeToBreach,
      String status) {}
}
