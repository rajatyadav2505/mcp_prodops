package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.ToilEstimationResult;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ToilEstimator {

  public List<ToilEstimationResult.ToilSummary> estimate(
      List<WorkloadHealth> workloadHealths, List<WarningEvent> warningEvents) {
    Map<String, Aggregate> aggregates = new LinkedHashMap<>();
    for (WorkloadHealth health : workloadHealths) {
      String key = health.namespace() + "/" + health.workloadName();
      Aggregate aggregate =
          aggregates.computeIfAbsent(
              key, ignored -> new Aggregate(health.namespace(), ownerTeam(health), 0, 0, 0));
      aggregate.restarts += health.totalRestarts();
      aggregate.scalingSignals +=
          (int)
              health.warningEvents().stream()
                  .filter(
                      event ->
                          event.reason().toLowerCase(java.util.Locale.ROOT).contains("scaling"))
                  .count();
    }
    for (WarningEvent event : warningEvents) {
      String key = event.namespace() + "/" + event.involvedName();
      Aggregate aggregate =
          aggregates.computeIfAbsent(
              key, ignored -> new Aggregate(event.namespace(), "unknown", 0, 0, 0));
      aggregate.warningEvents += event.count();
    }
    return aggregates.values().stream()
        .map(
            aggregate ->
                new ToilEstimationResult.ToilSummary(
                    aggregate.scope,
                    aggregate.ownerTeam,
                    aggregate.warningEvents,
                    aggregate.restarts,
                    aggregate.scalingSignals,
                    (aggregate.warningEvents * 1.5d)
                        + (aggregate.restarts * 2.0d)
                        + (aggregate.scalingSignals * 1.2d)))
        .sorted(
            java.util.Comparator.comparingDouble(ToilEstimationResult.ToilSummary::toilScore)
                .reversed())
        .toList();
  }

  private String ownerTeam(WorkloadHealth health) {
    return health.pods().stream()
        .map(pod -> pod.labels().get("owner-team"))
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse("unknown");
  }

  private static final class Aggregate {
    private final String scope;
    private final String ownerTeam;
    private int warningEvents;
    private int restarts;
    private int scalingSignals;

    private Aggregate(
        String scope, String ownerTeam, int warningEvents, int restarts, int scalingSignals) {
      this.scope = scope;
      this.ownerTeam = ownerTeam;
      this.warningEvents = warningEvents;
      this.restarts = restarts;
      this.scalingSignals = scalingSignals;
    }
  }
}
