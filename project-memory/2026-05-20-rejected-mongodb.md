# 2026-05-20 — Rejected MongoDB for document metadata

> EXAMPLE project-memory entry (rejected option).

- **Date:** 2026-05-20
- **Author:** Lead Architect (example)
- **Related ADR:** [ADR-001 (PostgreSQL)](../architecture/adr/ADR-001-postgresql.md), [ADR-005](../architecture/adr/ADR-005-document-storage-s3.md)
- **Related Jira:** PAY-188 (example)

## Context

For the DocumentIngestion / DocumentStorage metadata (variable, semi-structured
extraction results), the team considered introducing **MongoDB** as a document
store alongside PostgreSQL.

## Reason (why rejected)

- Would add **polyglot persistence** and a second datastore to operate, secure
  and back up — disproportionate for current scale.
- Postgres `jsonb` covers the semi-structured metadata needs well, while keeping
  ACID, one operational model, and reusing [ADR-001](../architecture/adr/ADR-001-postgresql.md) expertise.
- Document **binaries** already go to object storage ([ADR-005](../architecture/adr/ADR-005-document-storage-s3.md));
  metadata volume does not require a separate engine.

## Result

Decided **against** MongoDB. Store document metadata in PostgreSQL (`jsonb` where
flexibility is needed). No new ADR required — reinforces ADR-001/005.

## Consequences

- Single transactional store retained; lower operational surface.
- If a future workload genuinely needs document-DB semantics at scale, re-open
  with a superseding/extending ADR and an impact analysis.
