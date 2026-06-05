package eu.transplat.aip.github;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the Spring context boots with placeholder (blank) credentials and
 * security disabled — no network access required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "git.provider=github",
        "git.github.token=",
        "git.github.org=",
        "git.gitlab.token=",
        "git.repositories="
})
class GitHubMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
