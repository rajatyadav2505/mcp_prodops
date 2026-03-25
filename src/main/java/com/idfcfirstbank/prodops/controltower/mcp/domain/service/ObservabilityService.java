package com.idfcfirstbank.prodops.controltower.mcp.domain.service;

import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PromqlExecutionResult;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.DashboardPort;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.MetricsPort;
import com.idfcfirstbank.prodops.controltower.mcp.policy.GuardrailViolationException;
import com.idfcfirstbank.prodops.controltower.mcp.policy.ScopePolicy;
import com.idfcfirstbank.prodops.controltower.mcp.support.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityService {

  private final MetricsPort metricsPort;
  private final DashboardPort dashboardPort;
  private final ScopePolicy scopePolicy;
  private final ProdOpsProperties properties;

  public ObservabilityService(
      MetricsPort metricsPort,
      DashboardPort dashboardPort,
      ScopePolicy scopePolicy,
      ProdOpsProperties properties) {
    this.metricsPort = metricsPort;
    this.dashboardPort = dashboardPort;
    this.scopePolicy = scopePolicy;
    this.properties = properties;
  }

  public PromqlExecutionResult runPromqlInstant(
      String cluster, String query, Instant time, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    if (!properties.guardrails().rawPromqlEnabled()) {
      throw new GuardrailViolationException("Raw PromQL execution is disabled by policy.");
    }
    return metricsPort.instantQuery(cluster, query, time);
  }

  public PromqlExecutionResult runPromqlRange(
      String cluster, String query, Instant start, Instant end, Duration step, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    if (!properties.guardrails().rawPromqlEnabled()) {
      throw new GuardrailViolationException("Raw PromQL execution is disabled by policy.");
    }
    scopePolicy.verifyRange(Duration.between(start, end));
    if (step.compareTo(properties.guardrails().minStep()) < 0) {
      throw new GuardrailViolationException("Requested step is below the configured minimum.");
    }
    return metricsPort.rangeQuery(cluster, query, start, end, step);
  }

  public List<DashboardInfo> searchDashboards(
      String cluster, String query, List<String> tags, String folder, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    return dashboardPort.search(
        cluster, query, tags, folder, properties.guardrails().maxDashboards());
  }

  public DashboardInfo getDashboardSummary(String cluster, String dashboardUid, String identity) {
    scopePolicy.assertAllowed(scopePolicy.authorizeCluster(cluster, identity));
    return dashboardPort
        .getByUid(cluster, dashboardUid)
        .orElseThrow(
            () -> new NotFoundException("Dashboard UID is not available in the configured scope."));
  }
}
