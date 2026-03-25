package com.prodops.controltower.mcp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prodops.controltower.mcp.TestFixtures;
import com.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogViewServiceTest {

  @Mock private ServiceCatalogPort serviceCatalogPort;

  @Test
  void delegatesToTheCatalogPort() {
    when(serviceCatalogPort.listServices()).thenReturn(List.of(TestFixtures.serviceCatalogEntry()));

    ServiceCatalogViewService service = new ServiceCatalogViewService(serviceCatalogPort);

    assertThat(service.listServices()).hasSize(1);
    verify(serviceCatalogPort).listServices();
  }
}
