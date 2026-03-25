package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

public record PolicyDecision(PolicyOutcome outcome, String reason, String scope, String identity) {

  public boolean allowed() {
    return outcome == PolicyOutcome.ALLOW;
  }
}
