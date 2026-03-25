package com.prodops.controltower.mcp.domain.model;

import java.util.List;
import java.util.Map;

public record MetricSeries(
    String name,
    String query,
    String unit,
    Map<String, String> labels,
    List<MetricSeriesPoint> points) {}
