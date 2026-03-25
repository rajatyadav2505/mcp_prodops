package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record MetricValue(
    String name,
    String unit,
    double value,
    String status,
    Instant observedAt,
    String query,
    String explanation) {}
