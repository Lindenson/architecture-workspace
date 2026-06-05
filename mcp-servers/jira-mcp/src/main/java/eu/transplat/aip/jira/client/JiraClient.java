package eu.transplat.aip.jira.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.transplat.aip.jira.config.JiraProperties;
import eu.transplat.aip.jira.domain.IssueSummary;
import eu.transplat.aip.jira.domain.SearchResult;
import eu.transplat.aip.jira.domain.Transition;
import eu.transplat.aip.mcp.common.client.RestClientFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client over the Jira Cloud REST v3 API. Builds an authenticated
 * {@link RestClient} via {@link RestClientFactory#basic} and parses the relevant
 * fields out of the (large) Jira JSON payloads into the {@code domain} records.
 *
 * <p>This class lets exceptions propagate — the {@code service} layer is
 * responsible for turning them into {@code McpResponse.error/stale} so that no
 * exception ever escapes a tool or endpoint.
 */
@Component
public class JiraClient {

    private final JiraProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public JiraClient(JiraProperties properties) {
        this.properties = properties;
    }

    private RestClient client() {
        return RestClientFactory.basic(properties.getBaseUrl(), properties.getEmail(), properties.getApiToken());
    }

    /** GET /rest/api/3/search?jql=...&maxResults=... */
    public SearchResult search(String jql, int maxResults) {
        String body = client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/3/search")
                        .queryParam("jql", jql)
                        .queryParam("maxResults", maxResults)
                        .queryParam("fields", "summary,status,issuetype,assignee,priority,updated,parent")
                        .build())
                .retrieve()
                .body(String.class);
        return parseSearch(body, maxResults);
    }

    /** GET /rest/api/3/issue/{key} */
    public IssueSummary getIssue(String issueKey) {
        String body = client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/3/issue/{key}")
                        .queryParam("fields", "summary,status,issuetype,assignee,priority,updated,parent")
                        .build(issueKey))
                .retrieve()
                .body(String.class);
        try {
            return toIssue(mapper.readTree(body));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse issue " + issueKey, e);
        }
    }

    /** GET /rest/api/3/issue/{key}/transitions */
    public List<Transition> getTransitions(String issueKey) {
        String body = client().get()
                .uri("/rest/api/3/issue/{key}/transitions", issueKey)
                .retrieve()
                .body(String.class);
        List<Transition> result = new ArrayList<>();
        try {
            JsonNode arr = mapper.readTree(body).path("transitions");
            for (JsonNode t : arr) {
                result.add(new Transition(
                        t.path("id").asText(null),
                        t.path("name").asText(null),
                        t.path("to").path("name").asText(null)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse transitions for " + issueKey, e);
        }
        return result;
    }

    /** POST /rest/api/3/issue/{key}/transitions */
    public void transitionIssue(String issueKey, String transitionId) {
        Map<String, Object> payload = Map.of("transition", Map.of("id", transitionId));
        client().post()
                .uri("/rest/api/3/issue/{key}/transitions", issueKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /** POST /rest/api/3/issue — returns the created issue key. */
    public String createIssue(String projectKey, String issueType, String summary, String description) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("project", Map.of("key", projectKey));
        fields.put("issuetype", Map.of("name", issueType));
        fields.put("summary", summary);
        if (description != null && !description.isBlank()) {
            fields.put("description", adf(description));
        }
        Map<String, Object> payload = Map.of("fields", fields);
        String body = client().post()
                .uri("/rest/api/3/issue")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        try {
            return mapper.readTree(body).path("key").asText(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse create-issue response", e);
        }
    }

    /** PUT /rest/api/3/issue/{key} — fieldsJson is a raw Jira "fields" object. */
    public void updateIssue(String issueKey, String fieldsJson) {
        Object fields;
        try {
            fields = mapper.readValue(fieldsJson, Object.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("fieldsJson is not valid JSON", e);
        }
        Map<String, Object> payload = Map.of("fields", fields);
        client().put()
                .uri("/rest/api/3/issue/{key}", issueKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /** POST /rest/api/3/issue/{key}/comment */
    public void addComment(String issueKey, String body) {
        Map<String, Object> payload = Map.of("body", adf(body));
        client().post()
                .uri("/rest/api/3/issue/{key}/comment", issueKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /** POST /rest/api/3/issueLink */
    public void linkIssues(String inwardKey, String outwardKey, String linkType) {
        Map<String, Object> payload = Map.of(
                "type", Map.of("name", linkType),
                "inwardIssue", Map.of("key", inwardKey),
                "outwardIssue", Map.of("key", outwardKey));
        client().post()
                .uri("/rest/api/3/issueLink")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    // --- parsing helpers ---

    private SearchResult parseSearch(String body, int maxResults) {
        List<IssueSummary> issues = new ArrayList<>();
        int total;
        try {
            JsonNode root = mapper.readTree(body);
            total = root.path("total").asInt(0);
            for (JsonNode issue : root.path("issues")) {
                issues.add(toIssue(issue));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Jira search response", e);
        }
        return new SearchResult(total, maxResults, issues);
    }

    private IssueSummary toIssue(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        String assignee = fields.path("assignee").isMissingNode() || fields.path("assignee").isNull()
                ? null : fields.path("assignee").path("displayName").asText(null);
        String epicKey = fields.path("parent").isMissingNode() || fields.path("parent").isNull()
                ? null : fields.path("parent").path("key").asText(null);
        String priority = fields.path("priority").isMissingNode() || fields.path("priority").isNull()
                ? null : fields.path("priority").path("name").asText(null);
        return new IssueSummary(
                issue.path("key").asText(null),
                fields.path("summary").asText(null),
                fields.path("status").path("name").asText(null),
                fields.path("issuetype").path("name").asText(null),
                assignee,
                epicKey,
                priority,
                fields.path("updated").asText(null));
    }

    /** Wraps plain text in a minimal Atlassian Document Format node. */
    private static Map<String, Object> adf(String text) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", text)))));
    }
}
