package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.ConfidenceBreakdown;
import com.prodops.controltower.mcp.domain.model.ConfidenceFactor;
import com.prodops.controltower.mcp.domain.model.MetricSeries;
import com.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import com.prodops.controltower.mcp.domain.model.MetricValue;
import com.prodops.controltower.mcp.domain.model.SloStatusResult;
import com.prodops.controltower.mcp.domain.model.SloTarget;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SloStatusEngine {

  public Analysis analyze(
      List<SloTarget> sloTargets,
      List<MetricValue> metrics,
      Map<String, List<MetricSeries>> seriesByMetric) {
    List<SloStatusResult.SloBudgetStatus> budgets = new ArrayList<>();
    List<String> limitations = new ArrayList<>();
    for (SloTarget target : sloTargets) {
      String metricName = metricName(target);
      double currentRatio =
          metrics.stream()
              .filter(metric -> metric.name().equals(metricName))
              .mapToDouble(MetricValue::value)
              .findFirst()
              .orElse(0.0d);
      if (metrics.stream().noneMatch(metric -> metric.name().equals(metricName))) {
        limitations.add("No metric was available for SLO target " + target.name() + ".");
      }
      List<MetricSeries> series = seriesByMetric.getOrDefault(metricName, List.of());
      Duration timeToBreach = estimateTimeToBreach(currentRatio, series);
      double remainingBudgetPercent = Math.max(0.0d, (1.0d - currentRatio) * 100.0d);
      List<SloStatusResult.BurnRateWindow> burnRates =
          List.of(
              new SloStatusResult.BurnRateWindow(Duration.ofMinutes(5), currentRatio),
              new SloStatusResult.BurnRateWindow(Duration.ofHours(1), averageRatio(series)));
      budgets.add(
          new SloStatusResult.SloBudgetStatus(
              target.name(),
              target.objective(),
              target.threshold(),
              target.measurementWindow(),
              metricName,
              currentRatio,
              remainingBudgetPercent,
              burnRates,
              timeToBreach,
              status(currentRatio)));
    }
    SloStatusResult.SloBudgetStatus leadingRisk =
        budgets.stream()
            .max(
                java.util.Comparator.comparingDouble(SloStatusResult.SloBudgetStatus::currentRatio))
            .orElse(null);
    double uncertaintyPenalty = budgets.isEmpty() ? 0.24d : limitations.size() * 0.05d;
    double confidence = Math.max(0.35d, Math.min(0.9d, 0.82d - uncertaintyPenalty));
    return new Analysis(
        budgets,
        leadingRisk,
        new ConfidenceBreakdown(
            confidence,
            budgets.isEmpty() ? 0.2d : 0.52d,
            0.08d,
            uncertaintyPenalty,
            List.of(
                new ConfidenceFactor(
                    "catalog SLOs",
                    budgets.isEmpty() ? 0.0d : 0.18d,
                    "Curated service SLOs were available."),
                new ConfidenceFactor(
                    "metric alignment",
                    metrics.isEmpty() ? 0.0d : 0.22d,
                    "Golden-signal metrics were mapped deterministically to SLO targets."),
                new ConfidenceFactor(
                    "trend support",
                    seriesByMetric.isEmpty() ? 0.0d : 0.12d,
                    "Prometheus series supported burn and breach estimation.")),
            "SLO confidence is strongest when both current ratios and trend windows are populated."),
        limitations);
  }

  public Forecast forecast(
      SloStatusResult.SloBudgetStatus budgetStatus, Instant generatedAt, ConfidenceBreakdown base) {
    Duration timeToBreach = budgetStatus == null ? null : budgetStatus.timeToBreach();
    Instant projectedBreachAt = timeToBreach == null ? null : generatedAt.plus(timeToBreach);
    return new Forecast(projectedBreachAt, timeToBreach, base);
  }

  private String metricName(SloTarget target) {
    String normalized = target.name().toLowerCase(Locale.ROOT);
    if (normalized.contains("availability") || normalized.contains("error")) {
      return "error_rate_ratio";
    }
    if (normalized.contains("latency")) {
      return "latency_slo_ratio";
    }
    return "error_rate_ratio";
  }

  private Duration estimateTimeToBreach(double currentRatio, List<MetricSeries> series) {
    if (currentRatio >= 1.0d) {
      return Duration.ZERO;
    }
    List<MetricSeriesPoint> points =
        series.stream()
            .flatMap(item -> item.points().stream())
            .sorted(java.util.Comparator.comparing(MetricSeriesPoint::timestamp))
            .toList();
    if (points.size() < 2) {
      return null;
    }
    MetricSeriesPoint first = points.getFirst();
    MetricSeriesPoint last = points.getLast();
    double minutes =
        Math.max(1.0d, Duration.between(first.timestamp(), last.timestamp()).toMinutes());
    double slopePerMinute = (last.value() - first.value()) / minutes;
    if (slopePerMinute <= 0.0d) {
      return null;
    }
    long minutesToBreach = Math.round((1.0d - currentRatio) / slopePerMinute);
    return minutesToBreach <= 0 ? Duration.ZERO : Duration.ofMinutes(minutesToBreach);
  }

  private double averageRatio(List<MetricSeries> series) {
    return series.stream()
        .flatMap(item -> item.points().stream())
        .mapToDouble(MetricSeriesPoint::value)
        .average()
        .orElse(0.0d);
  }

  private String status(double ratio) {
    if (ratio >= 1.0d) {
      return "breached";
    }
    if (ratio >= 0.7d) {
      return "at_risk";
    }
    return "healthy";
  }

  public record Analysis(
      List<SloStatusResult.SloBudgetStatus> budgets,
      SloStatusResult.SloBudgetStatus leadingRisk,
      ConfidenceBreakdown confidenceBreakdown,
      List<String> limitations) {}

  public record Forecast(
      Instant projectedBreachAt, Duration timeToBreach, ConfidenceBreakdown confidenceBreakdown) {}
}
