package eu.transplat.aip.jira;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the Spring context (including the MCP server autoconfig and the
 * {@code ToolCallbackProvider}) loads with security disabled and blank Jira
 * credentials — i.e. without any network access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "jira.base-url=",
        "jira.email=",
        "jira.api-token=",
        "jira.project-keys=",
        "jira.write-enabled=false"
})
class JiraMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
