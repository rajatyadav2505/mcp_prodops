package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record PodDiagnostics(
    String cluster,
    String namespace,
    String podName,
    String phase,
    List<String> containerStates,
    int restartCount,
    String lastTerminationReason,
    List<WarningEvent> warningEvents,
    ObjectReference ownerReference,
    LogExcerpt logExcerpt,
    Instant generatedAt,
    DataFreshness dataFreshness) {}
