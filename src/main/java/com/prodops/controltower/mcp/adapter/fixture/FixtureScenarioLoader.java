package com.prodops.controltower.mcp.adapter.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.ClusterInfo;
import com.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.prodops.controltower.mcp.domain.model.HpaInfo;
import com.prodops.controltower.mcp.domain.model.IngressInfo;
import com.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.prodops.controltower.mcp.domain.model.PdbInfo;
import com.prodops.controltower.mcp.domain.model.PodInfo;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.ServiceInfo;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureScenarioLoader {

  private final ProdOpsProperties properties;
  private final ObjectMapper objectMapper;

  public FixtureScenarioLoader(ProdOpsProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public FixtureRepository loadRepository() {
    List<ClusterInfo> clusters = new ArrayList<>();
    List<NamespaceInfo> namespaces = new ArrayList<>();
    List<WorkloadInfo> workloads = new ArrayList<>();
    List<PodInfo> pods = new ArrayList<>();
    List<WarningEvent> warningEvents = new ArrayList<>();
    List<ServiceInfo> services = new ArrayList<>();
    List<IngressInfo> ingresses = new ArrayList<>();
    List<HpaInfo> hpas = new ArrayList<>();
    List<PdbInfo> pdbs = new ArrayList<>();
    List<DashboardInfo> dashboards = new ArrayList<>();
    List<FixtureMetricBundle> metrics = new ArrayList<>();
    List<LogExcerpt> logs = new ArrayList<>();
    List<ServiceCatalogEntry> catalogEntries = new ArrayList<>();

    for (String scenarioName : properties.fixture().scenarios()) {
      Path path = Path.of(properties.fixture().basePath(), scenarioName, "scenario.json");
      try {
        FixtureScenarioDocument document =
            objectMapper.readValue(path.toFile(), FixtureScenarioDocument.class);
        clusters.add(document.cluster());
        namespaces.addAll(document.namespaces());
        workloads.addAll(document.workloads());
        pods.addAll(document.pods());
        warningEvents.addAll(document.warningEvents());
        services.addAll(document.services());
        ingresses.addAll(document.ingresses());
        hpas.addAll(document.hpas());
        pdbs.addAll(document.pdbs());
        dashboards.addAll(document.dashboards());
        metrics.addAll(document.metrics());
        logs.addAll(document.logs());
        catalogEntries.addAll(document.catalogEntries());
      } catch (IOException exception) {
        throw new IllegalStateException("Failed to load fixture scenario from " + path, exception);
      }
    }

    return new FixtureRepository(
        clusters,
        namespaces,
        workloads,
        pods,
        warningEvents,
        services,
        ingresses,
        hpas,
        pdbs,
        dashboards,
        metrics,
        logs,
        catalogEntries);
  }

  public record FixtureScenarioDocument(
      ClusterInfo cluster,
      List<NamespaceInfo> namespaces,
      List<WorkloadInfo> workloads,
      List<PodInfo> pods,
      List<WarningEvent> warningEvents,
      List<ServiceInfo> services,
      List<IngressInfo> ingresses,
      List<HpaInfo> hpas,
      List<PdbInfo> pdbs,
      List<DashboardInfo> dashboards,
      List<FixtureMetricBundle> metrics,
      List<LogExcerpt> logs,
      List<ServiceCatalogEntry> catalogEntries) {}

  public record FixtureMetricBundle(
      String cluster,
      String namespace,
      String scope,
      List<MetricValue> goldenSignals,
      List<MetricSeries> series) {}

  public record FixtureRepository(
      List<ClusterInfo> clusters,
      List<NamespaceInfo> namespaces,
      List<WorkloadInfo> workloads,
      List<PodInfo> pods,
      List<WarningEvent> warningEvents,
      List<ServiceInfo> services,
      List<IngressInfo> ingresses,
      List<HpaInfo> hpas,
      List<PdbInfo> pdbs,
      List<DashboardInfo> dashboards,
      List<FixtureMetricBundle> metrics,
      List<LogExcerpt> logs,
      List<ServiceCatalogEntry> catalogEntries) {}
}
