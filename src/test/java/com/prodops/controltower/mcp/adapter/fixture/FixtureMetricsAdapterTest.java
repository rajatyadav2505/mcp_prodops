package com.prodops.controltower.mcp.adapter.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodops.controltower.mcp.TestFixtures;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixtureMetricsAdapterTest {

  @Test
  void mapsServiceCatalogPromqlTemplatesToFixtureSeries() {
    FixtureMetricsAdapter adapter = new FixtureMetricsAdapter(loader());

    var result =
        adapter.rangeQuery(
            "payments-dev",
            "sum(rate(http_requests_total{status=~\"5..\"}[5m]))",
            Instant.parse("2026-03-24T22:54:00Z"),
            Instant.parse("2026-03-25T00:00:00Z"),
            Duration.ofMinutes(5));

    assertThat(result.series()).isNotEmpty();
  }

  private FixtureScenarioLoader loader() {
    return new FixtureScenarioLoader(
        TestFixtures.prodOpsProperties(
            Path.of("src/test/resources/config/service-catalog-test.yaml"),
            Path.of("src/test/resources/config/risk-weights-test.yaml"),
            Path.of("src/test/resources/fixtures"),
            List.of("scenario_fixture_smoke")),
        new ObjectMapper().findAndRegisterModules());
  }
}
