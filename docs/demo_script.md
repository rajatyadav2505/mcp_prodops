# Demo Script

## Objective

Demonstrate that `ProdOps Control Tower MCP` is a real control-tower server that can support both engineers and executives using deterministic, evidence-backed, read-only workflows.

## Suggested sequence

1. Start the server in fixture HTTP mode.
2. Open MCP Inspector or Claude Code.
3. Show resource discovery first.
4. Move from inventory to diagnosis to cross-plane reasoning.
5. Show operational posture and deploy/comparison workflows.
6. End with management-facing summaries.

## Reviewer prompts

1. Prompt: `List the configured clusters and namespaces available to this server.`
   Tools/resources: `list_clusters`, `prodops://clusters`, `list_namespaces`
   Value shown: discoverability and scope governance

2. Prompt: `Show me the workloads in payments-uat.`
   Tools/resources: `list_workloads`, `prodops://cluster/prodops-uat/namespace/payments-uat/workloads`
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

6. Prompt: `Which Bitbucket change most likely broke payments-api in the last 60 minutes?`
   Tools/resources: `get_change_regression_attribution`, `get_root_cause_analysis`
   Value shown: explainable change blame with supporting and weakening evidence

7. Prompt: `Show the top Kibana error signatures for payments-api and correlate them with Jaeger traces.`
   Tools/resources: `summarize_kibana_errors`, `search_jaeger_traces`, `get_jaeger_trace_summary`
   Value shown: cross-plane log and trace evidence with deep links

8. Prompt: `What is the real-time SLO burn rate for payments-api and when will it breach?`
   Tools/resources: `check_slo_status`, `slo_breach_forecast`
   Value shown: deterministic SLO and error-budget posture

9. Prompt: `Show recent rollout revisions for payments-api and compare canary vs stable.`
   Tools/resources: `rollout_history`, `canary_health_check`, `compare_pre_post_deploy`
   Value shown: deploy-lineage reasoning and before/after comparison

10. Prompt: `What depends on payments-api, and is failure cascading downstream?`
    Tools/resources: `map_service_dependencies`, `detect_cascading_failure`, `estimate_blast_radius`
    Value shown: topology-aware dependency and propagation reasoning

11. Prompt: `Which workloads are wasting resources, and what should we right-size?`
    Tools/resources: `identify_resource_waste`, `right_sizing_recommendations`
    Value shown: read-only cost and capacity optimization guidance

12. Prompt: `Show me the security posture, image freshness, and network isolation for payments-api.`
    Tools/resources: `security_posture_scan`, `image_freshness_check`, `check_network_policies`, `check_ingress_health`
    Value shown: read-only security and exposure review

13. Prompt: `How does payments-api differ between UAT and PROD right now?`
    Tools/resources: `compare_clusters`, `cross_cluster_drift`, `daily_risk_trend`
    Value shown: multi-cluster comparison and trend framing

14. Prompt: `Export the incident timeline and summarize the war-room state.`
    Tools/resources: `incident_timeline_export`, `war_room_briefing`, `post_mortem_assistant`
    Value shown: operator and leadership reporting packs

15. Prompt: `Show me the explicit read-only contract this server follows.`
    Tools/resources: `prodops://governance/read-only-contract`
    Value shown: governance and safety transparency
