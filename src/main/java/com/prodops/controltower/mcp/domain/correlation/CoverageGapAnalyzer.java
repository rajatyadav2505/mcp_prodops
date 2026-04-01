package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.CoverageGapType;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.ObservabilityCoverageGap;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CoverageGapAnalyzer {

  public List<ObservabilityCoverageGap> analyze(IncidentContext context, Instant symptomOnset) {
    List<ObservabilityCoverageGap> gaps = new ArrayList<>();
    if (context.catalogEntry() == null
        || context.catalogEntry().repoSlug() == null
        || context.catalogEntry().repoSlug().isBlank()) {
      gaps.add(
          gap(
              "repo-mapping",
              CoverageGapType.REPO_MAPPING_MISSING,
              "Service catalog lacks Bitbucket repo mapping.",
              "Recent changes can only be correlated heuristically.",
              "Add repo workspace/project/repo metadata and ownership paths for this service.",
              0.12d));
    }
    if (context.catalogEntry() == null
        || context.catalogEntry().versionLabelKeys() == null
        || context.catalogEntry().versionLabelKeys().isEmpty()) {
      gaps.add(
          gap(
              "version-tags",
              CoverageGapType.SERVICE_VERSION_MISSING,
              "No version label keys are configured for the workload.",
              "Running version cannot be matched to a specific commit with confidence.",
              "Expose immutable version labels or image tags on workload metadata.",
              0.14d));
    }
    if (context.traceSummaries().isEmpty()) {
      gaps.add(
          gap(
              "trace-propagation",
              CoverageGapType.TRACE_PROPAGATION_MISSING,
              "No Jaeger traces were available in the analysis window.",
              "First failing dependency and critical path cannot be confirmed.",
              "Ensure traces are emitted with error spans and propagated across dependencies.",
              0.18d));
    }
    if (context.logEvents().isEmpty()) {
      gaps.add(
          gap(
              "log-fields",
              CoverageGapType.LOG_FIELDS_INSUFFICIENT,
              "No Kibana log events were available for the selected service.",
              "Error signatures and novelty detection are unavailable.",
              "Index sanitized service logs with severity, trace id, request id, and version tags.",
              0.15d));
    } else {
      boolean missingTraceIds =
          context.logEvents().stream()
              .map(LogEvent::traceId)
              .allMatch(traceId -> traceId == null || traceId.isBlank());
      if (missingTraceIds) {
        gaps.add(
            gap(
                "trace-id-in-logs",
                CoverageGapType.LOG_FIELDS_INSUFFICIENT,
                "Logs do not carry trace identifiers.",
                "Logs cannot be aligned to failing spans deterministically.",
                "Include trace ids in structured log fields for request-scoped correlation.",
                0.07d));
      }
    }
    if (context.bitbucketChanges().stream().allMatch(change -> change.pipelineRuns().isEmpty())) {
      gaps.add(
          gap(
              "pipeline-metadata",
              CoverageGapType.NO_PIPELINE_METADATA,
              "Bitbucket changes lack pipeline or deployment metadata.",
              "Successful or failed promotion signals are missing from attribution.",
              "Expose read-only pipeline and deployment metadata for merged changes.",
              0.08d));
    }
    if (context.catalogEntry() == null
        || context.catalogEntry().kibanaDataView() == null
        || context.catalogEntry().kibanaDataView().isBlank()) {
      gaps.add(
          gap(
              "data-view",
              CoverageGapType.NO_DATA_VIEW_HINT,
              "No Kibana data view hint is configured.",
              "Log queries may miss the right index pattern or field mapping.",
              "Add a Kibana data view or index-pattern hint to the service catalog.",
              0.05d));
    }
    Instant latestEvidence =
        List.of(
                context.workloadHealth().generatedAt(),
                context.logEvents().stream()
                    .map(LogEvent::observedAt)
                    .max(Instant::compareTo)
                    .orElse(Instant.EPOCH),
                context.traceSummaries().stream()
                    .map(trace -> trace.startTime())
                    .max(Instant::compareTo)
                    .orElse(Instant.EPOCH))
            .stream()
            .max(Instant::compareTo)
            .orElse(Instant.EPOCH);
    if (symptomOnset != null
        && latestEvidence != null
        && latestEvidence.isBefore(symptomOnset.minus(Duration.ofMinutes(10)))) {
      gaps.add(
          gap(
              "evidence-time-gap",
              CoverageGapType.EVIDENCE_TIME_GAP,
              "Evidence sources stop before the inferred symptom onset.",
              "The ranking has to rely on stale signals.",
              "Increase retention or widen read-only query coverage around the incident window.",
              0.1d));
    }
    return gaps;
  }

  private ObservabilityCoverageGap gap(
      String gapId,
      CoverageGapType type,
      String summary,
      String impact,
      String recommendation,
      double penalty) {
    return new ObservabilityCoverageGap(
        gapId, type, summary, impact, recommendation, penalty, null);
  }
}
