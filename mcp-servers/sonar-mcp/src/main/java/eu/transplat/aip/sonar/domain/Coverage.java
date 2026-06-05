package eu.transplat.aip.sonar.domain;

/**
 * Coverage and size measures (from {@code /api/measures/component}).
 *
 * @param projectKey            project key
 * @param coveragePct           overall coverage percentage ({@code coverage})
 * @param ncloc                 non-comment lines of code ({@code ncloc})
 * @param duplicatedLinesDensity duplicated-lines density % ({@code duplicated_lines_density})
 */
public record Coverage(
        String projectKey,
        Double coveragePct,
        Long ncloc,
        Double duplicatedLinesDensity) {
}
