# RAG Indexing Plan

> STATUS: starter. Defines **what** gets indexed into the [RAG layer](../README.md)
> and **when** to reindex. EXAMPLE — MVP-3.

## Sources to index (priority order)

| Source | Location | Priority | Notes |
|--------|----------|----------|-------|
| ADRs | [`../../architecture/adr/`](../../architecture/adr/README.md) | High | Decisions + rationale |
| Constraints & standards | [`../../architecture/constraints/`](../../architecture/constraints/), `../../architecture/standards/` | High | Governance rules |
| Project memory | [`../../project-memory/`](../../project-memory/README.md) | High | Why/why-not, rejected options |
| Domain model | [`../../domain/model/`](../../domain/model/bounded-contexts.md) | High | Contexts, aggregates, rules |
| Knowledge base | [`../../knowledge/`](../../knowledge/project-overview.md) | Medium | Overview, glossary, domain |
| History | [`../../history/`](../../history/decisions/README.md) | Medium | Decisions, incidents, lessons |
| Wiki export | (external) | Medium | TODO: wire wiki-mcp export |
| Code docs / READMEs | product repos | Medium | API docs, module READMEs |
| Tech debt & risks | [`../../quality/`](../../quality/technical-debt/README.md) | Low | Status-dependent |

## Excluded

- Secrets / credentials (never — see [security](../../architecture/constraints/security.md)).
- Raw PII/PCI data; `domain/raw/` binary artifacts.
- Generated/large binaries and build output.

## Chunking & metadata (EXAMPLE)

- Markdown chunked by heading (~500–800 tokens, overlap ~10%).
- Each chunk carries: `source_path`, `title`, `section`, `type` (adr/constraint/
  memory/…), `last_modified`, `confidence_hint`.

## Reindex triggers

- **On change:** Git push touching an indexed source (CI hook).
- **On ADR lifecycle:** ADR added / accepted / superseded.
- **Scheduled:** nightly full reconciliation.
- **On wiki update:** wiki-mcp change event.

> TODO: implement the indexer (rag-mcp) and pin the chunk size/embedding model.
