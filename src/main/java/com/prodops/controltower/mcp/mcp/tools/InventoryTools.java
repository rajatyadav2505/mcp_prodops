package com.prodops.controltower.mcp.mcp.tools;

import com.prodops.controltower.mcp.domain.model.NamespaceHealth;
import com.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.prodops.controltower.mcp.domain.model.PodDiagnostics;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.prodops.controltower.mcp.domain.service.InventoryService;
import com.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.prodops.controltower.mcp.support.ArgumentMap;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.List;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class InventoryTools {

  private final InventoryService inventoryService;
  private final McpInvocationSupport invocationSupport;

  public InventoryTools(InventoryService inventoryService, McpInvocationSupport invocationSupport) {
    this.inventoryService = inventoryService;
    this.invocationSupport = invocationSupport;
  }

  @McpTool(
      name = "list_clusters",
      description = "List configured clusters with environment and metadata.")
  public List<com.prodops.controltower.mcp.domain.model.ClusterInfo> listClusters() {
    return invocationSupport.invoke(
        "tool",
        "list_clusters",
        ArgumentMap.of(),
        () -> inventoryService.listClusters(invocationSupport.identity()));
  }

  @McpTool(
      name = "list_namespaces",
      description = "List namespaces in a cluster with optional team and critical filters.")
  public List<NamespaceInfo> listNamespaces(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Optional owner team filter", required = false) String team,
      @McpToolParam(description = "Return only critical namespaces") boolean criticalOnly) {
    return invocationSupport.invoke(
        "tool",
        "list_namespaces",
        ArgumentMap.of("cluster", cluster, "team", team, "criticalOnly", criticalOnly),
        () ->
            inventoryService.listNamespaces(
                cluster, team, criticalOnly, invocationSupport.identity()));
  }

  @McpTool(
      name = "list_workloads",
      description = "List workloads in a namespace, optionally filtered by kind or labels.")
  public List<WorkloadInfo> listWorkloads(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Optional workload kind", required = false) String kind,
      @McpToolParam(description = "Optional label selector", required = false)
          String labelSelector) {
    return invocationSupport.invoke(
        "tool",
        "list_workloads",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "kind",
            kind,
            "labelSelector",
            labelSelector),
        () ->
            inventoryService.listWorkloads(
                cluster,
                namespace,
                kind == null || kind.isBlank() ? null : WorkloadKind.valueOf(kind.toUpperCase()),
                labelSelector,
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_namespace_health",
      description = "Get concise namespace health with risk score, top issues, and metrics.")
  public NamespaceHealth getNamespaceHealth(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_namespace_health",
        ArgumentMap.of(
            "cluster", cluster, "namespace", namespace, "lookbackMinutes", lookbackMinutes),
        () ->
            inventoryService.getNamespaceHealth(
                cluster,
                namespace,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_workload_health",
      description =
          "Get workload health including replicas, pod states, restarts, warning events, metrics, and dashboards.")
  public WorkloadHealth getWorkloadHealth(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Workload name") String workloadName,
      @McpToolParam(description = "Workload kind", required = false) String workloadKind,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_workload_health",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "workloadName", workloadName,
            "workloadKind", workloadKind,
            "lookbackMinutes", lookbackMinutes),
        () ->
            inventoryService.getWorkloadHealth(
                cluster,
                namespace,
                workloadName,
                workloadKind == null || workloadKind.isBlank()
                    ? WorkloadKind.DEPLOYMENT
                    : WorkloadKind.valueOf(workloadKind.toUpperCase()),
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_pod_diagnostics",
      description =
          "Get pod diagnostics with phase, container states, restarts, warning events, owner references, and optional redacted logs.")
  public PodDiagnostics getPodDiagnostics(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Namespace name") String namespace,
      @McpToolParam(description = "Pod name") String podName,
      @McpToolParam(description = "Optional container name", required = false) String container,
      @McpToolParam(description = "Include redacted logs") boolean includeLogs,
      @McpToolParam(description = "Tail lines limit") @Min(1) int tailLines,
      @McpToolParam(description = "Lookback minutes") @Min(1) int lookbackMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_pod_diagnostics",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "podName", podName,
            "container", container,
            "includeLogs", includeLogs,
            "tailLines", tailLines,
            "lookbackMinutes", lookbackMinutes),
        () ->
            inventoryService.getPodDiagnostics(
                cluster,
                namespace,
                podName,
                container,
                includeLogs,
                tailLines,
                Duration.ofMinutes(lookbackMinutes),
                invocationSupport.identity()));
  }

  @McpTool(
      name = "get_recent_warning_events",
      description = "List recent warning events, optionally narrowed to namespace or workload.")
  public List<WarningEvent> getRecentWarningEvents(
      @McpToolParam(description = "Cluster name") String cluster,
      @McpToolParam(description = "Optional namespace", required = false) String namespace,
      @McpToolParam(description = "Optional workload name", required = false) String workload,
      @McpToolParam(description = "Lookback minutes") @Min(1) int sinceMinutes) {
    return invocationSupport.invoke(
        "tool",
        "get_recent_warning_events",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace",
            namespace,
            "workload",
            workload,
            "sinceMinutes",
            sinceMinutes),
        () ->
            inventoryService.getRecentWarningEvents(
                cluster,
                namespace,
                workload,
                Duration.ofMinutes(sinceMinutes),
                invocationSupport.identity()));
  }
}
