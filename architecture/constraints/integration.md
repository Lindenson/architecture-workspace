# Constraint: Integration between contexts & services

> STATUS: enforced constraint. Relates to
> [ADR-003 (Kafka)](../adr/ADR-003-kafka.md) and
> [ADR-004 (modular monolith + services)](../adr/ADR-004-modular-monolith-plus-services.md).

## Rules

1. **Cross-context calls go through the public Service layer** (in-process,
   inside the monolith) — never another context's repository or tables
   (see [layering](layering.md)).
2. **Cross-service communication is async events (Kafka) or REST contracts only.**
   No shared database between services.
3. **Every published REST API has an OpenAPI 3 contract** checked into the
   producing repo (see [api-standards](../standards/api-standards.md)).
4. **Published events use the transactional outbox** (see [ADR-003](../adr/ADR-003-kafka.md))
   and a versioned schema. Consumers must be **idempotent**.
5. **Idempotency for payments is mandatory.** Payment initiation requires a
   client `Idempotency-Key`; a repeated key returns the original result and never
   creates a second payment (see [domain-rules](../../domain/model/domain-rules.md)
   and [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md)).
6. **No synchronous cross-service call inside a DB transaction** — avoids
   distributed-transaction coupling; use events/sagas instead.

## Allowed integration matrix (EXAMPLE)

| From → To | Mechanism |
|-----------|-----------|
| Payments → Ledger | In-process Service call (same tx, core) |
| Payments → Identity/KYC | In-process Service call |
| Core → document-processing-service | Kafka event + REST (pre-signed upload) |
| Any context → Notifications | Kafka domain event |
| document-processing → document-storage | REST + pre-signed URL |

## Enforcement

| Rule | Enforced by |
|------|-------------|
| No cross-context repo access | ArchUnit slices, jQAssistant |
| OpenAPI present | CI contract check (TODO) |
| Idempotency on payments | Integration test + [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md) |

> TODO: add the event catalog / schema registry location once chosen.
