package eu.transplat.aip.jira.api;

import eu.transplat.aip.jira.service.JiraService;
import eu.transplat.aip.mcp.common.McpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST mirror of the Jira read tools. Protected by the shared
 * {@code InternalTokenAuthFilter} from mcp-common. Every endpoint returns the
 * canonical {@link McpResponse} shape — same contract as the MCP tools.
 */
@RestController
@RequestMapping("/api/jira")
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    /** DELIVERY_STATE slice consumed by the digital-twin orchestrator. */
    @GetMapping("/state")
    public McpResponse state() {
        return jiraService.getState();
    }

    @GetMapping("/issues")
    public McpResponse issues(@RequestParam String jql,
                              @RequestParam(name = "max", required = false) Integer max) {
        return jiraService.searchIssues(jql, max);
    }

    @GetMapping("/issue/{key}")
    public McpResponse issue(@PathVariable String key) {
        return jiraService.getIssue(key);
    }

    @GetMapping("/epics")
    public McpResponse epics(@RequestParam String project) {
        return jiraService.getEpics(project);
    }
}
