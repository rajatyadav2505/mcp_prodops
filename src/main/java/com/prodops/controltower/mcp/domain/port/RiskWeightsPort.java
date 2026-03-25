package com.prodops.controltower.mcp.domain.port;

import com.prodops.controltower.mcp.domain.scoring.RiskWeights;

public interface RiskWeightsPort {

  RiskWeights getWeights();
}
