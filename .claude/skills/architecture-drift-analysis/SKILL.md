---
name: architecture-drift-analysis
description: Detect divergence between code reality and the intended architecture (ADR, Structurizr, ArchUnit rules, Jira intent). Trigger on "drift analysis", "проверь дрейф архитектуры", "разошлась ли архитектура с кодом", "architecture drift", after a large merge or before a release.
---

# Architecture Drift Analysis

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
Surface every gap between what the code does and what the architecture says it
should do. Implements the OPERATING_MODEL Architecture Drift Detection checklist
and feeds the Architecture Drift Engine in MCP_ORCHESTRATION_MAP.

## Inputs / Sources
- github-mcp / GitLab MCP — current code structure (PRIMARY)
- jqassistant-mcp — dependency graph, cycles, forbidden edges
- ArchUnit reports; Structurizr model (structurizr-mcp)
- ADR repo under `architecture/adr/`; jira-mcp — intended scope; sonar-mcp

## Procedure (Drift Detection checklist)
1. Refresh dependency graph (or last snapshot) and ArchUnit results.
2. Detect: dependency cycles · layer violations · new edges between bounded
   contexts (e.g. payment-core ↔ document-store) · ADR violations · constraint
   breaches · Code↔Structurizr divergence · Code↔Jira-intent gaps.
3. Classify each drift signal by type and risk; PII/PCI boundary crossings and
   ledger-integrity edges are HIGH by default.
4. Cross-link to the ADR/constraint/component/Jira it contradicts.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** — the drift catalogue, by type
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components
- Deliverable: Architecture Drift Report in `reports/drift/`.

## Clear the drift flag when the scan is genuinely done
The `PostToolUse` hook appends every product-code change to the drift flag
(`.claude/.drift-pending`), and the session digest surfaces it. **After a
completed rescan, run:**

```bash
./automation/reset-drift-flag.sh        # archives the entries, clears the flag
```

Without this the flag is a one-way latch: one code edit and every future session
is warned forever, which teaches the team to ignore the warning — and then the
one that mattered is ignored too. Only clear it after an actual scan covering
those files; clearing it to silence the message is falsifying the record.

If the scan was partial (graph unreachable, only ArchUnit available), say so and
**leave the flag set**.

## Guardrails
Read-only over product code. Remediation that changes constraints/ADRs is a
draft only. Escalate critical drift, ADR violations, cycles and any PII/PCI
boundary crossing.

## Graceful degradation
If jqassistant-mcp/structurizr-mcp are unreachable, rely on Git + ArchUnit + ADR
diff, mark `DATA_STALE`, confidence ≤ MEDIUM, and note which drift classes could
not be checked (e.g. C4 divergence).
