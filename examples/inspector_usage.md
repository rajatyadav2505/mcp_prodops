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
3. List prompts and verify triage, morning brief, executive summary, handover, release, and capacity prompts.
4. Invoke `correlate_service_incident` against `prodops-uat / payments-uat / payments-api`.
5. Invoke `forecast_capacity_risk` against `prodops-uat / upi-ops / upi-recon`.
