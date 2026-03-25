# AGENTS.md

This repository builds `prodops-control-tower-mcp`, a Java 21 Spring Boot MCP server for Production Support Intelligence. Treat it as a production-grade, read-only operational system, not a demo.

## Product rules

- Keep the product narrative consistent: one control-tower product across Kubernetes, Prometheus, and Grafana.
- Preserve the hard read-only stance. Do not add mutating upstream actions of any kind.
- Never add Secret reads, token passthrough, `kubectl` shell-outs, exec, restart, scale, patch, or dashboard writes.
- Keep all timestamps as `Instant` and all durations as `Duration`.

## Architecture rules

- MCP annotations stay thin and delegate immediately to domain services.
- Domain services talk to typed ports, policy helpers, redaction, and scoring logic.
- Adapters own upstream API access.
- Do not place business logic in config classes or MCP tool/resource/prompt methods.
- Avoid map-shaped APIs where typed records already exist.

## Engineering rules

- Java only for the server. Small shell tooling is fine.
- Constructor injection only. No field injection.
- Prefer package-private classes where public access is unnecessary.
- Keep fixture mode deterministic and keep tests independent of live infrastructure.
- If you change scoring, policy, redaction, or an output schema, add or update tests.

## Verification

Run these before closing work:

- `./mvnw -B -ntp spotless:apply`
- `./mvnw -B -ntp clean verify`
- `docker build -t prodops-control-tower-mcp:local .`
