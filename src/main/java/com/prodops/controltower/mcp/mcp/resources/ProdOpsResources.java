package com.prodops.controltower.mcp.mcp.resources;

import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.prodops.controltower.mcp.domain.service.InventoryService;
import com.prodops.controltower.mcp.domain.service.ResourceService;
import com.prodops.controltower.mcp.mcp.McpContentSupport;
import com.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.prodops.controltower.mcp.support.ArgumentMap;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

@Service
public class ProdOpsResources {

  private final InventoryService inventoryService;
  private final ResourceService resourceService;
  private final McpInvocationSupport invocationSupport;
  private final McpContentSupport contentSupport;

  public ProdOpsResources(
      InventoryService inventoryService,
      ResourceService resourceService,
      McpInvocationSupport invocationSupport,
      McpContentSupport contentSupport) {
    this.inventoryService = inventoryService;
    this.resourceService = resourceService;
    this.invocationSupport = invocationSupport;
    this.contentSupport = contentSupport;
  }

  @McpResource(
      name = "prodops-clusters",
      title = "Configured clusters",
      uri = "prodops://clusters",
      description = "Configured clusters exposed by the ProdOps control tower.",
      mimeType = "application/json")
  public ReadResourceResult clusters() {
    return invocationSupport.invoke(
        "resource",
        "prodops://clusters",
        ArgumentMap.of(),
        () ->
            contentSupport.jsonResource(
                "prodops://clusters", inventoryService.listClusters(invocationSupport.identity())));
  }

  @McpResource(
      name = "prodops-cluster-namespaces",
      title = "Cluster namespaces",
      uri = "prodops://cluster/{cluster}/namespaces",
      description = "Namespaces in a configured cluster.",
      mimeType = "application/json")
  public ReadResourceResult namespaces(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster) {
    return invocationSupport.invoke(
        "resource",
        "prodops://cluster/{cluster}/namespaces",
        ArgumentMap.of("cluster", cluster),
        () ->
            contentSupport.jsonResource(
                "prodops://cluster/" + cluster + "/namespaces",
                inventoryService.listNamespaces(
                    cluster, null, false, invocationSupport.identity())));
  }

  @McpResource(
      name = "prodops-namespace-workloads",
      title = "Namespace workloads",
      uri = "prodops://cluster/{cluster}/namespace/{namespace}/workloads",
      description = "Workloads in a cluster namespace.",
      mimeType = "application/json")
  public ReadResourceResult workloads(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "namespace", description = "Namespace name", required = true)
          String namespace) {
    return invocationSupport.invoke(
        "resource",
        "prodops://cluster/{cluster}/namespace/{namespace}/workloads",
        ArgumentMap.of("cluster", cluster, "namespace", namespace),
        () ->
            contentSupport.jsonResource(
                "prodops://cluster/" + cluster + "/namespace/" + namespace + "/workloads",
                inventoryService.listWorkloads(
                    cluster, namespace, (WorkloadKind) null, null, invocationSupport.identity())));
  }

  @McpResource(
      name = "prodops-namespace-health",
      title = "Namespace health",
      uri = "prodops://cluster/{cluster}/namespace/{namespace}/health",
      description = "Current namespace health summary.",
      mimeType = "application/json")
  public ReadResourceResult namespaceHealth(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "namespace", description = "Namespace name", required = true)
          String namespace) {
    return invocationSupport.invoke(
        "resource",
        "prodops://cluster/{cluster}/namespace/{namespace}/health",
        ArgumentMap.of("cluster", cluster, "namespace", namespace),
        () ->
            contentSupport.jsonResource(
                "prodops://cluster/" + cluster + "/namespace/" + namespace + "/health",
                resourceService.namespaceHealth(cluster, namespace, invocationSupport.identity())));
  }

  @McpResource(
      name = "prodops-catalog-services",
      title = "Curated services",
      uri = "prodops://catalog/services",
      description = "Curated service catalog entries used for enhanced ProdOps intelligence.",
      mimeType = "application/json")
  public ReadResourceResult catalogServices() {
    return invocationSupport.invoke(
        "resource",
        "prodops://catalog/services",
        ArgumentMap.of(),
        () ->
            contentSupport.jsonResource(
                "prodops://catalog/services", resourceService.catalogServices()));
  }

  @McpResource(
      name = "prodops-catalog-slos",
      title = "Curated SLOs",
      uri = "prodops://catalog/slos",
      description = "Curated SLO targets from the service catalog.",
      mimeType = "application/json")
  public ReadResourceResult catalogSlos() {
    return invocationSupport.invoke(
        "resource",
        "prodops://catalog/slos",
        ArgumentMap.of(),
        () -> contentSupport.jsonResource("prodops://catalog/slos", resourceService.catalogSlos()));
  }

  @McpResource(
      name = "prodops-catalog-dashboards",
      title = "Curated dashboards",
      uri = "prodops://catalog/dashboards",
      description = "Curated dashboard mappings from the service catalog.",
      mimeType = "application/json")
  public ReadResourceResult catalogDashboards() {
    return invocationSupport.invoke(
        "resource",
        "prodops://catalog/dashboards",
        ArgumentMap.of(),
        () ->
            contentSupport.jsonResource(
                "prodops://catalog/dashboards", resourceService.catalogDashboards()));
  }

  @McpResource(
      name = "prodops-catalog-runbooks",
      title = "Curated runbooks",
      uri = "prodops://catalog/runbooks",
      description = "Curated runbook links from the service catalog.",
      mimeType = "application/json")
  public ReadResourceResult catalogRunbooks() {
    return invocationSupport.invoke(
        "resource",
        "prodops://catalog/runbooks",
        ArgumentMap.of(),
        () ->
            contentSupport.jsonResource(
                "prodops://catalog/runbooks", resourceService.catalogRunbooks()));
  }

  @McpResource(
      name = "prodops-governance-read-only",
      title = "Read-only contract",
      uri = "prodops://governance/read-only-contract",
      description = "Explicit read-only safety contract for the ProdOps server.",
      mimeType = "application/json")
  public ReadResourceResult readOnlyContract() {
    return invocationSupport.invoke(
        "resource",
        "prodops://governance/read-only-contract",
        ArgumentMap.of(),
        () ->
            contentSupport.jsonResource(
                "prodops://governance/read-only-contract", resourceService.readOnlyContract()));
  }

  @McpResource(
      name = "prodops-example-questions",
      title = "Example questions",
      uri = "prodops://examples/questions",
      description = "Example Production Support Intelligence questions for MCP-aware clients.",
      mimeType = "application/json")
  public ReadResourceResult exampleQuestions() {
    return invocationSupport.invoke(
        "resource",
        "prodops://examples/questions",
        ArgumentMap.of(),
        () ->
            contentSupport.jsonResource(
                "prodops://examples/questions", resourceService.exampleQuestions()));
  }
}
