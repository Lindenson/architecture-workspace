---
name: architecture-review
description: Run a full architecture health analysis of the project. Trigger on "architecture review", "проведи архитектурный анализ", "покажи состояние архитектуры", "проанализируй архитектуру", before sign-off on a structural change to the payment flow or document ingestion pipeline.
---

# Architecture Review

## Purpose
Produce an Architecture Health Report grounded in facts: model consistency,
dependency graph health, layering/ArchUnit compliance, drift versus ADR and
Structurizr. Backs OPERATING_MODEL `ARCHITECT_SESSION` and AGENT_RUNTIME is
`RUN_ARCHITECTURE_RESCAN` (read-only synthesis, not a write).

## Inputs / Sources
- digital-twin-core: `showProjectState`, `runArchitectureRescan`
- jqassistant-mcp (planned) — dependency graph, cycles, forbidden deps
- ArchUnit reports under `quality/` ; Structurizr model (structurizr-mcp, planned)
- github-mcp / GitLab MCP — recent structural changes; ADR repo under `architecture/adr/`
- sonar-mcp — architectural smells

## Procedure (ARCHITECT_SESSION workflow)
1. Pull current state via `showProjectState`; identify scope (whole system vs a
   bounded context e.g. payments, document-ingestion).
2. Repository → Structurizr → ADR → jQAssistant → ArchUnit → Sonar, in that
   trust order. Never override code with a diagram.
3. Build/refresh dependency view; flag cycles, layer violations, new
   cross-bounded-context edges (e.g. ingestion reaching into ledger internals).
4. Compare code reality against ADRs and Structurizr; record each divergence.
5. Cross-link findings to Jira issues, ADRs, Sonar issues, components.
6. Score health and rank issues by risk (PII/PCI boundaries weigh heavy).

## Output (contract)
- **Source(s) used** · **Confidence** (HIGH/MEDIUM/LOW)
- **Detected inconsistencies** (code↔ADR↔Structurizr↔rules)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components
- Deliverable: Architecture Health Report (+ Drift Report) in `reports/`.

## Guardrails
Analysis + report only. Changing ADRs/constraints/standards or promoting a
Structurizr model as approved is restricted — emit drafts under
`architecture/adr/drafts/`. Escalate critical drift, ADR violations, cycles,
and any breach of a PII/PCI boundary immediately.

## Graceful degradation
If jqassistant-mcp/structurizr-mcp (planned) are unavailable, fall back to Git +
ArchUnit reports + Sonar, mark `DATA_STALE`, and cap confidence at MEDIUM.
