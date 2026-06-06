package eu.transplat.aip.wiki.api;

import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.wiki.service.WikiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST mirror of the Wiki read tools. Protected by the shared
 * {@code InternalTokenAuthFilter} from mcp-common. Every endpoint returns the
 * canonical {@link McpResponse} shape — same contract as the MCP tools.
 *
 * <p>{@code GET /api/wiki/state} is the KNOWLEDGE_DOCUMENTS slice consumed by the
 * digital-twin orchestrator.
 */
@RestController
@RequestMapping("/api/wiki")
public class WikiController {

    private final WikiService wikiService;

    public WikiController(WikiService wikiService) {
        this.wikiService = wikiService;
    }

    /** KNOWLEDGE_DOCUMENTS slice consumed by the digital-twin orchestrator. */
    @GetMapping("/state")
    public McpResponse state() {
        return wikiService.getState();
    }

    @GetMapping("/search")
    public McpResponse search(@RequestParam String q,
                              @RequestParam(name = "limit", required = false) Integer limit) {
        return wikiService.searchPages(q, limit);
    }

    @GetMapping("/page/{id}")
    public McpResponse page(@PathVariable String id) {
        return wikiService.getPage(id);
    }

    @GetMapping("/pages")
    public McpResponse pages(@RequestParam String space,
                             @RequestParam(name = "limit", required = false) Integer limit) {
        return wikiService.listPages(space, limit);
    }
}
