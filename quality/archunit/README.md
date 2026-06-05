# ArchUnit Gate

> STATUS: governance doc. ArchUnit is level 3 in the
> [source-of-truth hierarchy](../../CLAUDE.md) — it asserts architecture rules as
> tests that **fail the build** on violation.

## Where the tests live

The executable rules live in the product codebase, in an `architecture-tests`
module (TODO: confirm path/module name once the product repo is linked). This
directory documents *what* is enforced; the *code* is authoritative.

Domain-boundary rules are templated in
[`../../domain/constraints/archunit-domain-rules.java`](../../domain/constraints/archunit-domain-rules.java).

## Constraints enforced (EXAMPLE)

| # | Rule | Backing constraint |
|---|------|--------------------|
| 1 | Controllers must not access Repositories | [layering](../../architecture/constraints/layering.md) |
| 2 | Layered architecture: API → Service → Repository only | [layering](../../architecture/constraints/layering.md) |
| 3 | No cyclic dependencies between context slices | [layering](../../architecture/constraints/layering.md) |
| 4 | No cross-context package access (slices isolated) | [integration](../../architecture/constraints/integration.md) |
| 5 | Controllers must not return/expose JPA entities | [persistence-standards](../../architecture/standards/persistence-standards.md) |
| 6 | DTOs must not reference entities | [domain ArchUnit rules](../../domain/constraints/archunit-domain-rules.java) |
| 7 | No new `RestTemplate` usage (once migration starts) | [TD-001](../technical-debt/TD-001-resttemplate-usage.md) |

## On failure

A failing rule is recorded as an [architecture violation](../architecture-violations/README.md)
and, if high-risk, escalated. ArchUnit and jQAssistant overlap by design —
ArchUnit gates the build, jQAssistant gives the nightly graph view.

> TODO: link the actual ArchUnit test class names + CI job.
