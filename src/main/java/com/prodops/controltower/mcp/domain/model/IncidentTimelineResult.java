package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record IncidentTimelineResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<IncidentTimelineEntry> entries,
    List<DeepLink> directLinks,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record IncidentTimelineEntry(
      Instant timestamp,
      String source,
      String category,
      String summary,
      double relevance,
      DeepLink deepLink) {}
}
