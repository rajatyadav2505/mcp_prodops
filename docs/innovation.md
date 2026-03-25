# Innovation Summary

## This is not a wrapper farm

Naive MCP servers often expose one tool per upstream API endpoint and leave the real reasoning burden to the client model. `ProdOps Control Tower MCP` is different: it turns three operational planes into one evidence-backed intelligence plane.

## Differentiators

### Cross-plane evidence packs

Flagship tools do not return loose prose. They return structured evidence with source, object, metric, query, timestamp, value hints, links, and confidence. That makes the output auditable and easier for external AI clients to explain.

### Transparent risk scoring

The risk model is explicit and configurable. Operations teams can tune weights for restarts, warning events, rollout freshness, error rate, latency degradation, CPU pressure, memory pressure, unavailable replicas, dependency uncertainty, and noise.

### Change correlation

The server reasons about whether a recent rollout is likely causal, possibly related, or unlikely. It combines rollout timing, warning events, and metric degradation into a structured change timeline instead of hand-wavy text.

### Blast-radius estimation

The server estimates likely impact using service selectors, ingress backends, owner references, scaling constraints, and topology hints. It does not overclaim certainty; it exposes confidence and limitations.

### Dual operating modes

- Discovery mode works from cluster labels, object metadata, dashboard search, and generic metric heuristics.
- Curated mode adds service-to-workload mappings, dashboards, SLOs, runbooks, and PromQL templates from YAML.

### Explainability packet

Composite outputs include:

- supporting evidence
- counterevidence
- alternative hypotheses
- confidence
- limitations
- freshness metadata

That is the core differentiator from a thin operational wrapper.
