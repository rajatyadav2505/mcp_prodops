package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.util.Map;

public record ClusterInfo(
    String name,
    String environment,
    String kubernetesVersion,
    String region,
    Map<String, String> metadata) {}
