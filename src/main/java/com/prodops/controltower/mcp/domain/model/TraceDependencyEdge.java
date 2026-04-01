package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;

public record TraceDependencyEdge(
    String sourceService, String targetService, Duration latency, boolean error) {}
