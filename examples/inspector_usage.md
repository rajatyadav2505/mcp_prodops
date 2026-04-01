# MCP Inspector Usage

## Start the server

```bash
SPRING_PROFILES_ACTIVE=fixture,http \
PRODOPS_BIND_ADDRESS=127.0.0.1 \
PORT=8080 \
MANAGEMENT_PORT=8081 \
./mvnw spring-boot:run
```

## Connect the inspector

- Transport: Streamable HTTP
- URL: `http://127.0.0.1:8080/mcp`

## Recommended checks

1. List tools and verify inventory, observability, and intelligence tools are present.
2. List resources and verify `prodops://clusters`, catalog, governance, and question resources.
3. List prompts and verify triage, morning brief, executive summary, handover, release, capacity, post-mortem, runbook, war-room, and weekly-report prompts.
4. Invoke `correlate_service_incident` against `prodops-uat / payments-uat / payments-api`.
5. Invoke `get_root_cause_analysis` and `get_change_regression_attribution` for `prodops-uat / payments-uat / payments-api`.
6. Invoke `check_slo_status` and `slo_breach_forecast` for `prodops-uat / payments-uat / payments-api`.
7. Invoke `rollout_history`, `canary_health_check`, and `compare_pre_post_deploy` for `prodops-uat / payments-uat / payments-api`.
8. Invoke `search_kibana_logs`, `summarize_kibana_errors`, `search_jaeger_traces`, and `get_jaeger_trace_summary`.
9. Invoke `map_service_dependencies`, `detect_cascading_failure`, and `incident_timeline_export`.
