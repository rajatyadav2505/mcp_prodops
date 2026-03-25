package com.idfcfirstbank.prodops.controltower.mcp.policy;

public class PolicyDeniedException extends RuntimeException {

  public PolicyDeniedException(String message) {
    super(message);
  }
}
