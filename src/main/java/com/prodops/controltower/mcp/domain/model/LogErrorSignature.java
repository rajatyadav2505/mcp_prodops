package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record LogErrorSignature(
    String signature,
    String severity,
    int count,
    Instant firstSeen,
    Instant lastSeen,
    boolean novel,
    String example,
    List<String> traceIds,
    DeepLink deepLink,
    double confidence) {}
