package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.ResourceWasteResult;
import com.prodops.controltower.mcp.domain.model.RightSizingResult;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResourceWasteAnalyzer {

  public List<ResourceWasteResult.ResourceWasteFinding> findWaste(
      List<WorkloadInfo> workloads, Map<String, UsageSnapshot> usageByWorkload, int limit) {
    return workloads.stream()
        .map(workload -> toWasteFinding(workload, usageByWorkload.get(workload.name())))
        .filter(finding -> finding != null)
        .sorted(
            java.util.Comparator.comparingDouble(
                    ResourceWasteResult.ResourceWasteFinding::wasteScore)
                .reversed())
        .limit(limit)
        .toList();
  }

  public List<RightSizingResult.RightSizingRecommendation> rightSize(
      List<WorkloadInfo> workloads, Map<String, UsageSnapshot> usageByWorkload, int limit) {
    return workloads.stream()
        .map(workload -> toRecommendation(workload, usageByWorkload.get(workload.name())))
        .filter(recommendation -> recommendation != null)
        .limit(limit)
        .toList();
  }

  private ResourceWasteResult.ResourceWasteFinding toWasteFinding(
      WorkloadInfo workload, UsageSnapshot usage) {
    if (usage == null || !workload.hasResourceRequests()) {
      return null;
    }
    double cpuUsageRatio =
        workload.requestedCpuCores() == null || workload.requestedCpuCores() == 0.0d
            ? 0.0d
            : usage.p95CpuCores() / workload.requestedCpuCores();
    double memoryUsageRatio =
        workload.requestedMemoryBytes() == null || workload.requestedMemoryBytes() == 0.0d
            ? 0.0d
            : usage.p95MemoryBytes() / workload.requestedMemoryBytes();
    double wasteScore = 1.0d - Math.max(cpuUsageRatio, memoryUsageRatio);
    if (Math.max(cpuUsageRatio, memoryUsageRatio) > 0.2d) {
      return null;
    }
    return new ResourceWasteResult.ResourceWasteFinding(
        workload.name(),
        workload.requestedCpuCores(),
        workload.requestedMemoryBytes(),
        cpuUsageRatio,
        memoryUsageRatio,
        wasteScore,
        "P95 observed usage stayed below 20% of requested resources over the comparison window.");
  }

  private RightSizingResult.RightSizingRecommendation toRecommendation(
      WorkloadInfo workload, UsageSnapshot usage) {
    if (usage == null || !workload.hasResourceRequests()) {
      return null;
    }
    Double recommendedCpu =
        workload.requestedCpuCores() == null
            ? null
            : Math.max(
                0.05d,
                round(
                    workload.requestedCpuCores()
                        * clamp(usage.p95CpuCores() / workload.requestedCpuCores() * 1.3d)));
    Double recommendedMemory =
        workload.requestedMemoryBytes() == null
            ? null
            : Math.max(
                64d * 1024d * 1024d,
                round(
                    workload.requestedMemoryBytes()
                        * clamp(usage.p95MemoryBytes() / workload.requestedMemoryBytes() * 1.3d)));
    if (recommendedCpu == null && recommendedMemory == null) {
      return null;
    }
    return new RightSizingResult.RightSizingRecommendation(
        workload.name(),
        workload.requestedCpuCores(),
        recommendedCpu,
        workload.requestedMemoryBytes(),
        recommendedMemory,
        "Recommendations target roughly 130% of observed P95 usage while preserving a deterministic floor.");
  }

  private double clamp(double ratio) {
    return Math.max(0.25d, Math.min(1.0d, ratio));
  }

  private double round(double value) {
    return Math.round(value * 100.0d) / 100.0d;
  }

  public record UsageSnapshot(
      double currentCpuCores,
      double currentMemoryBytes,
      double p95CpuCores,
      double p95MemoryBytes) {}
}
