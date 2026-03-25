# ADR-001: Read-only control-tower architecture

## Status

Accepted

## Context

The enterprise needs an MCP server that can support AI-assisted ProdOps workflows without creating a mutation path into production systems.

## Decision

Build one domain-specific MCP server with:

- Spring AI MCP exposure
- ports-and-adapters structure
- read-only upstream adapters
- deterministic evidence-backed reasoning
- fixture mode for demos and CI

## Consequences

- Stronger governance and easier review
- More implementation effort than a thin wrapper
- Clearer path to controlled non-prod rollout and later productionization
