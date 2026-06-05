# Persistence Standards

> STATUS: starter standard. Relates to [ADR-001 (PostgreSQL)](../adr/ADR-001-postgresql.md)
> and [layering constraint](../constraints/layering.md).

## Migrations

- **Flyway** is the migration tool (versioned, forward-only).
- Migration files: `V<version>__<description>.sql`, e.g.
  `V12__add_idempotency_key_to_payment.sql`. Never edit an applied migration —
  add a new one.
- Repeatable migrations (`R__...`) only for views/functions.
- Migrations are reviewed like code and run automatically per environment
  (see [environments](../../knowledge/environments.md)).
- Raw migration snapshots are mirrored for domain analysis under
  [`../../domain/raw/migrations/`](../../domain/raw/migrations/).

## Schema naming (EXAMPLE)

- Tables: `snake_case`, singular noun per aggregate (`payment`, `ledger_entry`).
- Each bounded context owns its tables; **no cross-context FK across contexts**
  (reference by id + event, not by foreign key) — see [integration](../constraints/integration.md).
- Primary keys: surrogate `id` (UUID v7 preferred for ordering). Money stored as
  `numeric` + explicit currency column — never floating point.
- Timestamps in UTC (`timestamptz`); include `created_at` / `updated_at`.

## ORM rules

- JPA entities live in the persistence layer **only**; they are **never returned
  from controllers** — map to DTOs (see [api-standards](../standards/api-standards.md),
  [domain ArchUnit rules](../../domain/constraints/archunit-domain-rules.java)).
- Repositories accessed only via the Service layer.
- Avoid N+1: use fetch joins / `@EntityGraph` / batch size
  (see [performance](../constraints/performance.md)).
- No business logic in entities beyond invariants of their own aggregate.

> TODO: confirm UUID strategy and the money/currency value-object approach.
