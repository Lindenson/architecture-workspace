package eu.transplat.aip.twin.domain;

import java.util.List;

/**
 * Release-readiness verdict combining the Sonar quality gate and Jira delivery
 * signals.
 *
 * @param readiness READY / READY_WITH_RISKS / NOT_READY
 * @param worstGate worst quality-gate status observed
 * @param reasons   human-readable reasons backing the verdict
 */
public record ReleaseReadiness(
        Readiness readiness,
        String worstGate,
        List<String> reasons) {

    /** Release-readiness verdict levels. */
    public enum Readiness {
        READY,
        READY_WITH_RISKS,
        NOT_READY
    }
}
