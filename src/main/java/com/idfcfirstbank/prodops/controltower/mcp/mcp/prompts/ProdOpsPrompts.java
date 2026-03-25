package com.idfcfirstbank.prodops.controltower.mcp.mcp.prompts;

import com.idfcfirstbank.prodops.controltower.mcp.domain.service.PromptTemplateService;
import com.idfcfirstbank.prodops.controltower.mcp.mcp.McpContentSupport;
import com.idfcfirstbank.prodops.controltower.mcp.mcp.McpInvocationSupport;
import com.idfcfirstbank.prodops.controltower.mcp.support.ArgumentMap;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

@Service
public class ProdOpsPrompts {

  private final PromptTemplateService promptTemplateService;
  private final McpInvocationSupport invocationSupport;
  private final McpContentSupport contentSupport;

  public ProdOpsPrompts(
      PromptTemplateService promptTemplateService,
      McpInvocationSupport invocationSupport,
      McpContentSupport contentSupport) {
    this.promptTemplateService = promptTemplateService;
    this.invocationSupport = invocationSupport;
    this.contentSupport = contentSupport;
  }

  @McpPrompt(
      name = "triage_service_incident",
      title = "Triage service incident",
      description = "Disciplined cross-plane incident triage workflow.")
  public GetPromptResult triageServiceIncident(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "namespace", description = "Namespace name", required = true) String namespace,
      @McpArg(
              name = "service_or_workload",
              description = "Service ID or workload name",
              required = true)
          String serviceOrWorkload,
      @McpArg(name = "lookback_minutes", description = "Lookback window minutes", required = true)
          int lookbackMinutes) {
    return invocationSupport.invoke(
        "prompt",
        "triage_service_incident",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "service_or_workload", serviceOrWorkload,
            "lookback_minutes", lookbackMinutes),
        () ->
            contentSupport.prompt(
                "Incident triage workflow",
                promptTemplateService.triageServiceIncident(
                    cluster, namespace, serviceOrWorkload, lookbackMinutes)));
  }

  @McpPrompt(
      name = "morning_prodops_brief",
      title = "Morning brief",
      description = "Shift-start overview of top risks, changes, and hotspots.")
  public GetPromptResult morningProdopsBrief(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "lookback_hours", description = "Lookback hours", required = true)
          int lookbackHours) {
    return invocationSupport.invoke(
        "prompt",
        "morning_prodops_brief",
        ArgumentMap.of("cluster", cluster, "lookback_hours", lookbackHours),
        () ->
            contentSupport.prompt(
                "Morning ProdOps brief",
                promptTemplateService.morningProdopsBrief(cluster, lookbackHours)));
  }

  @McpPrompt(
      name = "executive_incident_summary",
      title = "Executive incident summary",
      description = "CTO-facing incident framing with confidence, evidence, and limitations.")
  public GetPromptResult executiveIncidentSummary(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "namespace", description = "Namespace name", required = true) String namespace,
      @McpArg(
              name = "service_or_workload",
              description = "Service ID or workload name",
              required = true)
          String serviceOrWorkload,
      @McpArg(name = "lookback_minutes", description = "Lookback minutes", required = true)
          int lookbackMinutes) {
    return invocationSupport.invoke(
        "prompt",
        "executive_incident_summary",
        ArgumentMap.of(
            "cluster", cluster,
            "namespace", namespace,
            "service_or_workload", serviceOrWorkload,
            "lookback_minutes", lookbackMinutes),
        () ->
            contentSupport.prompt(
                "Executive incident summary",
                promptTemplateService.executiveIncidentSummary(
                    cluster, namespace, serviceOrWorkload, lookbackMinutes)));
  }

  @McpPrompt(
      name = "oncall_handover",
      title = "On-call handover",
      description = "Summarize changes, unresolved risks, and watchpoints.")
  public GetPromptResult oncallHandover(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "since", description = "Since RFC3339", required = true) String since,
      @McpArg(name = "until", description = "Until RFC3339", required = true) String until) {
    return invocationSupport.invoke(
        "prompt",
        "oncall_handover",
        ArgumentMap.of("cluster", cluster, "since", since, "until", until),
        () ->
            contentSupport.prompt(
                "On-call handover", promptTemplateService.oncallHandover(cluster, since, until)));
  }

  @McpPrompt(
      name = "release_risk_review",
      title = "Release risk review",
      description = "Review recent rollouts and identify risky changes.")
  public GetPromptResult releaseRiskReview(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(name = "namespace", description = "Namespace name", required = true) String namespace,
      @McpArg(name = "lookback_hours", description = "Lookback hours", required = true)
          int lookbackHours) {
    return invocationSupport.invoke(
        "prompt",
        "release_risk_review",
        ArgumentMap.of("cluster", cluster, "namespace", namespace, "lookback_hours", lookbackHours),
        () ->
            contentSupport.prompt(
                "Release risk review",
                promptTemplateService.releaseRiskReview(cluster, namespace, lookbackHours)));
  }

  @McpPrompt(
      name = "capacity_risk_review",
      title = "Capacity risk review",
      description = "Summarize near-term capacity risks and monitoring actions.")
  public GetPromptResult capacityRiskReview(
      @McpArg(name = "cluster", description = "Cluster name", required = true) String cluster,
      @McpArg(
              name = "namespace_or_scope",
              description = "Namespace or service scope",
              required = true)
          String namespaceOrScope,
      @McpArg(name = "horizon_minutes", description = "Forecast horizon minutes", required = true)
          int horizonMinutes) {
    return invocationSupport.invoke(
        "prompt",
        "capacity_risk_review",
        ArgumentMap.of(
            "cluster",
            cluster,
            "namespace_or_scope",
            namespaceOrScope,
            "horizon_minutes",
            horizonMinutes),
        () ->
            contentSupport.prompt(
                "Capacity risk review",
                promptTemplateService.capacityRiskReview(
                    cluster, namespaceOrScope, horizonMinutes)));
  }
}
