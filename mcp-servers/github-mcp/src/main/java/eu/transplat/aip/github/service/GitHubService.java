package eu.transplat.aip.github.service;

import eu.transplat.aip.github.client.GitClient;
import eu.transplat.aip.github.config.GitProperties;
import eu.transplat.aip.github.domain.BranchInfo;
import eu.transplat.aip.github.domain.ChangeSet;
import eu.transplat.aip.github.domain.CodeState;
import eu.transplat.aip.github.domain.CommitInfo;
import eu.transplat.aip.github.domain.PRInsight;
import eu.transplat.aip.github.domain.PullRequestInfo;
import eu.transplat.aip.github.domain.RepoInfo;
import eu.transplat.aip.github.domain.RepoSnapshot;
import eu.transplat.aip.github.domain.TagInfo;
import eu.transplat.aip.mcp.common.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP tool surface over the configured {@link GitClient}. Every method returns
 * the canonical {@link McpResponse}; failures are caught and turned into
 * {@code error} responses so nothing propagates out of a tool invocation.
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final GitClient client;
    private final GitProperties props;

    public GitHubService(GitClient client, GitProperties props) {
        this.client = client;
        this.props = props;
    }

    private String source() {
        return client.source();
    }

    @Tool(description = "Read repository metadata (name, default branch, language, visibility, last push time). "
            + "Repo is owner/name for GitHub or a project path/id for GitLab.")
    public McpResponse readRepository(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo) {
        try {
            RepoInfo info = client.getRepository(repo);
            if (info == null) {
                return McpResponse.error(source(), "Repository not found: " + repo);
            }
            return McpResponse.ok(info, source());
        } catch (Exception e) {
            return fail("readRepository", repo, e);
        }
    }

    @Tool(description = "Compact snapshot of a repository: default branch, last commit, branch/tag counts and open PR count.")
    public McpResponse repoSnapshot(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo) {
        try {
            return McpResponse.ok(snapshot(repo), source());
        } catch (Exception e) {
            return fail("repoSnapshot", repo, e);
        }
    }

    @Tool(description = "Recent commits on a branch (defaults to the default branch). limit caps the number returned (max 100).")
    public McpResponse readCommits(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo,
            @ToolParam(description = "Branch name; null/blank uses the default branch", required = false) String branch,
            @ToolParam(description = "Max commits to return (default 20)", required = false) Integer limit) {
        try {
            int lim = (limit == null || limit <= 0) ? 20 : limit;
            List<CommitInfo> commits = client.getCommits(repo, branch, lim);
            return McpResponse.ok(commits, source());
        } catch (Exception e) {
            return fail("readCommits", repo, e);
        }
    }

    @Tool(description = "List branches of a repository.")
    public McpResponse readBranches(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo) {
        try {
            List<BranchInfo> branches = client.getBranches(repo);
            return McpResponse.ok(branches, source());
        } catch (Exception e) {
            return fail("readBranches", repo, e);
        }
    }

    @Tool(description = "List tags of a repository.")
    public McpResponse readTags(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo) {
        try {
            List<TagInfo> tags = client.getTags(repo);
            return McpResponse.ok(tags, source());
        } catch (Exception e) {
            return fail("readTags", repo, e);
        }
    }

    @Tool(description = "List pull/merge requests in a given state (open, closed, merged, all). Defaults to open.")
    public McpResponse searchPullRequests(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo,
            @ToolParam(description = "State: open|closed|merged|all (default open)", required = false) String state) {
        try {
            List<PullRequestInfo> prs = client.getPullRequests(repo, state);
            return McpResponse.ok(prs, source());
        } catch (Exception e) {
            return fail("searchPullRequests", repo, e);
        }
    }

    @Tool(description = "Analyze a single pull/merge request: title, author, state, changed files, additions/deletions "
            + "and the distinct top-level directories it touches (a proxy for affected modules).")
    public McpResponse analyzePullRequest(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo,
            @ToolParam(description = "PR/MR number") Integer number) {
        try {
            if (number == null) {
                return McpResponse.error(source(), "PR number is required");
            }
            PRInsight insight = client.analyzePullRequest(repo, number);
            if (insight == null) {
                return McpResponse.error(source(), "Pull request not found: " + repo + "#" + number);
            }
            return McpResponse.ok(insight, source());
        } catch (Exception e) {
            return fail("analyzePullRequest", repo, e);
        }
    }

    @Tool(description = "Diff two refs (base..head): the changed file paths and the commit messages between them.")
    public McpResponse extractChangesets(
            @ToolParam(description = "Repository, e.g. 'octocat/Hello-World'") String repo,
            @ToolParam(description = "Base ref (branch, tag or SHA)") String baseRef,
            @ToolParam(description = "Head ref (branch, tag or SHA)") String headRef) {
        try {
            ChangeSet cs = client.compare(repo, baseRef, headRef);
            return McpResponse.ok(cs, source());
        } catch (Exception e) {
            return fail("extractChangesets", repo, e);
        }
    }

    @Tool(description = "Aggregate CODE_STATE / CHANGESET_STATE across all configured repositories: a snapshot per repo, "
            + "the total open PR count and the most recently active repo. Consumed by the digital-twin orchestrator.")
    public McpResponse getState() {
        List<String> repos = props.getRepositories();
        if (repos == null || repos.isEmpty()) {
            CodeState empty = new CodeState(List.of(), 0, null, Instant.now());
            return McpResponse.stale(empty, source(), "No repositories configured (git.repositories is empty)");
        }
        List<RepoSnapshot> snapshots = new ArrayList<>();
        int openPrTotal = 0;
        int failures = 0;
        String mostRecentlyActive = null;
        String mostRecentDate = null;
        for (String repo : repos) {
            try {
                RepoSnapshot snap = snapshot(repo);
                snapshots.add(snap);
                openPrTotal += snap.openPrCount();
                String date = snap.lastCommit() == null ? null : snap.lastCommit().date();
                if (date != null && (mostRecentDate == null || date.compareTo(mostRecentDate) > 0)) {
                    mostRecentDate = date;
                    mostRecentlyActive = snap.repo();
                }
            } catch (Exception e) {
                failures++;
                log.warn("getState: snapshot failed for {}: {}", repo, e.toString());
            }
        }
        CodeState state = new CodeState(snapshots, openPrTotal, mostRecentlyActive, Instant.now());
        if (snapshots.isEmpty()) {
            return McpResponse.error(source(), "All " + repos.size() + " configured repositories failed to load");
        }
        if (failures > 0) {
            return McpResponse.stale(state, source(),
                    failures + " of " + repos.size() + " repositories failed to load");
        }
        return McpResponse.ok(state, source());
    }

    /** Builds a {@link RepoSnapshot} from several upstream calls. */
    private RepoSnapshot snapshot(String repo) {
        RepoInfo info = client.getRepository(repo);
        String defaultBranch = info == null ? null : info.defaultBranch();
        List<CommitInfo> commits = client.getCommits(repo, defaultBranch, 1);
        CommitInfo last = commits.isEmpty() ? null : commits.get(0);
        int branchCount = client.getBranches(repo).size();
        int tagCount = client.getTags(repo).size();
        int openPr = client.getPullRequests(repo, "open").size();
        return new RepoSnapshot(
                info == null ? repo : info.name(),
                defaultBranch,
                last,
                branchCount,
                tagCount,
                openPr);
    }

    private McpResponse fail(String op, String repo, Exception e) {
        log.warn("{} failed for {}: {}", op, repo, e.toString());
        return McpResponse.error(source(), op + " failed for '" + repo + "': " + e.getMessage());
    }
}
