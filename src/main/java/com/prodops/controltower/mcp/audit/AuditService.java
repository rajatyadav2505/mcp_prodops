package com.prodops.controltower.mcp.audit;

import com.prodops.controltower.mcp.domain.model.PolicyDecision;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditService {

  private static final Logger LOGGER = LoggerFactory.getLogger("prodops.controltower.audit");

  public void log(
      String identity,
      String correlationId,
      String operation,
      String argumentHash,
      Instant startedAt,
      Instant endedAt,
      PolicyDecision policyDecision,
      int resultSize) {
    Duration duration = Duration.between(startedAt, endedAt);
    LOGGER.info(
        "identity={} correlationId={} operation={} argumentHash={} startedAt={} endedAt={} durationMs={} policy={} scope={} resultSize={}",
        identity,
        correlationId,
        operation,
        argumentHash,
        startedAt,
        endedAt,
        duration.toMillis(),
        policyDecision.outcome(),
        policyDecision.scope(),
        resultSize);
  }
}
