package eu.transplat.aip.jira.service;

import eu.transplat.aip.jira.client.JiraClient;
import eu.transplat.aip.jira.config.JiraProperties;
import eu.transplat.aip.jira.domain.DeliveryState;
import eu.transplat.aip.jira.domain.IssueSummary;
import eu.transplat.aip.jira.domain.SearchResult;
import eu.transplat.aip.jira.domain.Transition;
import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MCP tool surface for Jira. Read tools are always available; write tools are
 * gated by {@code jira.write-enabled}. No method ever lets an exception escape —
 * upstream failures become {@link McpResponse#error} (or {@code stale}), so the
 * server stays healthy even with unreachable / placeholder credentials.
 */
@Service
public class JiraService {

    private static final Logger log = LoggerFactory.getLogger(JiraService.class);
    private static final String SOURCE = "jira-mcp:Jira REST v3";
    private static final Set<String> DONE_STATUSES = Set.of("done", "closed", "resolved", "cancelled", "canceled");

    private final JiraClient client;
    private final JiraProperties properties;

    public JiraService(JiraClient client, JiraProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    // ----------------------------------------------------------------- READ

    @Tool(description = "Search Jira issues with a JQL query. Returns matching issues (key, summary, status, type, assignee, epic, priority, updated). maxResults defaults to 50.")
    public McpResponse searchIssues(String jql, Integer maxResults) {
        int max = maxResults == null || maxResults <= 0 ? 50 : maxResults;
        try {
            SearchResult result = client.search(jql, max);
            return McpResponse.ok(result, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("searchIssues(jql=" + jql + ")", e);
        }
    }

    @Tool(description = "Fetch a single Jira issue by its key (e.g. ARCH-123).")
    public McpResponse getIssue(String issueKey) {
        try {
            IssueSummary issue = client.getIssue(issueKey);
            return McpResponse.ok(issue, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("getIssue(" + issueKey + ")", e);
        }
    }

    @Tool(description = "List all Epics in a Jira project (issuetype = Epic).")
    public McpResponse getEpics(String projectKey) {
        try {
            String jql = "project = \"" + projectKey + "\" AND issuetype = Epic ORDER BY updated DESC";
            SearchResult result = client.search(jql, 100);
            return McpResponse.ok(result, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("getEpics(" + projectKey + ")", e);
        }
    }

    @Tool(description = "List the child issues that belong to a given Epic, by Epic key.")
    public McpResponse getIssuesForEpic(String epicKey) {
        try {
            String jql = "parent = \"" + epicKey + "\" ORDER BY updated DESC";
            SearchResult result = client.search(jql, 200);
            return McpResponse.ok(result, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("getIssuesForEpic(" + epicKey + ")", e);
        }
    }

    @Tool(description = "List the available workflow transitions for a Jira issue (id, name, target status).")
    public McpResponse getTransitions(String issueKey) {
        try {
            List<Transition> transitions = client.getTransitions(issueKey);
            return McpResponse.ok(transitions, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("getTransitions(" + issueKey + ")", e);
        }
    }

    // ---------------------------------------------------------------- WRITE

    @Tool(description = "Apply a workflow transition to a Jira issue. WRITE operation; requires jira.write-enabled=true. Use getTransitions to find the transition id.")
    public McpResponse transitionIssue(String issueKey, String transitionId) {
        McpResponse blocked = guardWrite("transitionIssue");
        if (blocked != null) {
            return blocked;
        }
        try {
            client.transitionIssue(issueKey, transitionId);
            return McpResponse.ok(Map.of("issueKey", issueKey, "transitionId", transitionId, "applied", true),
                    SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("transitionIssue(" + issueKey + ")", e);
        }
    }

    @Tool(description = "Create a new Jira issue. WRITE operation; requires jira.write-enabled=true. issueType is a name such as Story, Task, Bug or Epic.")
    public McpResponse createIssue(String projectKey, String issueType, String summary, String description) {
        McpResponse blocked = guardWrite("createIssue");
        if (blocked != null) {
            return blocked;
        }
        try {
            String key = client.createIssue(projectKey, issueType, summary, description);
            return McpResponse.ok(Map.of("key", key, "created", true), SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("createIssue(" + projectKey + ")", e);
        }
    }

    @Tool(description = "Update fields of a Jira issue. WRITE operation; requires jira.write-enabled=true. fieldsJson is a raw Jira 'fields' JSON object, e.g. {\"summary\":\"new title\"}.")
    public McpResponse updateIssue(String issueKey, String fieldsJson) {
        McpResponse blocked = guardWrite("updateIssue");
        if (blocked != null) {
            return blocked;
        }
        try {
            client.updateIssue(issueKey, fieldsJson);
            return McpResponse.ok(Map.of("issueKey", issueKey, "updated", true), SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("updateIssue(" + issueKey + ")", e);
        }
    }

    @Tool(description = "Add a comment to a Jira issue. WRITE operation; requires jira.write-enabled=true.")
    public McpResponse addComment(String issueKey, String body) {
        McpResponse blocked = guardWrite("addComment");
        if (blocked != null) {
            return blocked;
        }
        try {
            client.addComment(issueKey, body);
            return McpResponse.ok(Map.of("issueKey", issueKey, "commented", true), SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("addComment(" + issueKey + ")", e);
        }
    }

    @Tool(description = "Create a link between two Jira issues. WRITE operation; requires jira.write-enabled=true. linkType is a name such as 'Blocks' or 'Relates'.")
    public McpResponse linkIssues(String inwardKey, String outwardKey, String linkType) {
        McpResponse blocked = guardWrite("linkIssues");
        if (blocked != null) {
            return blocked;
        }
        try {
            client.linkIssues(inwardKey, outwardKey, linkType);
            return McpResponse.ok(
                    Map.of("inwardKey", inwardKey, "outwardKey", outwardKey, "linkType", linkType, "linked", true),
                    SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            return fail("linkIssues(" + inwardKey + "->" + outwardKey + ")", e);
        }
    }

    // ------------------------------------------------------- DELIVERY_STATE

    @Tool(description = "DELIVERY_STATE slice for the digital twin: for each configured Jira project, epic count, issue counts by status, and the list of open epics. Returns {projects:[{key, epicCount, issueCountByStatus, openEpics}], generatedAt}.")
    public McpResponse getState() {
        List<String> projectKeys = properties.getProjectKeys();
        if (projectKeys == null || projectKeys.isEmpty()) {
            return McpResponse.ok(new DeliveryState(List.of(), Instant.now()),
                    SOURCE, Confidence.MEDIUM);
        }

        List<DeliveryState.ProjectState> projects = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (String rawKey : projectKeys) {
            String key = rawKey == null ? "" : rawKey.trim();
            if (key.isEmpty()) {
                continue;
            }
            try {
                projects.add(buildProjectState(key));
            } catch (Exception e) {
                log.warn("getState: failed to build state for project {}: {}", key, e.toString());
                failures.add(key);
            }
        }

        DeliveryState state = new DeliveryState(projects, Instant.now());
        if (!failures.isEmpty()) {
            return McpResponse.stale(state, SOURCE,
                    "Partial DELIVERY_STATE: could not query projects " + failures);
        }
        return McpResponse.ok(state, SOURCE, Confidence.HIGH);
    }

    private DeliveryState.ProjectState buildProjectState(String projectKey) {
        SearchResult epics = client.search(
                "project = \"" + projectKey + "\" AND issuetype = Epic ORDER BY updated DESC", 100);
        SearchResult issues = client.search(
                "project = \"" + projectKey + "\" ORDER BY updated DESC", 500);

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (IssueSummary issue : issues.issues()) {
            String status = issue.status() == null ? "Unknown" : issue.status();
            byStatus.merge(status, 1, Integer::sum);
        }

        List<IssueSummary> openEpics = new ArrayList<>();
        for (IssueSummary epic : epics.issues()) {
            if (!isDone(epic.status())) {
                openEpics.add(epic);
            }
        }

        return new DeliveryState.ProjectState(projectKey, epics.issues().size(), byStatus, openEpics);
    }

    private static boolean isDone(String status) {
        return status != null && DONE_STATUSES.contains(status.toLowerCase());
    }

    // ------------------------------------------------------------- helpers

    /** Returns an error response when writes are disabled, otherwise null. */
    private McpResponse guardWrite(String operation) {
        if (!properties.isWriteEnabled()) {
            return McpResponse.error(SOURCE,
                    "Write operation '" + operation + "' is disabled. Set jira.write-enabled=true to allow it.");
        }
        return null;
    }

    private McpResponse fail(String operation, Exception e) {
        log.warn("Jira {} failed: {}", operation, e.toString());
        return McpResponse.error(SOURCE, operation + " failed: " + e.getMessage());
    }
}
