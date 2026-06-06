package eu.transplat.aip.rag.domain;

/**
 * A ranked search result.
 *
 * @param source  source group the chunk belongs to
 * @param ref     repo-relative file path
 * @param chunkNo ordinal of the chunk within its file
 * @param score   relevance score (higher = more relevant)
 * @param snippet the chunk content (possibly trimmed)
 */
public record SearchHit(String source, String ref, int chunkNo, double score, String snippet) {
}
