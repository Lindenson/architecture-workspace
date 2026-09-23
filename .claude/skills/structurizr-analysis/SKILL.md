---
name: structurizr-analysis
description: Analyze the Structurizr C4 model and check it against code reality. Trigger on "structurizr analysis", "проверь C4 модель", "сверь диаграммы с кодом", "C4 consistency", during architecture review or documentation update.
---

# Structurizr Analysis

> **Status:** structurizr-mcp is LIVE (port 8084). Use `readWorkspace`,
> `listElements`, `listRelationships` and `detectDrift` against the committed
> `architecture/c4/workspace.dsl`. Reading the DSL by hand is the fallback for a
> server outage, not the default.

## Purpose
Assess the C4 model (`ARCHITECTURE_MODEL`) for completeness and consistency with
the code, and surface Code↔Structurizr drift. Supports `ARCHITECT_SESSION` and
`UPDATE_ARCHITECTURE_MODEL`.

## Inputs / Sources
- structurizr-mcp / DSL files — containers, components, relationships
- github-mcp / GitLab MCP — actual code structure (PRIMARY)
- jqassistant-mcp — dependency graph to validate relationships
- ADR repo — decisions the model should reflect

## Procedure
1. Load the model: systems, containers, components, relationships.
2. Compare model elements to real modules/services and dependencies in code.
3. Flag: missing containers/components, stale elements, relationships present in
   code but absent in the model (e.g. ingestion → external OCR service), and
   diagrams contradicting ADRs.
4. Note where the model would need updating (handing off to
   `architecture-documentation-update`).

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (model↔code↔ADR)
- **Linked artifacts** (Code · ADR · components · Jira)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
Promoting/approving a model is restricted — propose DSL changes as drafts under
`knowledge/drafts/` or `architecture/adr/drafts/`. Code wins on conflict.

## Graceful degradation
Without structurizr-mcp, parse committed DSL only; if no model exists, report the
gap and recommend bootstrapping one. Mark `DATA_STALE`, confidence ≤ MEDIUM.
