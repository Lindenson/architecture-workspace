# digital-twin-core

The **orchestrator / brain** of the Architecture Intelligence Platform (AIP).

It fans out to the other AIP MCP servers over plain HTTP REST (their
`/api/<svc>/state` endpoints), merges the returned slices into a single
**DIGITAL_TWIN_MODEL**, and exposes high-level analysis commands as MCP tools
(also mirrored as REST endpoints under `/api/twin`).

Every tool/endpoint returns the canonical `eu.transplat.aip.mcp.common.McpResponse`
(`data` / `status` / `source` / `confidence` / `message` / `producedAt`) with
`source = "digital-twin-core"`.

## SHOW_PROJECT_STATE flow

`showProjectState()` (MCP tool) / `GET /api/twin/state`:

1. Fan out in turn to the **live** downstreams:
   - `jira-mcp` → `GET /api/jira/state` → **DELIVERY_STATE**
   - `github-mcp` → `GET /api/github/state` → **CODE_STATE**
   - `sonar-mcp` → `GET /api/sonar/state` → **QUALITY_STATE + DEBT_STATE**
   Each call uses `Authorization: Bearer <internal-token>` and parses the
   downstream `McpResponse` into a `DownstreamSlice`.
2. **ARCHITECTURE_STATE** (Structurizr / jQAssistant) and **KNOWLEDGE_STATE**
   (Wiki / RAG) are MVP-2+/MVP-3 and not yet running — they are emitted as
   `DATA_STALE` placeholder sub-states ("planned: server not running").
3. Merge the slices into `DigitalTwinModel`, each sub-state keeping its own
   `status` + `source` provenance.
4. Derive a short **recommendations** list from simple rules (e.g. Sonar
   `worstGate == ERROR` → "Quality Gate failing"; open blocking issues → note).
5. Compute **overall confidence** (resilience rule below) and the list of
   `staleSources`.

### Resilience / confidence rule

No tool ever throws. If a downstream is unreachable its slice is marked
`DATA_STALE` and overall confidence drops:

- **LOW** — any *critical* live slice (Sonar quality/debt, Jira delivery) is stale.
- **MEDIUM** — only a *non-critical* live slice (GitHub code) is stale.
- **HIGH** — every live slice is OK.

The always-planned architecture/knowledge placeholders do **not** by themselves
force LOW in MVP-1. A single downstream being down never fails the whole call.

## Tools (MCP) / endpoints (REST)

| MCP `@Tool`                | REST endpoint                          | Purpose |
|----------------------------|----------------------------------------|---------|
| `showProjectState()`       | `GET /api/twin/state`                  | Merged DIGITAL_TWIN_MODEL + recommendations |
| `analyzeTechDebt()`        | `GET /api/twin/tech-debt`              | TECH_DEBT_REPORT (total debt, by project, worst gate) |
| `analyzeReleaseReadiness()`| `GET /api/twin/release-readiness`      | READY / READY_WITH_RISKS / NOT_READY + reasons |
| `runArchitectureRescan()`  | —                                      | Triggers jQAssistant/Structurizr (planned MVP-2+); returns DATA_STALE + available signals |
| `generateReport(type)`     | `GET /api/twin/report?type=DAILY`      | Markdown report; type ∈ {DAILY,WEEKLY,ARCHITECTURE,TECH_DEBT,RELEASE} |
| `updateKnowledgeBase()`    | —                                      | RAG refresh (planned MVP-3); returns DATA_STALE |

Release-readiness rule: **NOT_READY** if any quality gate is ERROR;
**READY_WITH_RISKS** if there are open blocking issues or stale sources; otherwise
**READY**.

## Configuration / environment variables

Bound via `DigitalTwinProperties` (`prefix = digital-twin`). All secrets come
from config/env — never hardcoded.

| Env var                   | Property                              | Default                  |
|---------------------------|---------------------------------------|--------------------------|
| `AIP_INTERNAL_TOKEN`      | `digital-twin.internal-token` / `aip.security.internal-token` | _(blank → auth disabled)_ |
| `PORT_JIRA_URL`           | `digital-twin.downstream.jira-mcp`        | `http://localhost:8081` |
| `PORT_GITHUB_URL`         | `digital-twin.downstream.github-mcp`      | `http://localhost:8082` |
| `PORT_SONAR_URL`          | `digital-twin.downstream.sonar-mcp`       | `http://localhost:8083` |
| `PORT_STRUCTURIZR_URL`    | `digital-twin.downstream.structurizr-mcp` | `http://localhost:8084` |
| `PORT_JQA_URL`            | `digital-twin.downstream.jqassistant-mcp` | `http://localhost:8085` |
| `PORT_WIKI_URL`           | `digital-twin.downstream.wiki-mcp`        | `http://localhost:8086` |
| `PORT_RAG_URL`            | `digital-twin.downstream.rag-mcp`         | `http://localhost:8088` |

Optional config file: `${AIP_CONFIG_DIR:./config}/digital-twin.config.yml`
(also `../config/digital-twin.config.yml`).

Server port: **8080**. Actuator: `health`, `info`.

## Run

```bash
java -jar target/digital-twin-core.jar
```

(A reactor build under `mcp-servers/pom.xml` produces the jar.)
