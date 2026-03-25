# Demo Script

## Objective

Demonstrate that `ProdOps Control Tower MCP` is a real control-tower server that can support both engineers and executives using deterministic, evidence-backed, read-only workflows.

## Suggested sequence

1. Start the server in fixture HTTP mode.
2. Open MCP Inspector or Claude Code.
3. Show resource discovery first.
4. Move from inventory to diagnosis to cross-plane reasoning.
5. End with management-facing summaries.

## Reviewer prompts

1. Prompt: `List the configured clusters and namespaces available to this server.`
   Tools/resources: `list_clusters`, `prodops://clusters`, `list_namespaces`
   Value shown: discoverability and scope governance

2. Prompt: `Show me the workloads in payments-uat.`
   Tools/resources: `list_workloads`, `prodops://cluster/bank-uat/namespace/payments-uat/workloads`
   Value shown: runtime inventory without kubectl

3. Prompt: `Why is payments-api unhealthy in UAT right now?`
   Tools/resources: `correlate_service_incident`
   Value shown: flagship executive and operator summaries with evidence

4. Prompt: `Show the workload health for payments-api and the best related dashboard.`
   Tools/resources: `get_workload_health`, `get_dashboard_summary`
   Value shown: joined workload and Grafana context

5. Prompt: `What changed in Kubernetes just before the error rate jumped for payments-api?`
   Tools/resources: `get_change_correlation`
   Value shown: rollout-to-metric causality reasoning

6. Prompt: `Give me pod diagnostics for payments-api-7d86c6c49c-x9t6v including logs.`
   Tools/resources: `get_pod_diagnostics`
   Value shown: bounded, redacted log excerpts with events and owner references

7. Prompt: `Did the latest rollout correlate with the latency spike in upi-recon?`
   Tools/resources: `get_change_correlation`, `correlate_service_incident`
   Value shown: subtle degradation without crashloop dependency

8. Prompt: `Forecast capacity risk for upi-recon over the next 240 minutes.`
   Tools/resources: `forecast_capacity_risk`
   Value shown: deterministic near-term risk forecast

9. Prompt: `What is the likely blast radius if tradex-gateway keeps failing?`
   Tools/resources: `estimate_blast_radius`
   Value shown: topology-aware, uncertainty-aware impact estimation

10. Prompt: `Give me a CTO summary of the top five production risks in the last 24 hours.`
    Tools/resources: `morning_prodops_brief`, `get_namespace_health`, `correlate_service_incident`
    Value shown: executive lane summary anchored in evidence

11. Prompt: `Search dashboards related to payments-api and explain which one is the best evidence plane.`
    Tools/resources: `search_dashboards`, `get_dashboard_summary`
    Value shown: Grafana search and deep-link support

12. Prompt: `Show me the explicit read-only contract this server follows.`
    Tools/resources: `prodops://governance/read-only-contract`
    Value shown: governance and safety transparency
