---
name: adr-review
description: Audit Architecture Decision Records for staleness, contradictions and code divergence. Trigger on "ADR review", "проверь ADR", "актуальны ли архитектурные решения", "review decision records", during the weekly cadence or before a release.
---

# ADR Review

## Purpose
Verify that every ADR still matches the code, is internally consistent, and is
linked to the decisions it governs. Part of the weekly procedure
(OPERATING_MODEL) and the `KNOWLEDGE_SESSION` lineage.

## Inputs / Sources
- ADR repository under `architecture/adr/` (status, context, decision, consequences)
- github-mcp / GitLab MCP — code that should embody each decision
- jqassistant-mcp (planned) — verify the decision holds in the dependency graph
- jira-mcp — decisions driven by epics/stories
- rag-mcp (planned) — find superseded/duplicate decisions

## Procedure
1. Enumerate ADRs; read status (Proposed/Accepted/Superseded/Deprecated).
2. For each Accepted ADR, confirm the code still reflects it (Code > ADR on
   conflict — a divergence is drift, not a code bug).
3. Detect contradictions between ADRs and against current constraints/standards.
4. Find decisions implied by code or Jira but never recorded (missing ADRs),
   e.g. an undocumented async path in the document ingestion pipeline.
5. Cross-link each ADR to code, Jira, components, and Sonar where relevant.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (ADR↔code, ADR↔ADR, missing/stale ADRs)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
Changing, deleting or superseding an ADR is restricted. Produce proposed/updated
ADRs as drafts in `architecture/adr/drafts/` for architect approval. Escalate any
ADR violation found in code.
