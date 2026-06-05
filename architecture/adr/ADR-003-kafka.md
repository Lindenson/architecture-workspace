# ADR-003: Apache Kafka for asynchronous eventing between contexts

> EXAMPLE ADR — illustrative content for the starter workspace.

- **Status:** accepted
- **Date:** 2026-02-03 (example)
- **Deciders:** Lead Architect, Team Lead
- **Supersedes:** —
- **Superseded by:** —
- **Related Jira:** PAY-41 (example)

## Context

Bounded contexts must stay decoupled, yet react to each other's facts: a settled
payment notifies the customer; an ingested document signals OCR completion. Cross-
context **synchronous** coupling and shared databases are forbidden (see
[integration constraint](../constraints/integration.md)). We need reliable,
ordered, replayable async messaging that the team can operate.

## Decision

We will use **Apache Kafka** as the asynchronous event backbone between bounded
contexts and services. Producers publish domain events using the **transactional
outbox pattern**: the event is written to an `outbox` table in the same DB
transaction as the state change, and a relay publishes it to Kafka — guaranteeing
at-least-once delivery without dual-write inconsistency. Consumers must be
**idempotent** (de-dupe on event id / idempotency key).

We do **not** adopt Kafka Streams for stateful stream processing at this time
(rejected — see [project-memory note](../../project-memory/2026-03-10-rejected-kafka-streams.md)).

## Consequences

- Positive: contexts decoupled; events replayable; natural fit for sagas and
  the Notifications context; aligns with [target-state](../target-architecture/target-state.md)
  (more event-driven).
- Negative / trade-offs: eventual consistency across contexts; operational burden
  (Kafka cluster per env); need outbox + relay plumbing and consumer idempotency.
- Event schemas must be versioned/contracted (TODO: pick schema registry / Avro
  vs JSON Schema — track in a follow-up ADR).

## Alternatives considered

| Option | Pros | Cons | Why not chosen |
|--------|------|------|----------------|
| RabbitMQ | Simpler ops | Weaker replay/log semantics | Replayable log preferred |
| DB-table queue only | No new infra | Doesn't scale to cross-service eventing | Insufficient |
| Kafka Streams | Powerful stream processing | Too complex for team now | Deferred / rejected for now |

## Related

- ADRs: [ADR-004](ADR-004-modular-monolith-plus-services.md), [ADR-001](ADR-001-postgresql.md)
- Constraints: [integration](../constraints/integration.md)
- Memory: [rejected Kafka Streams](../../project-memory/2026-03-10-rejected-kafka-streams.md)
