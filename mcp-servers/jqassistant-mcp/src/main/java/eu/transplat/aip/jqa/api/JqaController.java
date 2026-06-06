package eu.transplat.aip.jqa.api;

import eu.transplat.aip.jqa.service.JqaService;
import eu.transplat.aip.mcp.common.McpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST mirror of the read-only {@link JqaService} tools, for the digital-twin
 * orchestrator and dashboards that call HTTP rather than MCP. Every endpoint
 * returns the canonical {@link McpResponse} envelope.
 *
 * <p>{@code GET /api/jqassistant/state} is the ARCHITECTURE_GRAPH contract the
 * digital twin consumes.
 */
@RestController
@RequestMapping("/api/jqassistant")
public class JqaController {

    private final JqaService service;

    public JqaController(JqaService service) {
        this.service = service;
    }

    /** ARCHITECTURE_GRAPH summary (label counts, dependencies, cycles, violations). */
    @GetMapping("/state")
    public McpResponse state() {
        return service.getState();
    }

    /** Package-level dependency cycles. */
    @GetMapping("/cycles")
    public McpResponse cycles() {
        return service.findCycles();
    }

    /** Layering violations (controller -> repository direct dependencies). */
    @GetMapping("/violations")
    public McpResponse violations() {
        return service.findLayeringViolations();
    }

    /** Read-only Cypher via query string: {@code GET /api/jqassistant/cypher?q=...&limit=...}. */
    @GetMapping("/cypher")
    public McpResponse cypherGet(@RequestParam("q") String q,
                                 @RequestParam(value = "limit", required = false) Integer limit) {
        return service.queryGraph(q, limit);
    }

    /** Read-only Cypher via JSON body: {@code {"cypher": "...", "limit": 100}}. */
    @PostMapping("/cypher")
    public McpResponse cypherPost(@RequestBody Map<String, Object> body) {
        String cypher = body.get("cypher") == null ? null : String.valueOf(body.get("cypher"));
        Integer limit = null;
        Object rawLimit = body.get("limit");
        if (rawLimit instanceof Number n) {
            limit = n.intValue();
        } else if (rawLimit != null) {
            try {
                limit = Integer.parseInt(String.valueOf(rawLimit));
            } catch (NumberFormatException ignored) {
                // leave limit null
            }
        }
        return service.queryGraph(cypher, limit);
    }
}
