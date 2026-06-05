package eu.transplat.aip.github.domain;

/**
 * A pull request / merge request summary.
 *
 * @param number    PR/MR number (or iid for GitLab)
 * @param title     title
 * @param author    author username
 * @param state     {@code open} / {@code closed} / {@code merged}
 * @param createdAt ISO-8601 creation timestamp, may be {@code null}
 * @param updatedAt ISO-8601 update timestamp, may be {@code null}
 */
public record PullRequestInfo(
        int number,
        String title,
        String author,
        String state,
        String createdAt,
        String updatedAt) {
}
