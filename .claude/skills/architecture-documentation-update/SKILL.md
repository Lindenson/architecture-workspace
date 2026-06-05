---
name: architecture-documentation-update
description: Reconcile architecture documentation (Structurizr, ADRs, component docs) with current code. Trigger on "update architecture docs", "обнови архитектурную документацию", "синхронизируй документацию с кодом", "refresh the model", after a rescan or significant structural change.
---

# Architecture Documentation Update

## Purpose
Bring architecture documentation back in line with code reality and propose the
diffs. Backs AGENT_RUNTIME `UPDATE_ARCHITECTURE_MODEL` and the
`KNOWLEDGE_SESSION` mode.

## Inputs / Sources
- jqassistant-mcp (planned) — extracted code structure / dependency graph
- structurizr-mcp (planned) — current C4 model DSL
- github-mcp / GitLab MCP — code structure (PRIMARY); ADR repo
- digital-twin-core `runArchitectureRescan` for the latest snapshot

## Procedure (UPDATE_ARCHITECTURE_MODEL workflow)
1. Extract current code structure (jQAssistant / Git) for the target scope.
2. Compare against the Structurizr component model and ADRs.
3. List documentation gaps: undocumented modules, stale C4 elements, missing
   ADRs (e.g. a new async document-ingestion queue not in the model).
4. Draft the corrected Structurizr DSL fragments and/or ADR updates.
5. Cross-link each proposed change to code and the decision it records.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (code↔Structurizr↔ADR)
- **Linked artifacts** (Code · ADR · components · Jira)
- Drafts under `architecture/adr/drafts/` and/or `knowledge/drafts/`
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
Promoting a Structurizr model as "approved" and changing ADRs/constraints are
restricted — drafts only. Code wins on every conflict; never edit docs to
contradict code.

## Graceful degradation
If structurizr-mcp/jqassistant-mcp (planned) are unavailable, work from Git +
existing model files, mark `DATA_STALE`, confidence ≤ MEDIUM, and limit output to
gap identification rather than full DSL regeneration.
