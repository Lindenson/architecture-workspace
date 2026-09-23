---
name: knowledge-reindex
description: Refresh the retrieval layer so the next session starts with the decisions just made already in context. Reindexes project-memory, specs and architecture docs into pgvector via rag-mcp. Trigger after /decision-capture, after a batch of ADR or memory writes, on "reindex the knowledge base", "обнови индекс", or when the session digest reports that retrieval degraded to newest-first.
---

# Knowledge Reindex — join point J4

> **Loop B → Loop A bridge (join point J3).** Runs after `/speckit-converge`.
> Reads the feature's artifacts; writes only `project-memory/` (append),
> `quality/technical-debt/`, `reports/`, and drafts. Never edits product code, a
> numbered ADR, a constraint or the C4 model.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
`decision-capture` writes the decision; this makes it *findable*. Without a
reindex, J4 degrades to "newest five entries by filename" — a retrieval mode
with no relevance at all.

Optional by design: the knowledge layer is off unless `KNOWLEDGE_ENABLED=true`.
When it is off, say so and stop — never fake a semantic index.

## Inputs / Sources
`rag-mcp` — `indexPath`, `reindexAll`, `updateEmbeddings`, `getState`.
Worth indexing: `project-memory/` · `specs/*/` (spec, impact, decisions,
critique) · `architecture/adr/` · `architecture/constraints/` · `domain/model/`
· `knowledge/`.

## Procedure
1. `rag-mcp.getState` — if `DISABLED` or unreachable, report and stop. A degraded
   retrieval layer reported honestly is fine; one reported as working is the
   failure this platform exists to prevent.
2. Prefer `indexPath` on what changed over `reindexAll` — a full reindex on every
   feature makes the hook slow enough that people switch it off.
3. **Verify with a probe**: search a phrase from the entry you just indexed and
   confirm it ranks. An index that accepts a write and returns nothing is worse
   than no index, because it looks like it worked.
4. Report counts: files, chunks, embedding provider.

## Output (contract)
```
Indexed: <paths> · <N> files → <M> chunks
Embedding provider: local | openai | none
Probe: "<phrase>" → rank <n> | NOT FOUND
State after: ENABLED | DISABLED · Sources · Confidence
```

## Guardrails
- Never index `.env`, `config/*.config.yml`, or `**/secrets/**`. A retrieval
  layer that swallowed a token hands it back on any query.
- Never call it semantic retrieval when the provider is `none` (Postgres
  full-text only). Name the active mode.
