package eu.transplat.aip.sonar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: the application context loads with security disabled and blank
 * SonarQube credentials, and without touching the network. Proves the server
 * starts with placeholder config (RESILIENCE requirement).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "sonar.base-url=",
        "sonar.token=",
        "sonar.project-keys="
})
class SonarMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
