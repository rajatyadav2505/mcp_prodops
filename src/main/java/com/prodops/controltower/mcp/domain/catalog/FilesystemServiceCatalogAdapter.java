package com.prodops.controltower.mcp.domain.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.port.ServiceCatalogPort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FilesystemServiceCatalogAdapter implements ServiceCatalogPort {

  private final ProdOpsProperties properties;
  private final ObjectMapper yamlMapper;
  private final Clock clock;
  private volatile CacheState cacheState = new CacheState(List.of(), Instant.EPOCH);

  public FilesystemServiceCatalogAdapter(
      ProdOpsProperties properties,
      @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper,
      Clock clock) {
    this.properties = properties;
    this.yamlMapper = yamlMapper;
    this.clock = clock;
  }

  @Override
  public List<ServiceCatalogEntry> listServices() {
    return loadIfNeeded().services();
  }

  @Override
  public Optional<ServiceCatalogEntry> findByWorkload(
      String cluster, String namespace, String workloadName) {
    return loadIfNeeded().services().stream()
        .filter(entry -> entry.cluster().equals(cluster))
        .filter(entry -> entry.namespace().equals(namespace))
        .filter(entry -> entry.workloadName().equals(workloadName))
        .findFirst();
  }

  @Override
  public Optional<ServiceCatalogEntry> findByServiceId(String serviceId) {
    return loadIfNeeded().services().stream()
        .filter(entry -> entry.serviceId().equals(serviceId))
        .findFirst();
  }

  private CacheState loadIfNeeded() {
    CacheState current = cacheState;
    if (Instant.now(clock)
        .isBefore(current.loadedAt().plus(properties.catalog().refreshInterval()))) {
      return current;
    }
    Path path = Path.of(properties.catalog().path());
    if (!Files.exists(path)) {
      cacheState = new CacheState(List.of(), Instant.now(clock));
      return cacheState;
    }
    try {
      CatalogDocument document = yamlMapper.readValue(path.toFile(), CatalogDocument.class);
      cacheState =
          new CacheState(
              document.services() == null ? List.of() : document.services(), Instant.now(clock));
      return cacheState;
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to load service catalog from " + path, exception);
    }
  }

  private record CatalogDocument(List<ServiceCatalogEntry> services) {}

  private record CacheState(List<ServiceCatalogEntry> services, Instant loadedAt) {}
}
