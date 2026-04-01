package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record RootCauseAnalysisResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    Instant symptomOnset,
    RootCauseCandidate primarySuspect,
    List<RootCauseCandidate> alternateSuspects,
    BitbucketChange offendingChangeCandidate,
    List<String> impactedDependencies,
    CausalEvidenceGraph evidenceGraph,
    ConfidenceBreakdown confidenceBreakdown,
    double confidence,
    List<String> supportingEvidence,
    List<String> weakeningEvidence,
    List<String> unknowns,
    List<ObservabilityCoverageGap> coverageGaps,
    RootCauseDossier dossier,
    List<DeepLink> directLinks,
    List<String> limitations,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
