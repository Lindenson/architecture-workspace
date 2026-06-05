package eu.transplat.aip.mcp.common;

import java.time.Instant;

/**
 * Canonical MCP response. Every tool returns this shape so the agent always
 * receives data together with its provenance ({@code source}) and a
 * {@link Confidence} level — the Output Contract from MCP_ORCHESTRATION_MAP.
 *
 * @param data       the payload (any serializable object)
 * @param status     OK / ERROR / DATA_STALE
 * @param source     human-readable source identifier (e.g. "jira-mcp:JIRA REST v3")
 * @param confidence HIGH / MEDIUM / LOW
 * @param message    optional note (error detail, staleness reason, …)
 * @param producedAt server-side timestamp
 */
public record McpResponse(
        Object data,
        McpStatus status,
        String source,
        Confidence confidence,
        String message,
        Instant producedAt) {

    public static McpResponse ok(Object data, String source, Confidence confidence) {
        return new McpResponse(data, McpStatus.OK, source, confidence, null, Instant.now());
    }

    public static McpResponse ok(Object data, String source) {
        return ok(data, source, Confidence.HIGH);
    }

    public static McpResponse stale(Object data, String source, String reason) {
        return new McpResponse(data, McpStatus.DATA_STALE, source, Confidence.LOW, reason, Instant.now());
    }

    public static McpResponse error(String source, String message) {
        return new McpResponse(null, McpStatus.ERROR, source, Confidence.LOW, message, Instant.now());
    }
}
