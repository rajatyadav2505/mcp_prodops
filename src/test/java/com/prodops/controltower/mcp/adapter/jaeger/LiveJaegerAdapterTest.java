package com.prodops.controltower.mcp.adapter.jaeger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.TraceSearchQuery;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LiveJaegerAdapterTest {

  private final WireMockServer server =
      new WireMockServer(
          WireMockConfiguration.wireMockConfig().bindAddress("127.0.0.1").dynamicPort());

  @BeforeEach
  void startServer() {
    server.start();
    configureFor("localhost", server.port());
  }

  @AfterEach
  void stopServer() {
    server.stop();
  }

  @Test
  void loadsTracesAndBuildsSummaryFromJaegerPayload() {
    server.stubFor(
        get(urlPathEqualTo("/api/traces"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "data": [
                            {
                              "traceID": "trace-pay-001",
                              "processes": {
                                "p1": {"serviceName": "payments-api"},
                                "p2": {"serviceName": "payments-ledger"}
                              },
                              "spans": [
                                {
                                  "traceID": "trace-pay-001",
                                  "spanID": "span-root",
                                  "operationName": "POST /payments",
                                  "startTime": 1742897645000000,
                                  "duration": 820000,
                                  "processID": "p1",
                                  "references": [],
                                  "tags": [{"key": "service.version", "value": "2026.03.25-4b91d1c"}]
                                },
                                {
                                  "traceID": "trace-pay-001",
                                  "spanID": "span-ledger",
                                  "operationName": "POST /ledger",
                                  "startTime": 1742897645100000,
                                  "duration": 60000,
                                  "processID": "p2",
                                  "references": [{"spanID": "span-root"}],
                                  "tags": [{"key": "error", "value": true}]
                                }
                              ]
                            }
                          ]
                        }
                        """)));

    LiveJaegerAdapter adapter =
        new LiveJaegerAdapter(properties(server.baseUrl()), RestClient.builder());
    var traces =
        adapter.searchTraces(
            new TraceSearchQuery(
                "payments-dev",
                "payments",
                "payments-api",
                null,
                Instant.parse("2026-03-25T10:00:00Z"),
                Instant.parse("2026-03-25T10:30:00Z"),
                false,
                null,
                10,
                Map.of()));

    assertThat(traces).hasSize(1);
    assertThat(traces.getFirst().traceId()).isEqualTo("trace-pay-001");
    assertThat(traces.getFirst().dependencyEdges()).hasSize(1);
    assertThat(traces.getFirst().criticalPathDuration()).isGreaterThan(Duration.ZERO);
  }

  private ProdOpsProperties properties(String baseUrl) {
    return new ProdOpsProperties(
        "ProdOps Control Tower MCP",
        "Production Support Intelligence",
        new ProdOpsProperties.CatalogProperties(
            true, Path.of("build/catalog.yaml").toString(), Duration.ofMinutes(5)),
        new ProdOpsProperties.RiskModelProperties(
            Path.of("build/risk.yaml").toString(), Duration.ofMinutes(5)),
        new ProdOpsProperties.GuardrailProperties(
            Duration.ofHours(6),
            Duration.ofHours(24),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            250,
            100,
            100,
            10,
            200,
            50,
            200,
            20,
            60,
            5,
            30,
            20,
            150,
            5,
            true,
            true),
        new ProdOpsProperties.CacheProperties(
            200, Duration.ofMinutes(2), Duration.ofMinutes(10), Duration.ofMinutes(5)),
        new ProdOpsProperties.RateLimitProperties(120, Duration.ofMinutes(1)),
        new ProdOpsProperties.HttpProperties(
            "127.0.0.1", "/mcp", "http://localhost:8080", List.of("http://localhost:*")),
        new ProdOpsProperties.SecurityProperties(
            false, true, "", "", "prodops-control-tower-mcp", "prodops.read"),
        new ProdOpsProperties.FixtureProperties("fixtures", List.of("scenario_fixture_smoke")),
        List.of(
            new ProdOpsProperties.ClusterProperties(
                "payments-dev",
                "dev",
                true,
                List.of("payments"),
                "owner-team",
                "criticality",
                new ProdOpsProperties.KubernetesProperties(false, "", "", true),
                new ProdOpsProperties.PrometheusProperties(
                    "http://localhost:19090", "", Duration.ofSeconds(30)),
                new ProdOpsProperties.GrafanaProperties(
                    "http://localhost:13000", "", "", Duration.ofSeconds(30)),
                new ProdOpsProperties.BitbucketProperties(
                    "", "", "", "", "", "", Duration.ofSeconds(30)),
                new ProdOpsProperties.KibanaProperties(
                    "", "", "", "", "/app/discover", Duration.ofSeconds(30)),
                new ProdOpsProperties.JaegerProperties(baseUrl, "", Duration.ofSeconds(30)))));
  }
}
