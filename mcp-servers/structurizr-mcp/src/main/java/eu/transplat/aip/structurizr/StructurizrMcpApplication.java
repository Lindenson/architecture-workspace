package eu.transplat.aip.structurizr;

import eu.transplat.aip.structurizr.config.StructurizrProperties;
import eu.transplat.aip.structurizr.service.StructurizrService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Structurizr MCP server entry point. Parses the Structurizr C4 model
 * ({@code workspace.dsl}) and exposes containers, components, relationships,
 * views, model validation and a code-drift hook to the AIP digital-twin
 * orchestrator over the Spring AI MCP protocol, and mirrors the read tools as
 * REST endpoints under {@code /api/structurizr}.
 *
 * <p>This is the {@code ARCHITECTURE_MODEL} source (MVP-2). The DSL file is
 * parsed lazily per request — never at startup — so the server boots even when
 * the workspace file is missing.
 */
@SpringBootApplication
@EnableConfigurationProperties(StructurizrProperties.class)
public class StructurizrMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(StructurizrMcpApplication.class, args);
    }

    /** Registers the {@link StructurizrService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider structurizrTools(StructurizrService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
