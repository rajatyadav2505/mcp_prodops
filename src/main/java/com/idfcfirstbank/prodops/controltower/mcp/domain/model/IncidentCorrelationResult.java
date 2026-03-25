package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record IncidentCorrelationResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    double riskScore,
    RiskLevel riskLevel,
    double confidence,
    Hypothesis primaryHypothesis,
    List<Hypothesis> alternativeHypotheses,
    List<EvidenceItem> counterevidence,
    List<EvidenceItem> evidence,
    List<DeepLink> deepLinks,
    List<String> nextQuestions,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
