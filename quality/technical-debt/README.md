# Technical Debt Register

> STATUS: governance register. Closing a debt is **architect-approved**; an agent
> may draft new debt under [`drafts/`](drafts/) (see [`../../CLAUDE.md`](../../CLAUDE.md)).

## Debt record format

Each debt is one file `TD-NNN-short-title.md` with these fields (per the
[bootstrap spec](../../.claude/BOOTSTRAP_ARCHITECT_AGENT.md) → "Technical Debt"):

| Field | Meaning |
|-------|---------|
| **ID** | `TD-NNN` (sequential, never reused) |
| **Source** | How found: Sonar rule, jQAssistant, ArchUnit, review, incident |
| **Description** | What the debt is |
| **Impact** | What it costs us (risk, slowdown, defect surface) |
| **Fix cost** | Rough effort (S/M/L or estimate) |
| **Priority** | Low / Medium / High / Critical |
| **Status** | open / in-progress / done / accepted (won't-fix) |
| **Related ADR** | e.g. ADR-003 |
| **Related Jira** | e.g. PAY-NNN |

Use [`_TEMPLATE.md`](_TEMPLATE.md).

## Register (EXAMPLE)

| ID | Title | Priority | Status | Source |
|----|-------|----------|--------|--------|
| [TD-001](TD-001-resttemplate-usage.md) | Legacy `RestTemplate` usage | Medium | open | Sonar / jQAssistant |
| [TD-002](TD-002-missing-idempotency-on-payment-endpoint.md) | Missing idempotency on payment endpoint | **High** | open | Review / integration constraint |

> TODO: keep this table in sync as debts are added/closed.
