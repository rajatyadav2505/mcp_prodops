package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.ClusterComparisonResult;
import com.prodops.controltower.mcp.domain.model.HealthVerdict;
import com.prodops.controltower.mcp.domain.model.RiskLevel;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClusterComparisonEngineTest {

  @Test
  void surfacesVersionAndRiskDifferencesAcrossClusters() {
    ClusterComparisonEngine engine = new ClusterComparisonEngine();

    var comparison =
        engine.compare(
            List.of(
                new ClusterComparisonResult.ClusterHealthComparison(
                    "payments-dev",
                    "payments/payments-api",
                    HealthVerdict.UNHEALTHY,
                    RiskLevel.HIGH,
                    71.0d,
                    1,
                    1,
                    "3b2f1a9"),
                new ClusterComparisonResult.ClusterHealthComparison(
                    "payments-prod",
                    "payments/payments-api",
                    HealthVerdict.HEALTHY,
                    RiskLevel.LOW,
                    12.0d,
                    1,
                    0,
                    "4c8d000")));

    WorkloadInfo left =
        new WorkloadInfo(
            "payments-dev",
            "payments",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            Map.of(),
            Map.of(),
            3,
            2,
            Instant.parse("2026-03-24T12:00:00Z"),
            Instant.parse("2026-03-24T23:30:00Z"),
            "payments-platform",
            "critical",
            "118",
            "img:a",
            Instant.parse("2026-03-20T10:00:00Z"),
            1.0d,
            1024d,
            2.0d,
            2048d,
            true,
            false,
            false,
            true);
    WorkloadInfo right =
        new WorkloadInfo(
            "payments-prod",
            "payments",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            Map.of(),
            Map.of(),
            4,
            4,
            Instant.parse("2026-03-24T12:00:00Z"),
            Instant.parse("2026-03-24T22:20:00Z"),
            "payments-platform",
            "critical",
            "120",
            "img:b",
            Instant.parse("2026-03-23T09:00:00Z"),
            0.75d,
            768d,
            1.5d,
            1536d,
            true,
            false,
            false,
            true);

    assertThat(comparison.differences()).isNotEmpty();
    assertThat(engine.drift(left, right)).isNotEmpty();
  }
}
