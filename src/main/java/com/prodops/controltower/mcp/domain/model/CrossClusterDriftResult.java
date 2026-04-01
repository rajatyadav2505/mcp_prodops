package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record CrossClusterDriftResult(
    String clusterA,
    String clusterB,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<DriftItem> driftItems,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record DriftItem(String field, String leftValue, String rightValue, String impact) {}
}
