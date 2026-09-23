---
name: decision-capture
description: After convergence or merge, extract the decisions actually made during a feature — including rejected options and consciously accepted trade-offs — and record them in project-memory/, refresh the journal index, and emit an ADR draft when the decision is durable. Trigger after /speckit-converge, after a merge, on "capture the decisions", "запиши решения", "record why we did it this way", or when a PR is closed without a memory entry.
---

# Decision Capture — join point J3

> **Loop B → Loop A bridge (join point J3).** Runs after `/speckit-converge`.
> Reads the feature's artifacts; writes only `project-memory/` (append),
> `quality/technical-debt/`, `reports/`, and drafts. Never edits product code, a
> numbered ADR, a constraint or the C4 model.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
Stop architectural knowledge from dying in a PR thread. ADRs record what was
**adopted**; this skill also captures what was **rejected**, what was deferred,
and what trade-off was knowingly accepted — the context future sessions need and
that no tool can reconstruct from code.

Appending to `project-memory/` is an autonomous right. Writing a final ADR is not.

## Inputs / Sources
- **`specs/<feature>/decisions.md` — the primary input.** Every `ANSWERED` and
  `DEFERRED` row is a decision a NAMED HUMAN made, with the options they chose
  between and the date. Start here: these are facts, not inference. Reconstructing
  intent from a diff is the fallback for decisions that never reached the ledger,
  and such a reconstruction is always weaker evidence — mark it as such.
- `specs/<feature>/` — `spec.md`, `impact.md`, `critique.md`, `plan.md`, `tasks.md`
  (`critique.md` raises the questions; `decisions.md` holds the answers. A
  `Human Decisions Required` item with no matching ledger row means the decision
  was made off-record — record it with `Reason: NOT RECORDED — ask <author>`)
- `specs/<feature>/gate-log.md` — a gate `OVERRIDE` row is itself a decision:
  someone chose to proceed past a stop, and gave a reason. Capture it.
- `github-mcp` — `analyzePullRequest`, `extractChangesets`, `readCommits`
- `jira-mcp` — the issue, its comments and transitions
- Convergence output: tasks appended in the `## Phase N: Convergence` section are
  evidence that the original plan was incomplete — worth a line
- `project-memory/` — check whether this decision supersedes an earlier entry

## Procedure

1. **Read `decisions.md` first.** Each `ANSWERED` row becomes a memory entry
   directly: question, options considered, chosen option, who decided, when. Each
   `DEFERRED` row becomes an entry too — deferral is a decision, and its revisit
   condition is the most useful line in the file a year later.

2. **Then collect what the ledger missed.** A decision is anything where a
   different reasonable choice existed: a resolved `DECISION REQUIRED` from
   `impact.md` with no ledger row, a resolved critic finding, a design choice
   visible in the diff that the plan did not mandate, a trade-off accepted under
   time pressure, a debt item created or closed. If the ledger is empty on a
   CROSS-CONTEXT or larger feature, say so in the output — either nothing was
   decided by a human, or the ledger was bypassed. Both are worth knowing.

3. **Filter.** Record decisions with a *durable consequence*. Skip mechanics
   (renamed a variable, split a method). Test: would a developer six months from
   now ask "why is it like this?"

4. **Classify each** as `adopted` · `rejected` · `deferred` · `accepted trade-off`
   · `supersedes <earlier entry>`.

5. **Write one file per decision** at
   `project-memory/YYYY-MM-DD-short-title.md` using
   [`_TEMPLATE.md`](../../../project-memory/_TEMPLATE.md):
   Date · Author · Context · Reason · Result · Consequences · Related ADR ·
   Related Jira. Add the metadata block (`bounded_context`, `components`,
   `status`) — it is what `rag-mcp` indexes and what the SessionStart hook
   retrieves.

6. **Refresh `project-memory/journal.md`** — rebuild the index from the files on
   disk, newest first. Never rewrite an existing entry: history is append-only;
   a changed mind is a new entry that supersedes the old one.

7. **Emit an ADR draft** to `architecture/adr/drafts/` when the decision is
   durable and system-wide (technology choice, boundary, contract style,
   persistence strategy). Use `architecture/adr/_TEMPLATE.md`. Mark it
   `STATUS: DRAFT — awaiting architect approval`. Do not touch numbered ADRs.

8. **Reindex** so the entries are retrievable next session (or run
   `knowledge-reindex`, which also probes that the entry is actually findable):
   `rag-mcp.indexPath project-memory/` (or `reindexAll` after a batch).

9. **Record debt deltas.** Prefer running `tech-debt-delta`, which does this
   against the `impact.md` baseline. Debt created by this feature gets a new file under
   `quality/technical-debt/` with a link back to the memory entry. Debt *closed*
   is proposed for closure — closing it is an architect decision.

## Output (contract)
```
- Decision rows consumed: <N answered, M deferred> from decisions.md
- Decisions found only in the diff (weaker evidence): <N>
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
