package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record IngressHealthResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<IngressBackendHealth> ingresses,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record IngressBackendHealth(
      String ingressName,
      List<String> hosts,
      String backendService,
      int readyPods,
      int totalPods,
      HealthVerdict verdict) {}
}
