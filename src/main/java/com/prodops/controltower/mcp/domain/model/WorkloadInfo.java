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
    String criticality) {}
