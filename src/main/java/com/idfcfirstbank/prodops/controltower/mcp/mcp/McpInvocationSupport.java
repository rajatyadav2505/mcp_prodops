package com.idfcfirstbank.prodops.controltower.mcp.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idfcfirstbank.prodops.controltower.mcp.audit.AuditService;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PolicyDecision;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PolicyOutcome;
import com.idfcfirstbank.prodops.controltower.mcp.observability.RequestRateLimiter;
import com.idfcfirstbank.prodops.controltower.mcp.policy.PolicyDeniedException;
import com.idfcfirstbank.prodops.controltower.mcp.support.HashingSupport;
import com.idfcfirstbank.prodops.controltower.mcp.support.RequestContextProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class McpInvocationSupport {

  private final RequestContextProvider requestContextProvider;
  private final RequestRateLimiter requestRateLimiter;
  private final HashingSupport hashingSupport;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public McpInvocationSupport(
      RequestContextProvider requestContextProvider,
      RequestRateLimiter requestRateLimiter,
      HashingSupport hashingSupport,
      AuditService auditService,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry,
      Clock clock) {
    this.requestContextProvider = requestContextProvider;
    this.requestRateLimiter = requestRateLimiter;
    this.hashingSupport = hashingSupport;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  public String identity() {
    return requestContextProvider.identity();
  }

  public <T> T invoke(String kind, String operation, Object arguments, Supplier<T> supplier) {
    String identity = requestContextProvider.identity();
    String correlationId = requestContextProvider.correlationId();
    requestRateLimiter.assertAllowed(identity, kind + ":" + operation);
    String argumentHash = hash(arguments);
    Instant startedAt = Instant.now(clock);
    Timer.Sample sample = Timer.start(meterRegistry);
    PolicyDecision decision =
        new PolicyDecision(PolicyOutcome.ALLOW, "Invocation permitted.", operation, identity);
    try {
      T result = supplier.get();
      meterRegistry
          .counter(
              "prodops.controltower.invocations",
              "kind",
              kind,
              "operation",
              operation,
              "outcome",
              "success")
          .increment();
      auditService.log(
          identity,
          correlationId,
          kind + ":" + operation,
          argumentHash,
          startedAt,
          Instant.now(clock),
          decision,
          sizeOf(result));
      return result;
    } catch (PolicyDeniedException exception) {
      PolicyDecision denied =
          new PolicyDecision(PolicyOutcome.DENY, exception.getMessage(), operation, identity);
      meterRegistry
          .counter(
              "prodops.controltower.invocations",
              "kind",
              kind,
              "operation",
              operation,
              "outcome",
              "denied")
          .increment();
      auditService.log(
          identity,
          correlationId,
          kind + ":" + operation,
          argumentHash,
          startedAt,
          Instant.now(clock),
          denied,
          0);
      throw exception;
    } finally {
      sample.stop(
          Timer.builder("prodops.controltower.invocation.latency")
              .tag("kind", kind)
              .tag("operation", operation)
              .register(meterRegistry));
    }
  }

  private String hash(Object arguments) {
    try {
      return hashingSupport.sha256(objectMapper.writeValueAsString(arguments));
    } catch (JsonProcessingException exception) {
      return hashingSupport.sha256(String.valueOf(arguments));
    }
  }

  private int sizeOf(Object result) {
    if (result == null) {
      return 0;
    }
    if (result instanceof Collection<?> collection) {
      return collection.size();
    }
    if (result instanceof Map<?, ?> map) {
      return map.size();
    }
    if (result instanceof String string) {
      return string.length();
    }
    return 1;
  }
}
