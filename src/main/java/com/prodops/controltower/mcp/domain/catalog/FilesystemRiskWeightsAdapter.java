package com.prodops.controltower.mcp.domain.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.port.RiskWeightsPort;
import com.prodops.controltower.mcp.domain.scoring.RiskWeights;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FilesystemRiskWeightsAdapter implements RiskWeightsPort {

  private final ProdOpsProperties properties;
  private final ObjectMapper yamlMapper;
  private final Clock clock;
  private volatile CacheState cacheState = new CacheState(RiskWeights.defaults(), Instant.EPOCH);

  public FilesystemRiskWeightsAdapter(
      ProdOpsProperties properties,
      @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper,
      Clock clock) {
    this.properties = properties;
    this.yamlMapper = yamlMapper;
    this.clock = clock;
  }

  @Override
  public RiskWeights getWeights() {
    CacheState current = cacheState;
    if (Instant.now(clock)
        .isBefore(current.loadedAt().plus(properties.riskModel().refreshInterval()))) {
      return current.riskWeights();
    }
    Path path = Path.of(properties.riskModel().path());
    if (!Files.exists(path)) {
      return RiskWeights.defaults();
    }
    try {
      RiskWeights weights = yamlMapper.readValue(path.toFile(), RiskWeights.class);
      cacheState = new CacheState(weights, Instant.now(clock));
      return weights;
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to load risk weights from " + path, exception);
    }
  }

  private record CacheState(RiskWeights riskWeights, Instant loadedAt) {}
}
