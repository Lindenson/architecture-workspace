# Coding Standards (Java 21)

> STATUS: starter standard (architect-approved). EXAMPLE conventions — adapt to
> the team's formatter/linter config. Relates to [ADR-002](../adr/ADR-002-spring-boot.md).

## Language & idioms (Java 21)

- Prefer **records** for immutable value/DTO types; **sealed** hierarchies for
  closed type sets; **pattern matching** in `switch`/`instanceof`.
- Favor immutability and `final` for fields/locals where it aids clarity.
- Use the Streams API for transformations, but keep readability — no clever
  one-liners that hide intent.
- Virtual threads for blocking IO where measured useful (see [performance](../constraints/performance.md)).

## Null-safety

- Public APIs: never return `null` collections — return empty.
- Use `Optional<T>` for "maybe absent" return values; do not use it for fields
  or method parameters.
- Validate inputs at the boundary (Bean Validation / explicit checks); fail fast.

## Error handling

- Throw **domain-specific** exceptions; do not leak persistence/framework
  exceptions across layers.
- No swallowing exceptions (no empty `catch`). Either handle meaningfully or
  propagate with context.
- Map exceptions to the standard API error model in one place (see [api-standards](../standards/api-standards.md)).

## Logging

- SLF4J with parameterized messages (`log.info("paid {}", id)`), never string
  concatenation.
- **Never log PII/PCI or secrets** (see [security](../constraints/security.md)).
- Include a correlation id on every request/event for traceability.
- Levels: ERROR (action needed), WARN (recoverable anomaly), INFO (business
  milestones), DEBUG (diagnostics). No INFO spam in hot paths.

## General

- Small, single-responsibility classes/methods; respect [layering](../constraints/layering.md).
- Public methods documented where intent isn't obvious; no commented-out code.
- Formatting/imports enforced by the build (TODO: Spotless / google-java-format?).

> TODO: link the exact formatter config and the static-analysis ruleset.
