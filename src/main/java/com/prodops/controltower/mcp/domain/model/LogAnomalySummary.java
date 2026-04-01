package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record LogAnomalySummary(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int recentCount,
    double recentPerMinute,
    double baselinePerMinute,
    double anomalyRatio,
    List<LogErrorSignature> dominantRecentSignatures,
    List<DeepLink> directLinks,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
