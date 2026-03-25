package com.prodops.controltower.mcp.domain.port;

import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface MetricsPort {

  List<MetricValue> goldenSignals(
      String cluster,
      String namespace,
      String serviceOrWorkload,
      Duration lookback,
      ServiceCatalogEntry catalogEntry);

  PromqlExecutionResult instantQuery(String cluster, String query, Instant evaluationTime);

  PromqlExecutionResult rangeQuery(
      String cluster, String query, Instant start, Instant end, Duration step);
}
