package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.Map;

public record WorkloadInfo(
    String cluster,
    String namespace,
    String name,
    WorkloadKind kind,
    Map<String, String> labels,
    Map<String, String> selector,
    Integer desiredReplicas,
    Integer readyReplicas,
    Instant createdAt,
    Instant updatedAt,
    String ownerTeam,
    String criticality,
    String revision,
    String image,
    Instant imageCreatedAt,
    Double requestedCpuCores,
    Double requestedMemoryBytes,
    Double limitCpuCores,
    Double limitMemoryBytes,
    Boolean runAsNonRoot,
    Boolean privileged,
    Boolean hostNetwork,
    Boolean hasReadinessProbe) {

  public WorkloadInfo {
    labels = labels == null ? Map.of() : Map.copyOf(labels);
    selector = selector == null ? Map.of() : Map.copyOf(selector);
  }

  public WorkloadInfo(
      String cluster,
      String namespace,
      String name,
      WorkloadKind kind,
      Map<String, String> labels,
      Map<String, String> selector,
      Integer desiredReplicas,
      Integer readyReplicas,
      Instant createdAt,
      Instant updatedAt,
      String ownerTeam,
      String criticality) {
    this(
        cluster,
        namespace,
        name,
        kind,
        labels,
        selector,
        desiredReplicas,
        readyReplicas,
        createdAt,
        updatedAt,
        ownerTeam,
        criticality,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public boolean hasResourceRequests() {
    return requestedCpuCores != null || requestedMemoryBytes != null;
  }

  public boolean hasResourceLimits() {
    return limitCpuCores != null || limitMemoryBytes != null;
  }
}
