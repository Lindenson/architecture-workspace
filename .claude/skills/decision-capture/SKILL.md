---
name: decision-capture
description: After convergence or merge, extract the decisions actually made during a feature — including rejected options and consciously accepted trade-offs — and record them in project-memory/, refresh the journal index, and emit an ADR draft when the decision is durable. Trigger after /speckit-converge, after a merge, on "capture the decisions", "запиши решения", "record why we did it this way", or when a PR is closed without a memory entry.
---

# Decision Capture — join point J3

## Purpose
Stop architectural knowledge from dying in a PR thread. ADRs record what was
**adopted**; this skill also captures what was **rejected**, what was deferred,
and what trade-off was knowingly accepted — the context future sessions need and
that no tool can reconstruct from code.

Appending to `project-memory/` is an autonomous right. Writing a final ADR is not.

## Inputs / Sources
- `specs/<feature>/` — `spec.md`, `impact.md`, `critique.md`, `plan.md`, `tasks.md`
  (`critique.md` is the richest source: every `Human Decisions Required` item was
  resolved somehow — record how)
- `github-mcp` — `analyzePullRequest`, `extractChangesets`, `readCommits`
- `jira-mcp` — the issue, its comments and transitions
- Convergence output: tasks appended in the `## Phase N: Convergence` section are
  evidence that the original plan was incomplete — worth a line
- `project-memory/` — check whether this decision supersedes an earlier entry

## Procedure

1. **Collect candidate decisions.** A decision is anything where a different
   reasonable choice existed: a resolved `DECISION REQUIRED` from `impact.md`, a
   resolved critic finding, a design choice visible in the diff that the plan did
   not mandate, a trade-off accepted under time pressure, a debt item created or
   closed.

2. **Filter.** Record decisions with a *durable consequence*. Skip mechanics
   (renamed a variable, split a method). Test: would a developer six months from
   now ask "why is it like this?"

3. **Classify each** as `adopted` · `rejected` · `deferred` · `accepted trade-off`
   · `supersedes <earlier entry>`.

4. **Write one file per decision** at
   `project-memory/YYYY-MM-DD-short-title.md` using
   [`_TEMPLATE.md`](../../../project-memory/_TEMPLATE.md):
   Date · Author · Context · Reason · Result · Consequences · Related ADR ·
   Related Jira. Add the metadata block (`bounded_context`, `components`,
   `status`) — it is what `rag-mcp` indexes and what the SessionStart hook
   retrieves.

5. **Refresh `project-memory/journal.md`** — rebuild the index from the files on
   disk, newest first. Never rewrite an existing entry: history is append-only;
   a changed mind is a new entry that supersedes the old one.

6. **Emit an ADR draft** to `architecture/adr/drafts/` when the decision is
   durable and system-wide (technology choice, boundary, contract style,
   persistence strategy). Use `architecture/adr/_TEMPLATE.md`. Mark it
   `STATUS: DRAFT — awaiting architect approval`. Do not touch numbered ADRs.

7. **Reindex** so the entries are retrievable next session:
   `rag-mcp.indexPath project-memory/` (or `reindexAll` after a batch).

8. **Record debt deltas.** Debt created by this feature gets a new file under
   `quality/technical-debt/` with a link back to the memory entry. Debt *closed*
   is proposed for closure — closing it is an architect decision.

## Output (contract)
```
- Memory entries written: <paths>
- Journal index: refreshed (N entries)
- ADR drafts emitted: <paths or none>
- Debt delta: created <ids> · proposed for closure <ids>
- Reindexed: yes/no
- Sources used · Confidence
- Decisions found but NOT recorded (and why)
```

## Guardrails
- Append-only. Never edit or delete an existing memory entry.
- Never write a numbered ADR, close a debt item, or change a constraint. Drafts
  and proposals only.
- Never infer a rationale that nobody stated. If the diff shows a choice but no
  reason is recorded anywhere, write the decision with
  `Reason: NOT RECORDED — ask <author>`. A fabricated rationale is worse than a
  gap, because the next session will trust it.
- Do not record secrets, tokens, or customer data in a memory entry.
