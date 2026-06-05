# ADR-002: Spring Boot as the application framework

> EXAMPLE ADR — illustrative content for the starter workspace.

- **Status:** accepted
- **Date:** 2026-01-15 (example)
- **Deciders:** Lead Architect, Team Lead
- **Supersedes:** —
- **Superseded by:** —
- **Related Jira:** PAY-13 (example)

## Context

We need a mature JVM application framework for both the modular monolith and the
extracted services: dependency injection, web/REST, data access, messaging
integration, security, observability and a large hiring pool. The team is
Java-21-first and already productive with the Spring ecosystem.

## Decision

We will use **Spring Boot 3.4.x on Java 21** as the application framework across
`paydocs-core` and all services. Standard building blocks: Spring Web (REST),
Spring Data JPA (over [PostgreSQL](ADR-001-postgresql.md)), Spring for Apache
Kafka (see [ADR-003](ADR-003-kafka.md)), Spring Security, and Spring Boot
Actuator + Micrometer/OpenTelemetry for observability.

## Consequences

- Positive: cohesive stack, fast onboarding, first-class testing
  (`@SpringBootTest`, Testcontainers), broad community support.
- Negative / trade-offs: framework lock-in and startup/memory overhead; we
  mitigate cold-start concerns for services later (target-state may consider
  native/AOT — see [target-state](../target-architecture/target-state.md)).
- We adopt virtual threads (Java 21) for IO-bound work where measured to help.

## Alternatives considered

| Option | Pros | Cons | Why not chosen |
|--------|------|------|----------------|
| Quarkus | Fast startup, native | Smaller team experience | Higher ramp-up cost now |
| Micronaut | Compile-time DI | Smaller ecosystem | Less familiar |
| Plain Jakarta EE | Standards-based | More wiring, less momentum | Slower delivery |

## Related

- ADRs: [ADR-001](ADR-001-postgresql.md), [ADR-003](ADR-003-kafka.md), [ADR-004](ADR-004-modular-monolith-plus-services.md)
- Standards: [coding-standards](../standards/coding-standards.md), [api-standards](../standards/api-standards.md)
