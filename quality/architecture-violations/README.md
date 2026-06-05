# Architecture Violations

> STATUS: governance register. Violations are **facts** detected by tooling
> (ArchUnit, jQAssistant) — they represent [drift](../../domain/drift/README.md)
> between intended architecture (ADR/constraints) and actual code. A high-risk
> violation is an **escalate-immediately** trigger (see [`../../CLAUDE.md`](../../CLAUDE.md)).

## What a violation record looks like

Each violation is `AV-NNN-short-title.md`:

| Field | Meaning |
|-------|---------|
| **ID** | `AV-NNN` |
| **Source** | ArchUnit rule / jQAssistant query that detected it |
| **Violated constraint/ADR** | what intended rule it breaks |
| **Location** | class/package/module |
| **Detected** | date / build / scan |
| **Severity** | Low / Medium / High / Critical |
| **Status** | open / fixing / fixed / accepted (with ADR) |
| **Related Debt / Jira** | links |

## Sources

- **ArchUnit** — fails the build on layering/integration rule breaks (see
  [archunit README](../archunit/README.md)).
- **jQAssistant** — graph queries for cycles, forbidden dependencies, cross-context
  access during the nightly scan.

## Register (EXAMPLE)

| ID | Title | Severity | Status |
|----|-------|----------|--------|
| [AV-001](AV-001-controller-accesses-repository.md) | Controller accesses Repository directly | High | open (example) |

> TODO: auto-populate from the nightly ArchUnit/jQAssistant run.
