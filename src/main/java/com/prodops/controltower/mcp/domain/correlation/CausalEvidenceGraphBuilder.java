package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.CausalEvidenceGraph;
import com.prodops.controltower.mcp.domain.model.EvidenceEdge;
import com.prodops.controltower.mcp.domain.model.EvidenceNode;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CausalEvidenceGraphBuilder {

  public CausalEvidenceGraph build(
      IncidentContext context,
      Instant symptomOnset,
      List<LogErrorSignature> signatures,
      List<RootCauseCandidate> rankedCandidates,
      int maxNodes) {
    final List<EvidenceNode> nodes = new ArrayList<>();
    final List<EvidenceEdge> edges = new ArrayList<>();

    context.warningEvents().stream()
        .sorted(Comparator.comparing(WarningEvent::lastTimestamp))
        .limit(3)
        .forEach(
            event ->
                nodes.add(
                    new EvidenceNode(
                        "event:" + event.reason(),
                        EvidenceSource.KUBERNETES,
                        event.lastTimestamp(),
                        event.involvedName(),
                        "warning-event",
                        0.78d,
                        0.08d,
                        null,
                        event.reason() + ": " + event.message())));

    context.workloadHealth().coreMetrics().stream()
        .filter(metric -> metric.value() >= 0.65d)
        .sorted(Comparator.comparingDouble(MetricValue::value).reversed())
        .limit(4)
        .forEach(
            metric ->
                nodes.add(
                    new EvidenceNode(
                        "metric:" + metric.name(),
                        EvidenceSource.PROMETHEUS,
                        metric.observedAt(),
                        context.workload().name(),
                        "metric-inflection",
                        metric.value(),
                        0.09d,
                        null,
                        metric.name() + "=" + metric.value())));

    signatures.stream()
        .limit(3)
        .forEach(
            signature ->
                nodes.add(
                    new EvidenceNode(
                        "log:" + signature.signature(),
                        EvidenceSource.KIBANA,
                        signature.lastSeen(),
                        context.workload().name(),
                        "log-signature",
                        Math.min(1.0d, signature.count() / 10.0d),
                        0.1d,
                        signature.deepLink(),
                        signature.example())));

    context.traceSummaries().stream()
        .limit(3)
        .forEach(
            trace ->
                nodes.add(
                    new EvidenceNode(
                        "trace:" + trace.traceId(),
                        EvidenceSource.JAEGER,
                        trace.startTime(),
                        trace.rootService(),
                        "trace-failure",
                        trace.error() ? 0.9d : 0.5d,
                        0.1d,
                        trace.deepLink(),
                        trace.firstFailingService() == null
                            ? trace.operation()
                            : trace.firstFailingService() + "/" + trace.firstFailingSpan())));

    context.bitbucketChanges().stream().limit(3).forEach(change -> nodes.add(changeNode(change)));

    if (rankedCandidates != null) {
      rankedCandidates.stream()
          .limit(2)
          .forEach(
              candidate ->
                  nodes.add(
                      new EvidenceNode(
                          "candidate:" + candidate.candidateId(),
                          EvidenceSource.FIXTURE,
                          symptomOnset,
                          candidate.entity(),
                          candidate.causeType().name().toLowerCase(Locale.ROOT),
                          candidate.score() / 100.0d,
                          candidate.confidence(),
                          null,
                          candidate.summary())));
    }

    if (!context.bitbucketChanges().isEmpty() && !context.warningEvents().isEmpty()) {
      BitbucketChange leadingChange = context.bitbucketChanges().getFirst();
      WarningEvent firstEvent = context.warningEvents().getFirst();
      edges.add(
          new EvidenceEdge(
              "change:" + leadingChange.changeId(),
              "event:" + firstEvent.reason(),
              EvidenceSource.BITBUCKET,
              firstEvent.lastTimestamp(),
              "precedes-warning",
              0.74d,
              0.08d,
              leadingChange.deepLinks().stream().findFirst().orElse(null),
              "Recent change landed before Kubernetes warning concentration."));
    }
    if (!context.traceSummaries().isEmpty() && !context.workloadHealth().coreMetrics().isEmpty()) {
      TraceSummary trace = context.traceSummaries().getFirst();
      MetricValue metric = context.workloadHealth().coreMetrics().getFirst();
      edges.add(
          new EvidenceEdge(
              "trace:" + trace.traceId(),
              "metric:" + metric.name(),
              EvidenceSource.JAEGER,
              metric.observedAt(),
              "explains-metric-regression",
              0.71d,
              0.07d,
              trace.deepLink(),
              "Trace failure lines up with degraded golden signal."));
    }

    List<EvidenceNode> limitedNodes =
        nodes.size() > maxNodes
            ? nodes.stream()
                .sorted(Comparator.comparingDouble(EvidenceNode::relevanceScore).reversed())
                .limit(maxNodes)
                .toList()
            : nodes;
    return new CausalEvidenceGraph(limitedNodes, edges);
  }

  private EvidenceNode changeNode(BitbucketChange change) {
    return new EvidenceNode(
        "change:" + change.changeId(),
        EvidenceSource.BITBUCKET,
        change.mergedAt() == null ? change.committedAt() : change.mergedAt(),
        change.serviceId(),
        "recent-change",
        0.8d,
        0.1d,
        change.deepLinks().stream().findFirst().orElse(null),
        change.title());
  }
}
