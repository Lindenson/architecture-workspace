package eu.transplat.aip.twin.domain;

import eu.transplat.aip.mcp.common.McpStatus;

/**
 * One named sub-state of the {@link DigitalTwinModel}, wrapping a single
 * {@link DownstreamSlice}. Keeps the downstream {@code data}, {@code status} and
 * {@code source} together so every slice of the merged twin carries its own
 * provenance and freshness.
 *
 * @param name   logical slice name (e.g. "delivery", "quality")
 * @param status normalized status of the underlying downstream call
 * @param source provenance label of the underlying downstream
 * @param data   the downstream state payload (may be null when stale)
 */
public record SubState(
        String name,
        McpStatus status,
        String source,
        Object data) {

    public static SubState from(String name, DownstreamSlice slice) {
        return new SubState(name, slice.status(), slice.source(), slice.data());
    }

    public boolean isStale() {
        return status != McpStatus.OK;
    }
}
