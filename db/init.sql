-- ============================================================================
--  Architecture Intelligence Platform — PostgreSQL schema bootstrap
-- ----------------------------------------------------------------------------
--  Database: architecture_ai (created by the postgres container from POSTGRES_DB)
--  Backs:    rag-mcp (MVP-3, pgvector) + the digital-twin knowledge store.
--
--  This script is mounted into /docker-entrypoint-initdb.d/ and runs ONCE on a
--  fresh volume. It is written to be fully idempotent (IF NOT EXISTS) so it can
--  also be re-applied by hand against an existing database:
--      psql "$DATABASE_URL" -f db/init.sql
-- ============================================================================

-- pgvector: vector(N) column type + ANN indexes for RAG embeddings.
CREATE EXTENSION IF NOT EXISTS vector;

-- ----------------------------------------------------------------------------
--  RAG corpus: documents -> chunks -> embeddings
-- ----------------------------------------------------------------------------

-- A source document ingested into the RAG layer (ADR, wiki page, code file, …).
CREATE TABLE IF NOT EXISTS documents (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source      TEXT        NOT NULL,            -- origin system: git | jira | wiki | adr | sonar | manual
    uri         TEXT,                            -- canonical locator (URL, file path, issue key)
    title       TEXT,
    content     TEXT,                            -- full raw text (pre-chunking)
    metadata    JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE  documents IS 'RAG source documents ingested from enterprise tools.';
COMMENT ON COLUMN documents.source   IS 'Origin system: git | jira | wiki | adr | sonar | manual.';
COMMENT ON COLUMN documents.metadata IS 'Free-form provenance: commit sha, author, labels, confidence, …';

-- Avoid re-ingesting the same logical document twice from the same source.
CREATE UNIQUE INDEX IF NOT EXISTS ux_documents_source_uri
    ON documents (source, uri)
    WHERE uri IS NOT NULL;

-- A chunk of a document (the unit that gets embedded and retrieved).
CREATE TABLE IF NOT EXISTS chunks (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  BIGINT  NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    ordinal      INTEGER NOT NULL,               -- 0-based position within the document
    content      TEXT    NOT NULL,
    token_count  INTEGER,
    UNIQUE (document_id, ordinal)
);
COMMENT ON TABLE chunks IS 'Chunked slices of documents; the retrieval unit for RAG.';

CREATE INDEX IF NOT EXISTS ix_chunks_document_id ON chunks (document_id);

-- Vector embeddings for a chunk. A chunk may have embeddings from >1 model.
CREATE TABLE IF NOT EXISTS embeddings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chunk_id    BIGINT       NOT NULL REFERENCES chunks (id) ON DELETE CASCADE,
    embedding   vector(1536) NOT NULL,           -- 1536 dims (e.g. text-embedding-3-small)
    model       TEXT         NOT NULL,           -- embedding model id, for reproducibility
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (chunk_id, model)
);
COMMENT ON TABLE embeddings IS 'pgvector embeddings per chunk per model; powers similarity search.';

-- Approximate-nearest-neighbour index for cosine similarity search.
-- ivfflat needs ANALYZE/data to pick list count well; 100 lists is a sane default.
CREATE INDEX IF NOT EXISTS ix_embeddings_vector
    ON embeddings USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- ----------------------------------------------------------------------------
--  Governance knowledge store (digital twin)
-- ----------------------------------------------------------------------------

-- Architecture Decision Records, mirrored from architecture/adr/ for querying.
CREATE TABLE IF NOT EXISTS architecture_decisions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    adr_number  INTEGER     NOT NULL,
    title       TEXT        NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'PROPOSED',  -- PROPOSED|ACCEPTED|SUPERSEDED|REJECTED|DEPRECATED
    decided_on  DATE,
    body        TEXT,
    related     JSONB       NOT NULL DEFAULT '{}'::jsonb, -- {adr:[], jira:[], components:[]}
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE architecture_decisions IS 'ADRs mirrored from architecture/adr/ (L2 source of truth).';

CREATE UNIQUE INDEX IF NOT EXISTS ux_adr_number ON architecture_decisions (adr_number);
CREATE INDEX IF NOT EXISTS ix_adr_status ON architecture_decisions (status);

-- Technical debt items, primarily synthesized from sonar-mcp DEBT_STATE.
CREATE TABLE IF NOT EXISTS technical_debts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         TEXT        NOT NULL,           -- stable debt id (e.g. TD-014 / sonar rule key)
    source       TEXT        NOT NULL,           -- sonar | archunit | jqassistant | manual
    description  TEXT,
    impact       TEXT,                           -- narrative impact
    fix_cost     TEXT,                           -- effort estimate (e.g. "2d", sonar effort)
    priority     TEXT,                           -- LOW | MEDIUM | HIGH | CRITICAL
    related_adr  INTEGER,                        -- adr_number cross-ref (soft link)
    related_jira TEXT,                           -- Jira issue key
    status       TEXT        NOT NULL DEFAULT 'OPEN',  -- OPEN | ACCEPTED | IN_PROGRESS | RESOLVED | WONT_FIX
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE technical_debts IS 'Tech-debt register; synthesized mainly from sonar-mcp + archunit.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_technical_debts_code ON technical_debts (code);
CREATE INDEX IF NOT EXISTS ix_technical_debts_status   ON technical_debts (status);
CREATE INDEX IF NOT EXISTS ix_technical_debts_priority ON technical_debts (priority);

-- Project memory: the "why" journal (Date·Author·Context·Reason·Result·Consequences).
CREATE TABLE IF NOT EXISTS project_memory (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entry_date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    author        TEXT,
    context       TEXT,
    reason        TEXT,
    result        TEXT,
    consequences  TEXT,
    related_adr   INTEGER,                        -- adr_number cross-ref
    related_jira  TEXT,                           -- Jira issue key
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE project_memory IS 'Decision journal mirroring project-memory/: why decisions were made (and rejected).';

CREATE INDEX IF NOT EXISTS ix_project_memory_entry_date ON project_memory (entry_date);
