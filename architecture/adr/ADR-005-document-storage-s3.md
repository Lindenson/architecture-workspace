# ADR-005: Object storage for document binaries, metadata in PostgreSQL

> EXAMPLE ADR — illustrative content for the starter workspace.

- **Status:** accepted
- **Date:** 2026-02-18 (example)
- **Deciders:** Lead Architect, Team Lead, Security/Compliance Officer
- **Supersedes:** —
- **Superseded by:** —
- **Related Jira:** PAY-78 (example)

## Context

The DocumentStorage context must hold potentially large, immutable document
binaries (invoices, scans, KYC evidence) with strict retention. Storing large
BLOBs in PostgreSQL bloats the transactional store, complicates backups and
contradicts [ADR-001](ADR-001-postgresql.md)'s intent to keep Postgres lean for
transactional workloads.

## Decision

We will store document **binaries** in an **S3-compatible object store**, and
keep document **metadata** (id, owner, content type, checksum, storage key,
retention, OCR status, links to Payment/Customer) in **PostgreSQL**. Documents
are **immutable**: a change produces a new object/version, never an in-place
edit. Access is via the document-storage service; clients use **pre-signed
URLs** for upload/download — binaries do not flow through the core.

Objects are encrypted at rest; PII handling follows the
[security constraint](../constraints/security.md).

## Consequences

- Positive: cheap, durable, scalable binary storage; Postgres stays lean;
  immutability supports audit/retention; offloads large transfers from the app.
- Negative / trade-offs: metadata↔object consistency must be managed (orphaned
  objects, failed uploads) — reconcile via lifecycle jobs + outbox events
  (see [ADR-003](ADR-003-kafka.md)); two stores to back up and secure.
- TODO: pick concrete provider (AWS S3 / MinIO / GCS) and retention policy with
  the compliance officer.

## Alternatives considered

| Option | Pros | Cons | Why not chosen |
|--------|------|------|----------------|
| BLOBs in Postgres | One store | Bloat, backup pain, contradicts ADR-001 | Rejected |
| Filesystem / NFS | Simple | Poor durability/scaling, hard multi-AZ | Rejected |
| Object store + metadata in PG | Durable + lean PG | Two-store consistency | **Chosen** |

## Related

- ADRs: [ADR-001](ADR-001-postgresql.md), [ADR-004](ADR-004-modular-monolith-plus-services.md), [ADR-003](ADR-003-kafka.md)
- Constraints: [security](../constraints/security.md)
- Domain: [aggregates](../../domain/model/aggregates.md)
