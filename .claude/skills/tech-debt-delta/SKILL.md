---
name: tech-debt-delta
description: After a feature converges, account for the technical debt it closed, created or knowingly deferred inside its blast radius. Produces a debt delta for the PR description and proposes closures for architect approval. Trigger after /speckit-converge, on "what debt did this feature create", "посчитай долг по фиче", "debt delta", or before opening a PR on a CROSS-CONTEXT or larger change.
---

# Technical Debt Delta — part of join point J3

> **Loop B → Loop A bridge (join point J3).** Runs after `/speckit-converge`.
> Reads the feature's artifacts; writes only `project-memory/` (append),
> `quality/technical-debt/`, `reports/`, and drafts. Never edits product code, a
> numbered ADR, a constraint or the C4 model.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
A feature that creates debt must say so in its own PR, not in next quarter's
audit. Debt discovered later is attributed to nobody and paid by everybody.

## Inputs / Sources
`specs/<feature>/impact.md` (its "Existing debt inside the radius" table is the
baseline) · `specs/<feature>/decisions.md` (a `DEFERRED` answer is usually a debt
decision) · `quality/technical-debt/` · `quality/architecture-violations/` ·
`sonar-mcp.technicalDebt` / `codeSmells` for the touched modules ·
`jqassistant-mcp.findCycles` / `godClasses` scoped to the radius ·
`github-mcp.extractChangesets` for the diff.

## Procedure
1. **Baseline** — the debt listed in `impact.md` when the feature started.
2. **Now** — rerun the same queries scoped to the blast radius.
3. **Classify the difference:**
   - `CLOSED` — a registered item no longer reproduces. Verify it is actually
     fixed, not merely moved or renamed.
   - `CREATED` — a new violation, cycle, god class, or a shortcut the feature
     took. A `DEFERRED` row in `decisions.md` is always a `CREATED` item.
   - `DEEPENED` — an existing item got worse (more call sites, wider radius).
   - `CARRIED` — untouched, but the feature now sits on top of it. List it.
4. **Write each CREATED item** to `quality/technical-debt/TD-<n>-<slug>.md` using
   `_TEMPLATE.md`, linking the feature, the `decisions.md` row that accepted it,
   and the cost of paying it later versus now.
5. **Propose CLOSED items for closure.** Closing a debt item is an architect
   decision — write the proposal, never flip the status yourself.
6. **Emit the PR block** so the reviewer sees the trade-off before approving.

## Output (contract)
```markdown
## Technical debt delta — <feature>
| Item | Change | Evidence | Decision row | Cost now / later |
CLOSED (proposed): ...   CREATED: ...   DEEPENED: ...   CARRIED: ...
Net: +N / -M items · Sonar debt <before> → <after>
Sources · Confidence
```

## Guardrails
- Never close a debt item; propose.
- Never register debt for something `decisions.md` explicitly rejected — that was
  a decision, not debt.
- No `impact.md` → no baseline. Report absolute state only and cap confidence at
  MEDIUM. A delta without a baseline is a number that looks like evidence.
