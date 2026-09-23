package com.wolper.aip.rag.repository;

/**
 * A chunk as it exists in the store, i.e. with its database identity.
 *
 * <p>Distinct from {@link com.wolper.aip.rag.domain.Chunk}, which is a chunk
 * on its way in and has no id yet. Re-embedding needs the id to update a row in
 * place, which is why this type exists at all.
 *
 * @param id      primary key in the chunk table
 * @param source  configured source group (e.g. "knowledge")
 * @param ref     repo-relative file path the chunk came from
 * @param chunkNo zero-based ordinal within the file
 * @param content the chunk text
 */
public record StoredChunk(long id, String source, String ref, int chunkNo, String content) {
}
