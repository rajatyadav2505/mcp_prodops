package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.ChangeCausality;
import com.prodops.controltower.mcp.domain.model.ChangeTimelineEntry;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChangeCorrelationEngine {

  public CorrelationAssessment assess(
      Instant rolloutTime, List<MetricValue> goldenSignals, List<ChangeTimelineEntry> timeline) {
    List<MetricValue> degradedMetrics =
        goldenSignals.stream().filter(metric -> metric.value() >= 0.7d).toList();
    long metricCount = degradedMetrics.size();
    boolean nearRollout =
        degradedMetrics.stream()
            .anyMatch(
                metric ->
                    Duration.between(rolloutTime, metric.observedAt()).abs().toMinutes() <= 20);
    double confidence =
        metricCount == 0
            ? 0.25d
            : Math.min(0.92d, 0.4d + (metricCount * 0.15d) + (nearRollout ? 0.2d : 0.0d));

    ChangeCausality causality;
    if (nearRollout && metricCount >= 2) {
      causality = ChangeCausality.LIKELY_CAUSAL;
    } else if (metricCount >= 1) {
      causality = ChangeCausality.POSSIBLY_RELATED;
    } else {
      causality = ChangeCausality.UNLIKELY;
    }

    List<ChangeTimelineEntry> orderedTimeline =
        timeline.stream().sorted(Comparator.comparing(ChangeTimelineEntry::timestamp)).toList();
    return new CorrelationAssessment(causality, confidence, orderedTimeline);
  }

  public record CorrelationAssessment(
      ChangeCausality causality, double confidence, List<ChangeTimelineEntry> timeline) {}
}
