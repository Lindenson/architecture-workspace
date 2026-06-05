package eu.transplat.aip.github.domain;

/**
 * A branch reference.
 *
 * @param name      branch name
 * @param commitSha SHA the branch points to, may be {@code null}
 * @param protected_ whether the branch is protected
 */
public record BranchInfo(
        String name,
        String commitSha,
        boolean protected_) {
}
