# wiki-mcp

Spring Boot **MCP server** that exposes the corporate **Wiki (Confluence Cloud)**
to the AIP digital twin. It speaks the Spring AI MCP protocol (SYNC, WebMVC + SSE)
and also mirrors the read operations as REST endpoints. Every tool and endpoint
returns the canonical `McpResponse` shape (`data`, `status`, `source`,
`confidence`, `message`, `producedAt`) from `mcp-common`.

This is the **KNOWLEDGE_DOCUMENTS** source (MVP-3) and is **optional**: set
`WIKI_ENABLED=false` to turn the whole capability off — every tool then returns a
`McpResponse.disabled(...)` response (status `DISABLED`), which orchestrators
exclude from confidence scoring and stale-source reporting. No network call is
made while disabled.

> Note: your session may already have a generic Atlassian/Confluence MCP. This
> dedicated server exists to serve the AIP digital-twin contract (notably the
> `GET /api/wiki/state` KNOWLEDGE_DOCUMENTS slice), not to replace ad-hoc tools.

It is part of the `mcp-servers` Maven reactor; build it from the repo root.

## What it does

- Searches and reads Confluence pages (text/CQL search, single page with body,
  per-space listing) and produces a per-space **KNOWLEDGE_DOCUMENTS** slice the
  digital-twin orchestrator consumes.
- Optionally updates a page (title / storage body, with automatic version bump),
  gated by a config flag so the server is **read-only by default**.
- Never lets an upstream failure escape: errors become `McpResponse.error` /
  `.stale`, so the server starts and stays healthy even with unreachable or
  placeholder credentials.

Uses the **Confluence Cloud REST API v1** (`/rest/api/content`) — a single stable
surface for CQL search, `body.storage` expansion and version-bumped updates.

## MCP tools

Read (available when `wiki.enabled=true`):

| Tool | Description |
| --- | --- |
| `searchPages(query, limit)` | CQL text search across configured spaces (or all). limit default 25. |
| `getPage(pageId)` | Single page with storage body, version, space, url. |
| `listPages(spaceKey, limit)` | Pages in a space, most-recently-updated first. limit default 25. |
| `readPages()` | Recently-updated pages across all configured spaces (flat summary). |
| `getState()` | KNOWLEDGE_DOCUMENTS slice for the configured spaces. |

Write (require `wiki.write-enabled=true`, else return `McpResponse.error`):

| Tool | Description |
| --- | --- |
| `updatePage(pageId, title, storageBody)` | Update title/body; bumps version. Empty title keeps current. |

When `wiki.enabled=false`, **all** tools return `McpResponse.disabled(...)`.

## REST endpoints (mirror of read tools)

Protected by the shared internal-token auth filter (`mcp-common`).

- `GET /api/wiki/state`
- `GET /api/wiki/search?q=..&limit=..`
- `GET /api/wiki/page/{id}`
- `GET /api/wiki/pages?space=..&limit=..`

Actuator: `GET /actuator/health`, `GET /actuator/info` (health is unauthenticated).

## Configuration (environment variables)

| Env var | Maps to | Default | Notes |
| --- | --- | --- | --- |
| `WIKI_ENABLED` | `wiki.enabled` | `true` | Master switch; `false` disables the whole source. |
| `WIKI_BASE_URL` | `wiki.base-url` | `https://your-org.atlassian.net/wiki` | Confluence Cloud base URL (include `/wiki`). |
| `WIKI_EMAIL` | `wiki.email` | _(blank)_ | Basic-auth username. |
| `WIKI_API_TOKEN` | `wiki.api-token` | _(blank)_ | Atlassian API token (Basic-auth password). |
| `WIKI_SPACE_KEYS` | `wiki.space-keys` | _(blank)_ | Comma-separated space keys for `getState`/`readPages`/search scope. |
| `WIKI_WRITE_ENABLED` | `wiki.write-enabled` | `false` | Master switch for write tools. |
| `AIP_INTERNAL_TOKEN` | `aip.security.internal-token` | _(blank)_ | Shared bearer token; blank disables auth (dev only). |
| `AIP_CONFIG_DIR` | config import dir | `./config` | Optional `wiki.config.yml` override location. |

Secrets are read only from config / env — never hardcoded. An optional
`wiki.config.yml` (under `AIP_CONFIG_DIR` or `../config`) can supply them too.

Server port: **8086**.

## Build & run

```bash
# from the repo root (reactor build)
mvn -pl mcp-servers/wiki-mcp -am package

# run
java -jar target/wiki-mcp.jar
```
