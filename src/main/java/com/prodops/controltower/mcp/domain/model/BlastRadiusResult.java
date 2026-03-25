package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record BlastRadiusResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    double riskScore,
    RiskLevel riskLevel,
    double confidence,
    List<BlastRadiusImpact> impacts,
    List<EvidenceItem> evidence,
    List<EvidenceItem> counterevidence,
    List<DeepLink> deepLinks,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
