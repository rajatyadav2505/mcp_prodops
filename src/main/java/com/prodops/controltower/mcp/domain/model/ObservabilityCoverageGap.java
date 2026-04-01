package com.prodops.controltower.mcp.domain.model;

public record ObservabilityCoverageGap(
    String gapId,
    CoverageGapType type,
    String summary,
    String impact,
    String recommendation,
    double confidencePenalty,
    DeepLink deepLink) {}
