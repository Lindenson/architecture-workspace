package com.wolper.aip.rag.repository;

import com.wolper.aip.rag.config.RagProperties;
import com.wolper.aip.rag.domain.Chunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VectorStore interpolates one value into SQL that cannot be bound — the table
 * name — and hands pgvector a hand-built text literal. Both are the kind of code
 * that is correct until someone changes a config value, so both are asserted.
 *
 * <p>No Postgres is needed: every check here happens before a statement is sent.
 */
class VectorStoreTest {

    private static RagProperties props(String table, int dimension) {
        RagProperties p = new RagProperties();
        p.getEmbeddings().setTable(table);
        p.getEmbeddings().setDimension(dimension);
        return p;
    }

    private static VectorStore store(String table) {
        return new VectorStore(new JdbcTemplate(), props(table, 384));
    }

    // ------------------------------------------------- identifier validation

    @Test
    @DisplayName("a plain identifier is accepted")
    void valid_table_name_accepted() {
        assertDoesNotThrow(() -> store("rag_chunks"));
        assertDoesNotThrow(() -> store("_private_v2"));
    }

    @Test
    @DisplayName("anything that is not a plain identifier is rejected at construction")
    void injection_attempts_rejected() {
        String[] hostile = {
                "rag_chunks; DROP TABLE users--",
                "rag_chunks WHERE 1=1",
                "public.rag_chunks",          // qualified names are not supported
                "rag-chunks",                 // hyphen is not an identifier char
                "\"quoted\"",
                "",
                " ",
                "1_starts_with_digit",
        };
        for (String name : hostile) {
            assertThrows(IllegalArgumentException.class, () -> store(name),
                    "must reject: " + name);
        }
        assertThrows(IllegalArgumentException.class, () -> store(null));
    }

    @Test
    @DisplayName("rejection happens at construction, not at query time")
    void rejection_is_at_construction() {
        // Failing early means a bad config cannot sit dormant until the first
        // search, hours after startup, in front of a user.
        assertThrows(IllegalArgumentException.class,
                () -> new VectorStore(new JdbcTemplate(), props("bad name", 384)));
    }

    // ------------------------------------------------------ pgvector literal

    private static String literal(float[] v) throws Exception {
        Method m = VectorStore.class.getDeclaredMethod("toVectorLiteral", float[].class);
        m.setAccessible(true);
        return (String) m.invoke(null, (Object) v);
    }

    @Test
    @DisplayName("the vector literal is the bracketed, comma-separated form pgvector expects")
    void vector_literal_format() throws Exception {
        assertEquals("[0.1,-0.5,2.0]", literal(new float[]{0.1f, -0.5f, 2f}));
        assertEquals("[]", literal(new float[]{}));
    }

    @Test
    @DisplayName("a null vector becomes a null embedding, not the string \"null\"")
    void null_vector_is_null() throws Exception {
        // The documented 'none' embedding provider stores rows without vectors;
        // they stay full-text searchable. Writing the text "null" would poison them.
        assertNull(literal(null));
    }

    // ------------------------------------------------------------- upsert guards

    @Test
    @DisplayName("an embedding count that disagrees with the chunk count is caught before any SQL")
    void mismatched_vector_count_rejected() {
        VectorStore vs = store("rag_chunks");
        List<Chunk> chunks = List.of(
                new Chunk("knowledge", "a.md", 0, "first"),
                new Chunk("knowledge", "a.md", 1, "second"));

        VectorStoreException e = assertThrows(VectorStoreException.class,
                () -> vs.upsertChunks(chunks, List.of(new float[]{1f})));
        assertTrue(e.getMessage().contains("does not match"),
                "the message must name the mismatch, not just fail");
    }

    @Test
    @DisplayName("an empty or null chunk list is a no-op, not an error")
    void empty_upsert_is_noop() {
        VectorStore vs = store("rag_chunks");
        assertDoesNotThrow(() -> vs.upsertChunks(List.of(), null));
        assertDoesNotThrow(() -> vs.upsertChunks(null, null));
    }

    // ------------------------------------------------------------- liveness

    @Test
    @DisplayName("isReachable never throws, and answers false when there is no database")
    void is_reachable_never_throws() {
        // A JdbcTemplate with no DataSource is exactly the "database is not
        // there" case. Callers use isReachable to choose between a normal and a
        // DATA_STALE answer, so an exception here would defeat the point of
        // asking — they would have to wrap the liveness check in a try/catch,
        // and some caller eventually would not.
        VectorStore vs = store("rag_chunks");

        assertDoesNotThrow(vs::isReachable);
        assertFalse(vs.isReachable(), "no DataSource means not reachable");
    }

    @Test
    @DisplayName("VectorStoreException is unchecked, so RagService can catch it for DATA_STALE")
    void exception_type_is_runtime() {
        assertTrue(RuntimeException.class.isAssignableFrom(VectorStoreException.class));
    }
}
