package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record IncidentFingerprint(
    String metricOnsetPattern,
    List<String> topLogSignatures,
    String spanErrorSignature,
    String workloadStatePattern,
    String dependencyPath,
    CauseType likelyCauseClass) {}
