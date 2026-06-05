package eu.transplat.aip.jira.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DELIVERY_STATE slice consumed by the digital-twin orchestrator: per configured
 * project, epic and issue counts plus the list of still-open epics.
 *
 * @param projects    per-project delivery snapshots
 * @param generatedAt when this slice was produced
 */
public record DeliveryState(List<ProjectState> projects, Instant generatedAt) {

    /**
     * @param key                project key
     * @param epicCount          number of epics in the project
     * @param issueCountByStatus issue counts keyed by status name
     * @param openEpics          epics not in a terminal/Done status
     */
    public record ProjectState(
            String key,
            int epicCount,
            Map<String, Integer> issueCountByStatus,
            List<IssueSummary> openEpics) {
    }
}
