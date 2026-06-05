package eu.transplat.aip.twin.api;

import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.twin.service.DigitalTwinService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST mirror of the high-level orchestrator tools, for dashboards and callers
 * that prefer HTTP over MCP. Every endpoint returns the canonical
 * {@link McpResponse} envelope — same contract as the MCP {@code @Tool}s.
 */
@RestController
@RequestMapping("/api/twin")
public class TwinController {

    private final DigitalTwinService service;

    public TwinController(DigitalTwinService service) {
        this.service = service;
    }

    /** Merged DIGITAL_TWIN_MODEL (SHOW_PROJECT_STATE). */
    @GetMapping("/state")
    public McpResponse state() {
        return service.showProjectState();
    }

    /** TECH_DEBT_REPORT derived from the Sonar slice. */
    @GetMapping("/tech-debt")
    public McpResponse techDebt() {
        return service.analyzeTechDebt();
    }

    /** Release-readiness verdict combining Sonar gate and Jira delivery. */
    @GetMapping("/release-readiness")
    public McpResponse releaseReadiness() {
        return service.analyzeReleaseReadiness();
    }

    /** Markdown report; type in {DAILY,WEEKLY,ARCHITECTURE,TECH_DEBT,RELEASE}. */
    @GetMapping("/report")
    public McpResponse report(@RequestParam(name = "type", defaultValue = "DAILY") String type) {
        return service.generateReport(type);
    }
}
