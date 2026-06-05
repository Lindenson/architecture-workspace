---
name: project-state-review
description: The flagship consolidated snapshot — architecture, delivery, quality, tech-debt, risk and knowledge gaps in one answer. Trigger on "show project state", "покажи состояние проекта", "что с проектом", "project status", as the default entry point for "where do we stand".
---

# Project State Review

## Purpose
Reconstruct the full digital twin and answer the success-criteria question
("Покажи текущее состояние проекта") in one consolidated, factual snapshot.
Backs AGENT_RUNTIME `SHOW_PROJECT_STATE` (digital-twin-core `showProjectState`)
and the OPERATING_MODEL Final Goal.

## Inputs / Sources (standard MCP call order)
1. github-mcp / GitLab MCP — Git status (PRIMARY) · 2. jira-mcp — delivery ·
3. sonar-mcp — quality · 4. jqassistant-mcp (planned) — architecture graph ·
5. structurizr-mcp (planned) — C4 · 6. wiki-mcp (planned) · 7. rag-mcp (planned).
Plus Project Memory and ADR repo.

## Procedure (SHOW_PROJECT_STATE pipeline)
1. Git status → Jira status → Sonar metrics → architecture graph → ADR state →
   knowledge base → tech-debt registry.
2. Merge into the unified `DIGITAL_TWIN_MODEL`; cross-link Jira↔Code, ADR↔Code,
   Sonar↔Modules, Structurizr↔jQAssistant, Wiki↔Architecture.
3. Resolve conflicts strictly by source-of-truth priority; log conflicts.
4. Generate the snapshot: architecture · delivery · quality · tech-debt · risk ·
   knowledge gaps · recommendations, with finance/document-processing context.

## Output (contract)
- **Snapshot** by domain (architecture, delivery, quality, tech-debt, risk,
  knowledge gaps)
- **Source(s) used** · **Confidence** (per domain)
- **Detected inconsistencies**
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components
- Deliverable: PROJECT_STATE_SNAPSHOT in `reports/`.

## Guardrails
Read/synthesize only — no source mutations. Never cache without refreshing
sources (code always refreshed). Where a planned MCP is offline, mark
`DATA_STALE` and lower that domain's confidence. Escalate critical findings.
Delegate deep dives to the matching subagent.
