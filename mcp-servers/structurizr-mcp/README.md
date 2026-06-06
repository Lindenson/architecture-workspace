# structurizr-mcp

Spring Boot **MCP server** that reads and validates the **Structurizr C4 model**
— a `workspace.dsl` file (default `architecture/c4/workspace.dsl`, MVP-2) — and
exposes containers, components, relationships, views, model validation and a
**code-drift hook** to the AIP Claude architect agent / digital-twin
orchestrator over the Spring AI MCP protocol, with a REST mirror under
`/api/structurizr`.

This is the **ARCHITECTURE_MODEL** source. Per the workspace source-of-truth
hierarchy, **code is authoritative** (Git → jQAssistant/ArchUnit → Structurizr).
The DSL should be **regenerated from code** (jQAssistant → Structurizr) and any
divergence surfaced via `detectDrift` rather than hand-edited as ground truth.

Part of the `eu.transplat.aip:mcp-servers` reactor. Returns the canonical
`McpResponse` (`data` / `status` / `source` / `confidence`) on every call, with
`source = "structurizr-mcp:workspace.dsl"`.

The DSL is parsed **lazily, per request** — never at startup — so the server
boots even when the workspace file is missing or invalid. Parse / missing-file
failures come back as `ERROR` / `DATA_STALE`, never as an exception.

## Tools (MCP `@Tool`)

| Tool | Args | Returns |
|------|------|---------|
| `readWorkspace` | — | Workspace summary: name, description, people, software systems with containers/components, relationship & view counts |
| `validateModel` | — | `{valid, errors, warnings}` — parse result plus best-effort sanity warnings |
| `listElements` | `type` | Elements of `person` / `system` / `container` / `component` with parent + technology |
| `listRelationships` | — | Relationships `{source, destination, description, technology}` |
| `getViews` | — | Views `{key, type, title}` |
| `detectDrift` | `actualComponentsCsv` | `{inModelNotInCode, inCodeNotInModel, matched}` — heuristic, case-insensitive name comparison vs. the model |
| `getState` | — | ARCHITECTURE_MODEL slice `{workspaceName, systems, containers, components, relationships, views, parsedOk, workspacePath}` (digital-twin contract) |

`validateModel` reports a parse failure as `{valid:false, errors:[…]}` (an OK
response with a structured result), not a tool error. Sanity warnings include
containers with no components and elements with no relationships.

`detectDrift` is a **heuristic** (case-insensitive contains matching) and
returns at `MEDIUM` confidence. Feed it the actual component/package names
observed in the code (e.g. from `jqassistant-mcp`). With no input it returns
`DATA_STALE` asking for the actual components.

## REST endpoints

| Method | Path | Maps to |
|--------|------|---------|
| GET | `/api/structurizr/state` | `getState()` |
| GET | `/api/structurizr/workspace` | `readWorkspace()` |
| GET | `/api/structurizr/validate` | `validateModel()` |
| GET | `/api/structurizr/views` | `getViews()` |
| GET | `/api/structurizr/elements?type=container` | `listElements(type)` |
| GET | `/api/structurizr/relationships` | `listRelationships()` |

Health/info: `/actuator/health`, `/actuator/info`.

## Configuration / env vars

Settings are supplied via env vars or an optional config file
(`${AIP_CONFIG_DIR:./config}/structurizr.config.yml` or
`../config/structurizr.config.yml`). Never hardcoded. File-based DSL parsing is
the primary mechanism for MVP-2; the Structurizr API settings are optional and
reserved for a later cloud/on-prem integration.

| Env var | Maps to | Default |
|---------|---------|---------|
| `STRUCTURIZR_WORKSPACE_PATH` | `structurizr.workspace-path` | `../architecture/c4/workspace.dsl` |
| `STRUCTURIZR_API_URL` | `structurizr.api.url` | _(blank)_ |
| `STRUCTURIZR_API_KEY` | `structurizr.api.key` | _(blank)_ |
| `STRUCTURIZR_API_SECRET` | `structurizr.api.secret` | _(blank)_ |
| `AIP_INTERNAL_TOKEN` | `aip.security.internal-token` | _(blank → auth off)_ |
| `AIP_CONFIG_DIR` | config import dir | `./config` |

Server port: **8084**.

## Build & run

```bash
# built by the mcp-servers reactor
java -jar target/structurizr-mcp.jar
```

Parsing uses `com.structurizr:structurizr-dsl` **5.0.3** (latest stable on Maven
Central; not BOM-managed, so pinned in `pom.xml`). `structurizr-core` comes in
transitively.
