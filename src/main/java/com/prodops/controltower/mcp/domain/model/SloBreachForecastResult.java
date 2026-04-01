package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SloBreachForecastResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    String sloName,
    double currentRatio,
    double remainingBudgetPercent,
    Duration timeToBreach,
    Instant projectedBreachAt,
    ConfidenceBreakdown confidenceBreakdown,
    List<EvidenceItem> evidence,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
