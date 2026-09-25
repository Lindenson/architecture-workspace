package com.wolper.aip.mcp.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The envelope contract, asserted rather than assumed.
 *
 * <p>Every claim this platform makes about its own answers lives here: an answer
 * always names its source, always carries a confidence, and a degraded source is
 * reported as {@code DATA_STALE} rather than as success or failure. The agent
 * contract (CLAUDE.md, every SKILL.md) tells the agent to trust these fields, so
 * a silent change to them is a change to everything built on top.
 */
class McpResponseContractTest {

    @Test
    @DisplayName("ok() carries the source and defaults to HIGH confidence")
    void ok_carries_source_and_confidence() {
        McpResponse r = McpResponse.ok("payload", "jqassistant-mcp");

        assertEquals(McpStatus.OK, r.status());
        assertEquals("jqassistant-mcp", r.source());
        assertEquals(Confidence.HIGH, r.confidence());
        assertEquals("payload", r.data());
        assertNull(r.message());
    }

    @Test
    @DisplayName("ok() honours an explicitly lowered confidence")
    void ok_honours_explicit_confidence() {
        // A tool that answered from a partial source must be able to say so
        // without pretending the answer is stale.
        McpResponse r = McpResponse.ok("partial", "structurizr-mcp", Confidence.MEDIUM);

        assertEquals(McpStatus.OK, r.status());
        assertEquals(Confidence.MEDIUM, r.confidence());
    }

    @Test
    @DisplayName("stale() keeps the data, drops confidence to LOW and states a reason")
    void stale_keeps_data_but_lowers_confidence() {
        // The resilience contract: a dead upstream degrades the answer, it does
        // not delete it. Returning null data here would make every caller branch
        // on null instead of on status.
        McpResponse r = McpResponse.stale("last known", "sonar-mcp", "Sonar unreachable");

        assertEquals(McpStatus.DATA_STALE, r.status());
        assertEquals("last known", r.data());
        assertEquals(Confidence.LOW, r.confidence());
        assertEquals("Sonar unreachable", r.message());
        assertEquals("sonar-mcp", r.source());
    }

    @Test
    @DisplayName("error() carries no data, LOW confidence and a message")
    void error_carries_no_data() {
        McpResponse r = McpResponse.error("jira-mcp", "boom");

        assertEquals(McpStatus.ERROR, r.status());
        assertNull(r.data());
        assertEquals(Confidence.LOW, r.confidence());
        assertEquals("boom", r.message());
    }

    @Test
    @DisplayName("disabled() is a confident answer, not a failure")
    void disabled_is_high_confidence() {
        // "The knowledge layer is off" is a FACT, known exactly. Reporting it at
        // LOW confidence would teach the agent to distrust a correct answer and
        // to retry something that is switched off on purpose.
        McpResponse r = McpResponse.disabled("rag-mcp", "KNOWLEDGE_ENABLED=false");

        assertEquals(McpStatus.DISABLED, r.status());
        assertEquals(Confidence.HIGH, r.confidence());
        assertNull(r.data());
    }

    @Test
    @DisplayName("every factory stamps a source and a producedAt")
    void every_response_is_attributable() {
        Instant before = Instant.now().minusSeconds(1);
        McpResponse[] all = {
                McpResponse.ok("d", "s"),
                McpResponse.ok("d", "s", Confidence.LOW),
                McpResponse.stale("d", "s", "why"),
                McpResponse.error("s", "why"),
                McpResponse.disabled("s", "why"),
        };
        for (McpResponse r : all) {
            assertNotNull(r.source(), "an unattributable answer is not usable as evidence");
            assertNotNull(r.confidence(), "confidence is never optional");
            assertNotNull(r.status());
            assertNotNull(r.producedAt());
            assertTrue(r.producedAt().isAfter(before), "producedAt must be the real production time");
        }
    }
}
