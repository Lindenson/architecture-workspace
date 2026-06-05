# Testing Standards

> STATUS: starter standard. EXAMPLE targets — TODO: confirm coverage gates with QA.

## Frameworks

- **JUnit 5** + AssertJ for assertions; Mockito for mocking.
- **Testcontainers** for PostgreSQL/Kafka integration tests (no H2 substitutes
  for behavior that depends on Postgres).
- **ArchUnit** for architecture rules (see below).

## Test pyramid

```
        /\        few   — E2E / contract (slow, high value)
       /  \       some  — integration (@SpringBootTest, Testcontainers)
      /____\      many  — unit (fast, isolated, domain logic)
```

- Most logic covered by fast unit tests; integration tests for wiring,
  persistence, messaging; a thin layer of end-to-end/contract tests.

## ArchUnit gate

- Architecture rules run as part of the test build and **fail the build** on
  violation. They enforce the [layering](../constraints/layering.md) and
  [integration](../constraints/integration.md) constraints, e.g.:
  - Controllers must not access Repositories.
  - No cross-context dependencies / no cycles.
  - Entities must not be exposed by controllers.
- See [`../../quality/archunit/README.md`](../../quality/archunit/README.md) and the
  domain rules in [`../../domain/constraints/archunit-domain-rules.java`](../../domain/constraints/archunit-domain-rules.java).

## Coverage targets (EXAMPLE)

| Scope | Target |
|-------|--------|
| Domain / service logic | ≥ 85% line, ≥ 70% branch |
| Overall project | ≥ 75% line |
| New/changed code (Sonar new-code) | ≥ 80% (quality gate) |

## Conventions

- One behavior per test; `@DisplayName` describing the behavior.
- No logic/branching in tests; prefer `@ParameterizedTest` for variants.
- Tests must be deterministic — no reliance on wall-clock/network/order.

> TODO: link JaCoCo config and the Sonar quality gate definition.
