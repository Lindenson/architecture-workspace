# ADR-001: PostgreSQL as the primary datastore

> EXAMPLE ADR — illustrative content for the starter workspace.

- **Status:** accepted
- **Date:** 2026-01-15 (example)
- **Deciders:** Lead Architect, Team Lead
- **Supersedes:** —
- **Superseded by:** —
- **Related Jira:** PAY-12 (example)

## Context

Paydocs handles payments and a double-entry ledger, which demand **strong
transactional consistency** (ACID) and mature support for constraints, indexing
and reporting. The team (~10) has deep operational experience with PostgreSQL
and limited appetite for operating a distributed datastore. Expected data
volumes for the first 2–3 years fit comfortably on a single well-provisioned
Postgres instance with read replicas.

## Decision

We will use **PostgreSQL 16** as the primary system of record for the core
(Payments, Ledger, Identity/KYC, Notifications). Document **binaries** are not
stored in Postgres — only metadata is (see
[ADR-005](ADR-005-document-storage-s3.md)). The vector store for the
[RAG layer](../../rag/README.md) uses the `pgvector` extension on the same
engine family.

## Consequences

- Positive: ACID across payments + ledger in one transaction; team expertise;
  rich tooling; Flyway migrations (see [persistence standards](../standards/persistence-standards.md)).
- Negative / trade-offs: **no horizontal sharding** initially — we scale
  vertically + read replicas. This is an accepted risk, tracked as
  [RISK-001](../../quality/risks/RISK-001-single-postgres-no-sharding.md).
- Cross-context direct DB access is forbidden (see
  [integration constraint](../constraints/integration.md)); each context owns its schema.

## Alternatives considered

| Option | Pros | Cons | Why not chosen |
|--------|------|------|----------------|
| MySQL | Familiar | Weaker constraint/JSON/extension story | Less suited to ledger + pgvector |
| Distributed SQL (e.g. CockroachDB) | Horizontal scale | Operational complexity, team unfamiliarity | Premature for current scale |
| MongoDB | Flexible schema | No strong multi-doc ACID guarantees needed here | Rejected, see [project-memory note](../../project-memory/2026-05-20-rejected-mongodb.md) |

## Related

- ADRs: [ADR-004](ADR-004-modular-monolith-plus-services.md), [ADR-005](ADR-005-document-storage-s3.md)
- Risks: [RISK-001](../../quality/risks/RISK-001-single-postgres-no-sharding.md)
- Standards: [persistence-standards](../standards/persistence-standards.md)
