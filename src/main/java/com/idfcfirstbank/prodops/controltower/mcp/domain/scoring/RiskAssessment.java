package com.idfcfirstbank.prodops.controltower.mcp.domain.scoring;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.RiskLevel;

public record RiskAssessment(double score, RiskLevel level) {}
