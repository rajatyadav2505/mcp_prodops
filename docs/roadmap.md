# Roadmap

## Current state

This repository is intentionally limited to one product domain: Production Support Intelligence. The current implementation already spans Kubernetes, Prometheus, Grafana, Bitbucket, Kibana, and Jaeger while keeping a hard read-only posture and deterministic reasoning model.

## Near-term roadmap

### Phase 1

- Controlled live rollout and upstream hardening in non-prod
- Curated service-catalog expansion for more services and teams
- Production auth integration and network-policy hardening
- Additional evidence-quality tuning for top critical services

### Phase 2

- Broader namespace coverage with explicit governance approval
- More service-specific PromQL, Kibana, and trace hints
- Richer fixture narratives for canary, drift, and security-posture scenarios
- Deeper runbook and leadership-reporting packs

### Phase 3

- Pattern reuse for adjacent enterprise systems using the same MCP delivery model
- Shared policy, audit, and deployment standards across enterprise MCP servers
- Reusable fixture kits for regulated change review and model evaluation

## How this generalizes without widening scope now

The reusable parts are the engineering pattern, not the current domain boundary:

- ports-and-adapters design
- deterministic scoring and evidence packet approach
- read-only safety controls
- fixture mode and CI pattern
- deployment, observability, and audit conventions

Future enterprise MCP servers should reuse the pattern while keeping each server domain-specific and governable.
