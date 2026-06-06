package eu.transplat.aip.jqa.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the Neo4j {@link Driver} bean.
 *
 * <p>RESILIENCE: {@link GraphDatabase#driver} does <em>not</em> open a connection;
 * it merely allocates the driver. We deliberately never call
 * {@code verifyConnectivity()} here or in a {@code @PostConstruct}, so the server
 * boots even when Neo4j is down or the credentials are placeholders. Connection
 * failures surface only when a tool actually runs a query, where they are caught
 * and converted into an {@code ERROR}/{@code DATA_STALE} response.
 */
@Configuration
public class Neo4jConfig {

    @Bean(destroyMethod = "close")
    public Driver driver(JqaProperties p) {
        JqaProperties.Neo4j n = p.getNeo4j();
        return GraphDatabase.driver(
                n.getUri(),
                AuthTokens.basic(n.getUser(), n.getPassword() == null ? "" : n.getPassword()));
    }
}
