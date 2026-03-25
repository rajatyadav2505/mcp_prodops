package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record MetricSeriesPoint(Instant timestamp, double value) {}
