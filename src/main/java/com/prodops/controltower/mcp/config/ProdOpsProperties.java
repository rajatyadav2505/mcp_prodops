package com.prodops.controltower.mcp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("prodops.controltower")
@Validated
public record ProdOpsProperties(
    @DefaultValue("ProdOps Control Tower MCP") String productName,
    @DefaultValue("Production Support Intelligence") String productDomain,
    @Valid @NotNull CatalogProperties catalog,
    @Valid @NotNull RiskModelProperties riskModel,
    @Valid @NotNull GuardrailProperties guardrails,
    @Valid @NotNull CacheProperties cache,
    @Valid @NotNull RateLimitProperties rateLimit,
    @Valid @NotNull HttpProperties http,
    @Valid @NotNull SecurityProperties security,
    @Valid @NotNull FixtureProperties fixture,
    @Valid @NotEmpty List<ClusterProperties> clusters) {

  public record CatalogProperties(
      @DefaultValue("true") boolean enabled,
      @NotBlank @DefaultValue("config/service-catalog.example.yaml") String path,
      @NotNull @DefaultValue("PT5M") Duration refreshInterval) {}

  public record RiskModelProperties(
      @NotBlank @DefaultValue("config/risk-weights.example.yaml") String path,
      @NotNull @DefaultValue("PT5M") Duration refreshInterval) {}

  public record GuardrailProperties(
      @NotNull @DefaultValue("PT6H") Duration maxLookback,
      @NotNull @DefaultValue("PT24H") Duration maxRange,
      @NotNull @DefaultValue("PT5M") Duration minStep,
      @NotNull @DefaultValue("PT15M") Duration defaultLookback,
      @Min(1) @Max(5000) @DefaultValue("250") int maxSeries,
      @Min(1) @Max(1000) @DefaultValue("100") int maxLogLines,
      @Min(1) @Max(100) @DefaultValue("10") int maxDashboards,
      @Min(1) @Max(1000) @DefaultValue("200") int maxEvents,
      @DefaultValue("true") boolean rawPromqlEnabled,
      @DefaultValue("true") boolean requireOriginValidation) {}

  public record CacheProperties(
      @Min(1) @Max(10000) @DefaultValue("200") int maxEntries,
      @NotNull @DefaultValue("PT2M") Duration namespaceInventoryTtl,
      @NotNull @DefaultValue("PT10M") Duration dashboardTtl,
      @NotNull @DefaultValue("PT5M") Duration catalogTtl) {}

  public record RateLimitProperties(
      @Min(1) @Max(10000) @DefaultValue("120") int requestsPerWindow,
      @NotNull @DefaultValue("PT1M") Duration window) {}

  public record HttpProperties(
      @NotNull @DefaultValue("127.0.0.1") String bindAddress,
      @NotNull @DefaultValue("/mcp") String mcpPath,
      @NotNull @DefaultValue("http://localhost:8080") String publicBaseUrl,
      @NotEmpty
          @DefaultValue(
              "[\"http://localhost:*\",\"https://localhost:*\",\"http://127.0.0.1:*\",\"https://127.0.0.1:*\"]")
          List<String> allowedOrigins) {}

  public record SecurityProperties(
      @DefaultValue("false") boolean jwtEnabled,
      @DefaultValue("true") boolean allowAnonymousLocalHttp,
      @DefaultValue("") String issuerUri,
      @DefaultValue("") String jwkSetUri,
      @DefaultValue("prodops-control-tower-mcp") String audience,
      @DefaultValue("prodops.read") String requiredScope) {}

  public record FixtureProperties(
      @NotBlank @DefaultValue("fixtures") String basePath,
      @NotEmpty
          @DefaultValue(
              "[\"scenario_payments_rollout_regression\",\"scenario_upi_recon_saturation\",\"scenario_tradex_alert_storm\"]")
          List<String> scenarios) {}

  public record ClusterProperties(
      @NotBlank String name,
      @NotBlank String environment,
      @DefaultValue("true") boolean enabled,
      @DefaultValue("[]") List<String> namespaceAllowlist,
      @DefaultValue("owner-team") String teamLabelKey,
      @DefaultValue("criticality") String criticalityLabelKey,
      @Valid @NotNull KubernetesProperties kubernetes,
      @Valid @NotNull PrometheusProperties prometheus,
      @Valid @NotNull GrafanaProperties grafana) {}

  public record KubernetesProperties(
      @DefaultValue("false") boolean inCluster,
      @DefaultValue("") String kubeconfig,
      @DefaultValue("") String context,
      @DefaultValue("true") boolean logsEnabled) {}

  public record PrometheusProperties(
      @DefaultValue("") String baseUrl,
      @DefaultValue("") String bearerTokenRef,
      @NotNull @DefaultValue("PT30S") Duration timeout) {}

  public record GrafanaProperties(
      @DefaultValue("") String baseUrl,
      @DefaultValue("") String bearerTokenRef,
      @DefaultValue("") String defaultFolder,
      @NotNull @DefaultValue("PT30S") Duration timeout) {}
}
