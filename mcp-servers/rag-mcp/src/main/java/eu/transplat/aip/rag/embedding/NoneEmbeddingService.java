package eu.transplat.aip.rag.embedding;

import java.util.List;

/**
 * "Vectors off" provider: signals that semantic search is disabled and only
 * full-text (keyword) search is available. Chunks are still stored, with a NULL
 * embedding column.
 */
public class NoneEmbeddingService implements EmbeddingService {

    @Override
    public String provider() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public int dimension() {
        return 0;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        throw new IllegalStateException("Embeddings provider is 'none' — vector embeddings are disabled (full-text search only).");
    }
}
