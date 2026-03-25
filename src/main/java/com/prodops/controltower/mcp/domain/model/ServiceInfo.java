package com.prodops.controltower.mcp.domain.model;

import java.util.Map;

public record ServiceInfo(
    String cluster, String namespace, String name, String type, Map<String, String> selector) {}
