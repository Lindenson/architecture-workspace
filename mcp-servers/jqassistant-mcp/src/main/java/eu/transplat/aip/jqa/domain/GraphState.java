package eu.transplat.aip.jqa.domain;

import java.time.Instant;
import java.util.Map;

/**
 * ARCHITECTURE_GRAPH summary consumed by the digital twin at
 * {@code GET /api/jqassistant/state}.
 *
 * @param labelCounts            node count per label of interest (Type, Package, Artifact)
 * @param dependencyCount        total number of {@code :DEPENDS_ON} relationships
 * @param cycleCount             number of package-level dependency cycles detected
 * @param layeringViolationCount number of layering violations detected
 * @param generatedAt            when this snapshot was produced
 */
public record GraphState(
        Map<String, Long> labelCounts,
        long dependencyCount,
        int cycleCount,
        int layeringViolationCount,
        Instant generatedAt) {
}
