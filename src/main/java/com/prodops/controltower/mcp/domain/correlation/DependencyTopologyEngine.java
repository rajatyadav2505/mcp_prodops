package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.CascadingFailureResult;
import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.HealthVerdict;
import com.prodops.controltower.mcp.domain.model.IngressInfo;
import com.prodops.controltower.mcp.domain.model.RiskLevel;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.ServiceDependencyMap;
import com.prodops.controltower.mcp.domain.model.ServiceInfo;
import com.prodops.controltower.mcp.domain.model.TraceDependencyEdge;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DependencyTopologyEngine {

  public Topology build(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      ServiceCatalogEntry targetCatalogEntry,
      List<ServiceCatalogEntry> allCatalogEntries,
      List<ServiceInfo> services,
      List<IngressInfo> ingresses,
      List<TraceSummary> traces,
      Map<String, WorkloadHealth> healthByService,
      int maxNodes) {
    String targetId = serviceId(targetCatalogEntry, serviceOrWorkload);
    Map<String, ServiceDependencyMap.ServiceDependencyNode> nodes = new LinkedHashMap<>();
    List<ServiceDependencyMap.ServiceDependencyEdge> edges = new ArrayList<>();
    nodes.put(
        targetId,
        new ServiceDependencyMap.ServiceDependencyNode(
            targetId,
            targetId,
            "target",
            health(targetId, healthByService),
            risk(targetId, healthByService)));

    Set<String> dependencies =
        new LinkedHashSet<>(
            targetCatalogEntry == null ? List.of() : targetCatalogEntry.dependencyServiceIds());
    Set<String> dependents =
        allCatalogEntries.stream()
            .filter(entry -> entry.dependencyServiceIds().contains(targetId))
            .map(ServiceCatalogEntry::serviceId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    for (TraceSummary trace : traces) {
      for (TraceDependencyEdge edge : trace.dependencyEdges()) {
        if (targetId.equals(edge.sourceService())) {
          dependencies.add(edge.targetService());
        }
        if (targetId.equals(edge.targetService())) {
          dependents.add(edge.sourceService());
        }
      }
    }

    dependencies.stream()
        .limit(maxNodes)
        .forEach(
            dependency ->
                addDependency(
                    targetId,
                    dependency,
                    "depends_on",
                    EvidenceSource.SERVICE_CATALOG,
                    0.72d,
                    nodes,
                    edges,
                    healthByService));
    dependents.stream()
        .limit(maxNodes)
        .forEach(
            dependent ->
                addDependency(
                    dependent,
                    targetId,
                    "calls",
                    EvidenceSource.SERVICE_CATALOG,
                    0.68d,
                    nodes,
                    edges,
                    healthByService));

    Set<String> targetServiceNames =
        services.stream()
            .filter(
                service ->
                    service.name().equals(serviceOrWorkload)
                        || service.selector().values().stream().anyMatch(targetId::equals))
            .map(ServiceInfo::name)
            .collect(java.util.stream.Collectors.toSet());
    ingresses.stream()
        .filter(
            ingress -> ingress.backendServices().stream().anyMatch(targetServiceNames::contains))
        .limit(Math.max(1, maxNodes / 2))
        .forEach(
            ingress -> {
              String ingressId = "ingress:" + ingress.name();
              nodes.putIfAbsent(
                  ingressId,
                  new ServiceDependencyMap.ServiceDependencyNode(
                      ingressId, ingress.name(), "ingress", HealthVerdict.HEALTHY, RiskLevel.LOW));
              edges.add(
                  new ServiceDependencyMap.ServiceDependencyEdge(
                      ingressId,
                      targetId,
                      "routes_to",
                      EvidenceSource.KUBERNETES,
                      0.8d,
                      "Ingress backends route traffic to the target service."));
            });

    double confidence =
        Math.min(
            0.9d,
            0.35d
                + Math.min(0.25d, dependencies.size() * 0.08d)
                + Math.min(0.2d, dependents.size() * 0.06d)
                + (traces.isEmpty() ? 0.0d : 0.12d));
    List<String> limitations = new ArrayList<>();
    if (targetCatalogEntry == null) {
      limitations.add(
          "Service-catalog dependency mapping was unavailable for the target workload.");
    }
    if (traces.isEmpty()) {
      limitations.add(
          "Jaeger dependency edges were unavailable, so topology relied on catalog and ingress hints.");
    }
    return new Topology(
        nodes.values().stream().limit(maxNodes).toList(), edges, confidence, limitations);
  }

  public CascadeAssessment detect(
      String targetId,
      WorkloadHealth targetHealth,
      Topology topology,
      Map<String, WorkloadHealth> healthByService) {
    List<CascadingFailureResult.CascadeImpact> impacts = new ArrayList<>();
    for (ServiceDependencyMap.ServiceDependencyEdge edge : topology.edges()) {
      if (!targetId.equals(edge.fromId())) {
        continue;
      }
      WorkloadHealth dependent = healthByService.get(edge.toId());
      if (dependent == null || dependent.verdict() == HealthVerdict.HEALTHY) {
        continue;
      }
      double severity = Math.min(1.0d, dependent.riskScore() / 100.0d);
      impacts.add(
          new CascadingFailureResult.CascadeImpact(
              edge.toId(),
              "downstream",
              dependent.verdict(),
              dependent.riskLevel(),
              severity,
              "Dependent workload health degraded while the focal service remained unhealthy.",
              dependent.linkedDashboards().stream()
                  .map(
                      dashboard ->
                          new DeepLink(
                              dashboard.title(),
                              EvidenceSource.GRAFANA,
                              dashboard.url(),
                              "Grafana dashboard"))
                  .toList()));
    }
    boolean cascading = targetHealth.verdict() != HealthVerdict.HEALTHY && !impacts.isEmpty();
    return new CascadeAssessment(cascading, impacts);
  }

  private void addDependency(
      String fromId,
      String toId,
      String type,
      EvidenceSource source,
      double confidence,
      Map<String, ServiceDependencyMap.ServiceDependencyNode> nodes,
      List<ServiceDependencyMap.ServiceDependencyEdge> edges,
      Map<String, WorkloadHealth> healthByService) {
    nodes.putIfAbsent(
        fromId,
        new ServiceDependencyMap.ServiceDependencyNode(
            fromId,
            fromId,
            "neighbor",
            health(fromId, healthByService),
            risk(fromId, healthByService)));
    nodes.putIfAbsent(
        toId,
        new ServiceDependencyMap.ServiceDependencyNode(
            toId, toId, "neighbor", health(toId, healthByService), risk(toId, healthByService)));
    edges.add(
        new ServiceDependencyMap.ServiceDependencyEdge(
            fromId,
            toId,
            type,
            source,
            confidence,
            "Dependency edge inferred from deterministic topology sources."));
  }

  private String serviceId(ServiceCatalogEntry catalogEntry, String serviceOrWorkload) {
    return catalogEntry == null ? serviceOrWorkload : catalogEntry.serviceId();
  }

  private HealthVerdict health(String serviceId, Map<String, WorkloadHealth> healthByService) {
    WorkloadHealth health = healthByService.get(serviceId);
    return health == null ? HealthVerdict.DEGRADED : health.verdict();
  }

  private RiskLevel risk(String serviceId, Map<String, WorkloadHealth> healthByService) {
    WorkloadHealth health = healthByService.get(serviceId);
    return health == null ? RiskLevel.MODERATE : health.riskLevel();
  }

  public record Topology(
      List<ServiceDependencyMap.ServiceDependencyNode> nodes,
      List<ServiceDependencyMap.ServiceDependencyEdge> edges,
      double confidence,
      List<String> limitations) {}

  public record CascadeAssessment(
      boolean cascading, List<CascadingFailureResult.CascadeImpact> impacts) {}
}
