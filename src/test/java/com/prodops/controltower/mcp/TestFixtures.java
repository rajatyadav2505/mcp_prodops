package com.prodops.controltower.mcp;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.SloTarget;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public final class TestFixtures {

  public static final Instant FIXED_INSTANT = Instant.parse("2026-03-25T00:00:00Z");

  private TestFixtures() {}

  public static Clock fixedClock() {
    return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
  }

  public static ProdOpsProperties prodOpsProperties(
      Path catalogPath, Path riskWeightsPath, Path fixtureBasePath, List<String> scenarios) {
    return prodOpsProperties(
        catalogPath,
        riskWeightsPath,
        fixtureBasePath,
        scenarios,
        "http://localhost:19090",
        "http://localhost:13000");
  }

  public static ProdOpsProperties prodOpsProperties(
      Path catalogPath,
      Path riskWeightsPath,
      Path fixtureBasePath,
      List<String> scenarios,
      String prometheusBaseUrl,
      String grafanaBaseUrl) {
    return new ProdOpsProperties(
        "ProdOps Control Tower MCP",
        "Production Support Intelligence",
        new ProdOpsProperties.CatalogProperties(
            true, catalogPath.toString(), Duration.ofMinutes(5)),
        new ProdOpsProperties.RiskModelProperties(
            riskWeightsPath.toString(), Duration.ofMinutes(5)),
        new ProdOpsProperties.GuardrailProperties(
            Duration.ofHours(6),
            Duration.ofHours(24),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            250,
            100,
            10,
            200,
            true,
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
        new ProdOpsProperties.FixtureProperties(fixtureBasePath.toString(), scenarios),
        List.of(clusterProperties(prometheusBaseUrl, grafanaBaseUrl)));
  }

  public static ProdOpsProperties.ClusterProperties clusterProperties() {
    return clusterProperties("http://localhost:19090", "http://localhost:13000");
  }

  public static ProdOpsProperties.ClusterProperties clusterProperties(
      String prometheusBaseUrl, String grafanaBaseUrl) {
    return new ProdOpsProperties.ClusterProperties(
        "payments-dev",
        "dev",
        true,
        List.of("payments"),
        "owner-team",
        "criticality",
        new ProdOpsProperties.KubernetesProperties(false, "", "", true),
        new ProdOpsProperties.PrometheusProperties(prometheusBaseUrl, "", Duration.ofSeconds(30)),
        new ProdOpsProperties.GrafanaProperties(grafanaBaseUrl, "", "", Duration.ofSeconds(30)));
  }

  public static ServiceCatalogEntry serviceCatalogEntry() {
    return new ServiceCatalogEntry(
        "payments-api",
        "Payments API",
        "payments-dev",
        "payments",
        "payments-api",
        WorkloadKind.DEPLOYMENT,
        "payments-api",
        "critical",
        "payments-platform",
        List.of("dash-payments-api"),
        List.of(new SloTarget("latency", "99.9", "250ms", "PT30D")),
        List.of("https://example.invalid/runbooks/payments-api"),
        java.util.Map.of(
            "error_rate", "sum(rate(http_requests_total{status=~\"5..\"}[5m]))",
            "latency", "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "cpu", "sum(rate(container_cpu_usage_seconds_total[5m]))",
            "memory", "sum(container_memory_working_set_bytes)"));
  }
}
