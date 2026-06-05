package eu.transplat.aip.mcp.common;

/**
 * Status of an MCP tool invocation. Mirrors AGENT_RUNTIME error handling:
 * a tool that cannot refresh its source returns {@link #DATA_STALE} so the
 * agent knows to lower its confidence.
 */
public enum McpStatus {
    OK,
    ERROR,
    DATA_STALE
}
