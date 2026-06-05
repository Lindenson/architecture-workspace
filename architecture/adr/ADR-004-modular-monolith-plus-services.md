# ADR-004: Modular monolith core + selective microservices

> EXAMPLE ADR — illustrative content for the starter workspace.

- **Status:** accepted
- **Date:** 2026-02-10 (example)
- **Deciders:** Lead Architect, Team Lead
- **Supersedes:** —
- **Superseded by:** —
- **Related Jira:** PAY-55 (example)

## Context

A ~10-person team must move fast while keeping payments + ledger strongly
consistent. A full microservices topology would impose distributed-transaction
and operational costs disproportionate to the team size. However, document OCR is
CPU/IO-heavy with a very different scaling and deployment profile from the
transactional core.

## Decision

We will build a **modular monolith** (`paydocs-core`) containing the
transactional contexts — Payments, Ledger, Identity/KYC, Notifications — with
**enforced module boundaries** (see [layering](../constraints/layering.md) and
the [ArchUnit gate](../../quality/archunit/README.md)). Each context is a
package/module with a public service interface; **no cross-context direct DB
access**.

We will extract to **separate services** only where justified: the
**document-processing service** (async OCR) and a thin **document-storage
service** (object-store gateway — see [ADR-005](ADR-005-document-storage-s3.md)).
Cross-service communication is Kafka events + REST contracts only (see
[ADR-003](ADR-003-kafka.md), [integration constraint](../constraints/integration.md)).

## Consequences

- Positive: transactional consistency where it matters; independent scaling for
  OCR; fewer moving parts than full microservices; clear extraction seams.
- Negative / trade-offs: discipline required to keep module boundaries from
  eroding into a big ball of mud — enforced by ArchUnit + jQAssistant and
  watched as [drift](../../domain/drift/README.md).
- The boundaries chosen here are the candidate seams for future extraction
  (see [target-state](../target-architecture/target-state.md)).

## Alternatives considered

| Option | Pros | Cons | Why not chosen |
|--------|------|------|----------------|
| Single layered monolith | Simplest | Boundaries erode; no independent OCR scaling | Insufficient for OCR profile |
| Full microservices | Independent everything | Distributed tx, ops cost for 10 people | Disproportionate complexity |
| Modular monolith + selective services | Balance | Requires boundary discipline | **Chosen** |

## Related

- ADRs: [ADR-001](ADR-001-postgresql.md), [ADR-003](ADR-003-kafka.md), [ADR-005](ADR-005-document-storage-s3.md)
- Constraints: [layering](../constraints/layering.md), [integration](../constraints/integration.md)
- Domain: [bounded-contexts](../../domain/model/bounded-contexts.md)
