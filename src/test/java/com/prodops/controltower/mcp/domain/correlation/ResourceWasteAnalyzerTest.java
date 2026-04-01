package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourceWasteAnalyzerTest {

  @Test
  void flagsOverProvisionedWorkloadsAndSuggestsSmallerRequests() {
    ResourceWasteAnalyzer analyzer = new ResourceWasteAnalyzer();
    WorkloadInfo workload =
        new WorkloadInfo(
            "payments-dev",
            "payments",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            Map.of("app", "payments-api"),
            Map.of("app", "payments-api"),
            3,
            3,
            Instant.parse("2026-03-24T12:00:00Z"),
            Instant.parse("2026-03-24T23:30:00Z"),
            "payments-platform",
            "critical",
            "118",
            "registry.example.invalid/payments-api:3b2f1a9",
            Instant.parse("2026-03-20T10:00:00Z"),
            1.0d,
            1024d,
            2.0d,
            2048d,
            true,
            false,
            false,
            true);

    var waste =
        analyzer.findWaste(
            List.of(workload),
            Map.of(
                "payments-api", new ResourceWasteAnalyzer.UsageSnapshot(0.12d, 140d, 0.15d, 160d)),
            5);
    var recommendations =
        analyzer.rightSize(
            List.of(workload),
            Map.of(
                "payments-api", new ResourceWasteAnalyzer.UsageSnapshot(0.12d, 140d, 0.15d, 160d)),
            5);

    assertThat(waste).hasSize(1);
    assertThat(recommendations).hasSize(1);
    assertThat(recommendations.getFirst().recommendedCpuCores()).isLessThan(1.0d);
  }
}
