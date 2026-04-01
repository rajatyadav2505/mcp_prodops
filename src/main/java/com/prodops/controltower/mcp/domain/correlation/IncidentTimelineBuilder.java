package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.IncidentTimelineResult;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.RolloutRevision;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IncidentTimelineBuilder {

  public List<IncidentTimelineResult.IncidentTimelineEntry> build(
      List<RolloutRevision> revisions,
      List<WarningEvent> warningEvents,
      List<com.prodops.controltower.mcp.domain.model.BitbucketChange> changes,
      List<MetricSeries> metricSeries,
      List<LogErrorSignature> signatures,
      List<TraceSummary> traces,
      int maxEntries) {
    List<IncidentTimelineResult.IncidentTimelineEntry> entries = new ArrayList<>();
    revisions.forEach(
        revision ->
            entries.add(
                new IncidentTimelineResult.IncidentTimelineEntry(
                    revision.rolledOutAt(),
                    "kubernetes",
                    "rollout",
                    "Revision " + revision.revision() + " deployed with image " + revision.image(),
                    0.86d,
                    null)));
    warningEvents.forEach(
        event ->
            entries.add(
                new IncidentTimelineResult.IncidentTimelineEntry(
                    event.lastTimestamp(),
                    "kubernetes",
                    "warning-event",
                    event.reason() + ": " + event.message(),
                    0.78d,
                    null)));
    changes.forEach(
        change ->
            entries.add(
                new IncidentTimelineResult.IncidentTimelineEntry(
                    change.mergedAt() == null ? change.committedAt() : change.mergedAt(),
                    "bitbucket",
                    "change",
                    change.title(),
                    0.74d,
                    change.deepLinks().stream().findFirst().orElse(null))));
    metricSeries.stream()
        .flatMap(
            series ->
                series.points().stream().map(point -> java.util.Map.entry(series.name(), point)))
        .forEach(
            entry ->
                entries.add(
                    new IncidentTimelineResult.IncidentTimelineEntry(
                        entry.getValue().timestamp(),
                        "prometheus",
                        "metric",
                        entry.getKey()
                            + " = "
                            + String.format(
                                java.util.Locale.ROOT, "%.2f", entry.getValue().value()),
                        0.52d,
                        null)));
    signatures.forEach(
        signature ->
            entries.add(
                new IncidentTimelineResult.IncidentTimelineEntry(
                    signature.firstSeen(),
                    "kibana",
                    "log-signature",
                    signature.signature(),
                    signature.confidence(),
                    signature.deepLink())));
    traces.forEach(
        trace ->
            entries.add(
                new IncidentTimelineResult.IncidentTimelineEntry(
                    trace.startTime(),
                    "jaeger",
                    "trace",
                    trace.firstFailingService() == null
                        ? trace.traceId()
                        : trace.firstFailingService() + " failed in trace " + trace.traceId(),
                    trace.error() ? 0.82d : 0.48d,
                    trace.deepLink())));
    return entries.stream()
        .sorted(Comparator.comparing(IncidentTimelineResult.IncidentTimelineEntry::timestamp))
        .limit(maxEntries)
        .toList();
  }
}
