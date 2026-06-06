package eu.transplat.aip.rag.domain;

import java.time.Instant;
import java.util.List;

/**
 * KNOWLEDGE/RAG state slice consumed by the digital twin at GET /api/rag/state.
 *
 * @param enabled       whether the RAG layer is on
 * @param provider      active embedding provider (local | openai | none)
 * @param dimension     vector dimension (0 when provider=none)
 * @param dbReachable   whether Postgres responded
 * @param indexedChunks number of stored chunks (null when DB unreachable)
 * @param sources       configured source directories
 * @param lastIndexedAt timestamp of the most recently indexed chunk (null if none/unknown)
 */
public record RagState(
        boolean enabled,
        String provider,
        int dimension,
        boolean dbReachable,
        Long indexedChunks,
        List<String> sources,
        Instant lastIndexedAt) {
}
