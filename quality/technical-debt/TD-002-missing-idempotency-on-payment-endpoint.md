# TD-002: Missing idempotency on the payment initiation endpoint

> EXAMPLE debt record — finance-flavored, high priority. Placeholders throughout.

- **ID:** TD-002
- **Source:** Code review + violation of [integration constraint](../../architecture/constraints/integration.md)
- **Status:** open
- **Priority:** **High**
- **Fix cost:** M
- **Related ADR:** [ADR-003](../../architecture/adr/ADR-003-kafka.md)
- **Related Jira:** PAY-231 (example)
- **Date raised:** 2026-05-28 (example)

## Description

`POST /api/v1/payments` does not currently enforce an `Idempotency-Key`. A
client retry (timeout, network blip, double-submit) can create a **duplicate
payment**, violating the invariant that "a Payment cannot be settled twice"
(see [domain-rules](../../domain/model/domain-rules.md)) and the
[integration constraint](../../architecture/constraints/integration.md) requiring
idempotency for payments.

## Impact

- **Financial / correctness risk:** duplicate fund movements and duplicate ledger
  entries; customer-impacting and compliance-relevant.
- **High priority** — must be fixed before scaling payment volume; a likely
  blocker for [release readiness](../../delivery/releases/README.md).
- Increases reconciliation/support load.

## Suggested fix

1. Require `Idempotency-Key` header on payment mutations (see [api-standards](../../architecture/standards/api-standards.md)).
2. Persist key → result mapping with a uniqueness constraint
   (`V<n>__add_idempotency_key_to_payment.sql`, per [persistence-standards](../../architecture/standards/persistence-standards.md)).
3. Return the original result on key replay; 409 on conflicting payload.
4. Add an integration test asserting a replayed key creates no second payment.

## Notes

Until fixed, document the duplicate-payment exposure as a release risk and watch
for incidents (see [history/incidents](../../history/incidents/README.md)).
