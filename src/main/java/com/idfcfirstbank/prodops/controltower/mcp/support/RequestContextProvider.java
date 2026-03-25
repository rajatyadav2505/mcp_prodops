package com.idfcfirstbank.prodops.controltower.mcp.support;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RequestContextProvider {

  public String identity() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return "anonymous";
    }
    return authentication.getName();
  }

  public String correlationId() {
    return Optional.ofNullable(MDC.get("correlationId"))
        .orElseGet(
            () -> {
              String value = UUID.randomUUID().toString();
              MDC.put("correlationId", value);
              return value;
            });
  }
}
