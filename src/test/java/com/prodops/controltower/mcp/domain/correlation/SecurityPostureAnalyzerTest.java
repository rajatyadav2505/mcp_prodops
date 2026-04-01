package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityPostureAnalyzerTest {

  @Test
  void penalizesPrivilegedAndRootLikeSettings() {
    SecurityPostureAnalyzer analyzer = new SecurityPostureAnalyzer();
    WorkloadInfo workload =
        new WorkloadInfo(
            "payments-dev",
            "payments",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            Map.of(),
            Map.of(),
            1,
            1,
            Instant.parse("2026-03-24T12:00:00Z"),
            Instant.parse("2026-03-24T23:30:00Z"),
            "payments-platform",
            "critical",
            "118",
            "registry.example.invalid/payments-api:3b2f1a9",
            Instant.parse("2026-03-20T10:00:00Z"),
            1.0d,
            1024d,
            null,
            null,
            false,
            true,
            true,
            false);

    var analysis = analyzer.analyze(workload);

    assertThat(analysis.score()).isLessThan(60);
    assertThat(analysis.findings()).anyMatch(finding -> finding.title().contains("Privileged"));
  }
}
