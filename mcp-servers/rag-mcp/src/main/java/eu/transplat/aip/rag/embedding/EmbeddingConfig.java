package eu.transplat.aip.rag.embedding;

import eu.transplat.aip.rag.config.RagProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires exactly one {@link EmbeddingService} based on {@code rag.embeddings.provider}.
 * <ul>
 *   <li>{@code local} (default / missing) — offline ONNX all-MiniLM-L6-v2.</li>
 *   <li>{@code openai} — OpenAI-compatible HTTP endpoint.</li>
 *   <li>{@code none} — vectors off, full-text search only.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class EmbeddingConfig {

    @Bean
    @ConditionalOnProperty(prefix = "rag.embeddings", name = "provider", havingValue = "local", matchIfMissing = true)
    public EmbeddingService localEmbeddingService(RagProperties properties) {
        return new LocalEmbeddingService(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rag.embeddings", name = "provider", havingValue = "openai")
    public EmbeddingService openAiEmbeddingService(RagProperties properties) {
        return new OpenAiEmbeddingService(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rag.embeddings", name = "provider", havingValue = "none")
    public EmbeddingService noneEmbeddingService() {
        return new NoneEmbeddingService();
    }
}
