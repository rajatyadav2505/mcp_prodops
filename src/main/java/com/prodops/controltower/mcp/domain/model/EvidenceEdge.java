package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record EvidenceEdge(
    String fromNodeId,
    String toNodeId,
    EvidenceSource source,
    Instant observedAt,
    String relation,
    double relevanceScore,
    double confidenceContribution,
    DeepLink deepLink,
    String redactedPayload) {}
