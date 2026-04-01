package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record LogSearchQuery(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    Instant start,
    Instant end,
    String severity,
    String text,
    String traceId,
    String requestId,
    String versionTag,
    String dataView,
    int limit) {}
