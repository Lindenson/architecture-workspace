package eu.transplat.aip.jira.domain;

/**
 * An available workflow transition for an issue.
 *
 * @param id     transition id (used by {@code transitionIssue})
 * @param name   transition name, e.g. "Start Progress"
 * @param toName target status name
 */
public record Transition(String id, String name, String toName) {
}
