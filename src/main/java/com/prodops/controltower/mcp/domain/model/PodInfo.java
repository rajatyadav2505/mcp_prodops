package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PodInfo(
    String cluster,
    String namespace,
    String name,
    String phase,
    boolean ready,
    int restartCount,
    List<String> containerStates,
    String lastTerminationReason,
    Instant createdAt,
    ObjectReference ownerReference,
    Map<String, String> labels,
    String image) {

  public PodInfo {
    containerStates = containerStates == null ? List.of() : List.copyOf(containerStates);
    labels = labels == null ? Map.of() : Map.copyOf(labels);
  }

  public PodInfo(
      String cluster,
      String namespace,
      String name,
      String phase,
      boolean ready,
      int restartCount,
      List<String> containerStates,
      String lastTerminationReason,
      Instant createdAt,
      ObjectReference ownerReference) {
    this(
        cluster,
        namespace,
        name,
        phase,
        ready,
        restartCount,
        containerStates,
        lastTerminationReason,
        createdAt,
        ownerReference,
        Map.of(),
        null);
  }
}
