package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record CoverageGapResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<ObservabilityCoverageGap> gaps,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
