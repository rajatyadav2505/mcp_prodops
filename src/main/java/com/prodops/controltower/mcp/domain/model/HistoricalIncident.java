package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record HistoricalIncident(
    String incidentId,
    String title,
    Instant occurredAt,
    String serviceId,
    CauseType causeType,
    IncidentFingerprint fingerprint,
    String operatorSummary,
    List<DeepLink> deepLinks) {}
