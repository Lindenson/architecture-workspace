package eu.transplat.aip.sonar.domain;

/**
 * Combined project report: gate + headline measures + counts + ratings.
 *
 * @param projectKey             project key
 * @param gateStatus             quality-gate status, e.g. OK / ERROR
 * @param bugs                   open/total bug count
 * @param vulnerabilities        vulnerability count
 * @param codeSmells             code-smell count
 * @param coveragePct            coverage percentage
 * @param duplicatedLinesDensity duplicated-lines density %
 * @param ncloc                  non-comment lines of code
 * @param debtMinutes            technical debt in minutes ({@code sqale_index})
 * @param debtHuman              human-readable debt, e.g. "3d 5h"
 * @param reliabilityRating      reliability rating, e.g. "A".."E"
 * @param securityRating         security rating, e.g. "A".."E"
 * @param maintainabilityRating  maintainability rating ({@code sqale_rating}), e.g. "A".."E"
 */
public record SonarReport(
        String projectKey,
        String gateStatus,
        long bugs,
        long vulnerabilities,
        long codeSmells,
        Double coveragePct,
        Double duplicatedLinesDensity,
        Long ncloc,
        long debtMinutes,
        String debtHuman,
        String reliabilityRating,
        String securityRating,
        String maintainabilityRating) {
}
