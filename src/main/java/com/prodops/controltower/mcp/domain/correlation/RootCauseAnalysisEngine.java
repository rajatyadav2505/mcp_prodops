package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.ObservabilityCoverageGap;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import com.prodops.controltower.mcp.domain.scoring.RiskWeights;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class RootCauseAnalysisEngine {

  public Analysis assess(
      IncidentContext context,
      List<LogErrorSignature> signatures,
      ChangeAttributionEngine.ChangeAssessment changeAssessment,
      List<ObservabilityCoverageGap> coverageGaps,
      RiskWeights weights) {
    Instant symptomOnset = inferSymptomOnset(context, signatures);
    List<RootCauseCandidate> candidates = new ArrayList<>();
    candidates.addAll(changeAssessment.candidates());
    RootCauseCandidate rolloutCandidate = rolloutCandidate(context, symptomOnset);
    RootCauseCandidate crashLoopCandidate = crashLoopCandidate(context);
    RootCauseCandidate dependencyCandidate = dependencyCandidate(context);
    RootCauseCandidate saturationCandidate = saturationCandidate(context);
    RootCauseCandidate observabilityCandidate =
        observabilityArtifactCandidate(context, coverageGaps);
    Stream.of(
            rolloutCandidate,
            crashLoopCandidate,
            dependencyCandidate,
            saturationCandidate,
            observabilityCandidate)
        .filter(Objects::nonNull)
        .filter(candidate -> candidate != null)
        .forEach(candidates::add);
    candidates =
        candidates.stream()
            .sorted(Comparator.comparingDouble(RootCauseCandidate::score).reversed())
            .toList();
    RootCauseCandidate primary = candidates.isEmpty() ? null : candidates.getFirst();
    List<RootCauseCandidate> alternates = candidates.stream().skip(1).limit(3).toList();
    double gapPenalty =
        coverageGaps.stream().mapToDouble(ObservabilityCoverageGap::confidencePenalty).sum();
    double confidence =
        primary == null
            ? 0.18d
            : Math.max(
                0.08d, Math.min(0.96d, 0.28d + (primary.score() / 100.0d) * 0.55d - gapPenalty));
    List<String> supporting = primary == null ? List.of() : primary.supportingEvidenceIds();
    List<String> weakening =
        primary == null
            ? List.of("No candidate received enough supporting evidence.")
            : primary.weakeningEvidenceIds();
    List<String> unknowns = new ArrayList<>(changeAssessment.unknowns());
    coverageGaps.stream().map(ObservabilityCoverageGap::summary).forEach(unknowns::add);
    ConfidenceBreakdown breakdown =
        new ConfidenceBreakdown(
            confidence,
            primary == null ? 0.0d : primary.score() / 100.0d,
            weakening.isEmpty() ? 0.06d : 0.14d,
            gapPenalty,
            List.of(
                new ConfidenceFactor(
                    "cross-plane agreement",
                    0.2d,
                    "Kubernetes, metrics, logs, and traces were compared together."),
                new ConfidenceFactor(
                    "change attribution",
                    changeAssessment.confidence(),
                    "Recent Bitbucket changes were ranked deterministically."),
                new ConfidenceFactor(
                    "coverage gaps", -gapPenalty, "Missing evidence sources reduce certainty.")),
            primary == null
                ? "Evidence is too sparse to make a strong claim."
                : primary.whyLeadingSuspect());
    return new Analysis(
        symptomOnset, primary, alternates, breakdown, confidence, supporting, weakening, unknowns);
  }

  public Instant inferSymptomOnset(IncidentContext context, List<LogErrorSignature> signatures) {
    List<Instant> signalTimes = new ArrayList<>();
    context.workloadHealth().coreMetrics().stream()
        .filter(metric -> metric.value() >= 0.7d)
        .map(MetricValue::observedAt)
        .forEach(signalTimes::add);
    context.warningEvents().stream().map(event -> event.firstTimestamp()).forEach(signalTimes::add);
    signatures.stream().map(LogErrorSignature::firstSeen).forEach(signalTimes::add);
    context.traceSummaries().stream().map(trace -> trace.startTime()).forEach(signalTimes::add);
    return signalTimes.stream().min(Instant::compareTo).orElse(context.analysisTime());
  }

  private RootCauseCandidate rolloutCandidate(IncidentContext context, Instant symptomOnset) {
    List<String> weakening = new ArrayList<>();
    boolean progressDeadline =
        context.warningEvents().stream()
            .anyMatch(
                event -> event.reason().toLowerCase(Locale.ROOT).contains("progressdeadline"));
    boolean backoff =
        context.warningEvents().stream()
            .anyMatch(event -> event.reason().toLowerCase(Locale.ROOT).contains("backoff"));
    boolean replicaGap =
        context.workloadHealth().readyReplicas() < context.workloadHealth().desiredReplicas();
    boolean restartPressure = context.workloadHealth().totalRestarts() > 0;
    boolean rolloutFresh = context.workloadHealth().rolloutAge().toMinutes() <= 90;
    if (!rolloutFresh || (!progressDeadline && !backoff && !replicaGap && !restartPressure)) {
      return null;
    }
    double score =
        32.0d
            + (context.workloadHealth().rolloutAge().toMinutes() <= 30 ? 10.0d : 4.0d)
            + (progressDeadline ? 18.0d : 0.0d)
            + (backoff ? 12.0d : 0.0d)
            + (replicaGap ? 12.0d : 0.0d)
            + Math.min(10.0d, context.workloadHealth().totalRestarts() * 2.0d);
    String firstFailingService =
        context.traceSummaries().isEmpty()
            ? null
            : context.traceSummaries().getFirst().firstFailingService();
    if (firstFailingService != null
        && !firstFailingService.isBlank()
        && !firstFailingService.equalsIgnoreCase(context.serviceOrWorkload())) {
      score = Math.max(0.0d, score - 18.0d);
      weakening.add("Jaeger shows " + firstFailingService + " failing before the focal rollout.");
    }
    score = Math.max(0.0d, Math.min(100.0d, score));
    if (score < 40.0d) {
      return null;
    }
    List<String> supporting = new ArrayList<>();
    supporting.add("symptom-onset=" + symptomOnset);
    if (progressDeadline) {
      supporting.add("progress-deadline");
    }
    if (backoff) {
      supporting.add("backoff-events");
    }
    if (replicaGap) {
      supporting.add("replica-gap");
    }
    if (restartPressure) {
      supporting.add("restart-count=" + context.workloadHealth().totalRestarts());
    }
    return new RootCauseCandidate(
        "rollout",
        CauseType.BAD_ROLLOUT,
        classify(score),
        context.workload().name(),
        "Workload rollout health degraded near symptom onset.",
        "Kubernetes rollout health signals align with the symptom window, but this hypothesis is weakened when traces show another service breaking first.",
        supporting,
        weakening,
        score,
        Math.min(0.86d, 0.3d + (score / 100.0d) * 0.5d),
        null,
        List.of());
  }

  private RootCauseCandidate crashLoopCandidate(IncidentContext context) {
    if (context.workloadHealth().totalRestarts() <= 0
        && context.warningEvents().stream()
            .noneMatch(event -> event.reason().toLowerCase(Locale.ROOT).contains("backoff"))) {
      return null;
    }
    double score =
        Math.min(
            100.0d,
            52.0d
                + Math.min(24.0d, context.workloadHealth().totalRestarts() * 4.0d)
                + (context.logEvents().isEmpty() ? 0.0d : 8.0d));
    return new RootCauseCandidate(
        "crash-loop",
        CauseType.WORKLOAD_CRASH_LOOP,
        classify(score),
        context.workload().name(),
        "Pod restart and BackOff signals point to workload instability.",
        "Kubernetes restart pressure is directly observed on the impacted workload.",
        List.of("restart-count=" + context.workloadHealth().totalRestarts(), "backoff-events"),
        List.of(),
        score,
        Math.min(0.9d, 0.35d + (score / 100.0d) * 0.5d),
        null,
        List.of());
  }

  private RootCauseCandidate dependencyCandidate(IncidentContext context) {
    if (context.traceSummaries().isEmpty()) {
      return null;
    }
    String firstFailingService = context.traceSummaries().getFirst().firstFailingService();
    if (firstFailingService == null
        || firstFailingService.isBlank()
        || firstFailingService.equalsIgnoreCase(context.serviceOrWorkload())) {
      return null;
    }
    double score = 72.0d;
    return new RootCauseCandidate(
        "dependency-failure",
        CauseType.DEPENDENCY_FAILURE,
        classify(score),
        firstFailingService,
        "Jaeger shows a downstream dependency failing before the focal service.",
        "First failing span and dependency edges point to a different service as the initial break.",
        List.of("first-failing-service=" + firstFailingService, "trace-errors"),
        List.of("recent-change-not-primary"),
        score,
        0.82d,
        null,
        List.of(firstFailingService));
  }

  private RootCauseCandidate saturationCandidate(IncidentContext context) {
    double cpu = metricValue(context.workloadHealth().coreMetrics(), "cpu_saturation_ratio");
    double memory = metricValue(context.workloadHealth().coreMetrics(), "memory_pressure_ratio");
    double latency = metricValue(context.workloadHealth().coreMetrics(), "latency_slo_ratio");
    if (cpu < 0.75d && memory < 0.75d) {
      return null;
    }
    double score = Math.min(100.0d, 48.0d + (cpu * 18.0d) + (memory * 12.0d) + (latency * 10.0d));
    return new RootCauseCandidate(
        "resource-saturation",
        CauseType.RESOURCE_SATURATION,
        classify(score),
        context.workload().name(),
        "High resource pressure is consistent with the latency and error profile.",
        "Prometheus saturation signals remain elevated through the incident window.",
        List.of("cpu=" + cpu, "memory=" + memory, "latency=" + latency),
        List.of(),
        score,
        Math.min(0.84d, 0.28d + (score / 100.0d) * 0.5d),
        null,
        List.of());
  }

  private RootCauseCandidate observabilityArtifactCandidate(
      IncidentContext context, List<ObservabilityCoverageGap> coverageGaps) {
    if (context.warningEvents().size() < 5 || !context.logEvents().isEmpty()) {
      return null;
    }
    double score = Math.max(25.0d, 40.0d - (coverageGaps.size() * 3.0d));
    return new RootCauseCandidate(
        "observability-artifact",
        CauseType.OBSERVABILITY_ARTIFACT,
        classify(score),
        context.workload().name(),
        "Warning volume is high, but corroborating logs and traces are thin.",
        "This may be alert noise or an instrumentation artifact rather than the primary failure.",
        List.of("warning-volume=" + context.warningEvents().size()),
        List.of("missing-log-evidence", "missing-trace-evidence"),
        score,
        0.42d,
        null,
        List.of());
  }

  private CausationClass classify(double score) {
    if (score >= 68.0d) {
      return CausationClass.LIKELY_ROOT_CAUSE;
    }
    if (score >= 50.0d) {
      return CausationClass.LIKELY_CONTRIBUTING_FACTOR;
    }
    if (score >= 30.0d) {
      return CausationClass.CORRELATED_BUT_NOT_CAUSAL;
    }
    return CausationClass.INSUFFICIENT_EVIDENCE;
  }

  private double metricValue(List<MetricValue> metrics, String metricName) {
    return metrics.stream()
        .filter(metric -> metric.name().equals(metricName))
        .mapToDouble(MetricValue::value)
        .findFirst()
        .orElse(0.0d);
  }

  public record Analysis(
      Instant symptomOnset,
      RootCauseCandidate primarySuspect,
      List<RootCauseCandidate> alternateSuspects,
      ConfidenceBreakdown confidenceBreakdown,
      double confidence,
      List<String> supportingEvidence,
      List<String> weakeningEvidence,
      List<String> unknowns) {}
}
