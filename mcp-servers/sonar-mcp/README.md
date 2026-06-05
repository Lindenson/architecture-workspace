# sonar-mcp

Spring Boot **MCP server** that exposes SonarQube code-quality data (quality
gate, technical debt, code smells, security, coverage) to the AIP digital-twin
orchestrator over the Spring AI MCP protocol, with a REST mirror under
`/api/sonar`.

Part of the `eu.transplat.aip:mcp-servers` reactor. Returns the canonical
`McpResponse` (`data` / `status` / `source` / `confidence`) on every call, with
`source = "sonar-mcp:SonarQube Web API"`.

## Tools (MCP `@Tool`)

| Tool | Args | Returns |
|------|------|---------|
| `qualityGate` | `projectKey` | Gate status (OK/ERROR) + failing conditions |
| `technicalDebt` | `projectKey` | `sqale_index` minutes + rating + human `Xd Yh` string |
| `codeSmells` | `projectKey`, `limit` | Top open code smells (rule, severity, component, message, line) |
| `securityIssues` | `projectKey` | Open vulnerabilities + security-hotspot summary |
| `coverage` | `projectKey` | Coverage %, `ncloc`, `duplicated_lines_density` |
| `fetchReport` | `projectKey` | Combined measures + gate + counts + ratings |
| `getState` | — | QUALITY_STATE/DEBT_STATE slice across all configured project keys (digital-twin contract) |

## REST endpoints

| Method | Path | Maps to |
|--------|------|---------|
| GET | `/api/sonar/state` | `getState()` |
| GET | `/api/sonar/quality-gate?project=KEY` | `qualityGate` |
| GET | `/api/sonar/debt?project=KEY` | `technicalDebt` |
| GET | `/api/sonar/report?project=KEY` | `fetchReport` |

Health/info: `/actuator/health`, `/actuator/info`.

## SonarQube Web API used

- `GET /api/qualitygates/project_status`
- `GET /api/measures/component` (bugs, vulnerabilities, code_smells, coverage,
  sqale_index, reliability_rating, security_rating, sqale_rating, ncloc,
  duplicated_lines_density)
- `GET /api/issues/search` (CODE_SMELL / VULNERABILITY, statuses OPEN,CONFIRMED)
- `GET /api/hotspots/search`

Authentication uses the SonarQube user token as the HTTP Basic **username**
with an empty password (`RestClientFactory.sonarToken`).

## Configuration / env vars

Secrets are supplied via env vars or an optional config file
(`${AIP_CONFIG_DIR:./config}/sonar.config.yml` or `../config/sonar.config.yml`).
Never hardcoded.

| Env var | Maps to | Default |
|---------|---------|---------|
| `SONAR_BASE_URL` | `sonar.base-url` | `https://sonar.your-org.com` |
| `SONAR_TOKEN` | `sonar.token` | _(blank)_ |
| `SONAR_PROJECT_KEYS` | `sonar.project-keys` (comma-separated list) | _(empty)_ |
| `AIP_INTERNAL_TOKEN` | `aip.security.internal-token` | _(blank → auth off)_ |
| `AIP_CONFIG_DIR` | config import dir | `./config` |

Server port: **8083**.

With placeholder/blank credentials the server still starts; tools return an
`ERROR`/`DATA_STALE` `McpResponse` instead of throwing.

## Build & run

```bash
# built by the mcp-servers reactor
java -jar target/sonar-mcp.jar
```
