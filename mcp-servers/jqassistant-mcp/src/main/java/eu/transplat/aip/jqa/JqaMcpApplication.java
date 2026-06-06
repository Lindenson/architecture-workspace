package eu.transplat.aip.jqa;

import eu.transplat.aip.jqa.config.JqaProperties;
import eu.transplat.aip.jqa.service.JqaService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * jQAssistant MCP server entry point. Exposes the bytecode dependency graph
 * (Neo4j, populated by jQAssistant) — cycles, layering violations, coupling,
 * blast-radius and read-only Cypher — to the AIP digital-twin orchestrator over
 * the Spring AI MCP protocol, and mirrors the read tools as REST endpoints under
 * {@code /api/jqassistant}. This is the ARCHITECTURE_GRAPH source (MVP-2).
 */
@SpringBootApplication
@EnableConfigurationProperties(JqaProperties.class)
public class JqaMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JqaMcpApplication.class, args);
    }

    /** Registers the {@link JqaService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider jqaTools(JqaService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
