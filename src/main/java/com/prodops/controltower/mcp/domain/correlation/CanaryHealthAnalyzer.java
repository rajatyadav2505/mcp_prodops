package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.CanaryHealthResult;
import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.PodInfo;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CanaryHealthAnalyzer {

  public Analysis analyze(
      WorkloadHealth workloadHealth, List<LogEvent> logEvents, List<TraceSummary> traceSummaries) {
    List<PodInfo> canaryPods = workloadHealth.pods().stream().filter(this::isCanary).toList();
    if (canaryPods.isEmpty()) {
      return new Analysis(false, null, null, lowConfidence());
    }
    List<PodInfo> stablePods =
        workloadHealth.pods().stream().filter(pod -> !isCanary(pod)).toList();
    CanaryHealthResult.CanaryCohortSummary canary =
        summarize("canary", canaryPods, logEvents, traceSummaries);
    CanaryHealthResult.CanaryCohortSummary stable =
        summarize("stable", stablePods, logEvents, traceSummaries);
    return new Analysis(
        true,
        canary,
        stable,
        new ConfidenceBreakdown(
            stablePods.isEmpty() ? 0.46d : 0.74d,
            0.44d,
            0.09d,
            stablePods.isEmpty() ? 0.19d : 0.08d,
            List.of(
                new ConfidenceFactor(
                    "pod cohort labels",
                    0.18d,
                    "Canary/stable grouping came from pod labels or naming hints."),
                new ConfidenceFactor(
                    "log alignment",
                    logEvents.isEmpty() ? 0.0d : 0.14d,
                    "Error log volume was split by pod cohort."),
                new ConfidenceFactor(
                    "trace alignment",
                    traceSummaries.isEmpty() ? 0.0d : 0.12d,
                    "Trace failures contributed cohort-relative evidence.")),
            "Canary confidence improves when both stable and canary cohorts are observable."));
  }

  private CanaryHealthResult.CanaryCohortSummary summarize(
      String cohort, List<PodInfo> pods, List<LogEvent> logEvents, List<TraceSummary> traces) {
    if (pods == null || pods.isEmpty()) {
      return new CanaryHealthResult.CanaryCohortSummary(cohort, 0, 0, 0, 0, 0, 0.0d);
    }
    List<String> podNames = pods.stream().map(PodInfo::name).toList();
    int readyPods = (int) pods.stream().filter(PodInfo::ready).count();
    int restarts = pods.stream().mapToInt(PodInfo::restartCount).sum();
    int errorLogCount =
        (int) logEvents.stream().filter(event -> podNames.contains(event.podName())).count();
    int traceErrorCount =
        (int)
            traces.stream()
                .filter(trace -> trace.podName() != null && podNames.contains(trace.podName()))
                .count();
    double healthScore =
        Math.max(
            0.0d,
            100.0d
                - ((pods.size() - readyPods) * 18.0d)
                - (restarts * 4.0d)
                - (errorLogCount * 2.0d)
                - (traceErrorCount * 2.0d));
    return new CanaryHealthResult.CanaryCohortSummary(
        cohort, pods.size(), readyPods, restarts, errorLogCount, traceErrorCount, healthScore);
  }

  private boolean isCanary(PodInfo pod) {
    Map<String, String> labels = pod.labels();
    return labels.values().stream()
            .anyMatch(value -> value != null && value.toLowerCase().contains("canary"))
        || pod.name().toLowerCase().contains("canary");
  }

  private ConfidenceBreakdown lowConfidence() {
    return new ConfidenceBreakdown(
        0.38d,
        0.12d,
        0.05d,
        0.22d,
        List.of(
            new ConfidenceFactor(
                "cohort detection",
                0.0d,
                "No canary pod cohort could be identified from labels or naming.")),
        "Canary analysis is intentionally conservative without distinct canary pod markers.");
  }

  public record Analysis(
      boolean canaryDetected,
      CanaryHealthResult.CanaryCohortSummary canary,
      CanaryHealthResult.CanaryCohortSummary stable,
      ConfidenceBreakdown confidenceBreakdown) {}
}
