package eu.transplat.aip.sonar.domain;

import java.util.List;

/**
 * Quality-gate status for a project (from {@code /api/qualitygates/project_status}).
 *
 * @param projectKey project key
 * @param status     overall gate status, e.g. OK / ERROR / NONE
 * @param conditions failing/relevant conditions
 */
public record QualityGate(
        String projectKey,
        String status,
        List<GateCondition> conditions) {

    /**
     * A single quality-gate condition.
     *
     * @param metricKey      metric, e.g. "new_coverage"
     * @param comparator     comparison operator, e.g. "LT"
     * @param errorThreshold configured error threshold
     * @param actualValue    actual measured value
     * @param status         condition status, e.g. OK / ERROR
     */
    public record GateCondition(
            String metricKey,
            String comparator,
            String errorThreshold,
            String actualValue,
            String status) {
    }
}
