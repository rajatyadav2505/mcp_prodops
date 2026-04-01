package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record RolloutHistoryResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<RolloutRevision> revisions,
    List<String> metricShiftNotes,
    List<DeepLink> directLinks,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
