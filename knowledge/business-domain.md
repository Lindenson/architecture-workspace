# Business Domain — Finance + Document Processing (EXAMPLE / STARTER TEMPLATE)

> STATUS: starter template. The authoritative, extracted domain model lives under
> [`../domain/`](../domain/README.md) (L1–L5 pipeline). This file is the
> human-readable summary. TODO: keep in sync with `domain/model/bounded-contexts.md`.

## The domain

Paydocs operates in **regulated finance**: money moves between parties and every
movement must be recorded, balanced and auditable. Around each payment sits a
set of **documents** — invoices, mandates, KYC evidence — that must be ingested,
classified, stored and retained per compliance rules.

Two intertwined problem spaces:
1. **Payments & accounting** — initiate, validate, settle, and post to a ledger
   with strict consistency and idempotency.
2. **Document processing** — accept arbitrary documents, extract structured data
   (OCR), store originals immutably, and link them to payments / customers.

## Bounded contexts (EXAMPLE — TODO: validate against `domain/model/`)

| Bounded context | One-line responsibility |
|-----------------|-------------------------|
| **Payments** | Accept and validate payment instructions, orchestrate settlement (idempotent). |
| **Ledger** | Double-entry accounting; immutable ledger entries; balances per account. |
| **DocumentIngestion** | Accept uploads, queue async OCR/extraction, emit document-ready events. |
| **DocumentStorage** | Immutable storage of document binaries; metadata index; retention. |
| **Identity / KYC** | Customer identity, KYC verification status, gating of payment flows. |
| **Notifications** | Outbound notifications (email/webhook) to customers and operators. |

## Context relationships (EXAMPLE)

- Payments → Ledger: posts settlement entries (in-process, transactional in the core).
- Payments → Identity/KYC: checks customer KYC status before allowing settlement.
- Payments → DocumentIngestion: links supporting documents to a payment.
- DocumentIngestion → DocumentStorage: stores originals, retrieves for OCR.
- All contexts → Notifications: emit domain events; Notifications reacts via Kafka.

See the C4 model in [`../architecture/c4/workspace.dsl`](../architecture/c4/workspace.dsl)
and the domain graph in [`../domain/visualization/domain-graph.dsl`](../domain/visualization/domain-graph.dsl).

## Key domain invariants (summary — full list in `domain/model/domain-rules.md`)

- A Payment cannot be settled twice (idempotency + state machine).
- Ledger entries are append-only; balances are derived, never edited.
- A payment above the KYC threshold requires a verified customer.
- Stored documents are immutable; a new version is a new object.

> TODO: confirm regulatory scope (PSD2 / AML / data retention period) with the
> compliance officer (see [stakeholders.md](stakeholders.md)).
