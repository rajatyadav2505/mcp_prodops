package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.RiskLevel;
import com.prodops.controltower.mcp.domain.model.SecurityPostureResult;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SecurityPostureAnalyzer {

  public Analysis analyze(WorkloadInfo workload) {
    List<SecurityPostureResult.SecurityPostureFinding> findings = new ArrayList<>();
    int score = 100;
    score = applyFindings(workload, findings, score);
    RiskLevel riskLevel =
        score < 50
            ? RiskLevel.CRITICAL
            : score < 70 ? RiskLevel.HIGH : score < 85 ? RiskLevel.MODERATE : RiskLevel.LOW;
    return new Analysis(score, riskLevel, findings);
  }

  private int applyFindings(
      WorkloadInfo workload,
      List<SecurityPostureResult.SecurityPostureFinding> findings,
      int score) {
    if (!Boolean.TRUE.equals(workload.runAsNonRoot())) {
      findings.add(
          new SecurityPostureResult.SecurityPostureFinding(
              "Run as non-root",
              "Workload does not clearly enforce non-root execution in pod or container security context.",
              "high",
              false));
      score -= 25;
    } else {
      findings.add(
          new SecurityPostureResult.SecurityPostureFinding(
              "Run as non-root", "Workload enforces non-root execution.", "low", true));
    }
    if (Boolean.TRUE.equals(workload.privileged())) {
      findings.add(
          new SecurityPostureResult.SecurityPostureFinding(
              "Privileged containers",
              "At least one container appears to request privileged mode.",
              "critical",
              false));
      score -= 25;
    }
    if (Boolean.TRUE.equals(workload.hostNetwork())) {
      findings.add(
          new SecurityPostureResult.SecurityPostureFinding(
              "Host networking",
              "Workload is attached to the host network namespace.",
              "high",
              false));
      score -= 15;
    }
    if (!workload.hasResourceLimits()) {
      findings.add(
          new SecurityPostureResult.SecurityPostureFinding(
              "Resource limits", "Workload does not declare resource limits.", "medium", false));
      score -= 15;
    }
    if (!Boolean.TRUE.equals(workload.hasReadinessProbe())) {
      findings.add(
          new SecurityPostureResult.SecurityPostureFinding(
              "Readiness probe", "Workload does not expose a readiness probe.", "medium", false));
      score -= 10;
    }
    return Math.max(0, score);
  }

  public record Analysis(
      int score,
      RiskLevel riskLevel,
      List<SecurityPostureResult.SecurityPostureFinding> findings) {}
}
