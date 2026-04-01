package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.LogEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogPatternAnalyzerTest {

  @Test
  void groupsKnownOperationalPatterns() {
    LogPatternAnalyzer analyzer = new LogPatternAnalyzer();

    var matches =
        analyzer.summarize(
            List.of(
                new LogEvent(
                    "payments-dev",
                    "payments",
                    "payments-api",
                    "payments-api-canary",
                    "app",
                    "ERROR",
                    "SocketTimeoutException: ledger timed out",
                    "SocketTimeoutException: ledger timed out",
                    "SocketTimeoutException",
                    null,
                    null,
                    null,
                    Instant.parse("2026-03-24T23:40:00Z"),
                    null),
                new LogEvent(
                    "payments-dev",
                    "payments",
                    "payments-api",
                    "payments-api-canary",
                    "app",
                    "ERROR",
                    "IllegalStateException: startup validator failed",
                    "IllegalStateException: startup validator failed",
                    "IllegalStateException",
                    null,
                    null,
                    null,
                    Instant.parse("2026-03-24T23:41:00Z"),
                    null)),
            5);

    assertThat(matches).extracting(match -> match.pattern()).contains("timeout", "illegal_state");
  }
}
