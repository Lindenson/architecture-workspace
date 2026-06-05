package eu.transplat.aip.github.domain;

/**
 * A compact point-in-time snapshot of a repository — the CODE_STATE slice the
 * digital-twin orchestrator consumes.
 *
 * @param repo          {@code owner/name} or path
 * @param defaultBranch default branch
 * @param lastCommit    most recent commit on the default branch, may be {@code null}
 * @param branchCount   number of branches
 * @param tagCount      number of tags
 * @param openPrCount   number of open pull/merge requests
 */
public record RepoSnapshot(
        String repo,
        String defaultBranch,
        CommitInfo lastCommit,
        int branchCount,
        int tagCount,
        int openPrCount) {
}
