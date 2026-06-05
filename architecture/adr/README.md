# Architecture Decision Records (ADRs)

> STATUS: governance doc. ADRs are **architect-approved** (see
> [`../../.claude/ROLE_ARCHITECT_AGENT.md`](../../.claude/ROLE_ARCHITECT_AGENT.md)).
> An agent may write *drafts* under [`drafts/`](drafts/) but never accept/supersede.

## What is an ADR?

An Architecture Decision Record captures **one** significant architectural
decision: the context that forced it, the decision taken, and the consequences
we accept. ADRs are immutable once accepted — we don't edit history, we
**supersede** it with a new ADR.

ADRs sit at level 5 in the [source-of-truth hierarchy](../../CLAUDE.md): code,
scans, ArchUnit, Structurizr and Sonar all outrank an ADR. If code contradicts
an ADR, that is **drift** — record it in
[`../../quality/architecture-violations/`](../../quality/architecture-violations/README.md),
don't silently rewrite the ADR.

## Lifecycle

```
proposed ──accept──▶ accepted ──supersede──▶ superseded
   │                                              ▲
   └────────────reject──────────────────────────-┘ (kept, status: rejected)
```

- **proposed** — drafted, under review. Lives in [`drafts/`](drafts/).
- **accepted** — approved by the Lead Architect; moved to this directory.
- **superseded** — replaced by a newer ADR; keep the file, add `Superseded by ADR-NNN`.
- **rejected** — considered and declined; keep for the record (see also
  [`../../project-memory/`](../../project-memory/README.md) for rejected-option notes).

## Numbering & naming

- Sequential: `ADR-001`, `ADR-002`, … never reused.
- Filename: `ADR-NNN-short-kebab-title.md`.
- Use the [`_TEMPLATE.md`](_TEMPLATE.md). Drafts go to [`drafts/`](drafts/) until accepted.

## Index (EXAMPLE)

| ADR | Title | Status |
|-----|-------|--------|
| [001](ADR-001-postgresql.md) | PostgreSQL as primary datastore | accepted |
| [002](ADR-002-spring-boot.md) | Spring Boot as application framework | accepted |
| [003](ADR-003-kafka.md) | Kafka for async eventing (outbox) | accepted |
| [004](ADR-004-modular-monolith-plus-services.md) | Modular monolith + selective services | accepted |
| [005](ADR-005-document-storage-s3.md) | Object storage for documents | accepted |

> TODO: keep this index updated whenever an ADR is added or superseded.
