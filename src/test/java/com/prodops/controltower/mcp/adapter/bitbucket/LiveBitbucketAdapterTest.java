package com.prodops.controltower.mcp.adapter.bitbucket;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LiveBitbucketAdapterTest {

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
  void loadsMergedPullRequestsWithDiffstatAndPipelineMetadata() {
    server.stubFor(
        get(urlPathEqualTo("/2.0/repositories/prodops/payments-api/pullrequests"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "values": [
                            {
                              "id": 482,
                              "title": "Enable settlement schema v4 for settlement binding",
                              "description": "Schema change",
                              "state": "MERGED",
                              "author": {"display_name": "priya.shah"},
                              "reviewers": [{"display_name": "anand.k"}],
                              "source": {
                                "branch": {"name": "release/payments-api"},
                                "commit": {"hash": "4b91d1c9aa770f8d2a66f9c5dd18f70f93e40012", "date": "2026-03-25T10:01:00Z"}
                              },
                              "destination": {"branch": {"name": "main"}},
                              "merge_commit": {"hash": "4b91d1c9aa770f8d2a66f9c5dd18f70f93e40012", "date": "2026-03-25T10:08:00Z"},
                              "created_on": "2026-03-25T09:25:00Z",
                              "updated_on": "2026-03-25T10:08:00Z",
                              "links": {"html": {"href": "https://bitbucket.example.internal/projects/PAY/repos/payments-api/pull-requests/482"}}
                            }
                          ]
                        }
                        """)));
    server.stubFor(
        get(urlPathEqualTo("/2.0/repositories/prodops/payments-api/pullrequests/482/diffstat"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "values": [
                            {
                              "new": {"path": "services/payments-api/src/main/java/com/prodops/payments/config/SettlementSchemaBinder.java"}
                            }
                          ]
                        }
                        """)));
    server.stubFor(
        get(urlPathEqualTo(
                "/2.0/repositories/prodops/payments-api/commit/4b91d1c9aa770f8d2a66f9c5dd18f70f93e40012/statuses"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "values": [
                            {
                              "key": "pipe-9041",
                              "name": "deploy-prodops-uat",
                              "state": {"name": "COMPLETED", "result_name": "FAILED"},
                              "url": "https://bitbucket.example.internal/projects/PAY/repos/payments-api/pipelines/9041",
                              "created_on": "2026-03-25T10:09:00Z",
                              "updated_on": "2026-03-25T10:11:00Z"
                            }
                          ]
                        }
                        """)));

    LiveBitbucketAdapter adapter =
        new LiveBitbucketAdapter(properties(server.baseUrl()), RestClient.builder());
    var changes =
        adapter.listChanges(
            new com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery(
                "payments-dev",
                "payments-api",
                "prodops",
                "payments-api",
                null,
                Instant.parse("2026-03-25T09:00:00Z"),
                Instant.parse("2026-03-25T11:00:00Z"),
                10));

    assertThat(changes).hasSize(1);
    assertThat(changes.getFirst().changedFiles())
        .contains(
            "services/payments-api/src/main/java/com/prodops/payments/config/SettlementSchemaBinder.java");
    assertThat(changes.getFirst().pipelineRuns()).hasSize(1);
    assertThat(changes.getFirst().pipelineRuns().getFirst().result()).isEqualTo("FAILED");
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
                    baseUrl, "", "prodops", "PAY", "payments-api", baseUrl, Duration.ofSeconds(30)),
                new ProdOpsProperties.KibanaProperties(
                    "", "", "", "", "/app/discover", Duration.ofSeconds(30)),
                new ProdOpsProperties.JaegerProperties("", "", Duration.ofSeconds(30)))));
  }
}
