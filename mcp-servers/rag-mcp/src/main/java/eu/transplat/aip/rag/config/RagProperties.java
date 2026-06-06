package eu.transplat.aip.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized configuration for the optional RAG knowledge layer. Everything is
 * tunable via env vars / config files (see application.yml) — never hardcoded.
 *
 * <p>The layer is intentionally optional: when {@link #isEnabled()} is false all
 * tools return {@code McpResponse.disabled(...)} and the server still boots.
 */
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** Master switch. When false, every tool returns a DISABLED response. */
    private boolean enabled = true;

    /** Default number of results returned by search/retrieve. */
    private int topK = 6;

    /** Embedding provider + storage settings. */
    @NestedConfigurationProperty
    private Embeddings embeddings = new Embeddings();

    /**
     * Directories (relative to the repo root) indexed by {@code reindexAll()}.
     * Defaults to the slow-changing knowledge/architecture corpus.
     */
    private List<String> sources = new ArrayList<>(List.of(
            "knowledge", "architecture", "history", "project-memory", "domain/model", "delivery/roadmap"));

    /** Chunking parameters. */
    @NestedConfigurationProperty
    private Chunk chunk = new Chunk();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public Embeddings getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(Embeddings embeddings) {
        this.embeddings = embeddings;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    /** Embedding provider configuration. */
    public static class Embeddings {

        /** local | openai | none. */
        private String provider = "local";

        /** Vector dimension. 384 for the bundled all-MiniLM-L6-v2 model. */
        private int dimension = 384;

        /** pgvector table name owned by this server. */
        private String table = "rag_chunks";

        /** OpenAI-compatible endpoint settings (used only when provider=openai). */
        @NestedConfigurationProperty
        private OpenAi openai = new OpenAi();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public OpenAi getOpenai() {
            return openai;
        }

        public void setOpenai(OpenAi openai) {
            this.openai = openai;
        }
    }

    /** OpenAI-compatible embeddings client settings (OpenAI / Ollama / LM Studio / …). */
    public static class OpenAi {

        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "text-embedding-3-small";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    /** Chunking parameters: max characters per chunk and overlap between chunks. */
    public static class Chunk {

        private int maxChars = 1200;
        private int overlap = 150;

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        public int getOverlap() {
            return overlap;
        }

        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }
    }
}
