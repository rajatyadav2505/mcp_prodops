package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ResourceWasteResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<ResourceWasteFinding> findings,
    ConfidenceBreakdown confidenceBreakdown,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record ResourceWasteFinding(
      String workloadName,
      Double requestedCpuCores,
      Double requestedMemoryBytes,
      double cpuUsageRatio,
      double memoryUsageRatio,
      double wasteScore,
      String rationale) {}
}
