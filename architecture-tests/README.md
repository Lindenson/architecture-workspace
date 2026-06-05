# architecture-tests — ArchUnit enforcement (template module)

A **standalone** Maven module (NOT part of the `mcp-servers/` reactor — it has no
parent and its own self-contained `pom.xml`) that the **product team copies into
their build** to enforce the AIP architecture constraints with
[ArchUnit](https://www.archunit.org/).

Because the product source code does not live in this workspace, the rules here
are **templates**: they target a configurable placeholder base package
(`com.yourorg.product`) and every test class carries
`@Disabled("Template: …")` so this repo's CI does not fail. Activate them in the
product build (see below).

## Versions

- Java 21
- JUnit Jupiter `5.11.4`
- ArchUnit `com.tngtech.archunit:archunit-junit5:1.4.2`
- maven-surefire-plugin `3.5.2`

## What each test enforces

| Test class                     | Source constraint | Rules |
|--------------------------------|-------------------|-------|
| `LayeredArchitectureTest`      | [`../architecture/constraints/layering.md`](../architecture/constraints/layering.md) | Controllers never touch repositories; layered `Api → Service → Persistence`; context slices free of cycles. |
| `NamingAndDtoRulesTest`        | layering.md rule 5 + [`../domain/constraints/`](../domain/constraints/) | `*Dto` must not reference an `@Entity`; entities live in `..domain..`; repositories are interfaces; no field injection (`@Autowired` on fields). |
| `DomainBoundaryTest`           | [`../architecture/constraints/integration.md`](../architecture/constraints/integration.md) + [`../domain/constraints/archunit-domain-rules.java`](../domain/constraints/archunit-domain-rules.java) | Bounded contexts interact only via `api`/`service`; no cross-context `persistence` access; context slices free of cycles. |

## Wiring into the product build

1. **Copy** this `architecture-tests/` module into the product repository (e.g. as
   a Maven module, or merge the `src/test/java` rules into an existing test
   module). Keep ArchUnit as a `test`-scoped dependency.
2. **Point it at the product code** — set the base package one of two ways:
   - edit `<archunit.base.package>` in `pom.xml`, or
   - pass `-Darchunit.base.package=com.acme.billing` on the Maven command line.
   The value is handed to the tests as the `archunit.base.package` system
   property (surefire `systemPropertyVariables`); the tests read it via
   `System.getProperty(...)` with the `com.yourorg.product` default.
3. **Remove the `@Disabled` annotation** from each test class you want enforced.
4. Adjust example specifics (e.g. the `payments`/`ledger` context names in
   `DomainBoundaryTest`, annotation FQNs) to the product's real packages.
5. Run `mvn test` — failures now block the build (the architecture gate).

## Generating new rules

These three classes are a starting set. **Claude (the architecture agent) can
GENERATE new ArchUnit rules** from approved ADRs and the constraint documents in
[`../architecture/constraints/`](../architecture/constraints/) and
[`../domain/constraints/`](../domain/constraints/): when a new constraint is
ratified, ask the agent to emit a matching `@ArchTest`/`@Test` rule (with a
`.because(...)` citing the source) and add it here. The
[`../quality/archunit/`](../quality/archunit/) area tracks which constraints are
already covered by an enforced rule.
