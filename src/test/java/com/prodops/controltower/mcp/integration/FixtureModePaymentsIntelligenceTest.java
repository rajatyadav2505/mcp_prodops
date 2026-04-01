package com.prodops.controltower.mcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.ProdOpsControlTowerMcpApplication;
import com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery;
import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
import com.prodops.controltower.mcp.domain.model.CoverageGapType;
import com.prodops.controltower.mcp.domain.port.BitbucketPort;
import com.prodops.controltower.mcp.domain.service.IntelligenceService;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("fixture")
@SpringBootTest(
    classes = {ProdOpsControlTowerMcpApplication.class, FixturePaymentsTestClockConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "prodops.controltower.catalog.path=src/test/resources/config/service-catalog-test.yaml",
      "prodops.controltower.risk-model.path=src/test/resources/config/risk-weights-test.yaml",
      "prodops.controltower.fixture.base-path=src/test/resources/fixtures",
      "prodops.controltower.fixture.scenarios[0]=scenario_fixture_smoke",
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
      "prodops.controltower.clusters[0].jaeger.timeout=PT30S"
    })
class FixtureModePaymentsIntelligenceTest {

  @Autowired private IntelligenceService intelligenceService;
  @Autowired private BitbucketPort bitbucketPort;
  @Autowired private Clock clock;

  @Test
  void fixtureBitbucketWiringReturnsRecentPaymentsChanges() {
    var changes =
        bitbucketPort.listChanges(
            new BitbucketChangeQuery(
                "payments-dev",
                "payments-api",
                "prodops",
                "payments-api",
                null,
                clock.instant().minus(Duration.ofMinutes(90)),
                clock.instant(),
                10));

    assertThat(changes).isNotEmpty();
    assertThat(changes).anyMatch(change -> change.commitSha().startsWith("3b2f1a9"));
  }

  @Test
  void fixtureRootCauseAnalysisIdentifiesTheLeadingChangeCandidate() {
    var result =
        intelligenceService.getRootCauseAnalysis(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(90), "alice");

    assertThat(result.primarySuspect()).isNotNull();
    assertThat(result.primarySuspect().causeType()).isEqualTo(CauseType.CHANGE_REGRESSION);
    assertThat(result.offendingChangeCandidate()).isNotNull();
    assertThat(result.offendingChangeCandidate().commitSha()).startsWith("3b2f1a9");
    assertThat(result.dossier().executiveSummary()).contains("Primary suspect");
  }

  @Test
  void fixtureChangeAttributionAndSimilarityFlowsReturnCorrelatedEvidence() {
    var attribution =
        intelligenceService.getChangeRegressionAttribution(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(90), "alice");
    var impact =
        intelligenceService.compareChangeImpact(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(30), "3b2f1a9", "alice");
    var similar =
        intelligenceService.findSimilarIncidents(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(90), "alice");
    var gaps =
        intelligenceService.getObservabilityCoverageGaps(
            "payments-dev", "payments", "payments-api", Duration.ofMinutes(90), "alice");

    assertThat(attribution.primarySuspect()).isNotNull();
    assertThat(attribution.primarySuspect().commitSha()).startsWith("3b2f1a9");
    assertThat(attribution.causationClass())
        .isIn(CausationClass.LIKELY_ROOT_CAUSE, CausationClass.LIKELY_CONTRIBUTING_FACTOR);
    assertThat(impact.metricDeltas()).isNotEmpty();
    assertThat(similar.matches()).isNotEmpty();
    assertThat(similar.matches().getFirst().similarity()).isGreaterThan(0.4d);
    assertThat(gaps.gaps())
        .extracting(gap -> gap.type())
        .doesNotContain(
            CoverageGapType.REPO_MAPPING_MISSING, CoverageGapType.TRACE_PROPAGATION_MISSING);
  }
}
