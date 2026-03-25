package com.idfcfirstbank.prodops.controltower.mcp.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.idfcfirstbank.prodops.controltower.mcp.TestFixtures;
import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.DashboardPort;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.MetricsPort;
import com.idfcfirstbank.prodops.controltower.mcp.policy.GuardrailViolationException;
import com.idfcfirstbank.prodops.controltower.mcp.policy.ScopePolicy;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObservabilityServiceTest {

  @Test
  void rejectsRawPromqlWhenDisabledByPolicy() {
    ProdOpsProperties disabledProperties =
        new ProdOpsProperties(
            "ProdOps Control Tower MCP",
            "Production Support Intelligence",
            new ProdOpsProperties.CatalogProperties(
                true, "build/test-catalog.yaml", Duration.ofMinutes(5)),
            new ProdOpsProperties.RiskModelProperties(
                "build/test-risk-weights.yaml", Duration.ofMinutes(5)),
            new ProdOpsProperties.GuardrailProperties(
                Duration.ofHours(6),
                Duration.ofHours(24),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                250,
                100,
                10,
                200,
                false,
                true),
            new ProdOpsProperties.CacheProperties(
                200, Duration.ofMinutes(2), Duration.ofMinutes(10), Duration.ofMinutes(5)),
            new ProdOpsProperties.RateLimitProperties(120, Duration.ofMinutes(1)),
            new ProdOpsProperties.HttpProperties(
                "127.0.0.1",
                "/mcp",
                "http://localhost:8080",
                List.of(
                    "http://localhost:*",
                    "https://localhost:*",
                    "http://127.0.0.1:*",
                    "https://127.0.0.1:*")),
            new ProdOpsProperties.SecurityProperties(
                false, true, "", "", "prodops-control-tower-mcp", "prodops.read"),
            new ProdOpsProperties.FixtureProperties(
                Path.of("build/test-fixtures").toString(), List.of("scenario_fixture_smoke")),
            List.of(TestFixtures.clusterProperties()));

    ObservabilityService service =
        new ObservabilityService(
            mock(MetricsPort.class),
            mock(DashboardPort.class),
            new ScopePolicy(disabledProperties),
            disabledProperties);

    assertThatThrownBy(
            () ->
                service.runPromqlInstant(
                    "payments-dev", "up", Instant.parse("2026-03-25T00:00:00Z"), "alice"))
        .isInstanceOf(GuardrailViolationException.class)
        .hasMessageContaining("disabled");
  }

  @Test
  void rejectsRangeQueriesOutsideConfiguredBounds() {
    ProdOpsProperties properties =
        TestFixtures.prodOpsProperties(
            Path.of("build/test-catalog.yaml"),
            Path.of("build/test-risk-weights.yaml"),
            Path.of("build/test-fixtures"),
            List.of("scenario_fixture_smoke"));
    ObservabilityService service =
        new ObservabilityService(
            mock(MetricsPort.class),
            mock(DashboardPort.class),
            new ScopePolicy(properties),
            properties);

    assertThatThrownBy(
            () ->
                service.runPromqlRange(
                    "payments-dev",
                    "up",
                    Instant.parse("2026-03-24T00:00:00Z"),
                    Instant.parse("2026-03-25T00:00:00Z"),
                    Duration.ofMinutes(1),
                    "alice"))
        .isInstanceOf(GuardrailViolationException.class)
        .hasMessageContaining("minimum");

    assertThatThrownBy(
            () ->
                service.runPromqlRange(
                    "payments-dev",
                    "up",
                    Instant.parse("2026-03-23T00:00:00Z"),
                    Instant.parse("2026-03-25T00:00:00Z"),
                    Duration.ofMinutes(5),
                    "alice"))
        .isInstanceOf(GuardrailViolationException.class)
        .hasMessageContaining("maximum");
  }
}
