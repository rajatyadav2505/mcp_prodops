package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.domain.model.TraceSearchQuery;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.port.JaegerTracePort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureJaegerAdapter implements JaegerTracePort {

  private final FixtureScenarioLoader loader;

  public FixtureJaegerAdapter(FixtureScenarioLoader loader) {
    this.loader = loader;
  }

  @Override
  public List<TraceSummary> searchTraces(TraceSearchQuery query) {
    return loader.loadRepository().traces().stream()
        .filter(bundle -> query.cluster().equals(bundle.cluster()))
        .filter(
            bundle ->
                query.namespace() == null
                    || query.namespace().isBlank()
                    || query.namespace().equals(bundle.namespace()))
        .filter(
            bundle ->
                query.serviceOrWorkload() == null
                    || query.serviceOrWorkload().isBlank()
                    || query.serviceOrWorkload().equals(bundle.serviceOrWorkload()))
        .flatMap(bundle -> bundle.traces().stream())
        .filter(
            trace ->
                query.traceId() == null
                    || query.traceId().isBlank()
                    || query.traceId().equals(trace.traceId()))
        .filter(
            trace ->
                query.operation() == null
                    || query.operation().isBlank()
                    || query.operation().equals(trace.operation()))
        .filter(trace -> query.start() == null || !trace.startTime().isBefore(query.start()))
        .filter(trace -> query.end() == null || !trace.startTime().isAfter(query.end()))
        .filter(trace -> !query.errorsOnly() || trace.error())
        .sorted(Comparator.comparing(TraceSummary::startTime).reversed())
        .limit(query.limit())
        .toList();
  }

  @Override
  public Optional<TraceSummary> getTrace(String cluster, String traceId) {
    return loader.loadRepository().traces().stream()
        .filter(bundle -> bundle.cluster().equals(cluster))
        .flatMap(bundle -> bundle.traces().stream())
        .filter(trace -> trace.traceId().equals(traceId))
        .findFirst();
  }
}
