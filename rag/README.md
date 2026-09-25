# RAG Layer

> STATUS: starter. The **Retrieval-Augmented Generation** layer that lets the
> digital twin answer questions grounded in the workspace's knowledge. This is
> an **MVP-3** capability (shipped, OPTIONAL — off unless `KNOWLEDGE_ENABLED=true`),
> built on `pgvector`.

## Pipeline

```
sources/ ──chunk──▶ chunks/ ──embed──▶ embeddings/ ──index──▶ indexing/ ──query──▶ retrieval
```

| Stage | Directory | What |
|-------|-----------|------|
| **Sources** | [`sources/`](sources/) | Raw documents to index (ADRs, wiki exports, code docs, project-memory) |
| **Chunks** | [`chunks/`](chunks/) | Sources split into retrievable passages (with metadata) |
| **Embeddings** | [`embeddings/`](embeddings/) | Vector embeddings of chunks |
| **Indexing** | [`indexing/`](indexing/indexing-plan.md) | What to index + reindex triggers |

## Storage

- Vectors and chunk metadata live in **PostgreSQL with `pgvector`**
  (see [ADR-001](../architecture/adr/ADR-001-postgresql.md)). The table DDL is in
  the product's `db/init.sql` (TODO: link once the schema exists) — not in this
  workspace.
- The `rag-mcp` server exposes `search` / `retrieveContext` over it.

## What gets indexed

See [indexing/indexing-plan.md](indexing/indexing-plan.md). Primary sources:
ADRs, constraints/standards, project-memory, wiki, code docs.

## Status / scope

- **MVP-3** — not yet built. The directories here are placeholders for the
  pipeline outputs.
- Confidence/citations: retrieval results must carry their source + confidence,
  consistent with the [output contract](../CLAUDE.md).

> TODO: wire the embedding model choice, chunking strategy, and the pgvector DDL link.
