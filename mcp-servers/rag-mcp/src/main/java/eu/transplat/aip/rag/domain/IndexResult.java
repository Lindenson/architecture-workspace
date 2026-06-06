package eu.transplat.aip.rag.domain;

import java.util.List;

/**
 * Payload of {@code indexPath()} / {@code reindexAll()}.
 *
 * @param paths        the source paths processed
 * @param filesIndexed number of files read and chunked
 * @param chunks       number of chunks stored
 * @param embedded     true when vectors were computed (provider available)
 * @param mode         "vector" or "fulltext" (when embeddings unavailable)
 */
public record IndexResult(List<String> paths, int filesIndexed, int chunks, boolean embedded, String mode) {
}
