# jira-mcp

Spring Boot **MCP server** that exposes Jira (Atlassian Cloud) data to the AIP
digital twin. It speaks the Spring AI MCP protocol (SYNC, WebMVC + SSE) and also
mirrors the read operations as REST endpoints. Every tool and endpoint returns
the canonical `McpResponse` shape (`data`, `status`, `source`, `confidence`,
`message`, `producedAt`) from `mcp-common`.

It is part of the `mcp-servers` Maven reactor; build it from the repo root.

## What it does

- Reads issues, epics, transitions and a per-project **DELIVERY_STATE** slice
  that the digital-twin orchestrator consumes.
- Optionally performs write operations (create/update/transition/comment/link),
  gated by a config flag so the server is **read-only by default**.
- Never lets an upstream failure escape: errors become `McpResponse.error` /
  `.stale`, so the server starts and stays healthy even with unreachable or
  placeholder credentials.

## MCP tools

Read (always available):

| Tool | Description |
| --- | --- |
| `searchIssues(jql, maxResults)` | JQL search (maxResults default 50). |
| `getIssue(issueKey)` | Single issue by key. |
| `getEpics(projectKey)` | Epics in a project. |
| `getIssuesForEpic(epicKey)` | Child issues of an epic. |
| `getTransitions(issueKey)` | Available workflow transitions. |
| `getState()` | DELIVERY_STATE slice for the configured projects. |

Write (require `jira.write-enabled=true`, else return `McpResponse.error`):

| Tool | Description |
| --- | --- |
| `transitionIssue(issueKey, transitionId)` | Apply a workflow transition. |
| `createIssue(projectKey, issueType, summary, description)` | Create an issue. |
| `updateIssue(issueKey, fieldsJson)` | Update fields (raw Jira `fields` JSON). |
| `addComment(issueKey, body)` | Add a comment. |
| `linkIssues(inwardKey, outwardKey, linkType)` | Link two issues. |

## REST endpoints (mirror of read tools)

Protected by the shared internal-token auth filter (`mcp-common`).

- `GET /api/jira/state`
- `GET /api/jira/issues?jql=..&max=..`
- `GET /api/jira/issue/{key}`
- `GET /api/jira/epics?project=..`

Actuator: `GET /actuator/health`, `GET /actuator/info` (health is unauthenticated).

## Configuration (environment variables)

| Env var | Maps to | Default | Notes |
| --- | --- | --- | --- |
| `JIRA_BASE_URL` | `jira.base-url` | `https://your-org.atlassian.net` | Atlassian Cloud base URL. |
| `JIRA_EMAIL` | `jira.email` | _(blank)_ | Basic-auth username. |
| `JIRA_API_TOKEN` | `jira.api-token` | _(blank)_ | Atlassian API token (Basic-auth password). |
| `JIRA_PROJECT_KEYS` | `jira.project-keys` | _(blank)_ | Comma-separated project keys for `getState`. |
| `JIRA_WRITE_ENABLED` | `jira.write-enabled` | `false` | Master switch for write tools. |
| `AIP_INTERNAL_TOKEN` | `aip.security.internal-token` | _(blank)_ | Shared bearer token; blank disables auth (dev only). |
| `AIP_CONFIG_DIR` | config import dir | `./config` | Optional `jira.config.yml` override location. |

Secrets are read only from config / env — never hardcoded. An optional
`jira.config.yml` (under `AIP_CONFIG_DIR` or `../config`) can supply them too.

Server port: **8081**.

## Build & run

```bash
# from the repo root (reactor build)
mvn -pl mcp-servers/jira-mcp -am package

# run
java -jar target/jira-mcp.jar
```
