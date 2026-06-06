package eu.transplat.aip.wiki.service;

import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.wiki.client.WikiClient;
import eu.transplat.aip.wiki.config.WikiProperties;
import eu.transplat.aip.wiki.domain.KnowledgeState;
import eu.transplat.aip.wiki.domain.PageContent;
import eu.transplat.aip.wiki.domain.PageSummary;
import eu.transplat.aip.wiki.domain.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP tool surface for the corporate Wiki (Confluence Cloud). This is the
 * optional KNOWLEDGE_DOCUMENTS source.
 *
 * <ul>
 *   <li>When {@code wiki.enabled=false} every tool returns
 *       {@link McpResponse#disabled} and no network call is made.</li>
 *   <li>Read tools are always available when enabled.</li>
 *   <li>Write tools are additionally gated by {@code wiki.write-enabled}.</li>
 * </ul>
 *
 * No method ever lets an exception escape — upstream failures become
 * {@link McpResponse#error} (or {@code stale}), so the server stays healthy even
 * with unreachable / placeholder credentials.
 */
@Service
public class WikiService {

    private static final Logger log = LoggerFactory.getLogger(WikiService.class);
    private static final String SOURCE = "wiki-mcp:Confluence REST";

    private final WikiClient client;
    private final WikiProperties properties;

    public WikiService(WikiClient client, WikiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    // ----------------------------------------------------------------- READ

    @Tool(description = "Search Wiki (Confluence) pages by text. Runs a CQL text search, scoped to the configured spaces when any are set, otherwise across all spaces. Returns matching pages (id, title, space, version, updated, url, excerpt). limit defaults to 25.")
    public McpResponse searchPages(String query, Integer limit) {
        McpResponse off = disabledIfOff();
        if (off != null) {
            return off;
        }
        int max = normalizeLimit(limit, 25);
        try {
            SearchResult result = client.search(buildSearchCql(query), max);
            return McpResponse.ok(result, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("searchPages(query=" + query + ")", e);
        }
    }

    @Tool(description = "Fetch a single Wiki (Confluence) page by id, including its body in storage (XHTML) format plus version, space and url.")
    public McpResponse getPage(String pageId) {
        McpResponse off = disabledIfOff();
        if (off != null) {
            return off;
        }
        try {
            PageContent page = client.getPage(pageId);
            return McpResponse.ok(page, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("getPage(" + pageId + ")", e);
        }
    }

    @Tool(description = "List pages in a Wiki (Confluence) space, most-recently-updated first. limit defaults to 25.")
    public McpResponse listPages(String spaceKey, Integer limit) {
        McpResponse off = disabledIfOff();
        if (off != null) {
            return off;
        }
        int max = normalizeLimit(limit, 25);
        try {
            SearchResult result = client.listPages(spaceKey, max);
            return McpResponse.ok(result, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("listPages(" + spaceKey + ")", e);
        }
    }

    @Tool(description = "List recently-updated pages across all configured Wiki (Confluence) spaces. Returns a flat summary list; useful as a quick knowledge overview.")
    public McpResponse readPages() {
        McpResponse off = disabledIfOff();
        if (off != null) {
            return off;
        }
        List<String> spaceKeys = configuredSpaceKeys();
        try {
            if (spaceKeys.isEmpty()) {
                // No configured spaces: recent pages across the whole instance.
                SearchResult result = client.search("type = page ORDER BY lastmodified DESC", 25);
                return McpResponse.ok(result, SOURCE, Confidence.MEDIUM);
            }
            List<PageSummary> pages = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            for (String key : spaceKeys) {
                try {
                    pages.addAll(client.listPages(key, 10).pages());
                } catch (Exception e) {
                    log.warn("readPages: failed for space {}: {}", key, e.toString());
                    failures.add(key);
                }
            }
            SearchResult result = new SearchResult(pages.size(), pages.size(), pages);
            if (!failures.isEmpty()) {
                return McpResponse.stale(result, SOURCE, "Partial result: could not query spaces " + failures);
            }
            return McpResponse.ok(result, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("readPages()", e);
        }
    }

    // ---------------------------------------------------------------- WRITE

    @Tool(description = "Update a Wiki (Confluence) page's title and/or storage-format body. WRITE operation; requires wiki.write-enabled=true. Bumps the page version automatically. Pass an empty title to keep the current one. storageBody must be Confluence 'storage' (XHTML) markup.")
    public McpResponse updatePage(String pageId, String title, String storageBody) {
        McpResponse off = disabledIfOff();
        if (off != null) {
            return off;
        }
        McpResponse blocked = guardWrite("updatePage");
        if (blocked != null) {
            return blocked;
        }
        try {
            PageContent updated = client.updatePage(pageId, title, storageBody);
            return McpResponse.ok(updated, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("updatePage(" + pageId + ")", e);
        }
    }

    // ----------------------------------------------------- KNOWLEDGE_DOCUMENTS

    @Tool(description = "KNOWLEDGE_DOCUMENTS slice for the digital twin: for each configured Wiki space, the page count and the most-recently-updated pages. Returns {spaces:[{key, pageCount, recent:[{id,title,updated}]}], generatedAt}.")
    public McpResponse getState() {
        if (!properties.isEnabled()) {
            return McpResponse.disabled(SOURCE, "Wiki source is disabled (wiki.enabled=false).");
        }
        List<String> spaceKeys = configuredSpaceKeys();
        if (spaceKeys.isEmpty()) {
            return McpResponse.ok(new KnowledgeState(List.of(), Instant.now()), SOURCE, Confidence.MEDIUM);
        }

        List<KnowledgeState.SpaceState> spaces = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (String key : spaceKeys) {
            try {
                spaces.add(buildSpaceState(key));
            } catch (Exception e) {
                log.warn("getState: failed to build state for space {}: {}", key, e.toString());
                failures.add(key);
            }
        }

        KnowledgeState state = new KnowledgeState(spaces, Instant.now());
        if (!failures.isEmpty()) {
            return McpResponse.stale(state, SOURCE,
                    "Partial KNOWLEDGE_DOCUMENTS: could not query spaces " + failures);
        }
        return McpResponse.ok(state, SOURCE, Confidence.HIGH);
    }

    private KnowledgeState.SpaceState buildSpaceState(String spaceKey) {
        SearchResult result = client.listPages(spaceKey, 50);
        List<KnowledgeState.RecentPage> recent = new ArrayList<>();
        for (PageSummary page : result.pages().stream().limit(5).toList()) {
            recent.add(new KnowledgeState.RecentPage(page.id(), page.title(), page.updated()));
        }
        return new KnowledgeState.SpaceState(spaceKey, result.pages().size(), recent);
    }

    // ------------------------------------------------------------- helpers

    /** Returns a disabled response when the whole source is off, otherwise null. */
    private McpResponse disabledIfOff() {
        if (!properties.isEnabled()) {
            return McpResponse.disabled(SOURCE, "Wiki source is disabled (wiki.enabled=false).");
        }
        return null;
    }

    /** Returns an error response when writes are disabled, otherwise null. */
    private McpResponse guardWrite(String operation) {
        if (!properties.isWriteEnabled()) {
            return McpResponse.error(SOURCE,
                    "Write operation '" + operation + "' is disabled. Set wiki.write-enabled=true to allow it.");
        }
        return null;
    }

    /** CQL text search, scoped to configured spaces when any are set. */
    private String buildSearchCql(String query) {
        String safe = query == null ? "" : query.replace("\"", "\\\"");
        StringBuilder cql = new StringBuilder("type = page AND text ~ \"").append(safe).append("\"");
        List<String> spaceKeys = configuredSpaceKeys();
        if (!spaceKeys.isEmpty()) {
            String joined = String.join(",", spaceKeys.stream().map(k -> "\"" + k + "\"").toList());
            cql.append(" AND space IN (").append(joined).append(")");
        }
        cql.append(" ORDER BY lastmodified DESC");
        return cql.toString();
    }

    private List<String> configuredSpaceKeys() {
        List<String> keys = properties.getSpaceKeys();
        if (keys == null) {
            return List.of();
        }
        List<String> trimmed = new ArrayList<>();
        for (String raw : keys) {
            if (raw != null && !raw.isBlank()) {
                trimmed.add(raw.trim());
            }
        }
        return trimmed;
    }

    private static int normalizeLimit(Integer limit, int dflt) {
        return limit == null || limit <= 0 ? dflt : limit;
    }

    private McpResponse fail(String operation, Exception e) {
        log.warn("Wiki {} failed: {}", operation, e.toString());
        return McpResponse.error(SOURCE, operation + " failed: " + e.getMessage());
    }
}
