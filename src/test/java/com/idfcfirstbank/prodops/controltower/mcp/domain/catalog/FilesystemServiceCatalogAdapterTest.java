package com.idfcfirstbank.prodops.controltower.mcp.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.idfcfirstbank.prodops.controltower.mcp.TestFixtures;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemServiceCatalogAdapterTest {

  @TempDir Path tempDir;

  @Test
  void loadsCatalogEntriesAndRefreshesAfterTheConfiguredInterval() throws Exception {
    Path catalogFile = tempDir.resolve("service-catalog.yaml");
    Files.writeString(catalogFile, catalogYaml("payments-api", "Payments API"));
    MutableClock clock = new MutableClock(TestFixtures.FIXED_INSTANT);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    var properties =
        TestFixtures.prodOpsProperties(
            catalogFile,
            tempDir.resolve("risk-weights.yaml"),
            tempDir.resolve("fixtures"),
            List.of("scenario_fixture_smoke"));
    FilesystemServiceCatalogAdapter adapter =
        new FilesystemServiceCatalogAdapter(properties, yamlMapper, clock);

    assertThat(adapter.listServices())
        .extracting(ServiceCatalogEntry::serviceId)
        .containsExactly("payments-api");
    assertThat(adapter.findByWorkload("payments-dev", "payments", "payments-api")).isPresent();

    Files.writeString(catalogFile, catalogYaml("checkout-api", "Checkout API"));
    clock.advance(Duration.ofMinutes(6));

    assertThat(adapter.findByServiceId("checkout-api")).isPresent();
    assertThat(adapter.listServices())
        .extracting(ServiceCatalogEntry::serviceId)
        .containsExactly("checkout-api");
  }

  private String catalogYaml(String serviceId, String displayName) {
    return """
                services:
                  - serviceId: %s
                    displayName: %s
                    cluster: payments-dev
                    namespace: payments
                    workloadName: %s
                    workloadKind: DEPLOYMENT
                    serviceName: %s
                    criticality: critical
                    ownerTeam: payments-platform
                    dashboardUids:
                      - dash-payments-api
                    sloTargets:
                      - name: latency
                        objective: "99.9"
                        threshold: "250ms"
                        measurementWindow: PT30D
                    runbookLinks:
                      - https://example.invalid/runbooks/%s
                    promqlTemplates:
                      error_rate: sum(rate(http_requests_total{status=~"5.."}[5m]))
                      latency: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
                      cpu: sum(rate(container_cpu_usage_seconds_total[5m]))
                      memory: sum(container_memory_working_set_bytes)
                """
        .formatted(serviceId, displayName, serviceId, serviceId, serviceId);
  }

  private static final class MutableClock extends Clock {

    private Instant currentInstant;

    private MutableClock(Instant currentInstant) {
      this.currentInstant = currentInstant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return currentInstant;
    }

    private void advance(Duration duration) {
      currentInstant = currentInstant.plus(duration);
    }
  }
}
