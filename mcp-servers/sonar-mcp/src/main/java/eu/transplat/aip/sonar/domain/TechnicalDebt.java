package eu.transplat.aip.sonar.domain;

/**
 * Technical-debt view derived from the {@code sqale_index} measure.
 *
 * @param projectKey  project key
 * @param debtMinutes remediation effort in minutes ({@code sqale_index})
 * @param debtHuman   human-readable effort, e.g. "3d 5h"
 * @param rating      maintainability rating ({@code sqale_rating}), e.g. "A".."E"
 */
public record TechnicalDebt(
        String projectKey,
        long debtMinutes,
        String debtHuman,
        String rating) {
}
