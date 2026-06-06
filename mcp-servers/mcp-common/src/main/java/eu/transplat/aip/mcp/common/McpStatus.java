package eu.transplat.aip.mcp.common;

/**
 * Status of an MCP tool invocation. Mirrors AGENT_RUNTIME error handling:
 * a tool that cannot refresh its source returns {@link #DATA_STALE} so the
 * agent knows to lower its confidence.
 */
public enum McpStatus {
    OK,
    ERROR,
    DATA_STALE,
    /**
     * The capability is intentionally turned off for this project (e.g. the RAG
     * knowledge layer is disabled by configuration). This is NOT a failure: the
     * orchestrator must exclude DISABLED slices from confidence scoring and from
     * "stale source" reporting.
     */
    DISABLED
}
