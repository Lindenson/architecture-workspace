package eu.transplat.aip.github.domain;

import java.util.List;

/**
 * Diff between two refs (base..head): the CHANGESET_STATE slice.
 *
 * @param repo            {@code owner/name} or path
 * @param baseRef         base ref
 * @param headRef         head ref
 * @param changedFiles    paths of files changed between the two refs
 * @param commitMessages  messages of the commits unique to {@code head}
 * @param totalCommits    number of commits between the refs
 */
public record ChangeSet(
        String repo,
        String baseRef,
        String headRef,
        List<String> changedFiles,
        List<String> commitMessages,
        int totalCommits) {
}
