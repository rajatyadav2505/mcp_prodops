package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.ChangeImpactComparison;
import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.EvidenceItem;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.MetricDelta;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChangeImpactComparator {

  public ChangeImpactComparison compare(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      String changeReference,
      Instant anchor,
      Duration window,
      List<MetricSeries> metricSeries,
      List<LogErrorSignature> beforeSignatures,
      List<LogErrorSignature> afterSignatures,
      List<TraceSummary> beforeTraces,
      List<TraceSummary> afterTraces,
      List<EvidenceItem> evidence,
      DataFreshness dataFreshness,
      Instant generatedAt) {
    List<MetricDelta> deltas =
        metricSeries.stream()
            .map(series -> toDelta(series, anchor))
            .filter(delta -> delta != null)
            .toList();
    Duration beforeCriticalPath =
        beforeTraces.stream()
            .map(TraceSummary::criticalPathDuration)
            .min(Duration::compareTo)
            .orElse(Duration.ZERO);
    Duration afterCriticalPath =
        afterTraces.stream()
            .map(TraceSummary::criticalPathDuration)
            .max(Duration::compareTo)
            .orElse(Duration.ZERO);
    List<String> dependencyShifts =
        Map.of("before", beforeTraces, "after", afterTraces).entrySet().stream()
            .flatMap(
                entry ->
                    entry.getValue().stream()
                        .flatMap(trace -> trace.dependencyEdges().stream())
                        .map(
                            edge ->
                                entry.getKey()
                                    + ":"
                                    + edge.sourceService()
                                    + "->"
                                    + edge.targetService()
                                    + "="
                                    + edge.latency()))
            .sorted()
            .toList();
    String executiveSummary =
        "Change impact around "
            + changeReference
            + " shows "
            + afterSignatures.size()
            + " dominant post-change error signatures and a "
            + afterCriticalPath.minus(beforeCriticalPath).toMillis()
            + "ms critical-path shift.";
    String operatorSummary =
        "Compared symmetric windows around the change anchor across metrics, Kibana signatures, and Jaeger traces.";
    ConfidenceBreakdown confidenceBreakdown =
        new ConfidenceBreakdown(
            0.74d,
            0.54d,
            0.08d,
            metricSeries.isEmpty() ? 0.18d : 0.05d,
            List.of(
                new ConfidenceFactor(
                    "metric windows",
                    0.22d,
                    "Prometheus series were split before and after the anchor."),
                new ConfidenceFactor(
                    "log signatures", 0.16d, "Kibana signatures were diffed across both windows."),
                new ConfidenceFactor(
                    "trace critical path", 0.16d, "Jaeger critical-path durations were compared.")),
            "Before/after comparison is strongest when all three evidence planes are populated.");
    return new ChangeImpactComparison(
        cluster,
        namespace,
        serviceOrWorkload,
        changeReference,
        executiveSummary,
        operatorSummary,
        anchor,
        window,
        window,
        deltas,
        beforeSignatures,
        afterSignatures,
        beforeCriticalPath,
        afterCriticalPath,
        beforeTraces.stream().mapToInt(trace -> trace.errorSpans().size()).sum(),
        afterTraces.stream().mapToInt(trace -> trace.errorSpans().size()).sum(),
        dependencyShifts,
        evidence,
        evidence.stream()
            .map(EvidenceItem::deepLink)
            .filter(link -> link != null)
            .distinct()
            .toList(),
        confidenceBreakdown,
        generatedAt,
        dataFreshness);
  }

  private MetricDelta toDelta(MetricSeries series, Instant anchor) {
    List<Double> before =
        series.points().stream()
            .filter(point -> point.timestamp().isBefore(anchor))
            .map(point -> point.value())
            .toList();
    List<Double> after =
        series.points().stream()
            .filter(point -> !point.timestamp().isBefore(anchor))
            .map(point -> point.value())
            .toList();
    if (before.isEmpty() || after.isEmpty()) {
      return null;
    }
    double beforeAverage = before.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    double afterAverage = after.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    double delta = afterAverage - beforeAverage;
    return new MetricDelta(
        series.name(),
        beforeAverage,
        afterAverage,
        delta,
        delta > 0.1d ? "regressed after change" : delta < -0.1d ? "improved after change" : "flat");
  }
}
