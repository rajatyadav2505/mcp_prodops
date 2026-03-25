package com.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record CapacityForecastResult(
    String cluster,
    String scope,
    String resource,
    Duration horizon,
    String executiveSummary,
    String operatorSummary,
    double riskScore,
    RiskLevel riskLevel,
    double confidence,
    double currentValue,
    double forecastValue,
    String threshold,
    List<EvidenceItem> evidence,
    List<EvidenceItem> counterevidence,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
