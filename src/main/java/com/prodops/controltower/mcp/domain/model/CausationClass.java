package com.prodops.controltower.mcp.domain.model;

public enum CausationClass {
  LIKELY_ROOT_CAUSE,
  LIKELY_CONTRIBUTING_FACTOR,
  CORRELATED_BUT_NOT_CAUSAL,
  INSUFFICIENT_EVIDENCE
}
