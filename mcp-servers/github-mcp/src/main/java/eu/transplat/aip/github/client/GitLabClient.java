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
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GitLab v4 REST implementation. Authenticates with the token both as a bearer
 * (via {@link RestClientFactory#bearer}) and as a {@code PRIVATE-TOKEN} header
 * for compatibility with personal-access and project-access tokens. Parses only
 * key fields.
 *
 * <p>The {@code repo} argument is a GitLab project path ({@code group/project})
 * or numeric id; it is URL-encoded into the {@code :id} path segment.
 */
public class GitLabClient implements GitClient {

    private static final String SOURCE = "github-mcp:GitLab API v4";

    private final RestClient http;

    public GitLabClient(GitProperties.GitLab cfg) {
        this.http = RestClientFactory.bearer(cfg.getApiUrl(), cfg.getToken())
                .mutate()
                .defaultHeader("PRIVATE-TOKEN", cfg.getToken() == null ? "" : cfg.getToken())
                .build();
    }

    @Override
    public String source() {
        return SOURCE;
    }

    /** URL-encodes the project id/path for the {@code /projects/:id} segment. */
    private static String pid(String repo) {
        return UriUtils.encode(repo == null ? "" : repo.trim(), StandardCharsets.UTF_8);
    }

    @Override
    public RepoInfo getRepository(String repo) {
        JsonNode n = http.get().uri("/api/v4/projects/{id}", pid(repo)).retrieve().body(JsonNode.class);
        if (n == null) {
            return null;
        }
        return new RepoInfo(
                n.path("path_with_namespace").asText(repo),
                n.path("default_branch").asText(null),
                null, // requires a separate /languages call; omitted to stay lightweight
                n.path("visibility").asText(null),
                n.path("last_activity_at").asText(null));
    }

    @Override
    public List<CommitInfo> getCommits(String repo, String branch, int limit) {
        int perPage = Math.max(1, Math.min(limit, 100));
        JsonNode arr = http.get()
                .uri(b -> b.path("/api/v4/projects/{id}/repository/commits")
                        .queryParamIfPresent("ref_name", java.util.Optional.ofNullable(blankToNull(branch)))
                        .queryParam("per_page", perPage)
                        .build(pid(repo)))
                .retrieve().body(JsonNode.class);
        List<CommitInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode c : arr) {
                out.add(new CommitInfo(
                        c.path("id").asText(null),
                        c.path("message").asText(c.path("title").asText(null)),
                        c.path("author_name").asText(null),
                        c.path("authored_date").asText(c.path("created_at").asText(null))));
            }
        }
        return out;
    }

    @Override
    public List<BranchInfo> getBranches(String repo) {
        JsonNode arr = http.get().uri("/api/v4/projects/{id}/repository/branches?per_page=100", pid(repo))
                .retrieve().body(JsonNode.class);
        List<BranchInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode b : arr) {
                out.add(new BranchInfo(
                        b.path("name").asText(null),
                        b.path("commit").path("id").asText(null),
                        b.path("protected").asBoolean(false)));
            }
        }
        return out;
    }

    @Override
    public List<TagInfo> getTags(String repo) {
        JsonNode arr = http.get().uri("/api/v4/projects/{id}/repository/tags?per_page=100", pid(repo))
                .retrieve().body(JsonNode.class);
        List<TagInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode t : arr) {
                out.add(new TagInfo(
                        t.path("name").asText(null),
                        t.path("commit").path("id").asText(null)));
            }
        }
        return out;
    }

    @Override
    public List<PullRequestInfo> getPullRequests(String repo, String state) {
        // GitLab states: opened / closed / merged / all
        String st = mapState(state);
        JsonNode arr = http.get().uri("/api/v4/projects/{id}/merge_requests?state={s}&per_page=100", pid(repo), st)
                .retrieve().body(JsonNode.class);
        List<PullRequestInfo> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode m : arr) {
                out.add(toMr(m));
            }
        }
        return out;
    }

    private static PullRequestInfo toMr(JsonNode m) {
        return new PullRequestInfo(
                m.path("iid").asInt(),
                m.path("title").asText(null),
                m.path("author").path("username").asText(null),
                m.path("state").asText(null),
                m.path("created_at").asText(null),
                m.path("updated_at").asText(null));
    }

    @Override
    public PRInsight analyzePullRequest(String repo, int number) {
        JsonNode m = http.get().uri("/api/v4/projects/{id}/merge_requests/{iid}", pid(repo), number)
                .retrieve().body(JsonNode.class);
        if (m == null) {
            return null;
        }
        JsonNode changes = http.get().uri("/api/v4/projects/{id}/merge_requests/{iid}/changes", pid(repo), number)
                .retrieve().body(JsonNode.class);
        Set<String> packages = new LinkedHashSet<>();
        int changed = 0;
        if (changes != null && changes.path("changes").isArray()) {
            for (JsonNode c : changes.path("changes")) {
                changed++;
                packages.add(topLevelDir(c.path("new_path").asText(c.path("old_path").asText(""))));
            }
        }
        packages.remove("");
        return new PRInsight(
                m.path("iid").asInt(number),
                m.path("title").asText(null),
                m.path("author").path("username").asText(null),
                m.path("state").asText(null),
                changed,
                0, // GitLab MR API does not expose aggregate line stats without diff parsing
                0,
                new ArrayList<>(packages));
    }

    @Override
    public ChangeSet compare(String repo, String baseRef, String headRef) {
        JsonNode n = http.get().uri("/api/v4/projects/{id}/repository/compare?from={from}&to={to}",
                        pid(repo), baseRef, headRef)
                .retrieve().body(JsonNode.class);
        List<String> changedFiles = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int totalCommits = 0;
        if (n != null) {
            JsonNode diffs = n.path("diffs");
            if (diffs.isArray()) {
                for (JsonNode d : diffs) {
                    changedFiles.add(d.path("new_path").asText(d.path("old_path").asText(null)));
                }
            }
            JsonNode commits = n.path("commits");
            if (commits.isArray()) {
                for (JsonNode c : commits) {
                    messages.add(c.path("message").asText(c.path("title").asText(null)));
                }
                totalCommits = commits.size();
            }
        }
        return new ChangeSet(repo, baseRef, headRef, changedFiles, messages, totalCommits);
    }

    private static String mapState(String state) {
        if (state == null || state.isBlank()) {
            return "opened";
        }
        return switch (state.trim().toLowerCase()) {
            case "open", "opened" -> "opened";
            case "closed" -> "closed";
            case "merged" -> "merged";
            default -> "all";
        };
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
