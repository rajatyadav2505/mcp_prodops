package com.prodops.controltower.mcp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prodops.controltower.mcp.TestFixtures;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.HealthVerdict;
import com.prodops.controltower.mcp.domain.model.NamespaceHealth;
import com.prodops.controltower.mcp.domain.model.RiskLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

  @Mock private ServiceCatalogViewService serviceCatalogViewService;

  @Mock private InventoryService inventoryService;

  @Test
  void mapsServiceCatalogEntriesIntoResourceViewsAndDelegatesHealthLookups() {
    when(serviceCatalogViewService.listServices())
        .thenReturn(List.of(TestFixtures.serviceCatalogEntry()));
    NamespaceHealth namespaceHealth =
        new NamespaceHealth(
            "payments-dev",
            "payments",
            1,
            1,
            1,
            1,
            44.0d,
            RiskLevel.MODERATE,
            HealthVerdict.DEGRADED,
            List.of("BackOff x1"),
            List.of(),
            List.of(),
            Instant.parse("2026-03-25T00:00:00Z"),
            new DataFreshness(
                Instant.parse("2026-03-25T00:00:00Z"),
                Instant.parse("2026-03-24T23:45:00Z"),
                Duration.ofMinutes(15),
                false));
    when(inventoryService.getNamespaceHealth(
            "payments-dev", "payments", Duration.ofMinutes(60), "alice"))
        .thenReturn(namespaceHealth);

    ResourceService service = new ResourceService(inventoryService, serviceCatalogViewService);

    assertThat(service.catalogServices()).hasSize(1);
    assertThat(service.catalogSlos()).hasSize(1);
    assertThat(service.catalogDashboards()).hasSize(1);
    assertThat(service.catalogRunbooks()).hasSize(1);
    assertThat(service.namespaceHealth("payments-dev", "payments", "alice"))
        .isEqualTo(namespaceHealth);
    assertThat(service.readOnlyContract().prohibitedActions())
        .contains("No Secret reads or Secret-value exposure");
    assertThat(service.exampleQuestions()).hasSize(6);

    verify(serviceCatalogViewService, times(4)).listServices();
    verify(inventoryService)
        .getNamespaceHealth("payments-dev", "payments", Duration.ofMinutes(60), "alice");
  }
}
