package com.prodops.controltower.mcp.domain.model;

public record MetricDelta(
    String metricName,
    double beforeValue,
    double afterValue,
    double delta,
    String interpretation) {}
