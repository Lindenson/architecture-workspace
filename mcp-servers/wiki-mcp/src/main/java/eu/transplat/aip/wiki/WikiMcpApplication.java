package eu.transplat.aip.wiki;

import eu.transplat.aip.wiki.config.WikiProperties;
import eu.transplat.aip.wiki.service.WikiService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wiki MCP server entry point. Exposes the corporate Wiki (Confluence Cloud)
 * read/write tools to the AIP digital-twin orchestrator over the Spring AI MCP
 * protocol, and mirrors the read tools as REST endpoints under {@code /api/wiki}.
 *
 * <p>This is the optional KNOWLEDGE_DOCUMENTS source: set {@code wiki.enabled=false}
 * to turn the whole capability off (every tool then returns a DISABLED response).
 */
@SpringBootApplication
@EnableConfigurationProperties(WikiProperties.class)
public class WikiMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(WikiMcpApplication.class, args);
    }

    /** Registers the {@link WikiService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider wikiTools(WikiService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
