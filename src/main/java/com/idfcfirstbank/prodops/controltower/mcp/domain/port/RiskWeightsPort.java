package com.idfcfirstbank.prodops.controltower.mcp.domain.port;

import com.idfcfirstbank.prodops.controltower.mcp.domain.scoring.RiskWeights;

public interface RiskWeightsPort {

  RiskWeights getWeights();
}
