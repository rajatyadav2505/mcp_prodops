package com.idfcfirstbank.prodops.controltower.mcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.idfcfirstbank.prodops.controltower.mcp.ProdOpsControlTowerMcpApplication;
import com.idfcfirstbank.prodops.controltower.mcp.TestFixtures;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.NamespaceHealth;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.RiskLevel;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.idfcfirstbank.prodops.controltower.mcp.domain.service.IntelligenceService;
import com.idfcfirstbank.prodops.controltower.mcp.domain.service.ResourceService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("fixture")
@SpringBootTest(
    classes = ProdOpsControlTowerMcpApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
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
      "prodops.controltower.clusters[0].grafana.timeout=PT30S"
    })
class FixtureModeApplicationIT {

  @TestConfiguration
  static class TestClockConfig {

    @Bean
    @Primary
    Clock testClock() {
      return TestFixtures.fixedClock();
    }
  }

  @Autowired private ResourceService resourceService;

  @Autowired private IntelligenceService intelligenceService;

  @Test
  void loadsFixtureBackedServicesAndCatalogData() {
    List<ServiceCatalogEntry> services = resourceService.catalogServices();
    NamespaceHealth namespaceHealth =
        resourceService.namespaceHealth("payments-dev", "payments", "alice");

    assertThat(services).hasSize(1);
    assertThat(services.getFirst().displayName()).isEqualTo("Payments API");
    assertThat(namespaceHealth.workloadCount()).isEqualTo(1);
    assertThat(namespaceHealth.warningEventCount()).isEqualTo(1);
    assertThat(namespaceHealth.riskLevel()).isEqualTo(RiskLevel.MODERATE);
    assertThat(namespaceHealth.verdict().name()).isEqualTo("UNHEALTHY");
    assertThat(
            intelligenceService
                .correlateServiceIncident(
                    "payments-dev", "payments", "payments-api", Duration.ofMinutes(60), "alice")
                .executiveSummary())
        .contains("payments-api");
  }
}
