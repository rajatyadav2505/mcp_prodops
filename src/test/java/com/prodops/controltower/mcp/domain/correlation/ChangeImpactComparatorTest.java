package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.ChangeImpactComparison;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChangeImpactComparatorTest {

  private final ChangeImpactComparator comparator = new ChangeImpactComparator();

  @Test
  void comparesBeforeAndAfterWindowsAcrossMetricsAndTraces() {
    ChangeImpactComparison comparison =
        comparator.compare(
            "prodops-uat",
            "payments-uat",
            "payments-api",
            "4b91d1c9",
            Instant.parse("2026-03-25T10:08:00Z"),
            Duration.ofMinutes(30),
            List.of(
                new MetricSeries(
                    "error_rate_ratio",
                    "query",
                    "ratio",
                    Map.of(),
                    List.of(
                        new MetricSeriesPoint(Instant.parse("2026-03-25T09:50:00Z"), 0.09d),
                        new MetricSeriesPoint(Instant.parse("2026-03-25T10:20:00Z"), 0.53d)))),
            List.of(
                new LogErrorSignature(
                    "before",
                    "ERROR",
                    1,
                    Instant.parse("2026-03-25T09:54:00Z"),
                    Instant.parse("2026-03-25T09:54:00Z"),
                    false,
                    "before",
                    List.of(),
                    null,
                    0.5d)),
            List.of(
                new LogErrorSignature(
                    "after",
                    "ERROR",
                    4,
                    Instant.parse("2026-03-25T10:12:00Z"),
                    Instant.parse("2026-03-25T10:22:00Z"),
                    true,
                    "after",
                    List.of(),
                    null,
                    0.8d)),
            List.of(
                new TraceSummary(
                    "trace-before",
                    "payments-api",
                    "POST /payments",
                    Instant.parse("2026-03-25T09:55:00Z"),
                    Duration.ofMillis(300),
                    false,
                    null,
                    null,
                    Duration.ofMillis(220),
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    "old",
                    "pod-before")),
            List.of(
                new TraceSummary(
                    "trace-after",
                    "payments-api",
                    "POST /payments",
                    Instant.parse("2026-03-25T10:20:00Z"),
                    Duration.ofMillis(900),
                    true,
                    "payments-api",
                    "bootstrap-config",
                    Duration.ofMillis(780),
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    "new",
                    "pod-after")),
            List.of(),
            new DataFreshness(
                Instant.parse("2026-03-25T10:30:00Z"),
                Instant.parse("2026-03-25T10:22:00Z"),
                Duration.ofMinutes(8),
                false),
            Instant.parse("2026-03-25T10:30:00Z"));

    assertThat(comparison.metricDeltas()).hasSize(1);
    assertThat(comparison.metricDeltas().getFirst().delta()).isPositive();
    assertThat(comparison.afterCriticalPathDuration())
        .isGreaterThan(comparison.beforeCriticalPathDuration());
  }
}
