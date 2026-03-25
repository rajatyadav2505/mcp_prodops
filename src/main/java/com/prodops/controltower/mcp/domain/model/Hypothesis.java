package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record Hypothesis(
    String statement,
    double confidence,
    String rationale,
    List<String> supportingEvidenceIds,
    List<String> counterEvidenceIds) {}
