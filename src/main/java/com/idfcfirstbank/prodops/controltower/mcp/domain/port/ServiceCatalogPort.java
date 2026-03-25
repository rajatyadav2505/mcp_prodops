package com.idfcfirstbank.prodops.controltower.mcp.domain.port;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import java.util.List;
import java.util.Optional;

public interface ServiceCatalogPort {

  List<ServiceCatalogEntry> listServices();

  Optional<ServiceCatalogEntry> findByWorkload(
      String cluster, String namespace, String workloadName);

  Optional<ServiceCatalogEntry> findByServiceId(String serviceId);
}
