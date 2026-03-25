package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record WarningEvent(
    String cluster,
    String namespace,
    String reason,
    String message,
    String involvedKind,
    String involvedName,
    int count,
    Instant firstTimestamp,
    Instant lastTimestamp) {}
