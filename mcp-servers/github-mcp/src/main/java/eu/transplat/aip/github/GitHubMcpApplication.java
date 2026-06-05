package eu.transplat.aip.github;

import eu.transplat.aip.github.config.GitProperties;
import eu.transplat.aip.github.service.GitHubService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * github-mcp — Spring Boot MCP server exposing GitHub/GitLab repository, commit
 * and pull-request data to the AIP digital twin.
 */
@SpringBootApplication
@EnableConfigurationProperties(GitProperties.class)
public class GitHubMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider githubTools(GitHubService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
