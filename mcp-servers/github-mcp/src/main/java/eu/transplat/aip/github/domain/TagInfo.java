package eu.transplat.aip.github.domain;

/**
 * A tag reference.
 *
 * @param name      tag name
 * @param commitSha SHA the tag points to, may be {@code null}
 */
public record TagInfo(
        String name,
        String commitSha) {
}
