package eu.transplat.aip.rag.domain;

import java.util.List;

/**
 * Payload of {@code search()}: the ranked hits plus the retrieval mode used.
 *
 * @param query the query string
 * @param mode  "vector" or "fulltext"
 * @param topK  the effective top-K applied
 * @param hits  ranked results
 */
public record SearchResult(String query, String mode, int topK, List<SearchHit> hits) {
}
