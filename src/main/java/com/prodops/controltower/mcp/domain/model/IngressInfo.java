package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record IngressInfo(
    String cluster,
    String namespace,
    String name,
    List<String> hosts,
    List<String> backendServices) {}
