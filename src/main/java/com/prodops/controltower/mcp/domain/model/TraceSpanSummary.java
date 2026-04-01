package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record TraceSpanSummary(
    String spanId,
    String parentSpanId,
    String serviceName,
    String operation,
    Instant startTime,
    Duration duration,
    boolean error,
    boolean retry,
    String podName,
    String versionTag,
    Map<String, String> tags) {}
