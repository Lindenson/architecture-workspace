package eu.transplat.aip.jira;

import eu.transplat.aip.jira.config.JiraProperties;
import eu.transplat.aip.jira.service.JiraService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Jira MCP server entry point. Exposes Jira (Atlassian Cloud) read/write tools to
 * the AIP digital-twin orchestrator over the Spring AI MCP protocol, and mirrors
 * the read tools as REST endpoints under {@code /api/jira}.
 */
@SpringBootApplication
@EnableConfigurationProperties(JiraProperties.class)
public class JiraMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiraMcpApplication.class, args);
    }

    /** Registers the {@link JiraService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider jiraTools(JiraService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
