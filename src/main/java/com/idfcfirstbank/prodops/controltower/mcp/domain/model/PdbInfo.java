package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

public record PdbInfo(
    String cluster,
    String namespace,
    String name,
    String minAvailable,
    String maxUnavailable,
    int disruptionsAllowed) {}
