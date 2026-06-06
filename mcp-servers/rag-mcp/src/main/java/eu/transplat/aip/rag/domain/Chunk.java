package eu.transplat.aip.rag.domain;

/**
 * A single indexable unit of text.
 *
 * @param source  configured source group / top-level dir (e.g. "knowledge")
 * @param ref     repo-relative file path the chunk came from
 * @param chunkNo zero-based ordinal of this chunk within the file
 * @param content the chunk text
 */
public record Chunk(String source, String ref, int chunkNo, String content) {
}
