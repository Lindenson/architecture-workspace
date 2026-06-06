package eu.transplat.aip.rag.embedding;

import eu.transplat.aip.mcp.common.client.RestClientFactory;
import eu.transplat.aip.rag.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hand-rolled OpenAI-compatible embeddings client. Works against OpenAI itself
 * and any compatible endpoint (Ollama, LM Studio, vLLM, …) by pointing
 * {@code rag.embeddings.openai.base-url} at it. The dimension is taken from
 * configuration (it must match the chosen model).
 *
 * <p>Credentials come from config/env (never hardcoded). When the API key is
 * blank the provider reports {@link #available()} = false so the service falls
 * back to full-text search.
 */
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private final RagProperties properties;

    public OpenAiEmbeddingService(RagProperties properties) {
        this.properties = properties;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public boolean available() {
        String key = properties.getEmbeddings().getOpenai().getApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public int dimension() {
        return properties.getEmbeddings().getDimension();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embed(List<String> texts) {
        if (!available()) {
            throw new IllegalStateException("OpenAI embeddings provider has no API key configured.");
        }
        RagProperties.OpenAi cfg = properties.getEmbeddings().getOpenai();
        RestClient client = RestClientFactory.bearer(cfg.getBaseUrl(), cfg.getApiKey());

        Map<String, Object> body = Map.of("model", cfg.getModel(), "input", texts);
        Map<String, Object> response = client.post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        List<float[]> out = new ArrayList<>();
        if (response == null) {
            return out;
        }
        Object data = response.get("data");
        if (data instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> item && item.get("embedding") instanceof List<?> vec) {
                    float[] arr = new float[vec.size()];
                    for (int i = 0; i < vec.size(); i++) {
                        arr[i] = ((Number) vec.get(i)).floatValue();
                    }
                    out.add(arr);
                }
            }
        }
        if (out.size() != texts.size()) {
            log.warn("OpenAI embeddings returned {} vectors for {} inputs.", out.size(), texts.size());
        }
        return out;
    }
}
