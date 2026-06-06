package eu.transplat.aip.twin.domain;

import eu.transplat.aip.mcp.common.McpStatus;

/**
 * The merged ARCHITECTURE_STATE for the digital twin, combining the two MVP-2
 * architecture sources into one section, each retaining its own status and
 * provenance:
 *
 * <ul>
 *   <li><b>graph</b> — the jQAssistant ARCHITECTURE_GRAPH summary
 *       ({@code labelCounts}, {@code dependencyCount}, {@code cycleCount},
 *       {@code layeringViolationCount}) from {@code GET /api/jqassistant/state}.</li>
 *   <li><b>model</b> — the Structurizr ARCHITECTURE_MODEL summary
 *       ({@code workspaceName}, {@code systems}, {@code containers},
 *       {@code components}, {@code relationships}, {@code views},
 *       {@code parsedOk}) from {@code GET /api/structurizr/state}.</li>
 * </ul>
 *
 * <p>Architecture sources are NON-CRITICAL for MVP-2: their being stale lowers
 * confidence to MEDIUM and adds to {@code staleSources}, but never forces the
 * overall confidence to LOW (see {@code DigitalTwinService}).
 *
 * @param graphStatus  status of the jQAssistant graph call
 * @param graphSource  provenance of the jQAssistant graph
 * @param graph        the ARCHITECTURE_GRAPH summary payload (may be null when stale)
 * @param modelStatus  status of the Structurizr model call
 * @param modelSource  provenance of the Structurizr model
 * @param model        the ARCHITECTURE_MODEL summary payload (may be null when stale)
 */
public record ArchitectureSlice(
        McpStatus graphStatus,
        String graphSource,
        Object graph,
        McpStatus modelStatus,
        String modelSource,
        Object model) {

    public static ArchitectureSlice from(DownstreamSlice graph, DownstreamSlice model) {
        return new ArchitectureSlice(
                graph.status(), graph.source(), graph.data(),
                model.status(), model.source(), model.data());
    }

    /** True when both architecture sources answered OK. */
    public boolean isOk() {
        return graphStatus == McpStatus.OK && modelStatus == McpStatus.OK;
    }
}
