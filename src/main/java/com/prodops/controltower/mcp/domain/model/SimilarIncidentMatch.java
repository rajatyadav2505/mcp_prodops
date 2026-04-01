package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record SimilarIncidentMatch(
    String incidentId,
    String title,
    double similarity,
    CausationClass causeClass,
    List<String> similarityReasons,
    Instant occurredAt,
    DeepLink deepLink) {}
