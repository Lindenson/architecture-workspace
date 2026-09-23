---
name: feature-impact-analysis
description: Build the context pack for a feature before planning it — blast radius in the dependency graph, affected bounded contexts, binding ADRs and constraints, existing technical debt in the touched area, and cross-service contracts. Run immediately after /speckit-specify and before /speckit-clarify. Trigger on "what does this feature touch", "оцени影响 фичи", "blast radius", "какие компоненты затронет", or when a spec exists without an impact.md.
---

# Feature Impact Analysis — join point J1

## Purpose
Turn a natural-language specification into a **fact-grounded scope**: which code
actually changes, which architectural decisions bind that code, and what is
already broken there. This is what makes `spec-critic` able to cite evidence
instead of opinion, and what keeps the implementer's context small.

Produces `specs/<feature>/impact.md`. Read-only over product code.

## Inputs / Sources
- `specs/<feature>/spec.md` — the feature under analysis
- `jqassistant-mcp` — `queryGraph`, `dependenciesOf`, `dependentsOf`, `godClasses`, `findCycles`
- `structurizr-mcp` — `listElements`, `listRelationships`, `detectDrift`
- `domain/model/bounded-contexts.md`, `domain/model/aggregates.md`
- `architecture/adr/`, `architecture/constraints/`
- `quality/technical-debt/`, `quality/architecture-violations/`, `quality/risks/`
- `sonar-mcp.technicalDebt` for the touched modules
- `rag-mcp.retrieveContext` — prior decisions about this area

## Procedure

### 1. Extract seed entities from the spec
Pull the nouns that map to code: aggregates, entities, endpoints, events,
tables. Resolve each to a fully-qualified name or package. Anything you cannot
resolve goes in the output as `UNRESOLVED` — never guess a mapping.

### 2. Compute the blast radius
Seeds are the starting set; the radius is everything that depends on them.

```cypher
// 1. forward: what the seeds depend on (what the change must respect)
MATCH (a)-[:DEPENDS_ON]->(b)
WHERE (a:Type OR a:Package) AND coalesce(a.fqn, a.name) IN $seeds
RETURN coalesce(a.fqn,a.name) AS from, coalesce(b.fqn,b.name) AS to
```

```cypher
// 2. reverse blast radius: who breaks if the seeds change (depth 3)
MATCH path = (caller)-[:DEPENDS_ON*1..3]->(seed:Type)
WHERE coalesce(seed.fqn, seed.name) IN $seeds
RETURN DISTINCT coalesce(caller.fqn, caller.name) AS impacted,
       length(path) AS distance
ORDER BY distance, impacted
```

```cypher
// 3. does the radius cross a bounded-context package boundary?
MATCH (a:Type)-[:DEPENDS_ON]->(b:Type)
WHERE coalesce(a.fqn,a.name) IN $radius
  AND NOT coalesce(b.fqn,b.name) STARTS WITH $ownContextPackage
  AND any(ctx IN $foreignContextPackages
          WHERE coalesce(b.fqn,b.name) STARTS WITH ctx)
RETURN coalesce(a.fqn,a.name) AS from, coalesce(b.fqn,b.name) AS to
```

Use `dependentsOf` / `dependenciesOf` when a single FQN is enough; drop to
`queryGraph` for the set-based queries above. If Neo4j is unavailable, say so,
fall back to Structurizr + package structure, and cap confidence at `MEDIUM`.

### 3. Map the radius onto the architecture
- Which Structurizr containers/components does it cover? (`listElements`)
- Which bounded contexts? Does the feature **cross** a boundary?
- Which cross-service relationships are touched? (`listRelationships`) — for each,
  record producer, consumer, contract, versioning, failure behaviour.

### 4. Collect the binding decisions
Every ADR and constraint whose scope intersects the radius. For each: ID, what it
forbids or requires here, and whether the current code already complies. An ADR
that binds but is already violated is a **finding**, not a footnote.

### 5. Collect what is already broken there
Open technical debt, architecture violations, risks and Sonar debt located inside
the radius. The feature will sit on top of these; the human needs to decide
whether to pay them down now or accept them consciously.

### 6. Classify the change
`LOCAL` (one component, no contract change) · `CROSS-COMPONENT` ·
`CROSS-CONTEXT` (crosses a bounded-context boundary — requires explicit
ownership decision) · `CROSS-SERVICE` (contract, versioning and failure
semantics required) · `ARCHITECTURAL` (needs an ADR before implementation).

This classification decides how heavy the rest of Loop B is.

## Output (contract)
Write `specs/<feature>/impact.md`:

```markdown
# Impact Analysis — <feature>

## Classification
LOCAL | CROSS-COMPONENT | CROSS-CONTEXT | CROSS-SERVICE | ARCHITECTURAL

## Sources used / Confidence
...  ·  HIGH | MEDIUM | LOW  ·  stale or unavailable sources: ...

## Seed entities
| Spec concept | Resolved to | Confidence |

## Blast radius
| Component / Type | Distance | Bounded context | Owner |
UNRESOLVED: ...

## Bounded contexts crossed
...  (none → say "none")

## Cross-service contracts touched
| Producer | Consumer | Contract | Version | Failure behaviour | Idempotent? |

## Binding ADRs and constraints
| ID | Requires / forbids here | Currently compliant? |

## Existing debt inside the radius
| ID | Severity | Relation to this feature |

## Architectural decisions this feature forces
DECISION REQUIRED: <question> — Option A ... Option B ... trade-offs ... impact ...

## Recommended scope for /speckit-plan
...
```

## Guardrails
- Read-only. Never modify product code, `spec.md`, ADRs or the C4 model.
- Never invent a mapping from a spec noun to a class. `UNRESOLVED` is a valid and
  useful answer; a wrong FQN silently shrinks the blast radius.
- Do not resolve `DECISION REQUIRED` items yourself — surface them for the human.
- Graph unavailable → degrade, mark `DATA_STALE`, cap confidence. Never
  substitute a plausible-looking radius for a measured one.
