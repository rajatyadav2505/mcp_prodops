package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record EvidenceNode(
    String nodeId,
    EvidenceSource source,
    Instant observedAt,
    String entity,
    String relation,
    double relevanceScore,
    double confidenceContribution,
    DeepLink deepLink,
    String redactedPayload) {}
