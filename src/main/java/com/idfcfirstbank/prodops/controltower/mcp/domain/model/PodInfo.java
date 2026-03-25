package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

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
    ObjectReference ownerReference) {}
