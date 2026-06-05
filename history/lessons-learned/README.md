# History — Lessons Learned

> STATUS: starter. Durable lessons distilled from [decisions](../decisions/README.md),
> [incidents](../incidents/README.md) and retros — patterns to repeat and
> anti-patterns to avoid. This is institutional memory, not a blame log.

## Format

One file per lesson (or a curated list), `YYYY-MM-DD-short-title.md`, with:

- **Context** — where this came from (incident/decision/retro).
- **Lesson** — the takeaway, stated as guidance.
- **Action taken** — what changed (constraint, standard, ADR, process).

## Examples of what belongs here (EXAMPLE)

- "Idempotency must be designed in for any money-moving endpoint, not bolted on."
  → led to the [integration constraint](../../architecture/constraints/integration.md) rule and [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md).
- "Async OCR must never block request threads." → [performance constraint](../../architecture/constraints/performance.md).

> TODO: add lessons as they emerge; link the artifact each one changed.
