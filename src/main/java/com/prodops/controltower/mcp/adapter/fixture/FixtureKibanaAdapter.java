package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.LogSearchQuery;
import com.prodops.controltower.mcp.domain.port.KibanaLogPort;
import com.prodops.controltower.mcp.redaction.RedactionService;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureKibanaAdapter implements KibanaLogPort {

  private final FixtureScenarioLoader loader;
  private final RedactionService redactionService;

  public FixtureKibanaAdapter(FixtureScenarioLoader loader, RedactionService redactionService) {
    this.loader = loader;
    this.redactionService = redactionService;
  }

  @Override
  public List<LogEvent> searchLogs(LogSearchQuery query) {
    return loader.loadRepository().kibanaLogs().stream()
        .filter(event -> query.cluster().equals(event.cluster()))
        .filter(
            event ->
                query.namespace() == null
                    || query.namespace().isBlank()
                    || query.namespace().equals(event.namespace()))
        .filter(
            event ->
                query.serviceOrWorkload() == null
                    || query.serviceOrWorkload().isBlank()
                    || query.serviceOrWorkload().equals(event.serviceOrWorkload()))
        .filter(event -> query.start() == null || !event.observedAt().isBefore(query.start()))
        .filter(event -> query.end() == null || !event.observedAt().isAfter(query.end()))
        .filter(
            event ->
                query.severity() == null
                    || query.severity().isBlank()
                    || query.severity().equalsIgnoreCase(event.severity()))
        .filter(
            event ->
                query.text() == null
                    || query.text().isBlank()
                    || event.sanitizedMessage().toLowerCase().contains(query.text().toLowerCase()))
        .filter(
            event ->
                query.traceId() == null
                    || query.traceId().isBlank()
                    || query.traceId().equals(event.traceId()))
        .filter(
            event ->
                query.requestId() == null
                    || query.requestId().isBlank()
                    || query.requestId().equals(event.requestId()))
        .filter(
            event ->
                query.versionTag() == null
                    || query.versionTag().isBlank()
                    || query.versionTag().equals(event.versionTag()))
        .sorted(Comparator.comparing(LogEvent::observedAt).reversed())
        .limit(query.limit())
        .map(
            event ->
                new LogEvent(
                    event.cluster(),
                    event.namespace(),
                    event.serviceOrWorkload(),
                    event.podName(),
                    event.container(),
                    event.severity(),
                    event.message(),
                    redactionService.redact(event.sanitizedMessage()),
                    event.exceptionSignature() == null
                        ? null
                        : redactionService.redact(event.exceptionSignature()),
                    event.traceId(),
                    event.requestId(),
                    event.versionTag(),
                    event.observedAt(),
                    event.deepLink()))
        .toList();
  }
}
