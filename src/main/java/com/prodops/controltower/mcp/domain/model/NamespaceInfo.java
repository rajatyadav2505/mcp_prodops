package com.prodops.controltower.mcp.domain.model;

import java.util.Map;

public record NamespaceInfo(
    String cluster,
    String name,
    Map<String, String> labels,
    String ownerTeam,
    String criticality,
    boolean critical) {}
