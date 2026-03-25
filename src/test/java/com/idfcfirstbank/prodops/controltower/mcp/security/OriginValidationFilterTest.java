package com.idfcfirstbank.prodops.controltower.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.idfcfirstbank.prodops.controltower.mcp.TestFixtures;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OriginValidationFilterTest {

  @Test
  void rejectsDisallowedOriginsForMcpRequests() throws ServletException, IOException {
    OriginValidationFilter filter =
        new OriginValidationFilter(
            TestFixtures.prodOpsProperties(
                Path.of("build/test-catalog.yaml"),
                Path.of("build/test-risk-weights.yaml"),
                Path.of("build/test-fixtures"),
                List.of("scenario_fixture_smoke")));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
    request.addHeader("Origin", "https://evil.example");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (FilterChain)
            (servletRequest, servletResponse) -> {
              throw new AssertionError("Filter chain should not run for rejected origins.");
            });

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getErrorMessage()).contains("Origin is not permitted");
  }

  @Test
  void allowsMissingOrTrustedOriginsForMcpRequests() throws ServletException, IOException {
    OriginValidationFilter filter =
        new OriginValidationFilter(
            TestFixtures.prodOpsProperties(
                Path.of("build/test-catalog.yaml"),
                Path.of("build/test-risk-weights.yaml"),
                Path.of("build/test-fixtures"),
                List.of("scenario_fixture_smoke")));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
    request.addHeader("Origin", "http://localhost:6274");
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] chainInvoked = {false};

    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) -> {
          chainInvoked[0] = true;
          ((MockHttpServletResponse) servletResponse).setStatus(204);
        });

    assertThat(chainInvoked[0]).isTrue();
    assertThat(response.getStatus()).isEqualTo(204);
  }
}
