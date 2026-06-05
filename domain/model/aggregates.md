# Aggregates (L4 — validated model)

> STATUS: starter. EXAMPLE aggregates with their roots, members and invariants.
> Validated against [`../raw/`](../README.md) facts; architect-approved.

## Payment (context: Payments)

- **Root:** `Payment`
- **Members:** `PaymentAttempt` (value/entity within the aggregate)
- **Key fields (EXAMPLE):** id, customerId (ref Identity/KYC), amount+currency,
  status, idempotencyKey, createdAt
- **Invariants:**
  - A Payment cannot be settled twice (state machine + idempotency).
  - Amount > 0; currency valid.
  - Settlement requires a verified customer above the KYC threshold.
- **Changes only via the root** — no external code mutates `PaymentAttempt` directly.

## LedgerAccount (context: Ledger)

- **Root:** `LedgerAccount`
- **Members:** `LedgerEntry` (append-only)
- **Invariants:**
  - Ledger entries are **append-only**; never edited or deleted.
  - Balance is **derived** from entries, never set directly.
  - Posted entries for a single business transaction must **balance** (Σ debits = Σ credits).

## Document (context: DocumentIngestion)

- **Root:** `Document`
- **Members:** `ExtractionResult` (OCR output)
- **Invariants:**
  - A stored document is **immutable**; a change creates a new version/object
    ([ADR-005](../../architecture/adr/ADR-005-document-storage-s3.md)).
  - OCR status transitions are one-way (PENDING → PROCESSING → DONE/FAILED).

## Customer (context: Identity/KYC)

- **Root:** `Customer`
- **Invariants:**
  - KYC status reflects the latest completed verification.
  - Identity attributes (PII) are handled per [security constraint](../../architecture/constraints/security.md).

> Aggregate boundaries are enforced — see [archunit-domain-rules.java](../constraints/archunit-domain-rules.java).
> TODO: confirm members/invariants against the extracted JPA model.
