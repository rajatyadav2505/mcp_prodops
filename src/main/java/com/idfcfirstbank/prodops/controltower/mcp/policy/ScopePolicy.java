package com.idfcfirstbank.prodops.controltower.mcp.policy;

import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PolicyDecision;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PolicyOutcome;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScopePolicy {

  private final ProdOpsProperties properties;

  public ScopePolicy(ProdOpsProperties properties) {
    this.properties = properties;
  }

  public PolicyDecision authorizeCluster(String cluster, String identity) {
    return properties.clusters().stream()
        .filter(config -> config.enabled() && config.name().equals(cluster))
        .findFirst()
        .map(
            config ->
                new PolicyDecision(
                    PolicyOutcome.ALLOW,
                    "Cluster is configured for read-only access.",
                    cluster,
                    identity))
        .orElseGet(
            () ->
                new PolicyDecision(
                    PolicyOutcome.DENY, "Cluster is outside configured scope.", cluster, identity));
  }

  public PolicyDecision authorizeNamespace(String cluster, String namespace, String identity) {
    return properties.clusters().stream()
        .filter(config -> config.enabled() && config.name().equals(cluster))
        .findFirst()
        .map(config -> evaluateNamespace(config.namespaceAllowlist(), cluster, namespace, identity))
        .orElseGet(
            () ->
                new PolicyDecision(
                    PolicyOutcome.DENY, "Cluster is outside configured scope.", cluster, identity));
  }

  public void verifyLookback(Duration lookback) {
    if (lookback.compareTo(properties.guardrails().maxLookback()) > 0) {
      throw new GuardrailViolationException("Requested lookback exceeds configured maximum.");
    }
  }

  public void verifyRange(Duration range) {
    if (range.compareTo(properties.guardrails().maxRange()) > 0) {
      throw new GuardrailViolationException("Requested range exceeds configured maximum.");
    }
  }

  public void verifyLogLines(int tailLines) {
    if (tailLines > properties.guardrails().maxLogLines()) {
      throw new GuardrailViolationException("Requested log tail exceeds configured maximum.");
    }
  }

  public void verifyDashboardLimit(int requested) {
    if (requested > properties.guardrails().maxDashboards()) {
      throw new GuardrailViolationException(
          "Requested dashboard count exceeds configured maximum.");
    }
  }

  public void assertAllowed(PolicyDecision decision) {
    if (!decision.allowed()) {
      throw new PolicyDeniedException(decision.reason());
    }
  }

  private PolicyDecision evaluateNamespace(
      List<String> allowlist, String cluster, String namespace, String identity) {
    if (allowlist == null || allowlist.isEmpty() || allowlist.contains(namespace)) {
      return new PolicyDecision(
          PolicyOutcome.ALLOW,
          "Namespace is within configured scope.",
          cluster + "/" + namespace,
          identity);
    }
    return new PolicyDecision(
        PolicyOutcome.DENY,
        "Namespace is outside configured allowlist.",
        cluster + "/" + namespace,
        identity);
  }
}
