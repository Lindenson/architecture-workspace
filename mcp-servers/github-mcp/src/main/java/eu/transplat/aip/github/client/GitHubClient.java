package eu.transplat.aip.github.client;

import com.fasterxml.jackson.databind.JsonNode;
import eu.transplat.aip.github.config.GitProperties;
import eu.transplat.aip.github.domain.BranchInfo;
import eu.transplat.aip.github.domain.ChangeSet;
import eu.transplat.aip.github.domain.CommitInfo;
import eu.transplat.aip.github.domain.PRInsight;
import eu.transplat.aip.github.domain.PullRequestInfo;
import eu.transplat.aip.github.domain.RepoInfo;
import eu.transplat.aip.github.domain.TagInfo;
import eu.transplat.aip.mcp.common.client.RestClientFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GitHub REST v3 implementation. Uses bearer-token auth plus the
 * {@code Accept: application/vnd.github+json} header. Parses only key fields.
 *
 * <p>The {@code repo} argument is {@code owner/name}; a bare {@code name} is
 * resolved against {@code git.github.org} when configured.
 */
public class GitHubClient implements GitClient {

    private static final String SOURCE = "github-mcp:GitHub REST";

    private final RestClient http;
    private final String org;

    public GitHubClient(GitProperties.GitHub cfg) {
        this.http = RestClientFactory.bearer(cfg.getApiUrl(), cfg.getToken())
                .mutate()
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        this.org = cfg.getOrg() == null ? "" : cfg.getOrg().trim();
    }

    @Override
    public String source() {
        return SOURCE;
    }

    /** Normalises {@code repo} to {@code owner/name}, defaulting the owner to the configured org. */
    private String fullName(String repo) {
        if (repo == null) {
            return "";
        }
        String r = repo.trim();
        if (r.contains("/")) {
            return r;
        }
        return org.isEmpty() ? r : org + "/" + r;
    }

    @Override
    public RepoInfo getRepository(String repo) {
        JsonNode n = http.get().uri("/repos/{r}", fullName(repo)).retrieve().body(JsonNode.class);
        if (n == null) {
            return null;
        }
        return new RepoInfo(
                n.path("full_name").asText(fullName(repo)),
                n.path("default_branch").asText(null),
                n.path("language").isNull() ? null : n.path("language").asText(null),
                n.path("visibility").asText(n.path("private").asBoolean(false) ? "private" : "public"),
                n.path("pushed_at").asText(null));
    }

    @Override
    public List<CommitInfo> getCommits(String repo, String branch, int limit) {
        int perPage = Math.max(1, Math.min(limit, 100));
        JsonNode arr = http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{r}/commits")
                        .queryParamIfPresent("sha", java.util.Optional.ofNullable(blankToNull(branch)))
                        .queryParam("per_page", perPage)
                        .build(fullName(repo)))
                .retrieve()
                .body(JsonNode.class);
        List<CommitInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode c : arr) {
                out.add(toCommit(c));
            }
        }
        return out;
    }

    private static CommitInfo toCommit(JsonNode c) {
        JsonNode commit = c.path("commit");
        JsonNode author = commit.path("author");
        String name = author.path("name").asText(null);
        if (name == null) {
            name = c.path("author").path("login").asText(null);
        }
        return new CommitInfo(
                c.path("sha").asText(null),
                commit.path("message").asText(null),
                name,
                author.path("date").asText(null));
    }

    @Override
    public List<BranchInfo> getBranches(String repo) {
        JsonNode arr = http.get().uri("/repos/{r}/branches?per_page=100", fullName(repo))
                .retrieve().body(JsonNode.class);
        List<BranchInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode b : arr) {
                out.add(new BranchInfo(
                        b.path("name").asText(null),
                        b.path("commit").path("sha").asText(null),
                        b.path("protected").asBoolean(false)));
            }
        }
        return out;
    }

    @Override
    public List<TagInfo> getTags(String repo) {
        JsonNode arr = http.get().uri("/repos/{r}/tags?per_page=100", fullName(repo))
                .retrieve().body(JsonNode.class);
        List<TagInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode t : arr) {
                out.add(new TagInfo(
                        t.path("name").asText(null),
                        t.path("commit").path("sha").asText(null)));
            }
        }
        return out;
    }

    @Override
    public List<PullRequestInfo> getPullRequests(String repo, String state) {
        String st = blankToNull(state) == null ? "open" : state.trim();
        JsonNode arr = http.get().uri("/repos/{r}/pulls?state={s}&per_page=100", fullName(repo), st)
                .retrieve().body(JsonNode.class);
        List<PullRequestInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode p : arr) {
                out.add(toPr(p));
            }
        }
        return out;
    }

    private static PullRequestInfo toPr(JsonNode p) {
        String state = p.path("state").asText(null);
        if (!p.path("merged_at").isNull() && !p.path("merged_at").asText("").isEmpty()) {
            state = "merged";
        }
        return new PullRequestInfo(
                p.path("number").asInt(),
                p.path("title").asText(null),
                p.path("user").path("login").asText(null),
                state,
                p.path("created_at").asText(null),
                p.path("updated_at").asText(null));
    }

    @Override
    public PRInsight analyzePullRequest(String repo, int number) {
        JsonNode p = http.get().uri("/repos/{r}/pulls/{n}", fullName(repo), number)
                .retrieve().body(JsonNode.class);
        if (p == null) {
            return null;
        }
        String state = p.path("state").asText(null);
        if (!p.path("merged_at").isNull() && !p.path("merged_at").asText("").isEmpty()) {
            state = "merged";
        }
        JsonNode files = http.get().uri("/repos/{r}/pulls/{n}/files?per_page=100", fullName(repo), number)
                .retrieve().body(JsonNode.class);
        Set<String> packages = new LinkedHashSet<>();
        if (files != null && files.isArray()) {
            for (JsonNode f : files) {
                packages.add(topLevelDir(f.path("filename").asText("")));
            }
        }
        packages.remove("");
        return new PRInsight(
                p.path("number").asInt(number),
                p.path("title").asText(null),
                p.path("user").path("login").asText(null),
                state,
                p.path("changed_files").asInt(0),
                p.path("additions").asInt(0),
                p.path("deletions").asInt(0),
                new ArrayList<>(packages));
    }

    @Override
    public ChangeSet compare(String repo, String baseRef, String headRef) {
        JsonNode n = http.get().uri("/repos/{r}/compare/{base}...{head}", fullName(repo), baseRef, headRef)
                .retrieve().body(JsonNode.class);
        List<String> changedFiles = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int totalCommits = 0;
        if (n != null) {
            totalCommits = n.path("total_commits").asInt(0);
            JsonNode files = n.path("files");
            if (files.isArray()) {
                for (JsonNode f : files) {
                    changedFiles.add(f.path("filename").asText(null));
                }
            }
            JsonNode commits = n.path("commits");
            if (commits.isArray()) {
                for (JsonNode c : commits) {
                    messages.add(c.path("commit").path("message").asText(null));
                }
            }
        }
        return new ChangeSet(fullName(repo), baseRef, headRef, changedFiles, messages, totalCommits);
    }

    static String topLevelDir(String path) {
        if (path == null) {
            return "";
        }
        int slash = path.indexOf('/');
        return slash < 0 ? "(root)" : path.substring(0, slash);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
