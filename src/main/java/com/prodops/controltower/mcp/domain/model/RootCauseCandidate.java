package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record RootCauseCandidate(
    String candidateId,
    CauseType causeType,
    CausationClass causationClass,
    String entity,
    String summary,
    String whyLeadingSuspect,
    List<String> supportingEvidenceIds,
    List<String> weakeningEvidenceIds,
    double score,
    double confidence,
    BitbucketChange offendingChange,
    List<String> impactedDependencies) {}
