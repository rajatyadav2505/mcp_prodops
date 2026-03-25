package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.adapter.fixture.FixtureScenarioLoader.FixtureMetricBundle;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureMetricsAdapter implements MetricsPort {

  private final FixtureScenarioLoader loader;

  public FixtureMetricsAdapter(FixtureScenarioLoader loader) {
    this.loader = loader;
  }

  @Override
  public List<com.prodops.controltower.mcp.domain.model.MetricValue> goldenSignals(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      java.time.Duration lookback,
      ServiceCatalogEntry catalogEntry) {
    return bundleFor(cluster, namespace, serviceOrWorkload)
        .map(FixtureMetricBundle::goldenSignals)
        .orElse(List.of());
  }

  @Override
  public PromqlExecutionResult instantQuery(String cluster, String query, Instant evaluationTime) {
    List<com.prodops.controltower.mcp.domain.model.MetricSeries> series =
        loader.loadRepository().metrics().stream()
            .filter(bundle -> bundle.cluster().equals(cluster))
            .filter(
                bundle ->
                    query.contains(bundle.scope())
                        || bundle.series().stream()
                            .anyMatch(metric -> metric.query().equals(query)))
            .findFirst()
            .map(FixtureMetricBundle::series)
            .orElse(List.of());
    return new PromqlExecutionResult(
        cluster,
        query,
        evaluationTime,
        series,
        false,
        List.of("Fixture-mode raw PromQL uses deterministic scenario mappings."));
  }

  @Override
  public PromqlExecutionResult rangeQuery(
      String cluster, String query, Instant start, Instant end, java.time.Duration step) {
    List<com.prodops.controltower.mcp.domain.model.MetricSeries> series =
        loader.loadRepository().metrics().stream()
            .filter(bundle -> bundle.cluster().equals(cluster))
            .filter(
                bundle ->
                    query.contains(bundle.scope())
                        || bundle.series().stream()
                            .anyMatch(metric -> metric.query().equals(query)))
            .findFirst()
            .map(FixtureMetricBundle::series)
            .orElse(List.of());
    return new PromqlExecutionResult(
        cluster,
        query,
        end,
        series,
        false,
        List.of("Fixture-mode range PromQL uses deterministic scenario mappings."));
  }

  private java.util.Optional<FixtureMetricBundle> bundleFor(
      String cluster, String namespace, String scope) {
    return loader.loadRepository().metrics().stream()
        .filter(bundle -> bundle.cluster().equals(cluster))
        .filter(bundle -> bundle.namespace().equals(namespace))
        .filter(bundle -> bundle.scope().equals(scope))
        .findFirst();
  }
}
