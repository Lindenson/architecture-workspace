package com.wolper.aip.jqa.service;

import com.wolper.aip.jqa.config.JqaProperties;
import com.wolper.aip.jqa.repository.GraphRepository;
import com.wolper.aip.mcp.common.McpResponse;
import com.wolper.aip.mcp.common.McpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * queryGraph exposes arbitrary Cypher to an LLM. The read-only guard is the only
 * thing preventing an agent from mutating or deleting the architecture graph
 * because a prompt talked it into doing so.
 *
 * <p>No Neo4j is needed: a recording fake stands in for the repository, which
 * also proves the guard runs BEFORE any database call.
 */
class JqaServiceGuardTest {

    /** Records what actually reached the database, if anything. */
    private static final class RecordingRepo extends GraphRepository {
        String lastCypher;
        Map<String, Object> lastParams;

        RecordingRepo() {
            super(null);
        }

        @Override
        public List<Map<String, Object>> read(String cypher, Map<String, Object> params) {
            this.lastCypher = cypher;
            this.lastParams = params;
            return List.of(Map.of("n", 1));
        }
    }

    private static JqaService service(RecordingRepo repo) {
        return new JqaService(repo, new JqaProperties());
    }

    // ------------------------------------------------------------ write guard

    @Test
    @DisplayName("every write keyword is rejected before touching the database")
    void write_statements_are_rejected() {
        String[] writes = {
                "CREATE (n:Type {fqn:'x'})",
                "MATCH (n) DELETE n",
                "MERGE (n:Type {fqn:'x'})",
                "MATCH (n) SET n.fqn = 'x'",
                "MATCH (n) REMOVE n.fqn",
                "DROP INDEX ix",
                "LOAD CSV FROM 'file:///x.csv' AS row RETURN row",
        };
        for (String cypher : writes) {
            RecordingRepo repo = new RecordingRepo();
            McpResponse r = service(repo).queryGraph(cypher, 10, Map.of());

            assertEquals(McpStatus.ERROR, r.status(), "should reject: " + cypher);
            assertNull(repo.lastCypher, "the guard must run before the database: " + cypher);
        }
    }

    @Test
    @DisplayName("the guard is case-insensitive — 'create' is not a loophole")
    void write_guard_is_case_insensitive() {
        RecordingRepo repo = new RecordingRepo();
        McpResponse r = service(repo).queryGraph("create (n:Type)", 10, Map.of());

        assertEquals(McpStatus.ERROR, r.status());
        assertNull(repo.lastCypher);
    }

    @Test
    @DisplayName("a legitimate read reaches the database and returns OK")
    void read_statements_pass() {
        RecordingRepo repo = new RecordingRepo();
        McpResponse r = service(repo).queryGraph("MATCH (n:Type) RETURN n", 10, Map.of());

        assertEquals(McpStatus.OK, r.status());
        assertNotNull(repo.lastCypher, "a read must actually be executed");
        assertEquals("jqassistant-mcp", r.source());
    }

    // ------------------------------------------------------------ input guards

    @Test
    @DisplayName("blank Cypher is rejected rather than sent")
    void blank_cypher_rejected() {
        RecordingRepo repo = new RecordingRepo();

        assertEquals(McpStatus.ERROR, service(repo).queryGraph("   ", 10, Map.of()).status());
        assertEquals(McpStatus.ERROR, service(repo).queryGraph(null, 10, Map.of()).status());
        assertNull(repo.lastCypher);
    }

    // ------------------------------------------------------------- parameters

    @Test
    @DisplayName("named parameters reach the driver — blast-radius queries depend on it")
    void params_are_passed_through() {
        RecordingRepo repo = new RecordingRepo();
        Map<String, Object> params = Map.of("seeds", List.of("com.acme.Order"));

        service(repo).queryGraph("MATCH (n) WHERE n.fqn IN $seeds RETURN n", 10, params);

        assertEquals(List.of("com.acme.Order"), repo.lastParams.get("seeds"));
    }

    @Test
    @DisplayName("null parameters are accepted as an empty map, not an NPE")
    void null_params_are_tolerated() {
        RecordingRepo repo = new RecordingRepo();
        McpResponse r = service(repo).queryGraph("MATCH (n) RETURN n", 10, null);

        assertEquals(McpStatus.OK, r.status());
        assertNotNull(repo.lastParams);
        assertTrue(repo.lastParams.isEmpty());
    }

    @Test
    @DisplayName("the caller's parameter map is copied, not aliased")
    void params_are_defensively_copied() {
        RecordingRepo repo = new RecordingRepo();
        java.util.Map<String, Object> mutable = new java.util.HashMap<>();
        mutable.put("seeds", List.of("a.B"));

        service(repo).queryGraph("MATCH (n) RETURN n", 10, mutable);
        mutable.put("injected", "after the call");

        assertFalse(repo.lastParams.containsKey("injected"),
                "a caller must not be able to change a query's parameters after it was issued");
    }

    // ------------------------------------------------------------- degradation

    @Test
    @DisplayName("a dead graph degrades instead of throwing")
    void unreachable_graph_degrades() {
        GraphRepository dead = new GraphRepository(null) {
            @Override
            public List<Map<String, Object>> read(String cypher, Map<String, Object> params) {
                throw new IllegalStateException("Connection refused");
            }
        };

        McpResponse r = new JqaService(dead, new JqaProperties())
                .queryGraph("MATCH (n) RETURN n", 10, Map.of());

        assertNotNull(r, "the tool must answer, never propagate the failure to the agent");
        assertNotEquals(McpStatus.OK, r.status());
        assertEquals("jqassistant-mcp", r.source(),
                "even a failure is attributable — the agent reports which source is down");
    }
}
