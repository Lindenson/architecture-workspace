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
   - `jqassistant-mcp` → `GET /api/jqassistant/state` → **ARCHITECTURE_GRAPH**
     (label counts, dependency count, cycle count, layering-violation count)
   - `structurizr-mcp` → `GET /api/structurizr/state` → **ARCHITECTURE_MODEL**
     (C4 workspace: systems, containers, components, relationships, views, parsedOk)
   Each call uses `Authorization: Bearer <internal-token>` and parses the
   downstream `McpResponse` into a `DownstreamSlice`.
2. **ARCHITECTURE_STATE** is now **live** (MVP-2): the jQAssistant graph summary
   and the Structurizr C4 model summary are combined into one `ArchitectureSlice`
   sub-state, each retaining its own status/provenance. **KNOWLEDGE_STATE**
   (Wiki / RAG) is still MVP-3 and emitted as a `DATA_STALE` placeholder.
3. Merge the slices into `DigitalTwinModel`, each sub-state keeping its own
   `status` + `source` provenance.
4. Derive a short **recommendations** list from simple rules (e.g. Sonar
   `worstGate == ERROR` → "Quality Gate failing"; open blocking issues → note;
   jQAssistant `cycleCount > 0` → "N dependency cycles detected";
   `layeringViolationCount > 0` → "layering violations detected"; Structurizr
   `parsedOk == false` → "C4 model failed to parse/validate").
5. Compute **overall confidence** (resilience rule below) and the list of
   `staleSources`.

### Resilience / confidence rule

No tool ever throws. If a downstream is unreachable its slice is marked
`DATA_STALE` and overall confidence drops:

- **Critical** slices = **delivery (jira-mcp)** + **quality/debt (sonar-mcp)**.
- **Non-critical** slices = **code (github-mcp)** + **architecture
  (jqassistant-mcp graph + structurizr-mcp C4 model)**.
- **LOW** — any *critical* slice is stale.
- **MEDIUM** — only *non-critical* slices are stale (incl. architecture).
- **HIGH** — every live slice is OK.

Architecture sources are NON-CRITICAL for MVP-2: their being down contributes to
MEDIUM and to `staleSources` but never forces LOW. The knowledge (Wiki/RAG)
placeholder is MVP-3 and never affects confidence. A single downstream being
down never fails the whole call.

## Tools (MCP) / endpoints (REST)

| MCP `@Tool`                | REST endpoint                          | Purpose |
|----------------------------|----------------------------------------|---------|
| `showProjectState()`       | `GET /api/twin/state`                  | Merged DIGITAL_TWIN_MODEL + recommendations |
| `analyzeTechDebt()`        | `GET /api/twin/tech-debt`              | TECH_DEBT_REPORT (total debt, by project, worst gate) |
| `analyzeReleaseReadiness()`| `GET /api/twin/release-readiness`      | READY / READY_WITH_RISKS / NOT_READY + reasons |
| `runArchitectureRescan()`  | —                                      | **Real scan (MVP-2)**: pulls jQAssistant `/state` + `/cycles` + `/violations` and Structurizr `/state` + `/validate`, assembles an `ARCHITECTURE_SNAPSHOT` (graph, cycles, layeringViolations, model, validation, driftSignals). HIGH if both servers OK; MEDIUM if one stale; DATA_STALE only if both unreachable |
| `generateReport(type)`     | `GET /api/twin/report?type=DAILY`      | Markdown report; type ∈ {DAILY,WEEKLY,ARCHITECTURE,TECH_DEBT,RELEASE}. ARCHITECTURE enriched with the rescan snapshot |
| `updateKnowledgeBase()`    | —                                      | RAG refresh (planned MVP-3); returns DATA_STALE |

### runArchitectureRescan / ARCHITECTURE_SNAPSHOT

`runArchitectureRescan()` is now a **real scan** of the architecture layer:

- jQAssistant (code-side truth): `/api/jqassistant/state` (graph summary),
  `/api/jqassistant/cycles`, `/api/jqassistant/violations`.
- Structurizr (intended design): `/api/structurizr/state` (C4 model summary),
  `/api/structurizr/validate`.

It assembles `{graph, cycles, layeringViolations, model, validation,
driftSignals, generatedAt}`. **driftSignals** are best-effort, high-level:
cycles and layering violations as code-side drift, C4 parse failure as model
drift, and a coarse "graph has N packages vs C4 has M containers/components"
note. Full component-level drift (name matching, via structurizr-mcp's
`detectDrift`) is future work. The scan never throws.

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
