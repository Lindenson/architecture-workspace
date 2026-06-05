# Target State (EXAMPLE / INTENT — NOT YET BUILT)

> STATUS: intent. Clearly a direction-of-travel template. TODO: refine with the
> Lead Architect; each adopted move needs its own ADR.

## Where we are heading

The current architecture is a **modular monolith core + two document services**
([ADR-004](../adr/ADR-004-modular-monolith-plus-services.md)). The target makes
the system **more event-driven** and extracts the document domain fully, while
keeping payments + ledger transactionally tight.

## Target moves (EXAMPLE)

| # | Move | From → To | Rationale | Likely ADR |
|---|------|-----------|-----------|------------|
| T1 | Fully extract Document Processing | in-core links → standalone event-driven service | Independent scaling of OCR; clearer boundary | TODO (new ADR) |
| T2 | Event-first integration | mixed in-process + events → events as default cross-context | Looser coupling, replay, auditability | extends [ADR-003](../adr/ADR-003-kafka.md) |
| T3 | Event schema governance | ad-hoc → schema registry + versioned contracts | Safe evolution of events | TODO (new ADR) |
| T4 | Read models / CQRS for reporting | direct queries → projections off events | Offload reporting from transactional store | TODO |
| T5 | Postgres scaling path | single instance → replicas + partitioning (sharding only if forced) | Mitigate [RISK-001](../../quality/risks/RISK-001-single-postgres-no-sharding.md) | extends [ADR-001](../adr/ADR-001-postgresql.md) |

## Explicit non-goals (for now)

- Full microservices for the transactional core (payments/ledger stay together).
- Kafka Streams (rejected — see [project-memory](../../project-memory/2026-03-10-rejected-kafka-streams.md)).
- Polyglot persistence beyond Postgres + object store (MongoDB rejected — see
  [project-memory](../../project-memory/2026-05-20-rejected-mongodb.md)).

## Guardrails for getting there

- Every extraction respects current [layering](../constraints/layering.md) /
  [integration](../constraints/integration.md) constraints — boundaries are the
  extraction seams.
- Moves are sequenced on the [roadmap](../../delivery/roadmap/roadmap.md) and
  recorded in [history](../../history/decisions/README.md).

> TODO: add a target C4 view under `../c4/` once a move is approved.
