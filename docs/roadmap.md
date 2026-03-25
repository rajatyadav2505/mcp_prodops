# Roadmap

## What stays in scope now

This repository is intentionally limited to one product domain: Production Support Intelligence. It should be productionized for Kubernetes, Prometheus, and Grafana first before it is generalized further.

## Near-term roadmap

### Phase 1

- Controlled non-prod rollout
- Curated service-catalog expansion
- Production auth integration
- Additional evidence quality tuning for top critical services

### Phase 2

- More service-specific PromQL templates
- Stronger SLO-aware risk framing
- Broader namespace coverage with explicit governance approval
- Additional executive reporting prompts

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
