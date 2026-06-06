package eu.transplat.aip.rag.embedding;

import eu.transplat.aip.rag.config.RagProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.transformers.TransformersEmbeddingModel;

import java.util.List;
import java.util.Map;

/**
 * Fully offline embeddings backed by Spring AI's {@link TransformersEmbeddingModel}
 * and the bundled all-MiniLM-L6-v2 ONNX model (384 dimensions). No API keys, no
 * network. The model is instantiated and initialized here (rather than relying on
 * the transformers starter autoconfiguration) so that provider switching stays
 * clean and only one {@link EmbeddingService} bean is ever active.
 */
public class LocalEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingService.class);

    private final RagProperties properties;
    private final TransformersEmbeddingModel model = new TransformersEmbeddingModel();
    private volatile boolean ready;

    public LocalEmbeddingService(RagProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        try {
            // Padding avoids "Supplied array is ragged" ONNX errors on batched input.
            model.setTokenizerOptions(Map.of("padding", "true"));
            model.afterPropertiesSet();
            ready = true;
            log.info("Local ONNX embedding model (all-MiniLM-L6-v2) initialized, dimension={}", dimension());
        } catch (Exception e) {
            // Never fail context startup: degrade to unavailable, the service maps to a clear response.
            ready = false;
            log.warn("Local ONNX embedding model failed to initialize; embeddings unavailable: {}", e.toString());
        }
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public boolean available() {
        return ready;
    }

    @Override
    public int dimension() {
        return properties.getEmbeddings().getDimension();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (!ready) {
            throw new IllegalStateException("Local embedding model is not initialized.");
        }
        return model.embed(texts);
    }
}
