package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record BlastRadiusImpact(
    String surface,
    String rationale,
    double confidence,
    List<String> impactedNamespaces,
    List<String> impactedWorkloads) {}
