# Domain Visualization

> STATUS: starter. Visual views of the domain model — bounded contexts,
> aggregates and cross-context flows. Should be **generated** from the model +
> scan, not hand-maintained (see [`../README.md`](../README.md)).

## Artifacts

- [`domain-graph.dsl`](domain-graph.dsl) — Structurizr-style DSL of the domain
  contexts and their relationships (a domain-level C4 view).
- [`c4-domain-view/`](c4-domain-view/) — rendered C4 domain views (generated;
  currently a `.gitkeep` placeholder).

## Relationship to architecture C4

- The [architecture C4 model](../../architecture/c4/README.md) shows *runtime
  containers/components*.
- This **domain view** shows *bounded contexts + aggregates* (the L4 model) and
  cross-context flows — a DDD lens rather than a deployment lens.

## Tooling

- Structurizr (DSL → diagrams) and graph export from jQAssistant
  (per [DIP](../../.claude/DOMAIN_INTELLIGENCE_BOOTSTRAP.md) → Domain Visualization).

> TODO: wire generation; keep `domain-graph.dsl` in sync with `../model/bounded-contexts.md`.
