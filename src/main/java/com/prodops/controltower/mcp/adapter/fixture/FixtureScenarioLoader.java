package com.prodops.controltower.mcp.adapter.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.ClusterInfo;
import com.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.model.HpaInfo;
import com.prodops.controltower.mcp.domain.model.IngressInfo;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.prodops.controltower.mcp.domain.model.PdbInfo;
import com.prodops.controltower.mcp.domain.model.PodInfo;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.ServiceInfo;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
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
    List<BitbucketChange> bitbucketChanges = new ArrayList<>();
    List<LogEvent> kibanaLogs = new ArrayList<>();
    List<FixtureTraceBundle> traces = new ArrayList<>();
    List<HistoricalIncident> historicalIncidents = new ArrayList<>();
    List<ServiceCatalogEntry> catalogEntries = new ArrayList<>();

    for (String scenarioName : properties.fixture().scenarios()) {
      Path path = Path.of(properties.fixture().basePath(), scenarioName, "scenario.json");
      try {
        FixtureScenarioDocument document =
            objectMapper.readValue(path.toFile(), FixtureScenarioDocument.class);
        clusters.add(document.cluster());
        namespaces.addAll(orEmpty(document.namespaces()));
        workloads.addAll(orEmpty(document.workloads()));
        pods.addAll(orEmpty(document.pods()));
        warningEvents.addAll(orEmpty(document.warningEvents()));
        services.addAll(orEmpty(document.services()));
        ingresses.addAll(orEmpty(document.ingresses()));
        hpas.addAll(orEmpty(document.hpas()));
        pdbs.addAll(orEmpty(document.pdbs()));
        dashboards.addAll(orEmpty(document.dashboards()));
        metrics.addAll(orEmpty(document.metrics()));
        logs.addAll(orEmpty(document.logs()));
        bitbucketChanges.addAll(orEmpty(document.bitbucketChanges()));
        kibanaLogs.addAll(orEmpty(document.kibanaLogs()));
        traces.addAll(orEmpty(document.traces()));
        historicalIncidents.addAll(orEmpty(document.historicalIncidents()));
        catalogEntries.addAll(orEmpty(document.catalogEntries()));
      } catch (IOException exception) {
        throw new IllegalStateException("Failed to load fixture scenario from " + path, exception);
      }
    }

    Instant referenceTime =
        StreamedInstants.of(
                workloads.stream()
                    .map(
                        workload ->
                            workload.updatedAt() == null
                                ? workload.createdAt()
                                : workload.updatedAt())
                    .toList(),
                warningEvents.stream().map(WarningEvent::lastTimestamp).toList(),
                metrics.stream()
                    .flatMap(bundle -> bundle.goldenSignals().stream())
                    .map(MetricValue::observedAt)
                    .toList(),
                metrics.stream()
                    .flatMap(bundle -> bundle.series().stream())
                    .flatMap(series -> series.points().stream())
                    .map(MetricSeriesPoint::timestamp)
                    .toList(),
                logs.stream().map(LogExcerpt::collectedAt).toList(),
                bitbucketChanges.stream()
                    .map(
                        change ->
                            change.mergedAt() == null ? change.committedAt() : change.mergedAt())
                    .toList(),
                kibanaLogs.stream().map(LogEvent::observedAt).toList(),
                traces.stream()
                    .flatMap(bundle -> bundle.traces().stream())
                    .map(TraceSummary::startTime)
                    .toList(),
                historicalIncidents.stream().map(HistoricalIncident::occurredAt).toList())
            .latest();

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
        bitbucketChanges,
        kibanaLogs,
        traces,
        historicalIncidents,
        referenceTime,
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
      List<BitbucketChange> bitbucketChanges,
      List<LogEvent> kibanaLogs,
      List<FixtureTraceBundle> traces,
      List<HistoricalIncident> historicalIncidents,
      List<ServiceCatalogEntry> catalogEntries) {}

  public record FixtureMetricBundle(
      String cluster,
      String namespace,
      String scope,
      List<MetricValue> goldenSignals,
      List<MetricSeries> series) {}

  public record FixtureTraceBundle(
      String cluster, String namespace, String serviceOrWorkload, List<TraceSummary> traces) {}

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
      List<BitbucketChange> bitbucketChanges,
      List<LogEvent> kibanaLogs,
      List<FixtureTraceBundle> traces,
      List<HistoricalIncident> historicalIncidents,
      Instant referenceTime,
      List<ServiceCatalogEntry> catalogEntries) {}

  private <T> List<T> orEmpty(List<T> values) {
    return values == null ? List.of() : values;
  }

  private static final class StreamedInstants {

    private final List<List<Instant>> sources;

    private StreamedInstants(List<List<Instant>> sources) {
      this.sources = sources;
    }

    @SafeVarargs
    static StreamedInstants of(List<Instant>... sources) {
      return new StreamedInstants(List.of(sources));
    }

    private Instant latest() {
      return sources.stream()
          .flatMap(List::stream)
          .filter(instant -> instant != null)
          .max(Instant::compareTo)
          .orElse(Instant.EPOCH);
    }
  }
}
