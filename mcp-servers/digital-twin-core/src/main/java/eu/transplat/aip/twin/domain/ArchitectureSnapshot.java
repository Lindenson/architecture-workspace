package eu.transplat.aip.twin.domain;

import java.time.Instant;
import java.util.List;

/**
 * The ARCHITECTURE_SNAPSHOT assembled by {@code runArchitectureRescan()}: a
 * point-in-time view of the architecture-scan layer, combining the jQAssistant
 * bytecode graph (code-side truth) with the Structurizr C4 model (intended
 * design), plus best-effort drift signals where the two disagree.
 *
 * @param graph              jQAssistant ARCHITECTURE_GRAPH summary payload
 *                           ({@code labelCounts, dependencyCount, cycleCount,
 *                           layeringViolationCount}); null when jQAssistant stale
 * @param cycles             package-level dependency cycles (from {@code /cycles});
 *                           empty when unavailable
 * @param layeringViolations layering violations (from {@code /violations});
 *                           empty when unavailable
 * @param model              Structurizr ARCHITECTURE_MODEL summary payload
 *                           ({@code workspaceName, systems, containers,
 *                           components, relationships, views, parsedOk}); null
 *                           when Structurizr stale
 * @param validation         Structurizr model validation payload
 *                           ({@code valid, errors, warnings}); null when stale
 * @param driftSignals       high-level, best-effort drift notes between code
 *                           (graph) and model (C4). Component-level drift is
 *                           future work — see {@code detectDrift} on structurizr-mcp.
 * @param generatedAt        snapshot timestamp
 */
public record ArchitectureSnapshot(
        Object graph,
        List<Object> cycles,
        List<Object> layeringViolations,
        Object model,
        Object validation,
        List<String> driftSignals,
        Instant generatedAt) {
}
