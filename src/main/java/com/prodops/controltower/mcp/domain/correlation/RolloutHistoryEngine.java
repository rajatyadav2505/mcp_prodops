package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import com.prodops.controltower.mcp.domain.model.RolloutRevision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RolloutHistoryEngine {

  public List<String> correlate(List<RolloutRevision> revisions, List<MetricSeries> metricSeries) {
    List<String> notes = new ArrayList<>();
    List<RolloutRevision> ordered =
        revisions.stream()
            .sorted(Comparator.comparing(RolloutRevision::rolledOutAt).reversed())
            .toList();
    for (RolloutRevision revision : ordered) {
      String note =
          metricSeries.stream()
              .map(series -> correlateRevision(series, revision))
              .filter(item -> item != null)
              .findFirst()
              .orElse(null);
      if (note != null) {
        notes.add(note);
      }
    }
    return notes;
  }

  private String correlateRevision(MetricSeries series, RolloutRevision revision) {
    List<Double> before =
        series.points().stream()
            .filter(point -> point.timestamp().isBefore(revision.rolledOutAt()))
            .map(MetricSeriesPoint::value)
            .toList();
    List<Double> after =
        series.points().stream()
            .filter(point -> !point.timestamp().isBefore(revision.rolledOutAt()))
            .map(MetricSeriesPoint::value)
            .toList();
    if (before.isEmpty() || after.isEmpty()) {
      return null;
    }
    double beforeAverage = before.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    double afterAverage = after.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    double delta = afterAverage - beforeAverage;
    if (Math.abs(delta) < 0.1d) {
      return null;
    }
    return "Revision "
        + revision.revision()
        + " shifted "
        + series.name()
        + " by "
        + String.format(java.util.Locale.ROOT, "%.2f", delta)
        + " after rollout.";
  }
}
