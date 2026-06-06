package eu.transplat.aip.rag.embedding;

import java.util.List;

/**
 * Pluggable text-embedding provider. Exactly one implementation is active at a
 * time, selected by {@code rag.embeddings.provider} (local | openai | none).
 *
 * <p>Implementations must never throw from {@link #available()} or
 * {@link #dimension()}; only {@link #embed(List)} may fail (network/model), and
 * the service layer wraps that in a resilient {@code McpResponse}.
 */
public interface EmbeddingService {

    /** Logical provider name (local | openai | none). */
    String provider();

    /** True when this provider can actually produce vectors right now. */
    boolean available();

    /** Vector dimension produced by this provider (0 when unavailable). */
    int dimension();

    /**
     * Embed a batch of texts into vectors.
     *
     * @throws IllegalStateException when the provider is not available
     */
    List<float[]> embed(List<String> texts);
}
