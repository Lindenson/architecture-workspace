package eu.transplat.aip.github.api;

import eu.transplat.aip.github.service.GitHubService;
import eu.transplat.aip.mcp.common.McpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP facade mirroring the MCP tools for orchestrator / debugging use. Repos
 * are addressed as two path segments {@code {owner}/{name}} which the controller
 * recombines into the provider-native {@code owner/name} identifier. Every
 * endpoint returns the canonical {@link McpResponse}.
 */
@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService service;

    public GitHubController(GitHubService service) {
        this.service = service;
    }

    private static String repo(String owner, String name) {
        return owner + "/" + name;
    }

    /** Aggregate CODE_STATE across all configured repositories. */
    @GetMapping("/state")
    public McpResponse state() {
        return service.getState();
    }

    @GetMapping("/repo/{owner}/{name}")
    public McpResponse repository(@PathVariable String owner, @PathVariable String name) {
        return service.readRepository(repo(owner, name));
    }

    @GetMapping("/repo/{owner}/{name}/snapshot")
    public McpResponse snapshot(@PathVariable String owner, @PathVariable String name) {
        return service.repoSnapshot(repo(owner, name));
    }

    @GetMapping("/repo/{owner}/{name}/commits")
    public McpResponse commits(@PathVariable String owner,
                               @PathVariable String name,
                               @RequestParam(required = false) String branch,
                               @RequestParam(required = false) Integer limit) {
        return service.readCommits(repo(owner, name), branch, limit);
    }

    @GetMapping("/pr/{owner}/{name}/{number}")
    public McpResponse pullRequest(@PathVariable String owner,
                                   @PathVariable String name,
                                   @PathVariable Integer number) {
        return service.analyzePullRequest(repo(owner, name), number);
    }
}
