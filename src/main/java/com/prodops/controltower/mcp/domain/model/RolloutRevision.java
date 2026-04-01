package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record RolloutRevision(
    String cluster,
    String namespace,
    String workloadName,
    WorkloadKind workloadKind,
    String revision,
    String image,
    String versionTag,
    Integer desiredReplicas,
    Integer readyReplicas,
    String status,
    Instant rolledOutAt) {}
