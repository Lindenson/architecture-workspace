package eu.transplat.aip.wiki.domain;

/**
 * A single Confluence page with its full storage-format body.
 *
 * @param id          page id
 * @param title       page title
 * @param spaceKey    space key the page lives in, or null
 * @param version     current version number, or null
 * @param url         absolute browser URL to the page, or null
 * @param body        body in Confluence "storage" (XHTML) representation, or null
 */
public record PageContent(
        String id,
        String title,
        String spaceKey,
        Integer version,
        String url,
        String body) {
}
