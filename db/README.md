# db — PostgreSQL schema (pgvector)

`init.sql` bootstraps the dedicated **`architecture_ai`** database. It backs two
consumers:

- **rag-mcp (MVP-3)** — the RAG corpus + vector store (`documents` → `chunks` →
  `embeddings`).
- **digital-twin-core knowledge store** — the governance tables
  (`architecture_decisions`, `technical_debts`, `project_memory`).

## How it runs

The script is mounted by `docker-compose.yml` into
`/docker-entrypoint-initdb.d/init.sql` and runs **once** when the postgres data
volume is first created. It is fully idempotent (`CREATE ... IF NOT EXISTS`), so
you can also re-apply it by hand:

```bash
psql "postgresql://$POSTGRES_USER:$POSTGRES_PASSWORD@localhost:5432/$POSTGRES_DB" -f db/init.sql
```

To re-run the auto-bootstrap from scratch:
`docker compose down -v` (drops the volume), then `docker compose up -d postgres`.

## Schema

### RAG corpus

| Table        | Purpose                                                            |
|--------------|--------------------------------------------------------------------|
| `documents`  | Source documents ingested from Git/Jira/Wiki/ADR/Sonar/manual.     |
| `chunks`     | Chunked slices of a document — the retrieval unit (FK → documents).|
| `embeddings` | `vector(1536)` embeddings per chunk per `model` (FK → chunks).     |

- `embeddings.embedding` is `vector(1536)` (sized for `text-embedding-3-small`;
  change the dimension if you switch models — see `.env` `EMBEDDINGS_MODEL`).
- An **ivfflat** index (`vector_cosine_ops`, `lists = 100`) accelerates cosine
  similarity search. Run `ANALYZE embeddings;` after the first bulk load so the
  planner uses the index well.
- `documents (source, uri)` is unique to avoid re-ingesting the same document.

### Governance knowledge store

| Table                    | Purpose                                                   |
|--------------------------|-----------------------------------------------------------|
| `architecture_decisions` | ADRs mirrored from `architecture/adr/` (L2 truth).        |
| `technical_debts`        | Tech-debt register, synthesized mainly from sonar-mcp.    |
| `project_memory`         | Decision journal mirroring `project-memory/` (the "why"). |

Cross-references between tables (`related_adr`, `related_jira`) are intentionally
**soft links** (plain columns / JSONB), not hard FKs — the ADRs and Jira issues
live in external systems of record, and the twin only mirrors them.

## Secrets

Connection credentials come from the repo-root `.env`
(`POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_PORT`). No secret is
stored here. See [`../config/README.md`](../config/README.md).
