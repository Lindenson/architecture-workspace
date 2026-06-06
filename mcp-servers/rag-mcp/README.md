# rag-mcp — RAG Knowledge MCP Server

An **optional, fully configurable** RAG knowledge layer for the architect agent.
It indexes the workspace docs into **Postgres + pgvector** and answers
**semantic** (vector) and **keyword** (full-text) search, returning the canonical
`McpResponse` envelope (`data`, `status`, `source`, `confidence`).

It runs with **no API keys** (local ONNX embeddings by default) and **degrades
gracefully**: the server boots even when Postgres is down, when the feature is
turned off, or when the embedding provider is `none`. (The local model is fetched
once from GitHub then cached — see "Offline / air-gapped note" below.)

Port: **8088**. Source label: `rag-mcp:pgvector`.

## Embedding providers (three options)

Selected by `rag.embeddings.provider`; exactly one is active.

| Provider | `RAG_EMBEDDINGS_PROVIDER` | What it does |
|----------|---------------------------|--------------|
| **local** (default) | `local` | Offline ONNX `all-MiniLM-L6-v2` via Spring AI `TransformersEmbeddingModel`. 384 dims. No API key, no network. |
| **openai** | `openai` | Hand-rolled OpenAI-compatible client (`POST /embeddings`). Works with OpenAI, Ollama, LM Studio, vLLM — point `RAG_OPENAI_BASE_URL` at the endpoint. |
| **none** | `none` | Vectors off. Chunks are still stored (NULL embedding) and search falls back to **full-text only**. |

### Switching provider (env vars)

```bash
# Local offline (default — nothing to set)
export RAG_EMBEDDINGS_PROVIDER=local

# OpenAI-compatible
export RAG_EMBEDDINGS_PROVIDER=openai
export RAG_OPENAI_BASE_URL=https://api.openai.com/v1     # or http://localhost:11434/v1 for Ollama
export RAG_OPENAI_API_KEY=sk-...                          # for Ollama/LM Studio any non-blank token
export RAG_OPENAI_MODEL=text-embedding-3-small
export RAG_EMBEDDINGS_DIMENSION=1536                      # must match the chosen model

# Full-text only (no vectors)
export RAG_EMBEDDINGS_PROVIDER=none
```

After switching providers between two **vector** models you can recompute vectors
in place with the `updateEmbeddings` tool — but if the new model's dimension
differs from the stored one, the table is fixed-width and you must run
`reindexAll` instead (the tool detects this and tells you).

## Disabling the layer entirely

- Set `RAG_ENABLED=false` — every tool returns `McpResponse.disabled(...)`
  (status `DISABLED`, *not* an error; orchestrators must not count it against
  confidence), or
- simply **don't start this server**.

## Database

This server **owns its own `rag_chunks` table**, created lazily on first use and
**sized to the configured dimension** (`vector(384)` by default). It is **separate
from the reference tables in `db/init.sql`**. It also creates the `vector`
extension, an `ivfflat` cosine index and a GIN full-text index.

**pgvector is required.** The repo's docker-compose Postgres uses the
`pgvector/pgvector` image, which provides it. The DB is **never** contacted at
startup; if it is unreachable, state tools return `DATA_STALE` and search tools
degrade rather than crash.

Connection (from env / `config/*.config.yml`, never hardcoded):

```bash
export RAG_DB_URL=jdbc:postgresql://localhost:5432/architecture_ai
export POSTGRES_USER=aip
export POSTGRES_PASSWORD=...
```

## Configuration (`rag.*`)

| Property | Env | Default |
|----------|-----|---------|
| `rag.enabled` | `RAG_ENABLED` | `true` |
| `rag.top-k` | `RAG_TOP_K` | `6` |
| `rag.embeddings.provider` | `RAG_EMBEDDINGS_PROVIDER` | `local` |
| `rag.embeddings.dimension` | `RAG_EMBEDDINGS_DIMENSION` | `384` |
| `rag.embeddings.table` | `RAG_EMBEDDINGS_TABLE` | `rag_chunks` |
| `rag.embeddings.openai.base-url` | `RAG_OPENAI_BASE_URL` | `https://api.openai.com/v1` |
| `rag.embeddings.openai.api-key` | `RAG_OPENAI_API_KEY` | *(blank)* |
| `rag.embeddings.openai.model` | `RAG_OPENAI_MODEL` | `text-embedding-3-small` |
| `rag.sources` | `RAG_SOURCES` | `knowledge,architecture,history,project-memory,domain/model,delivery/roadmap` |
| `rag.chunk.max-chars` | `RAG_CHUNK_MAX_CHARS` | `1200` |
| `rag.chunk.overlap` | `RAG_CHUNK_OVERLAP` | `150` |

Indexed file types: `md, adoc, txt, dsl, yaml, yml, java`.

## Tools (MCP) / endpoints (REST)

| Tool | REST | Description |
|------|------|-------------|
| `indexPath(path)` | `GET /api/rag/index?path=` · `POST /api/rag/index {"path":...}` | Read, chunk, embed and upsert files under a repo-relative path. |
| `reindexAll()` | `POST /api/rag/reindex` | Clear + reindex all configured `rag.sources`. |
| `search(query, topK)` | `GET /api/rag/search?q=&topK=` | Ranked chunks `{source, ref, score, snippet}`; notes vector vs. full-text mode. |
| `retrieveContext(query, topK)` | `GET /api/rag/context?q=&topK=` | Concatenated, citation-tagged context pack + citations for grounding. |
| `updateEmbeddings()` | — | Recompute vectors for stored chunks with the current provider; guards dimension mismatch. |
| `getState()` | `GET /api/rag/state` | KNOWLEDGE/RAG slice: `{enabled, provider, dimension, dbReachable, indexedChunks, sources, lastIndexedAt}`. The digital-twin contract. |

## Run

```bash
java -jar target/rag-mcp.jar
```

(Build with the multi-module Maven reactor under `mcp-servers/`.)

## Offline / air-gapped note (local provider)

With `provider=local`, the `all-MiniLM-L6-v2` ONNX model is **fetched once from
GitHub on first startup** and cached under
`${java.io.tmpdir}/spring-ai-onnx-generative/…`. The first boot needs outbound
network; subsequent boots are fully offline. No API key is ever required.

For a strictly air-gapped host:
- pre-seed the cache (copy `model.onnx` + `tokenizer.json` from a host that has
  fetched them) and set `spring.ai.embedding.transformer.onnx.model-uri` /
  `…tokenizer-resource` to the local files; **or**
- use `provider=none` (Postgres full-text only, zero model); **or**
- use `provider=openai` against an in-network OpenAI-compatible endpoint
  (e.g. local Ollama / LM Studio).

Verified end-to-end: 41 files → 84 chunks indexed; vector search returns ranked,
cited hits against pgvector.
