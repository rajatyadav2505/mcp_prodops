package com.prodops.controltower.mcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.ProdOpsControlTowerMcpApplication;
import com.prodops.controltower.mcp.domain.service.IntelligenceService;
import com.prodops.controltower.mcp.domain.service.InventoryService;
import com.prodops.controltower.mcp.domain.service.ObservabilityService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("fixture")
@SpringBootTest(
    classes = {ProdOpsControlTowerMcpApplication.class, FixturePaymentsTestClockConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "prodops.controltower.catalog.path=src/test/resources/config/service-catalog-test.yaml",
      "prodops.controltower.risk-model.path=src/test/resources/config/risk-weights-test.yaml",
      "prodops.controltower.fixture.base-path=src/test/resources/fixtures",
      "prodops.controltower.fixture.scenarios[0]=scenario_fixture_smoke",
      "prodops.controltower.fixture.scenarios[1]=scenario_fixture_prod_baseline",
      "prodops.controltower.clusters[0].name=payments-dev",
      "prodops.controltower.clusters[0].environment=dev",
      "prodops.controltower.clusters[0].enabled=true",
      "prodops.controltower.clusters[0].namespace-allowlist[0]=payments",
      "prodops.controltower.clusters[0].team-label-key=owner-team",
      "prodops.controltower.clusters[0].criticality-label-key=criticality",
      "prodops.controltower.clusters[0].kubernetes.in-cluster=false",
      "prodops.controltower.clusters[0].kubernetes.logs-enabled=true",
      "prodops.controltower.clusters[0].prometheus.base-url=http://localhost:19090",
      "prodops.controltower.clusters[0].prometheus.timeout=PT30S",
      "prodops.controltower.clusters[0].grafana.base-url=http://localhost:13000",
      "prodops.controltower.clusters[0].grafana.timeout=PT30S",
      "prodops.controltower.clusters[0].bitbucket.base-url=http://localhost:17990",
      "prodops.controltower.clusters[0].bitbucket.workspace=prodops",
      "prodops.controltower.clusters[0].bitbucket.project-key=PAY",
      "prodops.controltower.clusters[0].bitbucket.repo-slug=payments-api",
      "prodops.controltower.clusters[0].bitbucket.browse-base-url=http://localhost:17991/projects/PAY/repos/payments-api",
      "prodops.controltower.clusters[0].bitbucket.timeout=PT30S",
      "prodops.controltower.clusters[0].kibana.base-url=http://localhost:15601",
      "prodops.controltower.clusters[0].kibana.default-data-view=payments-logs-*",
      "prodops.controltower.clusters[0].kibana.elasticsearch-base-url=http://localhost:19200",
      "prodops.controltower.clusters[0].kibana.discover-path=/app/discover",
      "prodops.controltower.clusters[0].kibana.timeout=PT30S",
      "prodops.controltower.clusters[0].jaeger.base-url=http://localhost:16686",
      "prodops.controltower.clusters[0].jaeger.timeout=PT30S",
      "prodops.controltower.clusters[1].name=payments-prod",
      "prodops.controltower.clusters[1].environment=prod",
      "prodops.controltower.clusters[1].enabled=true",
      "prodops.controltower.clusters[1].namespace-allowlist[0]=payments",
      "prodops.controltower.clusters[1].team-label-key=owner-team",
      "prodops.controltower.clusters[1].criticality-label-key=criticality",
      "prodops.controltower.clusters[1].kubernetes.in-cluster=false",
      "prodops.controltower.clusters[1].kubernetes.logs-enabled=true",
      "prodops.controltower.clusters[1].prometheus.base-url=http://localhost:29090",
      "prodops.controltower.clusters[1].prometheus.timeout=PT30S",
      "prodops.controltower.clusters[1].grafana.base-url=http://localhost:23000",
      "prodops.controltower.clusters[1].grafana.timeout=PT30S",
      "prodops.controltower.clusters[1].bitbucket.base-url=http://localhost:27990",
      "prodops.controltower.clusters[1].bitbucket.workspace=prodops",
      "prodops.controltower.clusters[1].bitbucket.project-key=PAY",
      "prodops.controltower.clusters[1].bitbucket.repo-slug=payments-api",
      "prodops.controltower.clusters[1].bitbucket.browse-base-url=http://localhost:27991/projects/PAY/repos/payments-api",
      "prodops.controltower.clusters[1].bitbucket.timeout=PT30S",
      "prodops.controltower.clusters[1].kibana.base-url=http://localhost:25601",
      "prodops.controltower.clusters[1].kibana.default-data-view=payments-logs-*",
      "prodops.controltower.clusters[1].kibana.elasticsearch-base-url=http://localhost:29200",
      "prodops.controltower.clusters[1].kibana.discover-path=/app/discover",
      "prodops.controltower.clusters[1].kibana.timeout=PT30S",
      "prodops.controltower.clusters[1].jaeger.base-url=http://localhost:26686",
      "prodops.controltower.clusters[1].jaeger.timeout=PT30S"
    })
class FixtureModeWaveTwoOperationsTest {

  @Autowired private InventoryService inventoryService;
  @Autowired private ObservabilityService observabilityService;
  @Autowired private IntelligenceService intelligenceService;

  @Test
  void inventoryAndObservabilityWaveTwoFeaturesWorkInFixtureMode() {
    var ingress =
        inventoryService.checkIngressHealth("payments-dev", "payments", "payments-api", "alice");
    var policies =
        inventoryService.checkNetworkPolicies("payments-dev", "payments", "payments-api", "alice");
    var posture =
        inventoryService.securityPostureScan("payments-dev", "payments", "payments-api", "alice");
    var images =
        inventoryService.imageFreshnessCheck(
            "payments-dev", "payments", "payments-api", 2, "alice");
    var patterns =
        observabilityService.searchErrorPatterns(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var anomalies =
        observabilityService.logAnomalySummary(
            "payments-dev",
            "payments",
            "payments-api",
            Duration.ofMinutes(30),
            Duration.ofMinutes(30),
            "alice");

    assertThat(ingress.ingresses()).isNotEmpty();
    assertThat(policies.openExposure()).isFalse();
    assertThat(posture.score()).isGreaterThan(60);
    assertThat(images.staleImageCount()).isGreaterThanOrEqualTo(1);
    assertThat(patterns.matches()).isNotEmpty();
    assertThat(anomalies.recentCount()).isGreaterThan(0);
  }

  @Test
  void intelligenceWaveTwoFeaturesWorkInFixtureMode() {
    var slo =
        intelligenceService.checkSloStatus(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var forecast =
        intelligenceService.sloBreachForecast(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var dependencies =
        intelligenceService.mapServiceDependencies(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var cascade =
        intelligenceService.detectCascadingFailure(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var waste =
        intelligenceService.identifyResourceWaste(
            "payments-dev", "payments", null, Duration.ofMinutes(60), "alice");
    var rightSizing =
        intelligenceService.rightSizingRecommendations(
            "payments-dev", "payments", null, Duration.ofMinutes(60), "alice");
    var rollout =
        intelligenceService.rolloutHistory(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(120), "alice");
    var canary =
        intelligenceService.canaryHealthCheck(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var noise =
        intelligenceService.alertNoiseAnalysis(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var groups =
        intelligenceService.alertCorrelationGroups(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var clusterCompare =
        intelligenceService.compareClusters(
            java.util.List.of("payments-dev", "payments-prod"),
            "payments",
            "payments-api",
            Duration.ofMinutes(60),
            "alice");
    var drift =
        intelligenceService.crossClusterDrift(
            "payments-dev", "payments-prod", "payments", "payments-api", "alice");
    var trend =
        intelligenceService.dailyRiskTrend(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice");
    var timeline =
        intelligenceService.incidentTimelineExport(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(120), "alice");
    var toil =
        intelligenceService.toilEstimation(
            "payments-dev", "payments", Duration.ofMinutes(120), "alice");

    assertThat(slo.leadingRisk()).isNotNull();
    assertThat(forecast.sloName()).isNotNull();
    assertThat(dependencies.edges()).isNotEmpty();
    assertThat(cascade.cascading()).isIn(true, false);
    assertThat(waste.findings()).isNotEmpty();
    assertThat(rightSizing.recommendations()).isNotEmpty();
    assertThat(rollout.revisions()).hasSizeGreaterThanOrEqualTo(1);
    assertThat(canary.canaryDetected()).isTrue();
    assertThat(canary.canary().healthScore()).isLessThan(canary.stable().healthScore());
    assertThat(noise.reasons()).isNotEmpty();
    assertThat(groups.groups()).isNotEmpty();
    assertThat(clusterCompare.clusters()).hasSize(2);
    assertThat(drift.driftItems()).isNotEmpty();
    assertThat(trend.points()).isNotEmpty();
    assertThat(timeline.entries()).isNotEmpty();
    assertThat(toil.summaries()).isNotEmpty();
  }
}
