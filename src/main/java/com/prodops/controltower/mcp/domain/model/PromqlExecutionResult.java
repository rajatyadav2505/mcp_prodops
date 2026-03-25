package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record PromqlExecutionResult(
    String cluster,
    String query,
    Instant evaluatedAt,
    List<MetricSeries> series,
    boolean truncated,
    List<String> limitations) {}
