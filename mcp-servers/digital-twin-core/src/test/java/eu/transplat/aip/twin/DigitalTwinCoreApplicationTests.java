package eu.transplat.aip.twin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: the orchestrator context loads with security disabled and the
 * default downstream URLs, and without touching the network — proving the
 * server starts even when no downstream MCP server is running (RESILIENCE
 * requirement; fan-out happens lazily per request, not at startup).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false"
})
class DigitalTwinCoreApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
