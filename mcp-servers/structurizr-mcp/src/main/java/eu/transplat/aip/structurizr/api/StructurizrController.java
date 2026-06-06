package eu.transplat.aip.structurizr.api;

import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.structurizr.service.StructurizrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST mirror of the read-only {@link StructurizrService} tools, for the
 * digital-twin orchestrator and dashboards that call HTTP rather than MCP.
 * Every endpoint returns the canonical {@link McpResponse} envelope.
 */
@RestController
@RequestMapping("/api/structurizr")
public class StructurizrController {

    private final StructurizrService service;

    public StructurizrController(StructurizrService service) {
        this.service = service;
    }

    /** ARCHITECTURE_MODEL state slice for the digital twin. */
    @GetMapping("/state")
    public McpResponse state() {
        return service.getState();
    }

    /** Workspace summary: systems, containers, components, relationships, views. */
    @GetMapping("/workspace")
    public McpResponse workspace() {
        return service.readWorkspace();
    }

    /** Model validation: {valid, errors, warnings}. */
    @GetMapping("/validate")
    public McpResponse validate() {
        return service.validateModel();
    }

    /** All views defined in the workspace. */
    @GetMapping("/views")
    public McpResponse views() {
        return service.getViews();
    }

    /** Model elements of a given type (person / system / container / component). */
    @GetMapping("/elements")
    public McpResponse elements(@RequestParam("type") String type) {
        return service.listElements(type);
    }

    /** All relationships in the model. */
    @GetMapping("/relationships")
    public McpResponse relationships() {
        return service.listRelationships();
    }
}
