package com.idfcfirstbank.prodops.controltower.mcp.domain.service;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceCatalogViewService {

  private final ServiceCatalogPort serviceCatalogPort;

  public ServiceCatalogViewService(ServiceCatalogPort serviceCatalogPort) {
    this.serviceCatalogPort = serviceCatalogPort;
  }

  public List<ServiceCatalogEntry> listServices() {
    return serviceCatalogPort.listServices();
  }
}
