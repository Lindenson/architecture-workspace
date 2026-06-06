package eu.transplat.aip.wiki.domain;

/**
 * Flattened view of a Confluence page — only the fields the digital twin needs
 * for search/list results.
 *
 * @param id       page id
 * @param title    page title
 * @param spaceKey space key the page lives in, or null
 * @param version  current version number, or null
 * @param updated  last-updated timestamp (ISO-8601 string from Confluence), or null
 * @param url      absolute browser URL to the page, or null
 * @param excerpt  short text excerpt / snippet, or null
 */
public record PageSummary(
        String id,
        String title,
        String spaceKey,
        Integer version,
        String updated,
        String url,
        String excerpt) {
}
