# ProdOps Control Tower MCP Architecture

## Design goals

- One operational control-tower product, not three disconnected wrappers
- Deterministic, auditable reasoning with evidence and confidence
- Strict read-only posture with explicit policy and governance
- Clean separation between MCP exposure, domain services, and upstream adapters

## Component boundaries

```mermaid
flowchart TB
    subgraph Exposure["MCP Exposure Layer"]
      Tools["Tools"]
      Resources["Resources"]
      Prompts["Prompts"]
    end

    subgraph Domain["Domain Layer"]
      Inventory["InventoryService"]
      Observability["ObservabilityService"]
      Intelligence["IntelligenceService"]
      ResourceSvc["ResourceService"]
      PromptSvc["PromptTemplateService"]
      Scoring["RiskScoreEngine"]
      Correlation["Change / Blast Radius / Capacity Engines"]
    end

    subgraph Controls["Cross-cutting Controls"]
      Policy["ScopePolicy"]
      Redaction["RedactionService"]
      Audit["AuditService"]
      Rate["RequestRateLimiter"]
      Security["Spring Security + OriginValidationFilter"]
    end

    subgraph Adapters["Adapter Layer"]
      K8s["ClusterInventoryPort\nFixture or Live Kubernetes"]
      Prom["MetricsPort\nFixture or Live Prometheus"]
      Graf["DashboardPort\nFixture or Live Grafana"]
      Catalog["ServiceCatalogPort / RiskWeightsPort"]
    end

    Exposure --> Domain
    Domain --> Controls
    Domain --> Adapters
```

## Request flow

```mermaid
sequenceDiagram
    participant Client
    participant HTTP as HTTP / Stdio Transport
    participant MCP as MCP Tool Method
    participant Policy as Policy + Audit + Rate Limit
    participant Domain as Domain Service
    participant Adapters as K8s / Prom / Grafana Adapters

    Client->>HTTP: MCP request
    HTTP->>Policy: Origin, auth, correlation id, rate limit
    Policy->>MCP: allow
    MCP->>Domain: typed request only
    Domain->>Policy: scope + lookback guardrails
    Domain->>Adapters: read-only upstream calls
    Adapters-->>Domain: typed evidence inputs
    Domain-->>MCP: structured response with evidence
    MCP-->>Client: deterministic MCP output
```

## Security boundaries

```mermaid
flowchart LR
    Client["External MCP client"] --> Auth["JWT validation\noptional in remote mode"]
    Client --> Origin["Origin validation\nfor /mcp"]
    Auth --> MCP["MCP endpoint"]
    Origin --> MCP
    MCP --> Policy["Namespace scope / result caps / lookback caps"]
    Policy --> Upstreams["Read-only upstream APIs"]
    Upstreams --> Redaction["Redaction before output assembly"]
    Redaction --> Audit["Structured audit log with argument hash"]
```

## Runtime shape

- Primary transport is Spring MVC over stateless HTTP at `/mcp`
- Secondary transport is stdio for local development and local MCP clients
- Actuator remains separate from MCP on port `8081` by default
- Fixture adapters and live adapters are profile-switched and share the same ports-and-adapters contract

## Architectural consequences

- MCP annotations stay thin, so the server is testable without an MCP client
- The same domain services power tools, resources, and prompt templates
- Read-only safety is enforced in code paths, not only in documentation
- Curated and discovery modes coexist without changing the public tool contracts
