package com.prodops.controltower.mcp.adapter.kibana;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.LogSearchQuery;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class LiveKibanaAdapterTest {

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
  void searchesElasticCompatibleLogsAndNormalizesEvents() {
    server.stubFor(
        post(urlEqualTo("/logs-*/_search"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "hits": {
                            "hits": [
                              {
                                "_source": {
                                  "@timestamp": "2026-03-25T10:14:00Z",
                                  "message": "java.lang.IllegalStateException: Settlement schema v4 cannot deserialize payoutConfig",
                                  "log": {"level": "error"},
                                  "service": {"name": "payments-api", "version": "2026.03.25-4b91d1c"},
                                  "trace": {"id": "trace-pay-001"},
                                  "labels": {"request_id": "req-pay-001"},
                                  "kubernetes": {
                                    "namespace": "payments-uat",
                                    "pod": {"name": "payments-api-74c6d85d97-5r9vq"},
                                    "container": {"name": "app"}
                                  }
                                }
                              }
                            ]
                          }
                        }
                        """)));

    LiveKibanaAdapter adapter =
        new LiveKibanaAdapter(
            properties(loopbackBaseUrl()),
            RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()));

    var events =
        adapter.searchLogs(
            new LogSearchQuery(
                "payments-dev",
                "payments",
                "payments-api",
                Instant.parse("2026-03-25T10:00:00Z"),
                Instant.parse("2026-03-25T10:30:00Z"),
                "ERROR",
                null,
                null,
                null,
                null,
                "logs-*",
                25));

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().serviceOrWorkload()).isEqualTo("payments-api");
    assertThat(events.getFirst().traceId()).isEqualTo("trace-pay-001");
    assertThat(events.getFirst().message()).contains("IllegalStateException");
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
                    baseUrl, "", "logs-*", baseUrl, "/app/discover", Duration.ofSeconds(30)),
                new ProdOpsProperties.JaegerProperties("", "", Duration.ofSeconds(30)))));
  }

  private String loopbackBaseUrl() {
    return "http://127.0.0.1:" + server.port();
  }
}
