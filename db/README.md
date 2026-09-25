# db/

`init.sql` runs once, on a fresh Postgres volume, and installs **pgvector**.
Nothing else.

## Where the data actually lives

| Thing | Where | Why not Postgres |
|---|---|---|
| RAG chunks + embeddings | table created by `VectorStore` at runtime | the `vector(N)` column depends on the configured embedding dimension, so the schema is not knowable before configuration |
| ADRs | `architecture/adr/*.md` | reviewed in pull requests; a table row cannot be reviewed |
| Technical debt | `quality/technical-debt/*.md` | same — and it carries an argument, not just a status |
| Project memory | `project-memory/*.md` | append-only history that humans read and `rag-mcp` indexes |

## How it runs

`docker-compose.yml` mounts this file into
`/docker-entrypoint-initdb.d/init.sql`, where Postgres runs it **once**, when
the data volume is first created. It is idempotent, so it can also be applied by
hand:

```bash
psql "postgresql://$POSTGRES_USER:$POSTGRES_PASSWORD@localhost:5432/$POSTGRES_DB" -f db/init.sql
```

To re-run the bootstrap from scratch — dropping all RAG data with it:

```bash
docker compose down -v          # removes the volume
docker compose up -d postgres
```

## If you are upgrading

Earlier versions of `init.sql` created six tables that no code ever touched:
`documents`, `chunks`, `embeddings`, `architecture_decisions`,
`technical_debts`, `project_memory`. They are harmless, but they are noise and
they imply a storage model this platform does not use. To drop them:

```sql
DROP TABLE IF EXISTS embeddings, chunks, documents,
                     architecture_decisions, technical_debts, project_memory;
```

Check first that nothing of yours depends on them — this repository does not.

Note that the old schema declared `embeddings.embedding` as `vector(1536)`,
sized for an OpenAI model, while the shipped default is the local ONNX provider
at 384 dimensions. Nothing read the table, so the mismatch never surfaced. The
live store takes its dimension from `rag.embeddings.dimension`, and
`VectorStore.storedDimension()` reads it back from the catalog specifically so a
disagreement between config and storage is reported rather than discovered.

## Rule

Add a table here only together with the code that reads it, in the same commit.
Schema nothing reads is drift, and drift is what this platform exists to detect.
