# Ops Runbook

## Deployment modes

- `fixture,http`: local demos and deterministic CI-style smoke runs
- `fixture,stdio`: local MCP client usage without HTTP
- `live,http`: controlled non-prod and production-like deployment mode

## Configuration

Primary configuration lives in:

- [src/main/resources/application.yml](/Users/rajatyadav/MCP ProdOps/src/main/resources/application.yml)
- [config/config.example.yaml](/Users/rajatyadav/MCP ProdOps/config/config.example.yaml)
- [config/service-catalog.example.yaml](/Users/rajatyadav/MCP ProdOps/config/service-catalog.example.yaml)
- [config/risk-weights.example.yaml](/Users/rajatyadav/MCP ProdOps/config/risk-weights.example.yaml)

Important runtime knobs:

- `SPRING_PROFILES_ACTIVE`
- `PRODOPS_BIND_ADDRESS`
- `PORT`
- `MANAGEMENT_PORT`
- `PRODOPS_JWT_ENABLED`
- `PROMETHEUS_BASE_URL`
- `GRAFANA_BASE_URL`
- `KUBECONFIG`
- `KUBE_CONTEXT`

## Health checks

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- Prometheus metrics: `/actuator/prometheus`

In Kubernetes, readiness and liveness probes should use the management port.

## Logging

- Structured JSON logging is configured through `logback-spring.xml`
- Every request gets a correlation id
- Audit logs include identity when available, tool or resource name, argument hash, timing, policy decision, and result size

## Observability of the MCP server

- Actuator health endpoints
- Prometheus metrics
- tool latency timers
- tool denial counters
- adapter error counters
- cache behavior metrics through Caffeine and Micrometer

## Incident handling for this server

1. Confirm readiness and liveness.
2. Check whether failures are local to MCP transport, policy rejection, or upstream adapter access.
3. Inspect audit logs for repeated denials or rate-limit saturation.
4. If only live adapters are failing, rerun in fixture mode to separate product defects from upstream reachability issues.
5. Review token and origin configuration before widening access.

## Safe rollback

- Roll back the container image or Helm release
- Preserve the same ConfigMap, Secret references, and service account unless the issue is configuration-driven
- If necessary, switch back to fixture mode for validation without live dependencies
