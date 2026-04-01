package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record LogEvent(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String podName,
    String container,
    String severity,
    String message,
    String sanitizedMessage,
    String exceptionSignature,
    String traceId,
    String requestId,
    String versionTag,
    Instant observedAt,
    DeepLink deepLink) {}
