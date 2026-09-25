package com.wolper.aip.twin.domain;

import com.wolper.aip.mcp.common.Confidence;
import com.wolper.aip.mcp.common.McpStatus;

/**
 * A single downstream MCP server's state slice, normalized from the
 * {@code McpResponse} envelope returned by its {@code /api/<svc>/state}
 * endpoint. When a downstream is unreachable the slice is marked
 * {@link McpStatus#DATA_STALE} with an explanatory {@code message} rather than
 * propagating an exception (RESILIENCE requirement).
 *
 * @param status     normalized status of the downstream call
 * @param source     provenance label (downstream {@code source}, or the
 *                   orchestrator label when the call failed)
 * @param confidence confidence reported by the downstream (or LOW on failure)
 * @param data       the raw state payload (downstream {@code data}); may be null
 * @param message    optional note (staleness / error reason)
 */
public record DownstreamSlice(
        McpStatus status,
        String source,
        Confidence confidence,
        Object data,
        String message) {

    public boolean isOk() {
        return status == McpStatus.OK;
    }

    public boolean isStale() {
        return status == McpStatus.DATA_STALE;
    }

    /**
     * A stale slice produced when a downstream is unreachable.
     *
     * <p>There used to be a second, byte-identical factory called
     * {@code planned()}, for downstreams that did not exist yet. Every server
     * now ships, nothing called it, and two names for one outcome invite the
     * question "which do I use here?" — so there is one.
     */
    public static DownstreamSlice unreachable(String source, String message) {
        return new DownstreamSlice(McpStatus.DATA_STALE, source, Confidence.LOW, null, message);
    }
}
