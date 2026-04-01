package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.IncidentFingerprint;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import com.prodops.controltower.mcp.domain.model.SimilarIncidentMatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class IncidentFingerprintEngine {

  public IncidentFingerprint fingerprint(
      IncidentContext context, List<LogErrorSignature> signatures, RootCauseCandidate primary) {
    String metricPattern =
        context.workloadHealth().coreMetrics().stream()
            .filter(metric -> metric.value() >= 0.65d)
            .map(metric -> metric.name() + ":" + metric.status())
            .sorted()
            .collect(Collectors.joining(","));
    String workloadState =
        "ready="
            + context.workloadHealth().readyReplicas()
            + "/"
            + context.workloadHealth().desiredReplicas()
            + ",restarts="
            + context.workloadHealth().totalRestarts();
    String dependencyPath =
        context.traceSummaries().stream()
            .findFirst()
            .flatMap(trace -> trace.dependencyEdges().stream().findFirst())
            .map(edge -> edge.sourceService() + "->" + edge.targetService())
            .orElse(context.workload().name());
    String spanErrorSignature =
        context.traceSummaries().stream()
            .findFirst()
            .map(trace -> trace.firstFailingService() + "/" + trace.firstFailingSpan())
            .orElse("none");
    return new IncidentFingerprint(
        metricPattern,
        signatures.stream().limit(3).map(LogErrorSignature::signature).toList(),
        spanErrorSignature,
        workloadState,
        dependencyPath,
        primary == null ? CauseType.UNKNOWN : primary.causeType());
  }

  public List<SimilarIncidentMatch> match(
      IncidentFingerprint current, List<HistoricalIncident> historical, int limit) {
    return historical.stream()
        .map(incident -> toMatch(current, incident))
        .sorted(Comparator.comparingDouble(SimilarIncidentMatch::similarity).reversed())
        .limit(limit)
        .toList();
  }

  private SimilarIncidentMatch toMatch(IncidentFingerprint current, HistoricalIncident incident) {
    IncidentFingerprint historical = incident.fingerprint();
    List<String> reasons = new ArrayList<>();
    double score = 0.0d;
    if (current.metricOnsetPattern().equals(historical.metricOnsetPattern())) {
      score += 0.25d;
      reasons.add("metric onset pattern matched");
    }
    if (current.spanErrorSignature().equals(historical.spanErrorSignature())) {
      score += 0.2d;
      reasons.add("trace failure signature matched");
    }
    if (current.workloadStatePattern().equals(historical.workloadStatePattern())) {
      score += 0.15d;
      reasons.add("workload state pattern matched");
    }
    if (current.dependencyPath().equals(historical.dependencyPath())) {
      score += 0.15d;
      reasons.add("dependency path matched");
    }
    Set<String> currentSignatures =
        current.topLogSignatures().stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    Set<String> historicalSignatures =
        historical.topLogSignatures().stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    currentSignatures.retainAll(historicalSignatures);
    if (!currentSignatures.isEmpty()) {
      score += Math.min(0.2d, currentSignatures.size() * 0.08d);
      reasons.add("shared log signatures: " + String.join(", ", currentSignatures));
    }
    if (current.likelyCauseClass() == historical.likelyCauseClass()) {
      score += 0.05d;
      reasons.add("cause class aligned");
    }
    return new SimilarIncidentMatch(
        incident.incidentId(),
        incident.title(),
        Math.min(0.98d, score),
        score >= 0.7d
            ? CausationClass.LIKELY_ROOT_CAUSE
            : score >= 0.45d
                ? CausationClass.LIKELY_CONTRIBUTING_FACTOR
                : CausationClass.CORRELATED_BUT_NOT_CAUSAL,
        reasons,
        incident.occurredAt(),
        incident.deepLinks().stream().findFirst().orElse(null));
  }
}
