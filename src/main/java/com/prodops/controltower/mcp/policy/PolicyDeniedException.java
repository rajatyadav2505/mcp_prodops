package com.prodops.controltower.mcp.policy;

public class PolicyDeniedException extends RuntimeException {

  public PolicyDeniedException(String message) {
    super(message);
  }
}
