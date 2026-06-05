package eu.transplat.aip.sonar;

import eu.transplat.aip.sonar.config.SonarProperties;
import eu.transplat.aip.sonar.service.SonarService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * SonarQube MCP server entry point. Exposes SonarQube quality-gate,
 * technical-debt, coverage and security tools to the AIP digital-twin
 * orchestrator over the Spring AI MCP protocol, and mirrors the read tools as
 * REST endpoints under {@code /api/sonar}.
 */
@SpringBootApplication
@EnableConfigurationProperties(SonarProperties.class)
public class SonarMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SonarMcpApplication.class, args);
    }

    /** Registers the {@link SonarService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider sonarTools(SonarService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
