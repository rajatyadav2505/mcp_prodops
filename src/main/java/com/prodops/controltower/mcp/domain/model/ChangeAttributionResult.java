package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ChangeAttributionResult(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    BitbucketChange primarySuspect,
    List<BitbucketChange> alternateSuspects,
    CausationClass causationClass,
    String whyLeadingSuspect,
    List<EvidenceItem> evidence,
    List<EvidenceItem> counterevidence,
    ConfidenceBreakdown confidenceBreakdown,
    double confidence,
    List<String> unknowns,
    List<DeepLink> directLinks,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
