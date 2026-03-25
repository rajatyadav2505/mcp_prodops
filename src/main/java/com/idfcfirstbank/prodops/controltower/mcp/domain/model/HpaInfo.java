package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

public record HpaInfo(
    String cluster,
    String namespace,
    String name,
    Integer minReplicas,
    Integer maxReplicas,
    Integer currentReplicas,
    boolean scaleConstrained) {}
