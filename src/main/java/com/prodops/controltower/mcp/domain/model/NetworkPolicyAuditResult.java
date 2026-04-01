package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record NetworkPolicyAuditResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    boolean ingressIsolated,
    boolean egressIsolated,
    boolean openExposure,
    List<NetworkPolicyInfo> matchingPolicies,
    List<String> findings,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
