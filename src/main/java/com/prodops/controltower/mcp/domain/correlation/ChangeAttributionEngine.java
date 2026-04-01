package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.scoring.RiskWeights;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ChangeAttributionEngine {

  private static final List<String> DEFAULT_VERSION_KEYS =
      List.of("app.kubernetes.io/version", "version", "release");
  private static final Pattern WORD_PATTERN = Pattern.compile("[^a-z0-9]+");

  public ChangeAssessment assess(
      IncidentContext context,
      Instant symptomOnset,
      List<LogErrorSignature> signatures,
      RiskWeights weights) {
    if (context.bitbucketChanges().isEmpty()) {
      return new ChangeAssessment(
          List.of(),
          emptyBreakdown(),
          0.22d,
          List.of("No Bitbucket changes were available in the incident window."));
    }
    List<RootCauseCandidate> candidates =
        context.bitbucketChanges().stream()
            .map(change -> assessChange(context, symptomOnset, signatures, change, weights))
            .sorted(Comparator.comparingDouble(RootCauseCandidate::score).reversed())
            .toList();
    RootCauseCandidate leading = candidates.getFirst();
    double confidence = Math.min(0.95d, 0.25d + (leading.score() / 100.0d) * 0.6d);
    ConfidenceBreakdown breakdown =
        new ConfidenceBreakdown(
            confidence,
            leading.score() / 100.0d,
            leading.causationClass() == CausationClass.CORRELATED_BUT_NOT_CAUSAL ? 0.15d : 0.05d,
            context.logEvents().isEmpty() || context.traceSummaries().isEmpty() ? 0.15d : 0.05d,
            List.of(
                new ConfidenceFactor(
                    "temporal proximity",
                    0.18d,
                    "Change timing was compared to inferred symptom onset."),
                new ConfidenceFactor(
                    "version and file affinity",
                    0.2d,
                    "Running workload version and changed files were checked."),
                new ConfidenceFactor(
                    "log and trace alignment",
                    0.22d,
                    "Novel signatures and failing spans were compared to the change.")),
            leading.whyLeadingSuspect());
    List<String> unknowns = new ArrayList<>();
    if (runningVersion(context) == null) {
      unknowns.add("Running workload version could not be matched to a commit.");
    }
    if (context.traceSummaries().isEmpty()) {
      unknowns.add("No Jaeger traces were available to confirm the first failing service.");
    }
    return new ChangeAssessment(candidates, breakdown, confidence, unknowns);
  }

  private RootCauseCandidate assessChange(
      IncidentContext context,
      Instant symptomOnset,
      List<LogErrorSignature> signatures,
      BitbucketChange change,
      RiskWeights weights) {
    double temporal = temporalProximity(change, symptomOnset);
    double fileAffinity = fileAffinity(change, context);
    double pipeline = pipelineSignal(change);
    double logAlignment = logAlignment(change, signatures);
    double traceAlignment =
        traceAlignment(change, context.traceSummaries(), context.serviceOrWorkload());
    double topology = topologyAlignment(change, context);
    double versionAlignment = versionAlignment(change, context);
    double positiveScore =
        temporal * weights.changeRecencyWeight()
            + fileAffinity * weights.fileAffinityWeight()
            + pipeline * weights.pipelineFailureWeight()
            + logAlignment * weights.logSignatureNoveltyWeight()
            + traceAlignment * weights.traceFailureAlignmentWeight()
            + topology * weights.dependencyPropagationWeight()
            + versionAlignment * 10.0d;

    double negativePenalty = 0.0d;
    List<String> weakening = new ArrayList<>();
    if (symptomOnset != null && symptomOnset.isBefore(changeTime(change))) {
      negativePenalty += weights.evidenceConflictPenalty();
      weakening.add("symptom onset predates the change");
    }
    if (versionAlignment < 0.0d) {
      negativePenalty += weights.evidenceConflictPenalty() * 0.8d;
      weakening.add("running version does not align with the candidate change");
    }
    if (!context.traceSummaries().isEmpty()) {
      TraceSummary firstTrace = context.traceSummaries().getFirst();
      String firstFailingService = firstTrace.firstFailingService();
      if (firstFailingService != null
          && !firstFailingService.isBlank()
          && !firstFailingService.equalsIgnoreCase(change.serviceId())
          && !firstFailingService.equalsIgnoreCase(context.serviceOrWorkload())
          && (firstTrace.rootService() == null
              || !firstFailingService.equalsIgnoreCase(firstTrace.rootService()))) {
        negativePenalty += weights.evidenceConflictPenalty() * 0.6d;
        weakening.add("Jaeger shows a different service failing first");
      }
    }
    if (context.catalogEntry() == null || context.catalogEntry().repoSlug() == null) {
      negativePenalty += weights.uncertaintyPenalty() * 0.5d;
      weakening.add("repo-to-service mapping is incomplete");
    }

    double maxScore =
        weights.changeRecencyWeight()
            + weights.fileAffinityWeight()
            + weights.pipelineFailureWeight()
            + weights.logSignatureNoveltyWeight()
            + weights.traceFailureAlignmentWeight()
            + weights.dependencyPropagationWeight()
            + 10.0d;
    double score =
        Math.max(
            0.0d,
            Math.min(100.0d, Math.round(((positiveScore - negativePenalty) / maxScore) * 100.0d)));
    CausationClass causationClass =
        score >= 68.0d
            ? CausationClass.LIKELY_ROOT_CAUSE
            : score >= 50.0d
                ? CausationClass.LIKELY_CONTRIBUTING_FACTOR
                : score >= 30.0d
                    ? CausationClass.CORRELATED_BUT_NOT_CAUSAL
                    : CausationClass.INSUFFICIENT_EVIDENCE;
    List<String> supporting = new ArrayList<>();
    supporting.add("temporal:" + String.format(Locale.ROOT, "%.2f", temporal));
    supporting.add("file-affinity:" + String.format(Locale.ROOT, "%.2f", fileAffinity));
    supporting.add("pipeline:" + String.format(Locale.ROOT, "%.2f", pipeline));
    supporting.add("logs:" + String.format(Locale.ROOT, "%.2f", logAlignment));
    supporting.add("traces:" + String.format(Locale.ROOT, "%.2f", traceAlignment));
    supporting.add("topology:" + String.format(Locale.ROOT, "%.2f", topology));
    if (versionAlignment > 0.0d) {
      supporting.add("running version aligns with commit " + abbreviate(change.commitSha()));
    }
    return new RootCauseCandidate(
        "change-" + change.changeId(),
        CauseType.CHANGE_REGRESSION,
        causationClass,
        change.serviceId(),
        change.title(),
        whyLeading(change, causationClass, supporting, weakening),
        supporting,
        weakening,
        score,
        Math.min(0.95d, 0.25d + (score / 100.0d) * 0.6d),
        change,
        context.catalogEntry() == null || context.catalogEntry().dependencyServiceIds() == null
            ? List.of()
            : context.catalogEntry().dependencyServiceIds());
  }

  private double temporalProximity(BitbucketChange change, Instant symptomOnset) {
    if (symptomOnset == null) {
      return 0.2d;
    }
    long minutes = Math.abs(Duration.between(changeTime(change), symptomOnset).toMinutes());
    if (minutes <= 15) {
      return 1.0d;
    }
    if (minutes <= 45) {
      return 0.8d;
    }
    if (minutes <= 120) {
      return 0.55d;
    }
    return 0.2d;
  }

  private double fileAffinity(BitbucketChange change, IncidentContext context) {
    List<String> ownershipPaths =
        context.catalogEntry() == null || context.catalogEntry().fileOwnershipPaths() == null
            ? List.of(context.serviceOrWorkload())
            : context.catalogEntry().fileOwnershipPaths();
    return change.changedFiles().stream()
            .anyMatch(
                file ->
                    ownershipPaths.stream()
                        .anyMatch(path -> path != null && !path.isBlank() && file.startsWith(path)))
        ? 1.0d
        : change.changedFiles().stream()
                .anyMatch(
                    file ->
                        file.toLowerCase(Locale.ROOT)
                            .contains(context.serviceOrWorkload().toLowerCase(Locale.ROOT)))
            ? 0.7d
            : 0.2d;
  }

  private double pipelineSignal(BitbucketChange change) {
    if (change.pipelineRuns().isEmpty()) {
      return 0.2d;
    }
    return change.pipelineRuns().stream()
            .anyMatch(
                pipeline ->
                    pipeline.result() != null
                        && pipeline.result().toLowerCase(Locale.ROOT).contains("fail"))
        ? 1.0d
        : 0.4d;
  }

  private double logAlignment(BitbucketChange change, List<LogErrorSignature> signatures) {
    if (signatures.isEmpty()) {
      return 0.2d;
    }
    List<String> keywords = new ArrayList<>(keywords(change.title() + " " + change.description()));
    keywords.addAll(
        change.changedFiles().stream()
            .map(
                file -> {
                  int slash = file.lastIndexOf('/');
                  return slash >= 0 ? file.substring(slash + 1) : file;
                })
            .map(value -> value.replace(".java", "").replace(".yaml", ""))
            .toList());
    long overlaps =
        signatures.stream()
            .filter(
                signature ->
                    keywords.stream()
                        .anyMatch(
                            keyword ->
                                !keyword.isBlank()
                                    && signature
                                        .signature()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(keyword.toLowerCase(Locale.ROOT))))
            .count();
    return overlaps == 0 ? 0.25d : Math.min(1.0d, 0.4d + (overlaps * 0.2d));
  }

  private double traceAlignment(
      BitbucketChange change, List<TraceSummary> traces, String serviceOrWorkload) {
    if (traces.isEmpty()) {
      return 0.2d;
    }
    return traces.stream()
            .anyMatch(
                trace ->
                    serviceOrWorkload.equalsIgnoreCase(trace.rootService())
                        || change.serviceId().equalsIgnoreCase(trace.firstFailingService()))
        ? 0.9d
        : 0.35d;
  }

  private double topologyAlignment(BitbucketChange change, IncidentContext context) {
    if (change.serviceId() != null
        && change.serviceId().equalsIgnoreCase(context.serviceOrWorkload())) {
      return 1.0d;
    }
    return context.catalogEntry() != null
            && context.catalogEntry().dependencyServiceIds() != null
            && context.catalogEntry().dependencyServiceIds().contains(change.serviceId())
        ? 0.6d
        : 0.2d;
  }

  private double versionAlignment(BitbucketChange change, IncidentContext context) {
    String runningVersion = runningVersion(context);
    if (runningVersion == null || runningVersion.isBlank()) {
      return 0.0d;
    }
    String normalizedVersion = runningVersion.toLowerCase(Locale.ROOT);
    String normalizedCommit =
        change.commitSha() == null ? "" : change.commitSha().toLowerCase(Locale.ROOT);
    String shortSha = abbreviate(change.commitSha());
    String shortSha7 = abbreviate(change.commitSha(), 7);
    if (normalizedVersion.contains(shortSha.toLowerCase(Locale.ROOT))
        || normalizedVersion.contains(shortSha7.toLowerCase(Locale.ROOT))
        || (!normalizedCommit.isBlank() && normalizedCommit.startsWith(normalizedVersion))) {
      return 1.0d;
    }
    return runningVersion.contains(change.branch()) ? 0.4d : -0.6d;
  }

  private String runningVersion(IncidentContext context) {
    List<String> versionKeys =
        context.catalogEntry() == null || context.catalogEntry().versionLabelKeys() == null
            ? DEFAULT_VERSION_KEYS
            : context.catalogEntry().versionLabelKeys();
    Map<String, String> labels = context.workload().labels();
    return versionKeys.stream()
        .map(labels::get)
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private List<String> keywords(String value) {
    return WORD_PATTERN
        .splitAsStream(value.toLowerCase(Locale.ROOT))
        .filter(token -> token.length() > 3)
        .distinct()
        .toList();
  }

  private String whyLeading(
      BitbucketChange change,
      CausationClass causationClass,
      List<String> supporting,
      List<String> weakening) {
    return change.title()
        + " is ranked as "
        + causationClass.name().toLowerCase(Locale.ROOT).replace('_', ' ')
        + " because supporting evidence "
        + String.join(", ", supporting)
        + (weakening.isEmpty()
            ? "."
            : " while weakening evidence includes " + String.join(", ", weakening) + ".");
  }

  private Instant changeTime(BitbucketChange change) {
    return change.mergedAt() == null ? change.committedAt() : change.mergedAt();
  }

  private String abbreviate(String commitSha) {
    return abbreviate(commitSha, 8);
  }

  private String abbreviate(String commitSha, int length) {
    if (commitSha == null) {
      return "unknown";
    }
    return commitSha.length() <= length ? commitSha : commitSha.substring(0, length);
  }

  private ConfidenceBreakdown emptyBreakdown() {
    return new ConfidenceBreakdown(
        0.22d,
        0.0d,
        0.0d,
        0.18d,
        List.of(
            new ConfidenceFactor(
                "change data unavailable", 0.0d, "Bitbucket returned no candidate changes.")),
        "No change candidates were available to rank.");
  }

  public record ChangeAssessment(
      List<RootCauseCandidate> candidates,
      ConfidenceBreakdown confidenceBreakdown,
      double confidence,
      List<String> unknowns) {}
}
