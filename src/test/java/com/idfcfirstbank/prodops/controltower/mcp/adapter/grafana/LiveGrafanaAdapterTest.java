package com.idfcfirstbank.prodops.controltower.mcp.adapter.grafana;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.idfcfirstbank.prodops.controltower.mcp.TestFixtures;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DashboardPanel;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LiveGrafanaAdapterTest {

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
  void searchesAndLoadsDashboardsFromTheConfiguredEndpoint() {
    server.stubFor(
        get(urlEqualTo("/api/search?query=payments&tag=gold"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        [
                          {
                            "uid": "dash-payments-api",
                            "title": "Payments API Overview",
                            "url": "/d/dash-payments-api/payments-overview",
                            "tags": ["gold", "payments"],
                            "folderTitle": "Production"
                          }
                        ]
                        """)));
    server.stubFor(
        get(urlEqualTo("/api/dashboards/uid/dash-payments-api"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "meta": {
                            "folderTitle": "Production",
                            "url": "/d/dash-payments-api/payments-overview",
                            "tags": ["gold", "payments"]
                          },
                          "dashboard": {
                            "uid": "dash-payments-api",
                            "title": "Payments API Overview",
                            "datasource": "prometheus",
                            "panels": [
                              {
                                "title": "Latency",
                                "description": "P95 latency",
                                "targets": ["A"]
                              }
                            ],
                            "templating": {
                              "list": [
                                {"name": "namespace"}
                              ]
                            }
                          }
                        }
                        """)));

    LiveGrafanaAdapter adapter =
        new LiveGrafanaAdapter(
            TestFixtures.prodOpsProperties(
                Path.of("build/test-catalog.yaml"),
                Path.of("build/test-risk-weights.yaml"),
                Path.of("build/test-fixtures"),
                List.of("scenario_fixture_smoke"),
                server.baseUrl(),
                server.baseUrl()),
            RestClient.builder(),
            TestFixtures.fixedClock());

    List<DashboardInfo> dashboards =
        adapter.search("payments-dev", "payments", List.of("gold"), "Production", 5);
    DashboardInfo dashboard = adapter.getByUid("payments-dev", "dash-payments-api").orElseThrow();

    assertThat(dashboards).hasSize(1);
    assertThat(dashboards.getFirst().url()).contains("/d/dash-payments-api/payments-overview");
    assertThat(dashboard.panels())
        .containsExactly(new DashboardPanel("Latency", "P95 latency", List.of("A")));
    assertThat(dashboard.variables()).containsExactly("namespace");
    assertThat(dashboard.datasourceHints()).containsExactly("prometheus");
    verify(getRequestedFor(urlEqualTo("/api/search?query=payments&tag=gold")));
    verify(getRequestedFor(urlEqualTo("/api/dashboards/uid/dash-payments-api")));
  }
}
