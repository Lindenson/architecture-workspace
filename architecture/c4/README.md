# C4 Model

> STATUS: starter. The C4 model in [`workspace.dsl`](workspace.dsl) is a
> **Structurizr DSL** description of the Paydocs Platform.

## Source of truth

**Code is the source of truth** (see [`../../CLAUDE.md`](../../CLAUDE.md)). The
C4 model should be **generated from the code**, not hand-maintained:

```
codebase ──jQAssistant scan──▶ graph ──export──▶ Structurizr DSL/JSON ──render──▶ C4 diagrams
                                                       │
                                                       └──validate──▶ ArchUnit (boundaries match)
```

- `workspace.dsl` is currently a hand-written **starter**. Replace it with the
  generated model once the jQAssistant → Structurizr pipeline is wired.
- On any conflict between this model and the code/scan, **the code/scan wins**;
  the divergence is recorded as [architecture drift](../../domain/drift/README.md)
  / an [architecture violation](../../quality/architecture-violations/README.md).

## Views provided (EXAMPLE)

- **System Context** — Paydocs and its users (Customer, Operator, Compliance).
- **Containers** — modular monolith core, document services, Postgres, Kafka, object storage.
- **Components** — bounded contexts inside the core (Payments, Ledger, Identity/KYC, Notifications).

## Validation

- The container/component boundaries here must match the
  [layering](../constraints/layering.md) and [integration](../constraints/integration.md)
  constraints, enforced by [ArchUnit](../../quality/archunit/README.md).
- Promoting a model as "approved" is **architect-only** (see role doc).

> TODO: add the Structurizr render command / CI step and link rendered diagrams.
