package com.prodops.controltower.mcp.security;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OriginValidationFilter extends OncePerRequestFilter {

  private final ProdOpsProperties properties;

  public OriginValidationFilter(ProdOpsProperties properties) {
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(properties.http().mcpPath())
        || !properties.guardrails().requireOriginValidation();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String origin = request.getHeader("Origin");
    if (origin == null || isAllowed(origin, properties.http().allowedOrigins())) {
      filterChain.doFilter(request, response);
      return;
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin is not permitted for MCP access.");
  }

  private boolean isAllowed(String origin, List<String> allowedOrigins) {
    return allowedOrigins.stream()
        .anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, origin));
  }
}
