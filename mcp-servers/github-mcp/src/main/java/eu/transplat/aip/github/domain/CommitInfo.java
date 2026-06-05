package eu.transplat.aip.github.domain;

/**
 * A single commit, key fields only.
 *
 * @param sha     commit SHA
 * @param message commit message (first line / full, as returned by provider)
 * @param author  author display name or username
 * @param date    authored/committed date, ISO-8601, may be {@code null}
 */
public record CommitInfo(
        String sha,
        String message,
        String author,
        String date) {
}
