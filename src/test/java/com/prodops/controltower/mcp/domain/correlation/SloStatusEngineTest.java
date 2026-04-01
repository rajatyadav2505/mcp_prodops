package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.SloTarget;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SloStatusEngineTest {

  @Test
  void computesRemainingBudgetAndTimeToBreach() {
    SloStatusEngine engine = new SloStatusEngine();

    var analysis =
        engine.analyze(
            List.of(new SloTarget("availability", "99.9", "0.1% errors", "PT30D")),
            List.of(
                new MetricValue(
                    "error_rate_ratio",
                    "ratio",
                    0.8d,
                    "high",
                    Instant.parse("2026-03-24T23:55:00Z"),
                    "error_rate",
                    "fixture")),
            Map.of(
                "error_rate_ratio",
                List.of(
                    new MetricSeries(
                        "error_rate_ratio",
                        "error_rate",
                        "ratio",
                        Map.of(),
                        List.of(
                            new MetricSeriesPoint(Instant.parse("2026-03-24T23:00:00Z"), 0.4d),
                            new MetricSeriesPoint(Instant.parse("2026-03-24T23:55:00Z"), 0.8d))))));

    assertThat(analysis.leadingRisk()).isNotNull();
    assertThat(analysis.leadingRisk().remainingBudgetPercent()).isLessThan(25.0d);
    assertThat(analysis.leadingRisk().timeToBreach()).isNotNull();
  }
}
