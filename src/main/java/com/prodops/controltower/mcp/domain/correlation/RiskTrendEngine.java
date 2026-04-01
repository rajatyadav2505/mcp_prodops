package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.DailyRiskTrendResult;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.scoring.RiskScoreEngine;
import com.prodops.controltower.mcp.domain.scoring.RiskSignal;
import com.prodops.controltower.mcp.domain.scoring.RiskWeights;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class RiskTrendEngine {

  private final RiskScoreEngine riskScoreEngine;

  public RiskTrendEngine(RiskScoreEngine riskScoreEngine) {
    this.riskScoreEngine = riskScoreEngine;
  }

  public Analysis analyze(
      List<MetricSeries> metricSeries,
      int totalRestarts,
      int warningEvents,
      double rolloutFreshnessMinutes,
      double unavailableRatio,
      double dependencyUncertainty,
      double noise,
      RiskWeights weights) {
    TreeSet<java.time.Instant> timestamps =
        metricSeries.stream()
            .flatMap(series -> series.points().stream())
            .map(point -> point.timestamp())
            .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    List<DailyRiskTrendResult.RiskTrendPoint> points = new ArrayList<>();
    for (java.time.Instant timestamp : timestamps) {
      double errorRate = pointValue(metricSeries, "error_rate_ratio", timestamp);
      double latency = pointValue(metricSeries, "latency_slo_ratio", timestamp);
      double cpu = pointValue(metricSeries, "cpu_saturation_ratio", timestamp);
      double memory = pointValue(metricSeries, "memory_pressure_ratio", timestamp);
      var assessment =
          riskScoreEngine.assess(
              new RiskSignal(
                  totalRestarts,
                  warningEvents,
                  rolloutFreshnessMinutes,
                  errorRate,
                  latency,
                  cpu,
                  memory,
                  unavailableRatio,
                  dependencyUncertainty,
                  noise),
              weights);
      points.add(
          new DailyRiskTrendResult.RiskTrendPoint(
              timestamp, assessment.score(), assessment.level()));
    }
    String trend =
        points.size() < 2
            ? "stable"
            : points.getLast().riskScore() - points.getFirst().riskScore() > 8.0d
                ? "degrading"
                : points.getFirst().riskScore() - points.getLast().riskScore() > 8.0d
                    ? "improving"
                    : "stable";
    return new Analysis(points, trend);
  }

  private double pointValue(
      List<MetricSeries> metricSeries, String name, java.time.Instant timestamp) {
    return metricSeries.stream()
        .filter(series -> series.name().equals(name))
        .flatMap(series -> series.points().stream())
        .filter(point -> !point.timestamp().isAfter(timestamp))
        .max(Comparator.comparing(point -> point.timestamp()))
        .map(point -> point.value())
        .orElse(0.0d);
  }

  public record Analysis(List<DailyRiskTrendResult.RiskTrendPoint> points, String trend) {}
}
