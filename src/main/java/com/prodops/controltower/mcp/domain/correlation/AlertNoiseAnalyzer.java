package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.AlertNoiseAnalysisResult;
import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AlertNoiseAnalyzer {

  public Analysis analyze(List<WarningEvent> warningEvents, WorkloadHealth workloadHealth) {
    List<AlertNoiseAnalysisResult.AlertReasonSummary> reasons =
        warningEvents.stream()
            .collect(Collectors.groupingBy(WarningEvent::reason))
            .entrySet()
            .stream()
            .map(entry -> summarize(entry.getKey(), entry.getValue(), workloadHealth))
            .sorted(
                java.util.Comparator.comparingInt(
                        AlertNoiseAnalysisResult.AlertReasonSummary::occurrences)
                    .reversed())
            .toList();
    int noisy =
        (int) reasons.stream().filter(AlertNoiseAnalysisResult.AlertReasonSummary::noisy).count();
    int actionable =
        (int)
            reasons.stream()
                .filter(AlertNoiseAnalysisResult.AlertReasonSummary::actionable)
                .count();
    double uncertaintyPenalty = warningEvents.isEmpty() ? 0.24d : 0.08d;
    return new Analysis(
        reasons,
        noisy,
        actionable,
        new ConfidenceBreakdown(
            Math.max(0.4d, 0.8d - uncertaintyPenalty),
            0.46d,
            0.09d,
            uncertaintyPenalty,
            List.of(
                new ConfidenceFactor(
                    "warning event clustering",
                    warningEvents.isEmpty() ? 0.0d : 0.22d,
                    "Kubernetes Warning events were deduplicated by reason and object."),
                new ConfidenceFactor(
                    "workload corroboration",
                    workloadHealth.riskScore() > 0 ? 0.18d : 0.0d,
                    "Workload health provided context for noisy versus actionable signals.")),
            "Alert noise confidence improves when warning volume is corroborated by workload risk."));
  }

  private AlertNoiseAnalysisResult.AlertReasonSummary summarize(
      String reason, List<WarningEvent> events, WorkloadHealth workloadHealth) {
    int occurrences = events.stream().mapToInt(WarningEvent::count).sum();
    int deduplicatedObjects =
        (int)
            events.stream()
                .map(event -> event.involvedKind() + "/" + event.involvedName())
                .distinct()
                .count();
    boolean actionable =
        reason.toLowerCase(java.util.Locale.ROOT).contains("backoff")
            || reason.toLowerCase(java.util.Locale.ROOT).contains("deadline")
            || workloadHealth.riskScore() >= 60.0d;
    boolean noisy = occurrences >= 5 && deduplicatedObjects <= 2 && !actionable;
    String rationale =
        noisy
            ? "Repeated warnings concentrated on a small object set without strong cross-plane corroboration."
            : actionable
                ? "Warning reasons align with unhealthy workload symptoms or rollout failures."
                : "Mixed signal with moderate repetition but incomplete corroboration.";
    return new AlertNoiseAnalysisResult.AlertReasonSummary(
        reason, occurrences, deduplicatedObjects, noisy, actionable, rationale);
  }

  public record Analysis(
      List<AlertNoiseAnalysisResult.AlertReasonSummary> reasons,
      int noisyAlertCount,
      int actionableAlertCount,
      ConfidenceBreakdown confidenceBreakdown) {}
}
