package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record ConfidenceBreakdown(
    double overallConfidence,
    double positiveContribution,
    double negativeContribution,
    double uncertaintyPenalty,
    List<ConfidenceFactor> factors,
    String narrative) {}
