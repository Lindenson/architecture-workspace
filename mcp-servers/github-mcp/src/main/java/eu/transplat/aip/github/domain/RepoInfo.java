package eu.transplat.aip.github.domain;

/**
 * Repository metadata slice — the key fields the digital twin cares about.
 *
 * @param name          repository name ({@code owner/name} or path)
 * @param defaultBranch  default branch (e.g. {@code main})
 * @param language       primary language, may be {@code null}
 * @param visibility     {@code public} / {@code private} / {@code internal}
 * @param pushedAt       ISO-8601 timestamp of the last push, may be {@code null}
 */
public record RepoInfo(
        String name,
        String defaultBranch,
        String language,
        String visibility,
        String pushedAt) {
}
