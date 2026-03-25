package com.prodops.controltower.mcp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.prodops.controltower.mcp.TestFixtures;
import com.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.prodops.controltower.mcp.domain.model.ObjectReference;
import com.prodops.controltower.mcp.domain.model.PodDiagnostics;
import com.prodops.controltower.mcp.domain.model.PodInfo;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.prodops.controltower.mcp.domain.port.ClusterInventoryPort;
import com.prodops.controltower.mcp.domain.port.DashboardPort;
import com.prodops.controltower.mcp.domain.port.MetricsPort;
import com.prodops.controltower.mcp.domain.port.RiskWeightsPort;
import com.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import com.prodops.controltower.mcp.domain.scoring.RiskScoreEngine;
import com.prodops.controltower.mcp.policy.ScopePolicy;
import com.prodops.controltower.mcp.redaction.RedactionService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

  @Test
  void podDiagnosticsBoundsAndRedactsLogOutput() {
    ClusterInventoryPort clusterInventoryPort = mock(ClusterInventoryPort.class);
    MetricsPort metricsPort = mock(MetricsPort.class);
    DashboardPort dashboardPort = mock(DashboardPort.class);
    ServiceCatalogPort serviceCatalogPort = mock(ServiceCatalogPort.class);
    RiskWeightsPort riskWeightsPort = mock(RiskWeightsPort.class);

    WorkloadInfo workload =
        new WorkloadInfo(
            "payments-dev",
            "payments",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            Map.of("app", "payments-api"),
            Map.of("app", "payments-api"),
            2,
            1,
            Instant.parse("2026-03-24T22:00:00Z"),
            Instant.parse("2026-03-24T23:30:00Z"),
            "payments-platform",
            "critical");
    PodInfo pod =
        new PodInfo(
            "payments-dev",
            "payments",
            "payments-api-abc123",
            "Running",
            false,
            2,
            List.of("app:waiting"),
            "Error",
            Instant.parse("2026-03-24T23:45:00Z"),
            new ObjectReference("payments-dev", "payments", "Deployment", "payments-api"));

    when(clusterInventoryPort.listWorkloads("payments-dev", "payments", null, null))
        .thenReturn(List.of(workload));
    when(clusterInventoryPort.listPodsForWorkload("payments-dev", "payments", workload))
        .thenReturn(List.of(pod));
    when(clusterInventoryPort.listWarningEvents(
            "payments-dev", "payments", "payments-api-abc123", Duration.ofMinutes(60)))
        .thenReturn(
            List.of(
                new WarningEvent(
                    "payments-dev",
                    "payments",
                    "BackOff",
                    "Back-off restarting failed container",
                    "Pod",
                    "payments-api-abc123",
                    3,
                    Instant.parse("2026-03-24T23:50:00Z"),
                    Instant.parse("2026-03-24T23:58:00Z"))));
    when(clusterInventoryPort.getPodLogs(
            eq("payments-dev"),
            eq("payments"),
            eq("payments-api-abc123"),
            eq("app"),
            eq(2),
            eq(Duration.ofMinutes(60))))
        .thenReturn(
            Optional.of(
                new LogExcerpt(
                    "payments-api-abc123",
                    "app",
                    Instant.parse("2026-03-24T23:59:00Z"),
                    List.of(
                        "token=super-secret-token",
                        "authorization: bearer abc.def.ghi",
                        "safe line"),
                    true)));
    when(serviceCatalogPort.findByWorkload(any(), any(), any()))
        .thenReturn(Optional.of(TestFixtures.serviceCatalogEntry()));
    when(riskWeightsPort.getWeights())
        .thenReturn(com.prodops.controltower.mcp.domain.scoring.RiskWeights.defaults());

    InventoryService service =
        new InventoryService(
            clusterInventoryPort,
            metricsPort,
            dashboardPort,
            serviceCatalogPort,
            riskWeightsPort,
            new RiskScoreEngine(),
            new ScopePolicy(
                TestFixtures.prodOpsProperties(
                    Path.of("build/test-catalog.yaml"),
                    Path.of("build/test-risk-weights.yaml"),
                    Path.of("build/test-fixtures"),
                    List.of("scenario_fixture_smoke"))),
            new RedactionService(),
            TestFixtures.fixedClock());

    PodDiagnostics diagnostics =
        service.getPodDiagnostics(
            "payments-dev",
            "payments",
            "payments-api-abc123",
            null,
            true,
            2,
            Duration.ofMinutes(60),
            "alice");

    assertThat(diagnostics.logExcerpt()).isNotNull();
    assertThat(diagnostics.logExcerpt().lines()).hasSize(2);
    assertThat(diagnostics.logExcerpt().lines().getFirst()).isEqualTo("token=[REDACTED]");
    assertThat(diagnostics.logExcerpt().lines().get(1))
        .isEqualTo("authorization: bearer [REDACTED]");
    assertThat(diagnostics.logExcerpt().truncated()).isTrue();
    assertThat(diagnostics.warningEvents()).hasSize(1);
  }
}
