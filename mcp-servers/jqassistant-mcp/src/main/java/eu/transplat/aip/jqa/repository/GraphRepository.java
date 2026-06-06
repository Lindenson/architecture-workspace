package eu.transplat.aip.jqa.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Thin read-only wrapper around the Neo4j {@link Driver}. Runs Cypher inside a
 * managed read transaction and returns each record as a plain {@code Map}, so the
 * service layer can shape it into responses.
 *
 * <p>This class does not swallow errors: any driver/connection failure propagates
 * to the caller, which converts it into an {@code ERROR}/{@code DATA_STALE}
 * {@link eu.transplat.aip.mcp.common.McpResponse}. The session is always closed
 * via try-with-resources.
 */
@Repository
public class GraphRepository {

    private final Driver driver;

    public GraphRepository(Driver driver) {
        this.driver = driver;
    }

    /**
     * Execute a read-only Cypher query and materialize the result.
     *
     * @param cypher the (already write-guarded) Cypher statement
     * @param params named parameters; may be empty
     * @return one map per record, each key the returned column name
     */
    public List<Map<String, Object>> read(String cypher, Map<String, Object> params) {
        Map<String, Object> safeParams = params == null ? Map.of() : params;
        try (Session session = driver.session()) {
            return session.executeRead(tx ->
                    tx.run(cypher, safeParams).list(r -> r.asMap()));
        }
    }
}
