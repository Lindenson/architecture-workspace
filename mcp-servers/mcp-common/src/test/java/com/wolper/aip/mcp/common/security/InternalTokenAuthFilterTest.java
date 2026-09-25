package com.wolper.aip.mcp.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The only thing between a process on the same host and eight servers that can
 * read Jira, GitHub, Sonar and the dependency graph. Previously untested.
 */
class InternalTokenAuthFilterTest {

    private static final String TOKEN = "s3cret-internal-token";

    private static final class RecordingChain implements FilterChain {
        boolean called = false;
        @Override
        public void doFilter(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse s) {
            called = true;
        }
    }

    private static MockHttpServletResponse run(String authHeader, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        new InternalTokenAuthFilter(TOKEN).doFilter(request, response, new RecordingChain());
        return response;
    }

    @Test
    @DisplayName("a correct bearer token reaches the handler")
    void valid_token_passes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jira/issues");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        new InternalTokenAuthFilter(TOKEN).doFilter(request, response, chain);

        assertTrue(chain.called, "a valid token must reach the handler");
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("a missing Authorization header is rejected with 401")
    void missing_header_rejected() throws Exception {
        assertEquals(401, run(null, "/api/jira/issues").getStatus());
    }

    @Test
    @DisplayName("a wrong token is rejected and never reaches the handler")
    void wrong_token_rejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jira/issues");
        request.addHeader("Authorization", "Bearer not-the-token-at-all");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        new InternalTokenAuthFilter(TOKEN).doFilter(request, response, chain);

        assertFalse(chain.called, "a rejected request must not reach the handler");
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("a same-length wrong token is rejected")
    void same_length_wrong_token_rejected() throws Exception {
        // The comparison short-circuits on length, so only a same-length value
        // actually exercises the constant-time loop.
        assertEquals(401, run("Bearer " + "x".repeat(TOKEN.length()), "/api/x").getStatus());
    }

    @Test
    @DisplayName("a prefix of the real token is rejected")
    void prefix_token_rejected() throws Exception {
        assertEquals(401, run("Bearer " + TOKEN.substring(0, TOKEN.length() - 1), "/api/x").getStatus());
    }

    @Test
    @DisplayName("a non-Bearer scheme, or a bare token, is rejected")
    void wrong_scheme_rejected() throws Exception {
        assertEquals(401, run("Basic " + TOKEN, "/api/x").getStatus());
        assertEquals(401, run(TOKEN, "/api/x").getStatus());
    }

    @Test
    @DisplayName("the rejection body is machine-readable, not an HTML page")
    void rejection_body_is_machine_readable() throws Exception {
        MockHttpServletResponse response = run(null, "/api/x");

        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"status\":\"ERROR\""),
                "an agent parses this body; an HTML error page would be unreadable to it");
    }

    @Test
    @DisplayName("the health endpoint stays open for orchestrator probes")
    void health_is_exempt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        new InternalTokenAuthFilter(TOKEN).doFilter(request, response, chain);

        assertTrue(chain.called, "docker compose healthchecks carry no token");
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("a path merely containing /actuator/health is still protected")
    void health_exemption_is_prefix_only() throws Exception {
        assertEquals(401, run(null, "/api/x/actuator/health").getStatus());
    }
}
