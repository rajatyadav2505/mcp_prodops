package com.prodops.controltower.mcp.domain.scoring;

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
    double changeRecencyWeight,
    double fileAffinityWeight,
    double pipelineFailureWeight,
    double logSignatureNoveltyWeight,
    double traceFailureAlignmentWeight,
    double dependencyPropagationWeight,
    double evidenceConflictPenalty,
    double uncertaintyPenalty,
    int moderateThreshold,
    int highThreshold,
    int criticalThreshold) {

  public static RiskWeights defaults() {
    return new RiskWeights(
        10, 8, 7, 15, 14, 10, 9, 13, 7, 7, 12, 10, 11, 9, 12, 9, 8, 10, 35, 60, 80);
  }
}
