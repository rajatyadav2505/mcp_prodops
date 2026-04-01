package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.AlertCorrelationResult;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AlertCorrelationEngine {

  public List<AlertCorrelationResult.AlertCorrelationGroup> group(
      List<WarningEvent> warningEvents, int maxGroups) {
    List<WarningEvent> ordered =
        warningEvents.stream().sorted(Comparator.comparing(WarningEvent::lastTimestamp)).toList();
    List<List<WarningEvent>> buckets = new ArrayList<>();
    for (WarningEvent event : ordered) {
      List<WarningEvent> current = buckets.isEmpty() ? null : buckets.getLast();
      if (current == null || !belongs(current.getLast(), event)) {
        current = new ArrayList<>();
        buckets.add(current);
      }
      current.add(event);
    }
    List<AlertCorrelationResult.AlertCorrelationGroup> groups = new ArrayList<>();
    for (int index = 0; index < buckets.size() && groups.size() < maxGroups; index++) {
      List<WarningEvent> bucket = buckets.get(index);
      groups.add(toGroup(index + 1, bucket));
    }
    return groups;
  }

  private boolean belongs(WarningEvent previous, WarningEvent current) {
    Duration gap = Duration.between(previous.lastTimestamp(), current.firstTimestamp());
    return gap.abs().compareTo(Duration.ofMinutes(10)) <= 0
        && (previous.involvedName().equals(current.involvedName())
            || previous.reason().equals(current.reason()));
  }

  private AlertCorrelationResult.AlertCorrelationGroup toGroup(
      int index, List<WarningEvent> bucket) {
    String summary =
        bucket.stream()
                .map(WarningEvent::reason)
                .distinct()
                .sorted()
                .limit(2)
                .reduce((left, right) -> left + ", " + right)
                .orElse("warning event cluster")
            + " around "
            + bucket.getFirst().involvedName();
    return new AlertCorrelationResult.AlertCorrelationGroup(
        "alert-group-" + index,
        summary,
        bucket.getFirst().firstTimestamp(),
        bucket.getLast().lastTimestamp(),
        bucket.stream().map(WarningEvent::reason).distinct().toList(),
        bucket.stream()
            .map(
                event -> event.involvedKind().toLowerCase(Locale.ROOT) + "/" + event.involvedName())
            .distinct()
            .toList(),
        bucket.stream().mapToInt(WarningEvent::count).sum(),
        Math.min(0.88d, 0.45d + (bucket.size() * 0.08d)));
  }
}
