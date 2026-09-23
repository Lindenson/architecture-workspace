---
name: project-state-review
description: RIGHT NOW — the live consolidated snapshot of the system as it stands this minute: architecture, delivery, quality, tech-debt, risk and knowledge gaps in one answer, rebuilt from the MCP sources on every run. The default entry point. Trigger on "show project state", "покажи состояние проекта", "что с проектом", "project status", "where do we stand", "какая сейчас картина". For the scored month-over-month view use project-health-review instead.
---

# Project State Review

> **Loop A — architecture maintenance.** Read-only over product code.
> May write: `reports/`, `quality/`, `domain/raw/`, `domain/semantic/`,
> `project-memory/` (append), and drafts under `architecture/adr/drafts/`,
> `knowledge/drafts/`, `quality/technical-debt/drafts/`.
> Architect-owned and blocked for you: numbered ADRs, `architecture/constraints/`,
> `architecture/standards/`, `architecture/c4/workspace.dsl`, `domain/model/`,
> `delivery/roadmap/`. Emit a draft and escalate instead of editing them —
> drift is REPORTED, never erased by editing the model.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
Reconstruct the full digital twin and answer the success-criteria question
("Покажи текущее состояние проекта") in one consolidated, factual snapshot.
Backs AGENT_RUNTIME `SHOW_PROJECT_STATE` (digital-twin-core `showProjectState`)
and the OPERATING_MODEL Final Goal.

**Not the same as [`project-health-review`](../project-health-review/SKILL.md).**
This one answers *what is true now* and is rebuilt from live sources on every
run — use it on demand, any time. The health review answers *is it getting
better or worse*: it scores the same domains, compares against previous
snapshots in `reports/`, and runs on the monthly cadence. If the question has no
time dimension, it belongs here.

## Inputs / Sources (standard MCP call order)
1. github-mcp / GitLab MCP — Git status (PRIMARY) · 2. jira-mcp — delivery ·
3. sonar-mcp — quality · 4. jqassistant-mcp — architecture graph ·
5. structurizr-mcp — C4 · 6. wiki-mcp · 7. rag-mcp (6 and 7 only when the optional knowledge layer is enabled).
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
sources (code always refreshed). Where an MCP is offline, mark
`DATA_STALE` and lower that domain's confidence. Escalate critical findings.
Delegate deep dives to the matching subagent.
