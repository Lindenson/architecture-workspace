package eu.transplat.aip.wiki.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.transplat.aip.mcp.common.client.RestClientFactory;
import eu.transplat.aip.wiki.config.WikiProperties;
import eu.transplat.aip.wiki.domain.PageContent;
import eu.transplat.aip.wiki.domain.PageSummary;
import eu.transplat.aip.wiki.domain.SearchResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client over the Confluence Cloud REST API v1 ({@code /rest/api/content}).
 * Builds an authenticated {@link RestClient} via {@link RestClientFactory#basic}
 * and parses the relevant fields out of the (large) Confluence JSON payloads into
 * the {@code domain} records.
 *
 * <p>v1 is used (rather than v2 {@code /api/v2/pages}) because it offers a single
 * stable surface for CQL search, {@code body.storage} expansion and version-bumped
 * updates — which is the pragmatic choice called for by the MVP.
 *
 * <p>This class lets exceptions propagate — the {@code service} layer is
 * responsible for turning them into {@code McpResponse.error/stale} so that no
 * exception ever escapes a tool or endpoint.
 */
@Component
public class WikiClient {

    private final WikiProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public WikiClient(WikiProperties properties) {
        this.properties = properties;
    }

    private RestClient client() {
        return RestClientFactory.basic(properties.getBaseUrl(), properties.getEmail(), properties.getApiToken());
    }

    /** Browser base for building absolute page URLs (baseUrl minus a trailing /wiki). */
    private String webBase() {
        String base = properties.getBaseUrl();
        return base == null ? "" : base.replaceAll("/+$", "");
    }

    // ------------------------------------------------------------------ READ

    /** GET /rest/api/content/search?cql=...&limit=...&expand=space,version */
    public SearchResult search(String cql, int limit) {
        String body = client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/content/search")
                        .queryParam("cql", cql)
                        .queryParam("limit", limit)
                        .queryParam("expand", "space,version")
                        .build())
                .retrieve()
                .body(String.class);
        return parseSearch(body, limit);
    }

    /** GET /rest/api/content?spaceKey=KEY&type=page&limit=...&expand=space,version */
    public SearchResult listPages(String spaceKey, int limit) {
        String body = client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/content")
                        .queryParam("spaceKey", spaceKey)
                        .queryParam("type", "page")
                        .queryParam("orderby", "history.lastUpdated desc")
                        .queryParam("limit", limit)
                        .queryParam("expand", "space,version")
                        .build())
                .retrieve()
                .body(String.class);
        return parseSearch(body, limit);
    }

    /** GET /rest/api/content/{id}?expand=body.storage,version,space */
    public PageContent getPage(String pageId) {
        String body = client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/content/{id}")
                        .queryParam("expand", "body.storage,version,space")
                        .build(pageId))
                .retrieve()
                .body(String.class);
        try {
            return toPageContent(mapper.readTree(body));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse page " + pageId, e);
        }
    }

    // ----------------------------------------------------------------- WRITE

    /**
     * PUT /rest/api/content/{id} — bumps the version. When {@code title} is blank
     * the current title is preserved (a GET is done to read it and the current
     * version number).
     */
    public PageContent updatePage(String pageId, String title, String storageBody) {
        // Read current page to learn the version number and (if needed) the title.
        PageContent current = getPage(pageId);
        int nextVersion = (current.version() == null ? 0 : current.version()) + 1;
        String effectiveTitle = (title == null || title.isBlank()) ? current.title() : title;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", pageId);
        payload.put("type", "page");
        payload.put("title", effectiveTitle);
        payload.put("version", Map.of("number", nextVersion));
        payload.put("body", Map.of("storage", Map.of(
                "value", storageBody == null ? "" : storageBody,
                "representation", "storage")));

        String body = client().put()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/content/{id}")
                        .queryParam("expand", "body.storage,version,space")
                        .build(pageId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        try {
            return toPageContent(mapper.readTree(body));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse update response for page " + pageId, e);
        }
    }

    // -------------------------------------------------------- parsing helpers

    private SearchResult parseSearch(String body, int limit) {
        List<PageSummary> pages = new ArrayList<>();
        int size;
        try {
            JsonNode root = mapper.readTree(body);
            size = root.path("size").asInt(0);
            for (JsonNode page : root.path("results")) {
                pages.add(toPageSummary(page));
            }
            if (size == 0) {
                size = pages.size();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Confluence search response", e);
        }
        return new SearchResult(size, limit, pages);
    }

    private PageSummary toPageSummary(JsonNode page) {
        String spaceKey = page.path("space").path("key").asText(null);
        Integer version = page.path("version").has("number")
                ? page.path("version").path("number").asInt() : null;
        String updated = page.path("version").path("when").asText(null);
        String excerpt = page.path("excerpt").asText(null);
        return new PageSummary(
                page.path("id").asText(null),
                page.path("title").asText(null),
                spaceKey,
                version,
                updated,
                webUrl(page),
                excerpt);
    }

    private PageContent toPageContent(JsonNode page) {
        String spaceKey = page.path("space").path("key").asText(null);
        Integer version = page.path("version").has("number")
                ? page.path("version").path("number").asInt() : null;
        String storage = page.path("body").path("storage").path("value").asText(null);
        return new PageContent(
                page.path("id").asText(null),
                page.path("title").asText(null),
                spaceKey,
                version,
                webUrl(page),
                storage);
    }

    /** Builds an absolute page URL from {@code _links.webui} (preferred) or the id. */
    private String webUrl(JsonNode page) {
        JsonNode links = page.path("_links");
        String webui = links.path("webui").asText(null);
        if (webui != null && !webui.isBlank()) {
            // _links.base, when present, is the most accurate prefix for webui.
            String base = links.path("base").asText(null);
            if (base != null && !base.isBlank()) {
                return base.replaceAll("/+$", "") + webui;
            }
            return webBase() + webui;
        }
        String id = page.path("id").asText(null);
        return id == null ? null : webBase() + "/pages/viewpage.action?pageId=" + id;
    }
}
