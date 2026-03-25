package com.idfcfirstbank.prodops.controltower.mcp.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.idfcfirstbank.prodops.controltower.mcp.TestFixtures;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PolicyOutcome;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ScopePolicyTest {

  private final ScopePolicy policy =
      new ScopePolicy(
          TestFixtures.prodOpsProperties(
              java.nio.file.Path.of("build/test-catalog.yaml"),
              java.nio.file.Path.of("build/test-risk-weights.yaml"),
              java.nio.file.Path.of("build/test-fixtures"),
              java.util.List.of("scenario_fixture_smoke")));

  @Test
  void authorizesConfiguredClusterAndNamespace() {
    assertThat(policy.authorizeCluster("payments-dev", "alice").outcome())
        .isEqualTo(PolicyOutcome.ALLOW);
    assertThat(policy.authorizeNamespace("payments-dev", "payments", "alice").allowed()).isTrue();
  }

  @Test
  void deniesOutOfScopeNamespaceAndRejectsGuardrailViolations() {
    assertThat(policy.authorizeNamespace("payments-dev", "finance", "alice").outcome())
        .isEqualTo(PolicyOutcome.DENY);
    assertThatThrownBy(
            () ->
                policy.assertAllowed(policy.authorizeNamespace("payments-dev", "finance", "alice")))
        .isInstanceOf(PolicyDeniedException.class);
    assertThatThrownBy(() -> policy.verifyLookback(Duration.ofHours(12)))
        .isInstanceOf(GuardrailViolationException.class);
  }
}
