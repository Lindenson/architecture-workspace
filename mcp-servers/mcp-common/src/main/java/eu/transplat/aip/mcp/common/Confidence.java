package eu.transplat.aip.mcp.common;

/**
 * Confidence model shared across the platform (see OPERATING_MODEL / AGENT_RUNTIME).
 * <ul>
 *   <li>{@link #HIGH} — confirmed by code and several sources.</li>
 *   <li>{@link #MEDIUM} — partially confirmed.</li>
 *   <li>{@link #LOW} — stale or incomplete; requires manual verification.</li>
 * </ul>
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}
