package eu.transplat.aip.rag.domain;

/**
 * Payload of {@code updateEmbeddings()}.
 *
 * @param provider          provider used to recompute
 * @param dimension         provider dimension
 * @param updated           number of chunks whose embedding was recomputed
 * @param reindexRequired   true when a stored/provider dimension mismatch means a full reindex is needed
 * @param note              human-readable note
 */
public record UpdateEmbeddingsResult(
        String provider,
        int dimension,
        int updated,
        boolean reindexRequired,
        String note) {
}
