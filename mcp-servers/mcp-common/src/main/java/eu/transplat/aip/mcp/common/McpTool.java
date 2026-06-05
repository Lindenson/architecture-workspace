package eu.transplat.aip.mcp.common;

/**
 * Low-level MCP tool contract from the DEPLOYMENT spec. Servers primarily expose
 * tools to the agent via Spring AI {@code @Tool}-annotated service methods; this
 * interface is for command-style dispatch and internal orchestration where a
 * uniform {@code execute(request) -> response} shape is convenient.
 */
public interface McpTool {

    String name();

    McpResponse execute(McpRequest request);
}
