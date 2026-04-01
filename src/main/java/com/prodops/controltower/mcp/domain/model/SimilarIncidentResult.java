package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record SimilarIncidentResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<SimilarIncidentMatch> matches,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
