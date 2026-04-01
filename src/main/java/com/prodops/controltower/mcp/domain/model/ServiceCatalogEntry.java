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
    Map<String, String> promqlTemplates,
    String repoWorkspace,
    String repoProject,
    String repoSlug,
    List<String> fileOwnershipPaths,
    String traceServiceName,
    List<String> traceOperationHints,
    String kibanaQuery,
    String kibanaDataView,
    List<String> versionLabelKeys,
    List<String> deploymentLabelKeys,
    List<String> imageTagHints,
    List<String> dependencyServiceIds) {

  public ServiceCatalogEntry {
    dashboardUids = dashboardUids == null ? List.of() : List.copyOf(dashboardUids);
    sloTargets = sloTargets == null ? List.of() : List.copyOf(sloTargets);
    runbookLinks = runbookLinks == null ? List.of() : List.copyOf(runbookLinks);
    promqlTemplates = promqlTemplates == null ? Map.of() : Map.copyOf(promqlTemplates);
    fileOwnershipPaths = fileOwnershipPaths == null ? List.of() : List.copyOf(fileOwnershipPaths);
    traceOperationHints =
        traceOperationHints == null ? List.of() : List.copyOf(traceOperationHints);
    versionLabelKeys = versionLabelKeys == null ? List.of() : List.copyOf(versionLabelKeys);
    deploymentLabelKeys =
        deploymentLabelKeys == null ? List.of() : List.copyOf(deploymentLabelKeys);
    imageTagHints = imageTagHints == null ? List.of() : List.copyOf(imageTagHints);
    dependencyServiceIds =
        dependencyServiceIds == null ? List.of() : List.copyOf(dependencyServiceIds);
  }
}
