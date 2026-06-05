package eu.transplat.aip.github.client;

import eu.transplat.aip.github.domain.BranchInfo;
import eu.transplat.aip.github.domain.ChangeSet;
import eu.transplat.aip.github.domain.CommitInfo;
import eu.transplat.aip.github.domain.PRInsight;
import eu.transplat.aip.github.domain.PullRequestInfo;
import eu.transplat.aip.github.domain.RepoInfo;
import eu.transplat.aip.github.domain.TagInfo;

import java.util.List;

/**
 * Provider-agnostic access to a Git hosting service (GitHub REST or GitLab v4).
 * Implementations parse only the key fields into the {@code domain} records.
 *
 * <p>Implementations may throw on transport/parse failures; the service layer
 * is responsible for translating those into {@code McpResponse.error/stale} so
 * that nothing propagates out of a {@code @Tool} method.
 *
 * <p>The {@code repo} argument is the provider-native identifier: {@code owner/name}
 * for GitHub, a project path or numeric id for GitLab.
 */
public interface GitClient {

    /** Human-readable provenance, e.g. {@code "github-mcp:GitHub REST"}. */
    String source();

    RepoInfo getRepository(String repo);

    List<CommitInfo> getCommits(String repo, String branch, int limit);

    List<BranchInfo> getBranches(String repo);

    List<TagInfo> getTags(String repo);

    /** Pull/merge requests in the given state ({@code open}/{@code closed}/{@code all}). */
    List<PullRequestInfo> getPullRequests(String repo, String state);

    /** Deep analysis of a single PR/MR including file/line stats and touched top-level dirs. */
    PRInsight analyzePullRequest(String repo, int number);

    /** Diff base..head: changed file paths and commit messages unique to head. */
    ChangeSet compare(String repo, String baseRef, String headRef);
}
