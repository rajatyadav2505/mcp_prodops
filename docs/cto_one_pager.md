# CTO One Pager

## What it is

`ProdOps Control Tower MCP` is a productionizable MCP server that exposes Production Support Intelligence from Kubernetes, Prometheus, and Grafana to approved AI clients. It gives the bank a safe bridge between chat-based AI workflows and authoritative operational systems without embedding an LLM inside the server.

## Why it matters

- Faster triage: engineers can ask one control-tower question instead of manually traversing cluster state, metrics, and dashboards.
- Lower MTTR: the server correlates rollout timing, warning events, golden signals, and dashboard evidence.
- Safer AI operations: the system is strictly read-only and explicitly blocks mutating infrastructure actions.
- Executive visibility: the same server can answer both engineer-grade and CTO-grade questions with evidence-backed summaries.

## What is novel here

- It is not a thin wrapper around three APIs.
- It assembles cross-plane evidence packs with provenance, confidence, and counterevidence.
- It uses transparent service risk scoring that operations teams can tune through YAML.
- It brings curated service-catalog context and generic discovery mode into one governed runtime.

## Risk controls

- Read-only upstream access only
- No Secret reads or token passthrough
- Namespace and cluster scope controls
- Origin validation on MCP HTTP traffic
- Optional JWT issuer and audience validation
- Redaction of token-like strings and sensitive connection data
- Rate limits, result caps, lookback caps, log caps, and audit logs

## Rollout plan

1. Run in fixture mode and validate client workflows.
2. Roll out to controlled non-prod with one UAT cluster, Prometheus, and Grafana instance.
3. Start with discovery mode and limited namespace allowlists.
4. Add curated service catalog entries for the highest-value services.
5. Review audit logs, performance, and evidence quality before widening coverage.
6. Productionize with platform-managed identity, secret delivery, and network restrictions.

## Why it generalizes inside the bank

This repository establishes a reusable pattern: one domain-specific MCP server, typed schemas, clear safety contract, ports-and-adapters design, fixture mode for CI, and production-grade governance. The same delivery pattern can later be reused for payments operations, middleware, data platforms, or internal observability estates without widening this server's current scope.
