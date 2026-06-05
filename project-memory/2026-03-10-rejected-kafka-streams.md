# 2026-03-10 — Rejected Kafka Streams

> EXAMPLE project-memory entry (rejected option).

- **Date:** 2026-03-10
- **Author:** Lead Architect (example)
- **Related ADR:** [ADR-003 (Kafka)](../architecture/adr/ADR-003-kafka.md)
- **Related Jira:** PAY-96 (example)

## Context

While adopting Kafka for cross-context eventing ([ADR-003](../architecture/adr/ADR-003-kafka.md)),
we evaluated **Kafka Streams** for stateful stream processing (e.g. real-time
aggregation of payment/document events, projections).

## Reason (why rejected)

- Operational and conceptual complexity (state stores, rebalancing, changelog
  topics) is **high for a ~10-person team** with no prior Streams experience.
- The current needs are satisfied by plain consumers + the transactional outbox;
  no requirement yet justifies stateful streaming.
- Adding it now would broaden the on-call surface and slow delivery.

## Result

Decided **not** to adopt Kafka Streams for now. Use standard Kafka producers/
consumers with idempotent handling. Revisit if/when real-time projections or
read models are needed (see [target-state T4](../architecture/target-architecture/target-state.md)).

## Consequences

- Simpler operational model retained.
- If CQRS/read-model work (T4) lands, re-evaluate Streams vs. simpler projection
  consumers, and record a new ADR if adopted.
