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
            "Bitbucket HTTP API",
            "Kibana or Elasticsearch-compatible search API",
            "Jaeger HTTP API",
            "Curated YAML service catalog and risk weights"),
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
            "NetworkPolicies",
            "Dashboard metadata",
            "Metrics",
            "Bitbucket commits, pull requests, changed files, and pipeline metadata",
            "Kibana log events and signatures",
            "Jaeger traces and spans"),
        List.of(
            "Kubernetes get, list, watch, and pods/log (bounded and redacted)",
            "Prometheus query and query_range",
            "Grafana dashboard search and retrieval",
            "Bitbucket commit, pull-request, diffstat, and pipeline metadata reads",
            "Kibana or Elasticsearch-compatible log search only",
            "Jaeger trace search and detail retrieval"),
        List.of(
            "No create, update, patch, delete, restart, rollout trigger, exec, scale, annotate, cordon, drain, silence, acknowledge, save, import, or edit actions",
            "No Secret reads or Secret-value exposure",
            "No Bitbucket merge, comment, pipeline trigger, or deployment trigger",
            "No Kibana saved-object or alert writes",
            "No Jaeger writes",
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
            "Which Bitbucket change most likely broke payments-api in the last 60 minutes?",
            "Explainable change attribution"),
        new QuestionExample(
            "Show the top Kibana error signatures for upi-recon and correlate them with Jaeger traces.",
            "Cross-plane log and trace investigation"),
        new QuestionExample(
            "What is the real-time SLO burn rate and remaining error budget for payments-api?",
            "SLO status and breach forecasting"),
        new QuestionExample(
            "Did the latest deploy make payments-api better or worse?",
            "Pre/post deployment impact comparison"),
        new QuestionExample(
            "Is the tradex-gateway failure cascading into downstream services?",
            "Dependency and cascade analysis"),
        new QuestionExample(
            "What is the likely blast radius if tradex-gateway keeps failing?",
            "Blast radius estimation"),
        new QuestionExample(
            "Export the full incident timeline for the current payments-api outage.",
            "Incident chronology and post-mortem support"),
        new QuestionExample(
            "Which critical services are closest to SLO risk today?", "Capacity and SLO review"),
        new QuestionExample(
            "Give me a CTO summary of the top five production risks in the last 24 hours.",
            "Executive summary workflow"));
  }
}
