package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ImageFreshnessCheckResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    int staleImageCount,
    List<ImageFreshnessEntry> images,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record ImageFreshnessEntry(
      String workloadName,
      String image,
      Instant imageCreatedAt,
      Instant referenceTimestamp,
      String ageBasis,
      Duration age,
      boolean stale) {}
}
