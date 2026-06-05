# Domain Drift

> STATUS: starter. **Domain drift** is divergence between the validated domain
> model ([`../model/`](../model/bounded-contexts.md)) and the *facts extracted
> from code/DB* ([`../raw/`](../README.md)). Per the
> [hierarchy](../../CLAUDE.md), the **code-derived facts win** — drift is
> reported, not silently fixed.

## What counts as drift (per the DIP)

- **Entity without an aggregate** — a persistent entity not mapped to any aggregate.
- **Table without an entity** — schema not represented in the model.
- **DTO leaking persistence** — a DTO referencing a JPA entity.
- **Cross-context dependency** — a context touching another's internals/tables.
- **Inconsistent naming** — entity/table/context names that don't align.
- **Orphan migration** — a migration not reflected in the model/entities.
- **New cross-context FK** — a foreign key crossing a context boundary.

## Report format (`domain-drift-report.md` — EXAMPLE shape)

| Drift type | Location | Detected | Severity | Suggested action |
|------------|----------|----------|----------|------------------|
| DTO leaks entity | `payments.api.PaymentDto` → `Payment` | scan 2026-06-05 | High | Map to record; add ArchUnit rule |
| Table without entity | `legacy_fee` | scan 2026-06-05 | Medium | Model or drop |

- High-severity drift (e.g. cross-context access, ADR violation) is an
  **escalate-immediately** trigger and is also logged as an
  [architecture violation](../../quality/architecture-violations/README.md).

## How it's produced

The Domain MCP merges L1 (jQAssistant) + L2 (DB/migrations) + L3 (semantic) and
diffs against `model/`, emitting `domain-drift-report.md`.

> TODO: generate the report from the pipeline; this README defines the format.
