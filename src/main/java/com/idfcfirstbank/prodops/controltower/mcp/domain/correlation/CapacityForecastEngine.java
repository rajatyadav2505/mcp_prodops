package com.idfcfirstbank.prodops.controltower.mcp.domain.correlation;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.MetricSeries;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.MetricSeriesPoint;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CapacityForecastEngine {

  public Forecast forecast(List<MetricSeries> series, Duration horizonMinutes) {
    if (series.isEmpty() || series.getFirst().points().size() < 2) {
      return new Forecast(0.0d, 0.0d, 0.25d, "Insufficient series history for forecast.");
    }
    List<MetricSeriesPoint> points = series.getFirst().points();
    MetricSeriesPoint first = points.getFirst();
    MetricSeriesPoint last = points.getLast();
    double current = last.value();
    double minutesBetween =
        Math.max(1.0d, Duration.between(first.timestamp(), last.timestamp()).toSeconds() / 60.0d);
    double slopePerMinute = (last.value() - first.value()) / minutesBetween;
    double forecast = current + (slopePerMinute * horizonMinutes.toMinutes());
    double confidence = Math.min(0.85d, 0.45d + Math.min(0.3d, points.size() * 0.03d));
    return new Forecast(
        current,
        forecast,
        confidence,
        slopePerMinute >= 0 ? "Positive slope detected." : "Flat or declining slope.");
  }

  public record Forecast(
      double currentValue, double forecastValue, double confidence, String rationale) {}
}
