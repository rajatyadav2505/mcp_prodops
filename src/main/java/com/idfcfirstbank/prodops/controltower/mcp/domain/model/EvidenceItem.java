package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record EvidenceItem(
    String id,
    EvidenceSource source,
    EvidenceType type,
    ObjectReference objectReference,
    String title,
    String summary,
    String metricName,
    String query,
    String rawReference,
    Instant observedAt,
    Double numericValue,
    DeepLink deepLink,
    double confidence) {}
