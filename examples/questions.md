# Example Questions

- Why is `payments-api` unhealthy in UAT right now?
- Which namespaces show the highest operational risk in the last 60 minutes?
- Did the latest rollout correlate with the latency spike in `upi-recon`?
- Which Bitbucket change most likely broke `payments-api` in the last 60 minutes?
- Show the top Kibana error signatures for `upi-recon` and correlate them with Jaeger traces.
- Did a recent PR cause the latency spike, or is this a downstream dependency issue?
- Compare the impact before and after commit `XYZ` on `tradex-gateway`.
- Find similar incidents to the current outage.
- Where are our observability blind spots for root cause attribution?
- What is the likely blast radius if `tradex-gateway` keeps failing?
- Which critical services are closest to SLO risk today?
- Give me a CTO summary of the top five production risks in the last 24 hours.
- Show me the best Grafana dashboard and the relevant evidence for `payments-api`.
- What changed in Kubernetes just before the error rate jumped?
- Give me an on-call handover for prodops-uat from 2026-03-24T18:00:00Z to 2026-03-25T06:00:00Z.
- Review recent release risk in payments-uat over the last 12 hours.
