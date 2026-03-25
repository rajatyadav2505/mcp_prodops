package com.idfcfirstbank.prodops.controltower.mcp.domain.scoring;

public record RiskSignal(
    int restarts,
    int warningEvents,
    double rolloutFreshnessMinutes,
    double errorRateRatio,
    double latencyRatio,
    double cpuSaturationRatio,
    double memoryPressureRatio,
    double unavailableReplicaRatio,
    double dependencyUncertaintyRatio,
    double noiseRatio) {}
