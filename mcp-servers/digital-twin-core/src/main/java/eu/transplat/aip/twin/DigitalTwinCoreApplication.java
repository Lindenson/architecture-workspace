package eu.transplat.aip.twin;

import eu.transplat.aip.twin.config.DigitalTwinProperties;
import eu.transplat.aip.twin.service.DigitalTwinService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Digital Twin Core — the orchestrator/brain of the AIP. Fans out to the other
 * MCP servers (Jira, GitHub, Sonar live; Structurizr/jQAssistant/Wiki/RAG
 * planned) over plain HTTP REST, merges their state slices into the
 * DIGITAL_TWIN_MODEL and exposes high-level analysis commands as MCP
 * {@code @Tool}s, mirrored as REST endpoints under {@code /api/twin}.
 */
@SpringBootApplication
@EnableConfigurationProperties(DigitalTwinProperties.class)
public class DigitalTwinCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalTwinCoreApplication.class, args);
    }

    /** Registers the {@link DigitalTwinService} {@code @Tool} methods with the MCP server. */
    @Bean
    public ToolCallbackProvider twinTools(DigitalTwinService s) {
        return MethodToolCallbackProvider.builder().toolObjects(s).build();
    }
}
