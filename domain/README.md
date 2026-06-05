# Domain Intelligence

> STATUS: starter. Implements the **Domain Intelligence Pipeline (DIP)** — see
> [`../.claude/DOMAIN_INTELLIGENCE_BOOTSTRAP.md`](../.claude/DOMAIN_INTELLIGENCE_BOOTSTRAP.md).
> **Core principle: domains are extracted, interpreted and validated — not
> described by hand.**

## The L1–L5 pipeline

| Layer | Name | What | Lives in |
|-------|------|------|----------|
| **L1** | Structural (FACTS) | jQAssistant: classes, packages, deps | [`raw/jqa-graph/`](raw/jqa-graph/) |
| **L2** | Persistence (DATA MODEL) | DB schema + Flyway migrations | [`raw/db-schema/`](raw/db-schema/), [`raw/migrations/`](raw/migrations/) |
| **L3** | Semantic (MEANING) | naming, annotations, API → entities/aggregate/context candidates | [`semantic/`](semantic/) |
| **L4** | Domain Model (INTELLIGENCE) | Claude reasoning → validated contexts/aggregates/rules | [`model/`](model/) |
| **L5** | Governance (CONTROL) | ADR, constraints, ArchUnit → enforced boundaries | [`constraints/`](constraints/) |

## Directory map

```
domain/
  raw/         auto-extracted FACTS — DO NOT hand-edit (jqa-graph, db-schema, migrations, jpa-model)
  semantic/    inferred candidates (entity-map.yaml, inferred-aggregates, bounded-context-candidates)
  model/       VALIDATED model (bounded-contexts.md, aggregates.md, domain-rules.md)  ← human/Claude-validated
  constraints/ enforceable rules (domain-rules.adoc, archunit-domain-rules.java)
  visualization/ domain-graph.dsl, c4-domain-view/
  drift/       domain-drift-report.md
```

- **`raw/` is auto-extracted** by the pipeline (jQAssistant scan, DB introspection,
  migration + JPA extraction). Treat as read-only inputs.
- **`model/` is validated** — the curated, agreed domain model (L4), reviewed by
  the architect. On conflict with `raw/`, `raw/` (code-derived facts) wins and the
  difference is logged as [drift](drift/README.md).

## Flow

```
L1 jQAssistant ─┐
L2 DB + Flyway ─┼─▶ L3 semantic candidates ─▶ L4 Claude synthesis ─▶ model/ ─▶ L5 enforce (ArchUnit/ADR)
JPA + API     ─┘                                            └─▶ drift detection
```

> TODO: wire the extraction jobs; until then `raw/` holds `.gitkeep` placeholders.
