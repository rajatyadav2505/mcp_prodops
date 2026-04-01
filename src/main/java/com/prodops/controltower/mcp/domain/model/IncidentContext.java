package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record IncidentContext(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    WorkloadInfo workload,
    WorkloadHealth workloadHealth,
    List<WarningEvent> warningEvents,
    ServiceCatalogEntry catalogEntry,
    List<BitbucketChange> bitbucketChanges,
    List<LogEvent> logEvents,
    List<TraceSummary> traceSummaries,
    List<HistoricalIncident> historicalIncidents,
    Instant analysisTime,
    DataFreshness dataFreshness) {}
