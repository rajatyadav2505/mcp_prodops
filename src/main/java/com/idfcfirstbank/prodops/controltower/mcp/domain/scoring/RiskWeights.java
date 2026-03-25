package com.idfcfirstbank.prodops.controltower.mcp.domain.scoring;

public record RiskWeights(
    double restartWeight,
    double warningEventWeight,
    double rolloutFreshnessWeight,
    double errorRateWeight,
    double latencyWeight,
    double cpuSaturationWeight,
    double memoryPressureWeight,
    double unavailableReplicaWeight,
    double dependencyUncertaintyWeight,
    double noiseWeight,
    int moderateThreshold,
    int highThreshold,
    int criticalThreshold) {

  public static RiskWeights defaults() {
    return new RiskWeights(10, 8, 7, 15, 14, 10, 9, 13, 7, 7, 35, 60, 80);
  }
}
