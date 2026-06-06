package eu.transplat.aip.rag.api;

import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.rag.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST mirror of the {@link RagService} tools for the digital-twin orchestrator
 * and dashboards that call HTTP rather than MCP. Every endpoint returns the
 * canonical {@link McpResponse} envelope. {@code GET /api/rag/state} is the
 * contract the digital twin consumes for the KNOWLEDGE/RAG slice.
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService service;

    public RagController(RagService service) {
        this.service = service;
    }

    /** KNOWLEDGE/RAG state slice. */
    @GetMapping("/state")
    public McpResponse state() {
        return service.getState();
    }

    /** Ranked search (vector when available, else full-text). */
    @GetMapping("/search")
    public McpResponse search(@RequestParam("q") String q,
                              @RequestParam(value = "topK", required = false) Integer topK) {
        return service.search(q, topK);
    }

    /** Grounding context pack for a query. */
    @GetMapping("/context")
    public McpResponse context(@RequestParam("q") String q,
                               @RequestParam(value = "topK", required = false) Integer topK) {
        return service.retrieveContext(q, topK);
    }

    /** Index a single repo-relative path (query-param form). */
    @GetMapping("/index")
    public McpResponse indexGet(@RequestParam("path") String path) {
        return service.indexPath(path);
    }

    /** Index a single repo-relative path (JSON body {"path": "..."}). */
    @PostMapping("/index")
    public McpResponse indexPost(@RequestBody Map<String, String> body) {
        return service.indexPath(body == null ? null : body.get("path"));
    }

    /** Clear and reindex all configured sources. */
    @PostMapping("/reindex")
    public McpResponse reindex() {
        return service.reindexAll();
    }
}
