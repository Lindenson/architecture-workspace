package eu.transplat.aip.structurizr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: the application context loads with security disabled and a
 * workspace path that may not exist, without parsing the DSL at startup. Proves
 * the server boots even when the C4 model file is missing (RESILIENCE
 * requirement — parsing is lazy, per request).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "structurizr.workspace-path=./does-not-exist/workspace.dsl"
})
class StructurizrMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
