package com.wolper.aip.jira.service;

import com.wolper.aip.jira.client.JiraClient;
import com.wolper.aip.jira.config.JiraProperties;
import com.wolper.aip.mcp.common.McpResponse;
import com.wolper.aip.mcp.common.McpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jira.write-enabled is the switch that decides whether an agent can create and
 * modify tickets in a real tracker. It defaults to false, and nothing verified
 * that the default actually held.
 *
 * <p>The fake client throws on every call, so any write that slips past the gate
 * fails loudly rather than silently passing the test.
 */
class JiraWriteGateTest {

    /** Any call that reaches this client means the gate did not hold. */
    private static final class ExplodingClient extends JiraClient {
        ExplodingClient() {
            super(new JiraProperties());
        }

        @Override
        public String createIssue(String projectKey, String issueType, String summary, String description) {
            throw new AssertionError("write reached the Jira client while writes were disabled");
        }
    }

    private static JiraService service(boolean writeEnabled) {
        JiraProperties p = new JiraProperties();
        p.setWriteEnabled(writeEnabled);
        return new JiraService(new ExplodingClient(), p);
    }

    @Test
    @DisplayName("writes are disabled by default")
    void default_is_read_only() {
        assertFalse(new JiraProperties().isWriteEnabled(),
                "a tracker an agent can write to must be opt-in, never opt-out");
    }

    @Test
    @DisplayName("createIssue is blocked when writes are disabled")
    void create_blocked() {
        McpResponse r = service(false).createIssue("KIME", "Bug", "summary", "description");

        assertEquals(McpStatus.ERROR, r.status());
        assertTrue(r.message().contains("write-enabled"),
                "the refusal must name the switch, so the human knows how to allow it deliberately");
    }

    @Test
    @DisplayName("updateIssue is blocked when writes are disabled")
    void update_blocked() {
        assertEquals(McpStatus.ERROR, service(false).updateIssue("KIME-1", "{}").status());
    }

    @Test
    @DisplayName("addComment is blocked when writes are disabled")
    void comment_blocked() {
        assertEquals(McpStatus.ERROR, service(false).addComment("KIME-1", "hello").status());
    }

    @Test
    @DisplayName("transitionIssue is blocked when writes are disabled")
    void transition_blocked() {
        assertEquals(McpStatus.ERROR, service(false).transitionIssue("KIME-1", "31").status());
    }

    @Test
    @DisplayName("the refusal is an ERROR answer, never an exception")
    void refusal_is_an_answer() {
        // The agent must be able to read the refusal and explain it, not crash on it.
        McpResponse r = assertDoesNotThrow(
                () -> service(false).createIssue("KIME", "Bug", "s", "d"));
        assertNotNull(r.source());
        assertNotNull(r.message());
    }
}
