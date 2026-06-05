package eu.transplat.aip.mcp.common;

import java.util.Map;

/**
 * Canonical MCP request envelope (see DEPLOYMENT spec, MCP Protocol Layer).
 *
 * @param command       logical command/tool name
 * @param payload       arbitrary named arguments
 * @param correlationId trace id propagated across the orchestration
 */
public record McpRequest(String command, Map<String, Object> payload, String correlationId) {

    public McpRequest {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static McpRequest of(String command, Map<String, Object> payload) {
        return new McpRequest(command, payload, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T arg(String key) {
        return (T) payload.get(key);
    }

    public String string(String key, String defaultValue) {
        Object v = payload.get(key);
        return v == null ? defaultValue : String.valueOf(v);
    }
}
