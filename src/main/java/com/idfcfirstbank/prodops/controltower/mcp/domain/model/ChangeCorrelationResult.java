package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ChangeCorrelationResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    ChangeCausality causality,
    double confidence,
    double riskScore,
    RiskLevel riskLevel,
    List<ChangeTimelineEntry> timeline,
    List<EvidenceItem> evidence,
    List<EvidenceItem> counterevidence,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
