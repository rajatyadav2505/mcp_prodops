package com.prodops.controltower.mcp.domain.model;

import java.util.List;
import java.util.Map;

public record ServiceCatalogEntry(
    String serviceId,
    String displayName,
    String cluster,
    String namespace,
    String workloadName,
    WorkloadKind workloadKind,
    String serviceName,
    String criticality,
    String ownerTeam,
    List<String> dashboardUids,
    List<SloTarget> sloTargets,
    List<String> runbookLinks,
    Map<String, String> promqlTemplates) {}
