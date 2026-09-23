# decisions.md — the human decision ledger

> Copy to `specs/<feature>/decisions.md` at the start of every Loop B feature.
> `feature-impact-analysis` and `spec-critic` **append questions** here.
> **Humans answer.** `decision-capture` reads it. The gate blocks on OPEN rows.

This file is the missing half of "HUMAN GATE". Without it the gates are arrows
on a diagram: questions get asked in `critique.md`, answered in a chat window,
and evaporate. Six months later nobody knows who decided that payment-service
owns cancellation, or why.

---

## Questions

Statuses: `OPEN` · `ANSWERED` · `DEFERRED` (needs a reason and a revisit date) ·
`SUPERSEDED`.

`/speckit-plan` and `/speckit-implement` are blocked while any row is `OPEN`
(configurable: `gate.require_decisions_closed` in `.claude/aip.config.yml`).
`DEFERRED` does not block — deferring is itself a decision, and a recorded one.

| ID | Question | Raised by | Options | Status | Answer | Answered by | Date | → |
|----|----------|-----------|---------|--------|--------|-------------|------|---|
| Q1 | Which service owns order cancellation? | impact.md `CROSS-CONTEXT` | order-service / payment-service | OPEN | | | | |
| Q2 | Is cancellation idempotent on retry? | critique.md `F1` | yes, by request-id / no | OPEN | | | | |
| Q3 | Do we pay down TD-002 now or accept it? | impact.md debt | pay now / accept, revisit Q1 2027 | OPEN | | | | |

`→` links the outcome: an ADR draft, a `project-memory/` entry, or a Jira key.

---

## Rules

**For the agent**
- You may **add** rows and fill `Question`, `Raised by`, `Options`. Nothing else.
- Never write `Answer`, `Answered by`, or change `Status` to `ANSWERED`. Filling
  in a human's answer is the exact failure this ledger exists to prevent.
- One row per genuine decision. Do not pad the ledger with questions you can
  answer from `impact.md` — a ledger of twelve trivial rows gets rubber-stamped,
  and the one real question goes through with it.
- Options must be real alternatives with different consequences, not a
  rhetorical pair.

**For the human**
- An answer needs a reason only when the reason is not obvious. `Q1: payment-service`
  is fine; `Q3: accept` needs "because the migration window is Q1".
- `DEFERRED` requires a revisit condition in the Answer column.
- Answering here is what the record shows. A decision made in Slack and not
  written here did not happen, as far as every future session is concerned.

**For the record**
- Append-only. A changed decision is a new row that marks the old `SUPERSEDED`.
- This file travels into the PR. It is the evidence a reviewer reads to see
  *what was decided by a person* versus *what the agent chose on its own*.

---

## Why a table and not prose

Because a hook has to count open rows, `decision-capture` has to read outcomes,
and CI has to assert that no `OPEN` row reached a merge. Prose is unparseable,
and an unparseable gate is a convention, not a gate.

## Relationship to the other artifacts

```
impact.md    "this crosses a bounded context"      → raises Q1
critique.md  "no idempotency defined, CROSS-SERVICE" → raises Q2
decisions.md  HUMAN ANSWERS                          ← the man in the middle
   │
   ├─▶ gate: no OPEN rows → plan/implement may proceed
   ├─▶ decision-capture: answered rows → project-memory/ + ADR draft
   └─▶ PR: reviewers see who decided what, and when
```
