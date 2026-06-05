# Domain Rules / Invariants (L4 — validated model)

> STATUS: starter. The business invariants the model must always uphold. These
> drive [domain constraints](../constraints/domain-rules.adoc) and tests. EXAMPLE set.

## Payments

- **R-PAY-1:** A Payment cannot be **settled twice** (idempotency + state machine).
- **R-PAY-2:** Payment initiation is **idempotent** on `Idempotency-Key`
  (see [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md),
  [integration](../../architecture/constraints/integration.md)).
- **R-PAY-3:** A payment above the KYC threshold requires a **verified customer**.
- **R-PAY-4:** Amount must be > 0 with a valid currency.

## Ledger

- **R-LED-1:** Ledger entries are **append-only** (no edit/delete).
- **R-LED-2:** Balances are **derived**, never directly mutated.
- **R-LED-3:** Entries for one transaction must **balance** (debits = credits).
- **R-LED-4:** Every settlement posts a balanced set of entries atomically with
  the payment state change.

## Documents

- **R-DOC-1:** Stored documents are **immutable**; updates create a new version.
- **R-DOC-2:** OCR runs **asynchronously**; ingestion never blocks on OCR
  (see [performance](../../architecture/constraints/performance.md)).
- **R-DOC-3:** Document binaries live in object storage; only metadata in Postgres
  ([ADR-005](../../architecture/adr/ADR-005-document-storage-s3.md)).

## Cross-cutting

- **R-X-1:** No cross-context direct DB access; reference other contexts by id + event.
- **R-X-2:** Every state change to Payment/LedgerEntry/Document emits an
  **audit record** ([security](../../architecture/constraints/security.md)).

> TODO: assign each rule an enforcement mechanism (ArchUnit / integration test /
> DB constraint) and link it.
