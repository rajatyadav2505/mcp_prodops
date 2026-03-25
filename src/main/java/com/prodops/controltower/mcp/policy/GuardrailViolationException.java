package com.prodops.controltower.mcp.policy;

public class GuardrailViolationException extends RuntimeException {

  public GuardrailViolationException(String message) {
    super(message);
  }
}
