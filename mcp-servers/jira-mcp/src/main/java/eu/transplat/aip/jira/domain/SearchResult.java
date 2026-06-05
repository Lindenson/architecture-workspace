package eu.transplat.aip.jira.domain;

import java.util.List;

/**
 * Result of a JQL search.
 *
 * @param total      total matches reported by Jira
 * @param maxResults page size requested
 * @param issues     flattened issues on this page
 */
public record SearchResult(int total, int maxResults, List<IssueSummary> issues) {
}
