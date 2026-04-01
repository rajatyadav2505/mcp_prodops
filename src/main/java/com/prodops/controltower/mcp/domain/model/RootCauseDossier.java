package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record RootCauseDossier(
    String operatorSummary,
    String executiveSummary,
    Instant symptomOnset,
    RootCauseCandidate primarySuspect,
    List<RootCauseCandidate> alternateSuspects,
    BitbucketChange offendingChangeCandidate,
    List<String> impactedDependencies,
    String causalChainSummary,
    ConfidenceBreakdown confidenceBreakdown,
    DataFreshness freshness,
    List<String> limitations,
    List<DeepLink> directLinks) {}
