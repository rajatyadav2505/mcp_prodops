package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record AlertCorrelationResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int totalAlerts,
    int groupCount,
    List<AlertCorrelationGroup> groups,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record AlertCorrelationGroup(
      String groupId,
      String summary,
      Instant start,
      Instant end,
      List<String> reasons,
      List<String> involvedObjects,
      int alertCount,
      double confidence) {}
}
