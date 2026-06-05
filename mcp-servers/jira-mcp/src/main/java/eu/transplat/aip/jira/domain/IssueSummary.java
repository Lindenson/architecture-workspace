package eu.transplat.aip.jira.domain;

/**
 * Flattened view of a Jira issue — only the key fields the digital twin needs.
 *
 * @param key       issue key, e.g. ARCH-123
 * @param summary   short title
 * @param status    workflow status name, e.g. "In Progress"
 * @param issueType issue type name, e.g. "Story", "Epic", "Bug"
 * @param assignee  assignee display name, or null if unassigned
 * @param epicKey   parent epic key, or null
 * @param priority  priority name, or null
 * @param updated   last-updated timestamp (ISO-8601 string from Jira)
 */
public record IssueSummary(
        String key,
        String summary,
        String status,
        String issueType,
        String assignee,
        String epicKey,
        String priority,
        String updated) {
}
