package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.Map;

public record TraceSearchQuery(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String operation,
    Instant start,
    Instant end,
    boolean errorsOnly,
    String traceId,
    int limit,
    Map<String, String> tags) {}
