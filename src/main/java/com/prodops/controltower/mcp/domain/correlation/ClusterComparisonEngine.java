package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.ClusterComparisonResult;
import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.CrossClusterDriftResult;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClusterComparisonEngine {

  public Comparison compare(List<ClusterComparisonResult.ClusterHealthComparison> comparisons) {
    List<String> differences = new ArrayList<>();
    if (comparisons.size() >= 2) {
      ClusterComparisonResult.ClusterHealthComparison first = comparisons.getFirst();
      for (int index = 1; index < comparisons.size(); index++) {
        ClusterComparisonResult.ClusterHealthComparison current = comparisons.get(index);
        if (first.riskLevel() != current.riskLevel()) {
          differences.add(
              first.cluster()
                  + " risk is "
                  + first.riskLevel()
                  + " while "
                  + current.cluster()
                  + " is "
                  + current.riskLevel()
                  + ".");
        }
        if (first.versionTag() != null
            && current.versionTag() != null
            && !first.versionTag().equals(current.versionTag())) {
          differences.add(
              first.cluster()
                  + " runs version "
                  + first.versionTag()
                  + " while "
                  + current.cluster()
                  + " runs "
                  + current.versionTag()
                  + ".");
        }
      }
    }
    return new Comparison(
        differences,
        new ConfidenceBreakdown(
            Math.max(0.42d, Math.min(0.86d, 0.56d + (comparisons.size() * 0.08d))),
            0.38d,
            0.08d,
            comparisons.size() < 2 ? 0.24d : 0.08d,
            List.of(
                new ConfidenceFactor(
                    "scope parity",
                    comparisons.size() < 2 ? 0.0d : 0.16d,
                    "Clusters were compared with a consistent namespace/workload scope."),
                new ConfidenceFactor(
                    "health snapshots",
                    comparisons.isEmpty() ? 0.0d : 0.18d,
                    "Namespace/workload health snapshots were available per cluster.")),
            "Cluster comparison confidence rises with consistent scope and version metadata."));
  }

  public List<CrossClusterDriftResult.DriftItem> drift(WorkloadInfo left, WorkloadInfo right) {
    List<CrossClusterDriftResult.DriftItem> driftItems = new ArrayList<>();
    addIfDifferent(
        driftItems,
        "image",
        left.image(),
        right.image(),
        "Different images can hide release drift.");
    addIfDifferent(
        driftItems,
        "revision",
        left.revision(),
        right.revision(),
        "Different rollout revisions suggest deploy drift.");
    addIfDifferent(
        driftItems,
        "desiredReplicas",
        String.valueOf(left.desiredReplicas()),
        String.valueOf(right.desiredReplicas()),
        "Replica count mismatch can change risk posture and throughput.");
    addIfDifferent(
        driftItems,
        "requestedCpuCores",
        stringify(left.requestedCpuCores()),
        stringify(right.requestedCpuCores()),
        "Resource request drift can change scheduling and capacity behavior.");
    addIfDifferent(
        driftItems,
        "requestedMemoryBytes",
        stringify(left.requestedMemoryBytes()),
        stringify(right.requestedMemoryBytes()),
        "Memory request drift can change OOM and binpacking behavior.");
    addIfDifferent(
        driftItems,
        "runAsNonRoot",
        stringify(left.runAsNonRoot()),
        stringify(right.runAsNonRoot()),
        "Security-context drift can change posture across environments.");
    return driftItems;
  }

  private void addIfDifferent(
      List<CrossClusterDriftResult.DriftItem> driftItems,
      String field,
      String left,
      String right,
      String impact) {
    if (left == null && right == null) {
      return;
    }
    if (java.util.Objects.equals(left, right)) {
      return;
    }
    driftItems.add(new CrossClusterDriftResult.DriftItem(field, left, right, impact));
  }

  private String stringify(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  public record Comparison(List<String> differences, ConfidenceBreakdown confidenceBreakdown) {}
}
