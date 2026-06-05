---
name: jqassistant-analysis
description: Run precise dependency/architecture graph analysis via jQAssistant — cycles, layering and forbidden-dependency violations, coupling, blast radius. Trigger on "jqassistant analysis", "построй граф зависимостей", "проверь циклы", "что заденет рефакторинг", "dependency graph", before refactoring or for architecture review.
---

# jQAssistant Analysis

> **MVP status:** jqassistant-mcp is *planned*. Until live, fall back to ArchUnit
> reports plus static inspection of build modules and imports over Git.

## Purpose
Give exact, bytecode-grounded answers on the Architecture Graph: type/package
dependencies, cycles, layer/forbidden-dependency violations, coupling metrics,
and impact (blast radius) of a change. Produces `ARCHITECTURE_GRAPH` and
`ARCHITECTURE_DRIFT_SIGNALS`; feeds `RUN_ARCHITECTURE_RESCAN`.

## Inputs / Sources
- jqassistant-mcp (planned) — dependency graph / Cypher over scanned bytecode
- ArchUnit reports under `quality/` — codified rules
- github-mcp / GitLab MCP — module/build structure for context

## Procedure
1. Refresh or load the dependency graph (24h cache allowed).
2. Query for: dependency cycles · layer violations (api→service→client→mcp) ·
   forbidden cross-bounded-context edges (e.g. document-ingestion → payment
   internals) · god classes / high coupling.
3. For impact requests, compute the dependency closure (who calls / depends on X)
   to estimate blast radius before a refactor.
4. Tie each finding to the violated ADR/constraint and affected components.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (cycles, layering, forbidden deps)
- **Linked artifacts** (Code · ADR · components · Jira · Sonar)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
Read-only over the graph. Escalate cycles and high-risk forbidden dependencies.

## Graceful degradation
Without jqassistant-mcp, derive what you can from ArchUnit + module structure,
state that bytecode-level precision is unavailable, mark `DATA_STALE`, confidence
≤ MEDIUM.
