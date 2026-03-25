package com.prodops.controltower.mcp.domain.service;

import com.prodops.controltower.mcp.domain.model.CatalogDashboardReference;
import com.prodops.controltower.mcp.domain.model.CatalogRunbookReference;
import com.prodops.controltower.mcp.domain.model.CatalogSloReference;
import com.prodops.controltower.mcp.domain.model.NamespaceHealth;
import com.prodops.controltower.mcp.domain.model.QuestionExample;
import com.prodops.controltower.mcp.domain.model.ReadOnlyContract;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {

  private final InventoryService inventoryService;
  private final ServiceCatalogViewService serviceCatalogViewService;

  public ResourceService(
      InventoryService inventoryService, ServiceCatalogViewService serviceCatalogViewService) {
    this.inventoryService = inventoryService;
    this.serviceCatalogViewService = serviceCatalogViewService;
  }

  public List<ServiceCatalogEntry> catalogServices() {
    return serviceCatalogViewService.listServices();
  }

  public List<CatalogSloReference> catalogSlos() {
    return serviceCatalogViewService.listServices().stream()
        .map(
            entry ->
                new CatalogSloReference(entry.serviceId(), entry.displayName(), entry.sloTargets()))
        .toList();
  }

  public List<CatalogDashboardReference> catalogDashboards() {
    return serviceCatalogViewService.listServices().stream()
        .map(
            entry ->
                new CatalogDashboardReference(
                    entry.serviceId(), entry.displayName(), entry.dashboardUids()))
        .toList();
  }

  public List<CatalogRunbookReference> catalogRunbooks() {
    return serviceCatalogViewService.listServices().stream()
        .map(
            entry ->
                new CatalogRunbookReference(
                    entry.serviceId(), entry.displayName(), entry.runbookLinks()))
        .toList();
  }

  public NamespaceHealth namespaceHealth(String cluster, String namespace, String identity) {
    return inventoryService.getNamespaceHealth(
        cluster, namespace, Duration.ofMinutes(60), identity);
  }

  public ReadOnlyContract readOnlyContract() {
    return new ReadOnlyContract(
        List.of(
            "Kubernetes API",
            "Prometheus HTTP API",
            "Grafana HTTP API",
            "Curated YAML service catalog"),
        List.of(
            "Namespaces",
            "Deployments",
            "StatefulSets",
            "DaemonSets",
            "Jobs",
            "CronJobs",
            "Pods",
            "Events",
            "Services",
            "Ingress",
            "HPA",
            "PDB",
            "Dashboard metadata",
            "Metrics"),
        List.of("get", "list", "watch", "pods/log (bounded and redacted)"),
        List.of(
            "No create, update, patch, delete, restart, rollout trigger, exec, scale, annotate, cordon, drain, silence, acknowledge, save, import, or edit actions",
            "No Secret reads or Secret-value exposure",
            "No kubectl shelling, exec, or port-forward"),
        List.of(
            "Bearer tokens, passwords, secret-like strings, and credential-bearing connection strings are redacted.",
            "Log excerpts are bounded by policy and sanitized before response assembly."),
        List.of(
            "Only configured clusters are exposed.",
            "Optional namespace allowlists are enforced before any upstream read.",
            "HTTP Origin validation and optional JWT audience validation protect remote mode."));
  }

  public List<QuestionExample> exampleQuestions() {
    return List.of(
        new QuestionExample(
            "Why is payments-api unhealthy in UAT right now?", "Flagship incident correlation"),
        new QuestionExample(
            "Which namespaces show the highest operational risk in the last 60 minutes?",
            "Namespace risk ranking"),
        new QuestionExample(
            "Did the latest rollout correlate with the latency spike in upi-recon?",
            "Change correlation"),
        new QuestionExample(
            "What is the likely blast radius if tradex-gateway keeps failing?",
            "Blast radius estimation"),
        new QuestionExample(
            "Which critical services are closest to SLO risk today?", "Capacity and SLO review"),
        new QuestionExample(
            "Give me a CTO summary of the top five production risks in the last 24 hours.",
            "Executive summary workflow"));
  }
}
