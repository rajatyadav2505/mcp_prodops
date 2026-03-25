package com.idfcfirstbank.prodops.controltower.mcp.domain.scoring;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class RiskScoreEngine {

  public RiskAssessment assess(RiskSignal signal, RiskWeights weights) {
    double numerator =
        normalized(signal.restarts(), 5) * weights.restartWeight()
            + normalized(signal.warningEvents(), 10) * weights.warningEventWeight()
            + inverseFreshness(signal.rolloutFreshnessMinutes()) * weights.rolloutFreshnessWeight()
            + bounded(signal.errorRateRatio()) * weights.errorRateWeight()
            + bounded(signal.latencyRatio()) * weights.latencyWeight()
            + bounded(signal.cpuSaturationRatio()) * weights.cpuSaturationWeight()
            + bounded(signal.memoryPressureRatio()) * weights.memoryPressureWeight()
            + bounded(signal.unavailableReplicaRatio()) * weights.unavailableReplicaWeight()
            + bounded(signal.dependencyUncertaintyRatio()) * weights.dependencyUncertaintyWeight()
            + bounded(signal.noiseRatio()) * weights.noiseWeight();

    double denominator =
        weights.restartWeight()
            + weights.warningEventWeight()
            + weights.rolloutFreshnessWeight()
            + weights.errorRateWeight()
            + weights.latencyWeight()
            + weights.cpuSaturationWeight()
            + weights.memoryPressureWeight()
            + weights.unavailableReplicaWeight()
            + weights.dependencyUncertaintyWeight()
            + weights.noiseWeight();

    double score = Math.round((numerator / denominator) * 100.0d);
    return new RiskAssessment(score, level(score, weights));
  }

  private double normalized(double value, double max) {
    return Math.min(1.0d, value / max);
  }

  private double bounded(double value) {
    return Math.min(1.0d, Math.max(0.0d, value));
  }

  private double inverseFreshness(double minutes) {
    if (minutes <= 0) {
      return 1.0d;
    }
    if (minutes <= 15) {
      return 1.0d;
    }
    if (minutes <= 60) {
      return 0.7d;
    }
    if (minutes <= 240) {
      return 0.35d;
    }
    return 0.1d;
  }

  private RiskLevel level(double score, RiskWeights weights) {
    if (score >= weights.criticalThreshold()) {
      return RiskLevel.CRITICAL;
    }
    if (score >= weights.highThreshold()) {
      return RiskLevel.HIGH;
    }
    if (score >= weights.moderateThreshold()) {
      return RiskLevel.MODERATE;
    }
    return RiskLevel.LOW;
  }
}
