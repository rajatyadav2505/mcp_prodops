package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ServiceDependencyMap(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<ServiceDependencyNode> nodes,
    List<ServiceDependencyEdge> edges,
    double confidence,
    List<DeepLink> directLinks,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record ServiceDependencyNode(
      String entityId,
      String displayName,
      String relationToTarget,
      HealthVerdict verdict,
      RiskLevel riskLevel) {}

  public record ServiceDependencyEdge(
      String fromId,
      String toId,
      String type,
      EvidenceSource source,
      double confidence,
      String rationale) {}
}
