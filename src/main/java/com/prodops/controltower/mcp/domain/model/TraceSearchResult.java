package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record TraceSearchResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int totalTraces,
    boolean truncated,
    List<TraceSummary> traces,
    List<DeepLink> deepLinks,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
