package com.prodops.controltower.mcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.ProdOpsControlTowerMcpApplication;
import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
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
      FixtureModeCheckoutIntelligenceTest.CheckoutClockConfig.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "prodops.controltower.catalog.path=src/test/resources/config/service-catalog-test.yaml",
      "prodops.controltower.risk-model.path=src/test/resources/config/risk-weights-test.yaml",
      "prodops.controltower.fixture.base-path=fixtures",
      "prodops.controltower.fixture.scenarios[0]=scenario_checkout_dependency_regression",
      "prodops.controltower.clusters[0].name=prodops-uat",
      "prodops.controltower.clusters[0].environment=UAT",
      "prodops.controltower.clusters[0].enabled=true",
      "prodops.controltower.clusters[0].namespace-allowlist[0]=checkout-uat",
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
      "prodops.controltower.clusters[0].bitbucket.project-key=CHK",
      "prodops.controltower.clusters[0].bitbucket.repo-slug=checkout-api",
      "prodops.controltower.clusters[0].bitbucket.browse-base-url=https://bitbucket.example.internal/projects/CHK/repos/checkout-api",
      "prodops.controltower.clusters[0].bitbucket.timeout=PT30S",
      "prodops.controltower.clusters[0].kibana.base-url=https://kibana.example.internal",
      "prodops.controltower.clusters[0].kibana.default-data-view=checkout-logs-*",
      "prodops.controltower.clusters[0].kibana.elasticsearch-base-url=https://elasticsearch.example.internal",
      "prodops.controltower.clusters[0].kibana.discover-path=/app/discover",
      "prodops.controltower.clusters[0].kibana.timeout=PT30S",
      "prodops.controltower.clusters[0].jaeger.base-url=https://jaeger.example.internal",
      "prodops.controltower.clusters[0].jaeger.timeout=PT30S"
    })
class FixtureModeCheckoutIntelligenceTest {

  @TestConfiguration(proxyBeanMethods = false)
  static class CheckoutClockConfig {

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2026-03-25T10:35:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired private IntelligenceService intelligenceService;

  @Test
  void recentChangeIsNotBlamedWhenJaegerShowsDependencyFailureFirst() {
    var rootCause =
        intelligenceService.getRootCauseAnalysis(
            "prodops-uat", "checkout-uat", "checkout-api", Duration.ofMinutes(60), "alice");
    var attribution =
        intelligenceService.getChangeRegressionAttribution(
            "prodops-uat", "checkout-uat", "checkout-api", Duration.ofMinutes(60), "alice");

    assertThat(rootCause.primarySuspect()).isNotNull();
    assertThat(rootCause.primarySuspect().causeType()).isEqualTo(CauseType.DEPENDENCY_FAILURE);
    assertThat(rootCause.primarySuspect().entity()).isEqualTo("merchant-profile");
    assertThat(rootCause.offendingChangeCandidate()).isNull();
    assertThat(attribution.causationClass())
        .isIn(CausationClass.CORRELATED_BUT_NOT_CAUSAL, CausationClass.INSUFFICIENT_EVIDENCE);
    assertThat(attribution.confidence()).isLessThan(0.5d);
    assertThat(attribution.whyLeadingSuspect()).contains("different service failing first");
  }
}
