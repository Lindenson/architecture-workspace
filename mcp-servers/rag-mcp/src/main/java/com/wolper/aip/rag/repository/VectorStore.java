package com.wolper.aip.rag.repository;

import com.wolper.aip.rag.config.RagProperties;
import com.wolper.aip.rag.domain.Chunk;
import com.wolper.aip.rag.domain.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Postgres + pgvector store for the RAG corpus.
 *
 * <p>Owns a single, <b>dimension-sized</b> table (name from
 * {@code rag.embeddings.table}, default {@code rag_chunks}) holding the chunk
 * text and its embedding. The {@code vector(N)} column is fixed-width, so a
 * change of embedding provider that changes the dimension requires a rebuild —
 * {@link #storedDimension()} exists so {@code RagService} can detect that and
 * say so instead of failing cryptically.
 *
 * <p><b>Resilience contract.</b> The server must boot and stay up with Postgres
 * unreachable (see {@code application.yml}: management health db is disabled and
 * SQL init is {@code never}). Therefore:
 * <ul>
 *   <li>the schema is created <b>lazily, per request</b> — never at startup;</li>
 *   <li>every SQL failure is translated into {@link VectorStoreException}, which
 *       {@code RagService} reports as {@code DATA_STALE};</li>
 *   <li>{@link #isReachable()} never throws — it answers the question it is
 *       asked and nothing more.</li>
 * </ul>
 *
 * <p><b>Why raw JdbcTemplate.</b> pgvector's {@code vector} type has no JDBC
 * mapping; vectors are passed as their text literal and cast with
 * {@code ?::vector}. An ORM would buy nothing here and hide the cast.
 */
@Repository
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);

    /**
     * A table name is an identifier and cannot be a bind parameter, so it is
     * interpolated — and therefore must be validated. Anything outside this
     * pattern is rejected at construction rather than reaching the database.
     */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

    private final JdbcTemplate jdbc;
    private final String table;
    private final int dimension;

    /** Guards schema creation so it is attempted once per successful run, not per query. */
    private volatile boolean schemaReady = false;

    public VectorStore(JdbcTemplate jdbc, RagProperties properties) {
        this.jdbc = jdbc;
        String configured = properties.getEmbeddings().getTable();
        if (configured == null || !SAFE_IDENTIFIER.matcher(configured).matches()) {
            throw new IllegalArgumentException(
                    "rag.embeddings.table must be a plain SQL identifier, got: " + configured);
        }
        this.table = configured;
        this.dimension = properties.getEmbeddings().getDimension();
    }

    // ---------------------------------------------------------------- schema

    /**
     * Create the table and indexes if absent. Called before every operation;
     * cheap after the first success because of {@link #schemaReady}.
     *
     * <p>{@code CREATE EXTENSION vector} is attempted but not required to
     * succeed: on a managed Postgres the extension is usually pre-installed and
     * the application role may not be allowed to create it. Failing there would
     * make the whole store unusable on a perfectly working database.
     */
    private void ensureSchema() {
        if (schemaReady) {
            return;
        }
        try {
            try {
                jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
            } catch (DataAccessException e) {
                log.debug("CREATE EXTENSION vector not permitted or already present: {}", e.getMessage());
            }
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        source     TEXT        NOT NULL,
                        ref        TEXT        NOT NULL,
                        chunk_no   INTEGER     NOT NULL,
                        content    TEXT        NOT NULL,
                        embedding  vector(%d),
                        indexed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        UNIQUE (source, ref, chunk_no)
                    )""".formatted(table, dimension));
            jdbc.execute("CREATE INDEX IF NOT EXISTS ix_%s_source ON %s (source)".formatted(table, table));
            jdbc.execute("""
                    CREATE INDEX IF NOT EXISTS ix_%s_fts
                        ON %s USING gin (to_tsvector('simple', content))"""
                    .formatted(table, table));
            // ivfflat needs data to choose list count well; it is created here so a
            // cold index still works, and can be rebuilt after a large reindex.
            try {
                jdbc.execute("""
                        CREATE INDEX IF NOT EXISTS ix_%s_vector
                            ON %s USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)"""
                        .formatted(table, table));
            } catch (DataAccessException e) {
                log.debug("ivfflat index not created (sequential scan will be used): {}", e.getMessage());
            }
            schemaReady = true;
        } catch (DataAccessException e) {
            throw new VectorStoreException("RAG schema unavailable: " + rootMessage(e), e);
        }
    }

    // ------------------------------------------------------------- liveness

    /**
     * Whether Postgres answers at all. Never throws: callers use this to decide
     * between a normal answer and a {@code DATA_STALE} one, and an exception
     * here would defeat that.
     */
    public boolean isReachable() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.debug("RAG store unreachable: {}", e.toString());
            return false;
        }
    }

    // ---------------------------------------------------------------- reads

    public long countChunks() {
        ensureSchema();
        try {
            Long n = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
            return n == null ? 0L : n;
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to count chunks: " + rootMessage(e), e);
        }
    }

    public Instant lastIndexedAt() {
        ensureSchema();
        try {
            Timestamp ts = jdbc.queryForObject("SELECT max(indexed_at) FROM " + table, Timestamp.class);
            return ts == null ? null : ts.toInstant();
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to read last indexed timestamp: " + rootMessage(e), e);
        }
    }

    /**
     * The dimension the {@code embedding} column was actually created with, or
     * {@code null} when the table does not exist yet.
     *
     * <p>Read from the catalog rather than from configuration on purpose: the
     * point is to detect the case where config and storage disagree.
     */
    public Integer storedDimension() {
        try {
            List<String> types = jdbc.queryForList("""
                    SELECT format_type(a.atttypid, a.atttypmod)
                      FROM pg_attribute a
                     WHERE a.attrelid = to_regclass(?)
                       AND a.attname  = 'embedding'
                       AND a.attnum > 0
                       AND NOT a.attisdropped""", String.class, table);
            if (types.isEmpty() || types.get(0) == null) {
                return null;
            }
            String t = types.get(0);                       // e.g. "vector(384)"
            int open = t.indexOf('('), close = t.indexOf(')');
            if (open < 0 || close <= open) {
                return null;
            }
            return Integer.parseInt(t.substring(open + 1, close).trim());
        } catch (NumberFormatException e) {
            return null;
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to read stored vector dimension: " + rootMessage(e), e);
        }
    }

    /** Every chunk, for re-embedding. Ordered by id so batching is deterministic. */
    public List<StoredChunk> allChunks() {
        ensureSchema();
        try {
            return jdbc.query(
                    "SELECT id, source, ref, chunk_no, content FROM " + table + " ORDER BY id",
                    (rs, i) -> new StoredChunk(
                            rs.getLong("id"),
                            rs.getString("source"),
                            rs.getString("ref"),
                            rs.getInt("chunk_no"),
                            rs.getString("content")));
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to read chunks: " + rootMessage(e), e);
        }
    }

    // --------------------------------------------------------------- writes

    /** Remove every chunk belonging to a source group, ahead of a reindex. */
    public int deleteBySource(String source) {
        ensureSchema();
        try {
            return jdbc.update("DELETE FROM " + table + " WHERE source = ?", source);
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to delete source '" + source + "': " + rootMessage(e), e);
        }
    }

    /**
     * Insert or replace chunks.
     *
     * @param chunks  the chunks; empty is a no-op
     * @param vectors embeddings aligned with {@code chunks}, or {@code null}
     *                when no vector provider is available — the rows are then
     *                stored with a null embedding and remain full-text
     *                searchable, which is the documented {@code none}-provider
     *                behaviour rather than a failure
     */
    public void upsertChunks(List<Chunk> chunks, List<float[]> vectors) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        ensureSchema();
        if (vectors != null && vectors.size() != chunks.size()) {
            throw new VectorStoreException(
                    "Embedding count " + vectors.size() + " does not match chunk count " + chunks.size());
        }
        String sql = """
                INSERT INTO %s (source, ref, chunk_no, content, embedding, indexed_at)
                VALUES (?, ?, ?, ?, ?::vector, now())
                ON CONFLICT (source, ref, chunk_no) DO UPDATE
                   SET content    = EXCLUDED.content,
                       embedding  = EXCLUDED.embedding,
                       indexed_at = now()""".formatted(table);
        try {
            // BatchPreparedStatementSetter (not the Parameterized variant) because it
            // hands us the row index: pairing a chunk with its vector by
            // chunks.indexOf(chunk) would be O(n^2) and would silently mis-pair
            // whenever two chunks have equal content.
            jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Chunk chunk = chunks.get(i);
                    ps.setString(1, chunk.source());
                    ps.setString(2, chunk.ref());
                    ps.setInt(3, chunk.chunkNo());
                    ps.setString(4, chunk.content());
                    ps.setString(5, vectors == null ? null : toVectorLiteral(vectors.get(i)));
                }

                @Override
                public int getBatchSize() {
                    return chunks.size();
                }
            });
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to upsert " + chunks.size() + " chunk(s): " + rootMessage(e), e);
        }
    }

    /** Replace one chunk's embedding in place. Returns rows affected (0 or 1). */
    public int updateEmbedding(long id, float[] vector) {
        ensureSchema();
        try {
            return jdbc.update(
                    "UPDATE " + table + " SET embedding = ?::vector, indexed_at = now() WHERE id = ?",
                    toVectorLiteral(vector), id);
        } catch (DataAccessException e) {
            throw new VectorStoreException("Failed to update embedding for chunk " + id + ": " + rootMessage(e), e);
        }
    }

    // -------------------------------------------------------------- search

    /**
     * Cosine similarity search. Score is {@code 1 - distance}, so higher is more
     * relevant — matching {@link SearchHit}'s contract. Rows with no embedding
     * are excluded rather than ranked arbitrarily.
     */
    public List<SearchHit> vectorSearch(float[] query, int k) {
        ensureSchema();
        String literal = toVectorLiteral(query);
        try {
            return jdbc.query("""
                    SELECT source, ref, chunk_no, content,
                           1 - (embedding <=> ?::vector) AS score
                      FROM %s
                     WHERE embedding IS NOT NULL
                     ORDER BY embedding <=> ?::vector
                     LIMIT ?""".formatted(table),
                    (rs, i) -> new SearchHit(
                            rs.getString("source"), rs.getString("ref"), rs.getInt("chunk_no"),
                            rs.getDouble("score"), rs.getString("content")),
                    literal, literal, k);
        } catch (DataAccessException e) {
            throw new VectorStoreException("Vector search failed: " + rootMessage(e), e);
        }
    }

    /**
     * Postgres full-text search — the fallback when no embedding provider is
     * available, and the only mode the {@code none} provider ever uses.
     *
     * <p>The {@code simple} configuration is deliberate: the corpus is technical
     * English mixed with identifiers, where stemming does more harm than good.
     */
    public List<SearchHit> fullTextSearch(String query, int k) {
        ensureSchema();
        try {
            return jdbc.query("""
                    SELECT source, ref, chunk_no, content,
                           ts_rank(to_tsvector('simple', content),
                                   plainto_tsquery('simple', ?)) AS score
                      FROM %s
                     WHERE to_tsvector('simple', content) @@ plainto_tsquery('simple', ?)
                     ORDER BY score DESC
                     LIMIT ?""".formatted(table),
                    (rs, i) -> new SearchHit(
                            rs.getString("source"), rs.getString("ref"), rs.getInt("chunk_no"),
                            rs.getDouble("score"), rs.getString("content")),
                    query, query, k);
        } catch (DataAccessException e) {
            throw new VectorStoreException("Full-text search failed: " + rootMessage(e), e);
        }
    }

    // ------------------------------------------------------------ internals

    /** pgvector text literal: {@code [0.1,0.2,0.3]}. */
    private static String toVectorLiteral(float[] v) {
        if (v == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(v.length * 8 + 2).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    /**
     * The deepest cause's message. Spring wraps JDBC exceptions several layers
     * deep, and the outer message ("StatementCallback; bad SQL grammar…") hides
     * the one line an operator needs.
     */
    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String m = cur.getMessage();
        return m == null ? cur.getClass().getSimpleName() : m.trim();
    }
}
