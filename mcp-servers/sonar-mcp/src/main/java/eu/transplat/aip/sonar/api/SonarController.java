package eu.transplat.aip.sonar.api;

import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.sonar.service.SonarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST mirror of the read-only {@link SonarService} tools, for the digital-twin
 * orchestrator and dashboards that call HTTP rather than MCP. Every endpoint
 * returns the canonical {@link McpResponse} envelope.
 */
@RestController
@RequestMapping("/api/sonar")
public class SonarController {

    private final SonarService service;

    public SonarController(SonarService service) {
        this.service = service;
    }

    /** QUALITY_STATE/DEBT_STATE slice across all configured project keys. */
    @GetMapping("/state")
    public McpResponse state() {
        return service.getState();
    }

    /** Quality-gate status and failing conditions for a single project. */
    @GetMapping("/quality-gate")
    public McpResponse qualityGate(@RequestParam("project") String project) {
        return service.qualityGate(project);
    }

    /** Technical debt for a single project. */
    @GetMapping("/debt")
    public McpResponse debt(@RequestParam("project") String project) {
        return service.technicalDebt(project);
    }

    /** Combined measures + gate report for a single project. */
    @GetMapping("/report")
    public McpResponse report(@RequestParam("project") String project) {
        return service.fetchReport(project);
    }
}
