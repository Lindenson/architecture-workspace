package eu.transplat.aip.jqa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: the application context loads with security disabled and blank /
 * placeholder Neo4j credentials, without any Neo4j running. The {@code Driver}
 * bean is created but never connects (RESILIENCE requirement), proving the
 * server starts with placeholder config.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "jqassistant.neo4j.uri=bolt://localhost:7687",
        "jqassistant.neo4j.user=neo4j",
        "jqassistant.neo4j.password=",
        "jqassistant.scan-dir=",
        "jqassistant.cli="
})
class JqaMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
