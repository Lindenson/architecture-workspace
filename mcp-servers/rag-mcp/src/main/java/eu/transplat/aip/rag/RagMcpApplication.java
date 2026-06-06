package eu.transplat.aip.rag;

import eu.transplat.aip.rag.config.RagProperties;
import eu.transplat.aip.rag.service.RagService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RAG knowledge MCP server entry point. Exposes an optional, fully configurable
 * semantic/keyword knowledge layer over the workspace docs (Postgres+pgvector),
 * with local offline ONNX embeddings by default. Mirrors the read tools as REST
 * under {@code /api/rag}.
 *
 * <p>RESILIENCE: the context boots even when Postgres is down, the feature is
 * disabled, or the provider is {@code none}. The DB is never queried at startup.
 */
@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class RagMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagMcpApplication.class, args);
    }

    /** Registers the {@link RagService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider ragTools(RagService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
