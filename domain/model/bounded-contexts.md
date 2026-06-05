# Bounded Contexts (L4 — validated model)

> STATUS: starter, to be **validated** against extracted facts in
> [`../raw/`](../README.md). Human/Claude-curated; the architect signs off.
> Summary version for stakeholders is in [`../../knowledge/business-domain.md`](../../knowledge/business-domain.md).

## Contexts and their aggregates

| Bounded context | Responsibility | Aggregates (roots) | Unit |
|-----------------|----------------|--------------------|------|
| **Payments** | Validate + orchestrate payments (idempotent) | `Payment` | core (monolith) |
| **Ledger** | Double-entry, append-only accounting | `LedgerAccount` (+ `LedgerEntry`) | core |
| **Identity / KYC** | Customer identity + verification gating | `Customer` | core |
| **Notifications** | Outbound notifications from events | `Notification` | core |
| **DocumentIngestion** | Accept uploads, run async OCR/extraction | `Document` | document-processing service |
| **DocumentStorage** | Immutable binary storage + metadata | `StoredObject` | document-storage service |

See aggregates in [aggregates.md](aggregates.md) and invariants in
[domain-rules.md](domain-rules.md). Unit placement is per
[ADR-004](../../architecture/adr/ADR-004-modular-monolith-plus-services.md).

## Context relationships (allowed)

| From → To | Mechanism | Constraint |
|-----------|-----------|------------|
| Payments → Ledger | in-process service (same tx) | [integration](../../architecture/constraints/integration.md) |
| Payments → Identity/KYC | in-process service | idem |
| any → Notifications | Kafka event | [ADR-003](../../architecture/adr/ADR-003-kafka.md) |
| core → DocumentIngestion | Kafka event + REST | idem |
| DocumentIngestion → DocumentStorage | REST + pre-signed URL | [ADR-005](../../architecture/adr/ADR-005-document-storage-s3.md) |

- **No cross-context direct DB access / no FK across contexts** (reference by id + event).
- Boundaries are enforced by ArchUnit / jQAssistant (see [domain constraints](../constraints/domain-rules.adoc)).

> TODO: reconcile this list with `semantic/bounded-context-candidates/` once extracted.
