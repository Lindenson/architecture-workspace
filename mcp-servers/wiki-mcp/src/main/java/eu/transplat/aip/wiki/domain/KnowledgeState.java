package eu.transplat.aip.wiki.domain;

import java.time.Instant;
import java.util.List;

/**
 * KNOWLEDGE_DOCUMENTS slice consumed by the digital-twin orchestrator: per
 * configured Confluence space, the page count and the most-recently-updated
 * pages.
 *
 * @param spaces      per-space knowledge snapshots
 * @param generatedAt when this slice was produced
 */
public record KnowledgeState(List<SpaceState> spaces, Instant generatedAt) {

    /**
     * @param key       space key
     * @param pageCount number of pages found in the space (capped by query limit)
     * @param recent    most-recently-updated pages in the space
     */
    public record SpaceState(String key, int pageCount, List<RecentPage> recent) {
    }

    /**
     * @param id      page id
     * @param title   page title
     * @param updated last-updated timestamp (ISO-8601 string from Confluence)
     */
    public record RecentPage(String id, String title, String updated) {
    }
}
