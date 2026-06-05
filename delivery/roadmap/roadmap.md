# Paydocs Roadmap (EXAMPLE / STARTER TEMPLATE)

> STATUS: example quarterly themes. TODO: replace with real plan; link Jira epics.

## 2026 — quarterly themes (EXAMPLE)

| Quarter | Theme | Key outcomes | Architecture link |
|---------|-------|--------------|--------------------|
| Q1 | Foundations | Modular monolith core, Postgres, CI, ArchUnit gate | [ADR-001](../../architecture/adr/ADR-001-postgresql.md), [ADR-004](../../architecture/adr/ADR-004-modular-monolith-plus-services.md) |
| Q2 | Payments hardening | Idempotency, ledger invariants, audit logging | [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md), [security](../../architecture/constraints/security.md) |
| Q3 | Document processing at scale | Extract document-processing service, async OCR SLAs | [target T1](../../architecture/target-architecture/target-state.md), [ADR-005](../../architecture/adr/ADR-005-document-storage-s3.md) |
| Q4 | Event-first & insights | Event schema governance, read models/reporting | [target T2–T4](../../architecture/target-architecture/target-state.md), [ADR-003](../../architecture/adr/ADR-003-kafka.md) |

## Notes

- Themes are directional; scope per quarter is finalized with the PO each planning cycle.
- Each themed architecture change requires an ADR before build (see [adr README](../../architecture/adr/README.md)).

> TODO: add the linked Jira epic keys (e.g. PAY-100, PAY-150 …).
