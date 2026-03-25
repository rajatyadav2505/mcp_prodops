package com.prodops.controltower.mcp.adapter.prometheus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.prodops.controltower.mcp.TestFixtures;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LivePrometheusAdapterTest {

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
  void executesInstantQueriesAgainstTheConfiguredEndpoint() {
    server.stubFor(
        post(urlEqualTo("/api/v1/query"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "status": "success",
                          "data": {
                            "resultType": "vector",
                            "result": [
                              {
                                "metric": {"job": "payments-api"},
                                "value": [1742860800, "0.67"]
                              }
                            ]
                          }
                        }
                        """)));
    LivePrometheusAdapter adapter =
        new LivePrometheusAdapter(
            properties(server.baseUrl()), RestClient.builder(), TestFixtures.fixedClock());

    PromqlExecutionResult result =
        adapter.instantQuery("payments-dev", "up", Instant.parse("2026-03-25T00:00:00Z"));

    assertThat(result.cluster()).isEqualTo("payments-dev");
    assertThat(result.series()).hasSize(1);
    assertThat(result.series().getFirst().points()).hasSize(1);
    assertThat(result.series().getFirst().points().getFirst().value()).isEqualTo(0.67d);
    verify(postRequestedFor(urlEqualTo("/api/v1/query")));
  }

  @Test
  void executesRangeQueriesAndConvertsTheSeriesPayload() {
    server.stubFor(
        post(urlEqualTo("/api/v1/query_range"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "status": "success",
                          "data": {
                            "resultType": "matrix",
                            "result": [
                              {
                                "metric": {"job": "payments-api"},
                                "values": [
                                  [1742860800, "0.42"],
                                  [1742861100, "0.52"]
                                ]
                              }
                            ]
                          }
                        }
                        """)));
    LivePrometheusAdapter adapter =
        new LivePrometheusAdapter(
            properties(server.baseUrl()), RestClient.builder(), TestFixtures.fixedClock());

    PromqlExecutionResult result =
        adapter.rangeQuery(
            "payments-dev",
            "up",
            Instant.parse("2026-03-24T23:00:00Z"),
            Instant.parse("2026-03-25T00:00:00Z"),
            Duration.ofMinutes(5));

    List<MetricSeries> series = result.series();
    assertThat(series).hasSize(1);
    assertThat(series.getFirst().points())
        .extracting(point -> point.value())
        .containsExactly(0.42d, 0.52d);
    verify(postRequestedFor(urlEqualTo("/api/v1/query_range")));
  }

  private static com.prodops.controltower.mcp.config.ProdOpsProperties properties(String baseUrl) {
    return TestFixtures.prodOpsProperties(
        Path.of("build/test-catalog.yaml"),
        Path.of("build/test-risk-weights.yaml"),
        Path.of("build/test-fixtures"),
        List.of("scenario_fixture_smoke"),
        baseUrl,
        baseUrl);
  }
}
