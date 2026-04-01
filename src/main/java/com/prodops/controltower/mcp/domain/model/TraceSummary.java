package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record TraceSummary(
    String traceId,
    String rootService,
    String operation,
    Instant startTime,
    Duration duration,
    boolean error,
    String firstFailingService,
    String firstFailingSpan,
    Duration criticalPathDuration,
    List<TraceSpanSummary> errorSpans,
    List<TraceSpanSummary> latencyHotspots,
    List<TraceDependencyEdge> dependencyEdges,
    DeepLink deepLink,
    String versionTag,
    String podName) {}
