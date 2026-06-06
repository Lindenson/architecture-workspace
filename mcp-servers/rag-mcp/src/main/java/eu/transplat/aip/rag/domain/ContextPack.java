package eu.transplat.aip.rag.domain;

import java.util.List;

/**
 * Payload of {@code retrieveContext()}: a single concatenated context string the
 * agent can paste into a prompt, plus the list of citations backing it.
 *
 * @param query     the query string
 * @param mode      "vector" or "fulltext"
 * @param context   concatenated, citation-tagged context pack
 * @param citations the hits referenced in the context
 */
public record ContextPack(String query, String mode, String context, List<SearchHit> citations) {
}
