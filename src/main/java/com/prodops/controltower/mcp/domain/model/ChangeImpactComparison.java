package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ChangeImpactComparison(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String changeReference,
    String executiveSummary,
    String operatorSummary,
    Instant comparisonAnchor,
    Duration beforeWindow,
    Duration afterWindow,
    List<MetricDelta> metricDeltas,
    List<LogErrorSignature> beforeLogSignatures,
    List<LogErrorSignature> afterLogSignatures,
    Duration beforeCriticalPathDuration,
    Duration afterCriticalPathDuration,
    int beforeFailingSpanCount,
    int afterFailingSpanCount,
    List<String> dependencyLatencyShifts,
    List<EvidenceItem> evidence,
    List<DeepLink> directLinks,
    ConfidenceBreakdown confidenceBreakdown,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
