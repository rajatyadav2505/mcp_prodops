# Read-Only Safety Contract

## Upstream systems read by this server

- Kubernetes API
- Prometheus HTTP API
- Grafana HTTP API
- Local YAML files for service catalog and risk weights

## Kubernetes objects in scope

- Namespaces
- Deployments
- StatefulSets
- DaemonSets
- Jobs
- CronJobs
- Pods
- Events
- Services
- Ingress resources
- HorizontalPodAutoscalers
- PodDisruptionBudgets
- Owner references and non-sensitive metadata
- `pods/log` only when explicitly requested, bounded, and redacted

## Allowed verbs and API surfaces

- Kubernetes: `get`, `list`, `watch`, and bounded `pods/log`
- Prometheus: query and query_range over the HTTP API
- Grafana: dashboard search and dashboard retrieval APIs only

## Explicitly prohibited

- create
- update
- patch
- delete
- restart
- rollout trigger
- scale
- exec
- port-forward
- annotate
- cordon
- drain
- silence
- acknowledge
- save
- import
- any dashboard write API
- any Secret read or Secret-value exposure

## Redaction behavior

The server redacts:

- bearer tokens
- passwords
- secret-like key/value pairs
- API keys
- token-like long base64 strings
- credential-bearing connection strings

Redaction occurs before tool output assembly. Bounded log excerpts are sanitized line by line.

## Scope control

- Only configured clusters are accessible.
- Optional namespace allowlists are enforced before namespace-level reads.
- Lookback windows, result sizes, series counts, dashboard counts, and log-line counts are bounded by policy.
- HTTP MCP requests must pass origin validation when enabled.
- JWT validation can require issuer, audience, expiry, and scope in remote mode.

## How this is enforced

- `ScopePolicy` denies out-of-scope clusters and namespaces.
- `OriginValidationFilter` rejects invalid MCP origins.
- `SecurityConfig` supports resource-server validation for HTTP deployments.
- Adapter implementations expose only read paths.
- Tests assert architecture boundaries, guardrails, redaction, and origin rejection.
