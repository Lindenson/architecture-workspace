package eu.transplat.aip.wiki;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the Spring context (including the MCP server autoconfig and the
 * {@code ToolCallbackProvider}) loads with security disabled and blank Wiki
 * credentials — i.e. without any network access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "wiki.enabled=true",
        "wiki.base-url=",
        "wiki.email=",
        "wiki.api-token=",
        "wiki.space-keys=",
        "wiki.write-enabled=false"
})
class WikiMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
