# Constraint: Layering & module boundaries

> STATUS: enforced constraint (architect-approved). Violations are recorded in
> [`../../quality/architecture-violations/`](../../quality/architecture-violations/README.md)
> and enforced by [ArchUnit](../../quality/archunit/README.md) + jQAssistant.
> Relates to [ADR-004](../adr/ADR-004-modular-monolith-plus-services.md).

## Layers (within each bounded context / module)

```
Controller (API)  ─▶  Service (application/domain)  ─▶  Repository (persistence)
```

## Rules

1. **Controller → Service → Repository** is the only allowed downward call chain.
2. **Controllers MUST NOT access Repositories directly.** All data access goes
   through a Service. (Enforced — see [AV-001 example](../../quality/architecture-violations/AV-001-controller-accesses-repository.md).)
3. **No cross-context direct DB access.** A module must not query another
   module's tables/repositories. Cross-context interaction is via the other
   context's public Service interface (in-process) or async events
   (see [integration](integration.md)).
4. **No cyclic dependencies** between modules/contexts. jQAssistant reports
   cycles as drift.
5. **Persistence types stay in the persistence layer.** JPA entities must not be
   returned from controllers — map to DTOs (see [api-standards](../standards/api-standards.md),
   [persistence-standards](../standards/persistence-standards.md)).
6. **Inward-only dependencies:** API depends on Service, Service on Repository;
   never the reverse.

## Enforcement (EXAMPLE mapping)

| Rule | Enforced by |
|------|-------------|
| Controller ↛ Repository | ArchUnit `layeredArchitecture()` |
| No cross-context access | ArchUnit slices + jQAssistant package rules |
| No cycles | ArchUnit `slices().should().beFreeOfCycles()` |
| Entity not in API | ArchUnit `noClasses().that()...Entity...dependOnControllers` |

> TODO: link the exact ArchUnit test class names once the
> `architecture-tests` module exists (see [archunit README](../../quality/archunit/README.md)).
