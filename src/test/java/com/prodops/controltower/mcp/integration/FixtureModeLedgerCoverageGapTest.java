package com.prodops.controltower.mcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.ProdOpsControlTowerMcpApplication;
import com.prodops.controltower.mcp.domain.model.CoverageGapType;
import com.prodops.controltower.mcp.domain.service.IntelligenceService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("fixture")
@SpringBootTest(
    classes = {
      ProdOpsControlTowerMcpApplication.class,
      FixtureModeLedgerCoverageGapTest.LedgerClockConfig.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "prodops.controltower.catalog.path=src/test/resources/config/service-catalog-test.yaml",
      "prodops.controltower.risk-model.path=src/test/resources/config/risk-weights-test.yaml",
      "prodops.controltower.fixture.base-path=fixtures",
      "prodops.controltower.fixture.scenarios[0]=scenario_ledger_observability_gap",
      "prodops.controltower.clusters[0].name=prodops-uat",
      "prodops.controltower.clusters[0].environment=UAT",
      "prodops.controltower.clusters[0].enabled=true",
      "prodops.controltower.clusters[0].namespace-allowlist[0]=ledger-ops",
      "prodops.controltower.clusters[0].team-label-key=owner-team",
      "prodops.controltower.clusters[0].criticality-label-key=criticality",
      "prodops.controltower.clusters[0].kubernetes.in-cluster=false",
      "prodops.controltower.clusters[0].kubernetes.logs-enabled=true",
      "prodops.controltower.clusters[0].prometheus.base-url=http://localhost:19090",
      "prodops.controltower.clusters[0].prometheus.timeout=PT30S",
      "prodops.controltower.clusters[0].grafana.base-url=http://localhost:13000",
      "prodops.controltower.clusters[0].grafana.timeout=PT30S",
      "prodops.controltower.clusters[0].bitbucket.base-url=https://bitbucket.example.internal",
      "prodops.controltower.clusters[0].bitbucket.workspace=prodops",
      "prodops.controltower.clusters[0].bitbucket.project-key=LED",
      "prodops.controltower.clusters[0].bitbucket.repo-slug=ledger-sync",
      "prodops.controltower.clusters[0].bitbucket.browse-base-url=https://bitbucket.example.internal/projects/LED/repos/ledger-sync",
      "prodops.controltower.clusters[0].bitbucket.timeout=PT30S",
      "prodops.controltower.clusters[0].kibana.base-url=https://kibana.example.internal",
      "prodops.controltower.clusters[0].kibana.default-data-view=ledger-logs-*",
      "prodops.controltower.clusters[0].kibana.elasticsearch-base-url=https://elasticsearch.example.internal",
      "prodops.controltower.clusters[0].kibana.discover-path=/app/discover",
      "prodops.controltower.clusters[0].kibana.timeout=PT30S",
      "prodops.controltower.clusters[0].jaeger.base-url=https://jaeger.example.internal",
      "prodops.controltower.clusters[0].jaeger.timeout=PT30S"
    })
class FixtureModeLedgerCoverageGapTest {

  @TestConfiguration(proxyBeanMethods = false)
  static class LedgerClockConfig {

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2026-03-25T10:35:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired private IntelligenceService intelligenceService;

  @Test
  void lowConfidenceScenarioReportsCoverageGapsInsteadOfFalseCertainty() {
    var rootCause =
        intelligenceService.getRootCauseAnalysis(
            "prodops-uat", "ledger-ops", "ledger-sync", Duration.ofMinutes(90), "alice");
    var gaps =
        intelligenceService.getObservabilityCoverageGaps(
            "prodops-uat", "ledger-ops", "ledger-sync", Duration.ofMinutes(90), "alice");

    assertThat(rootCause.confidence()).isLessThan(0.25d);
    assertThat(rootCause.coverageGaps()).isNotEmpty();
    assertThat(rootCause.dossier().limitations()).isNotEmpty();
    assertThat(gaps.executiveSummary()).contains("coverage gaps");
    assertThat(gaps.gaps())
        .extracting(gap -> gap.type())
        .contains(
            CoverageGapType.REPO_MAPPING_MISSING,
            CoverageGapType.SERVICE_VERSION_MISSING,
            CoverageGapType.TRACE_PROPAGATION_MISSING,
            CoverageGapType.LOG_FIELDS_INSUFFICIENT);
  }
}
