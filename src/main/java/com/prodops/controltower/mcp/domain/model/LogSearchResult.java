package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record LogSearchResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int totalHits,
    boolean truncated,
    List<LogEvent> events,
    List<LogErrorSignature> topSignatures,
    List<DeepLink> deepLinks,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
