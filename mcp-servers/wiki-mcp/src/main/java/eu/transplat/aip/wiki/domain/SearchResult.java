package eu.transplat.aip.wiki.domain;

import java.util.List;

/**
 * Result of a Confluence content search.
 *
 * @param size  number of pages returned on this page of results
 * @param limit page size requested
 * @param pages flattened page summaries
 */
public record SearchResult(int size, int limit, List<PageSummary> pages) {
}
