# automation — scan & pipeline scripts

Bash automation for the AIP MVP-1 layer. All scripts:

- start with `#!/usr/bin/env bash` and `set -euo pipefail`;
- source the repo-root `.env` (if present) via `lib.sh`;
- authenticate to the MCP servers with `Authorization: Bearer $AIP_INTERNAL_TOKEN`;
- **degrade gracefully** — a missing `curl`/`jq`/`java`, or an unreachable server,
  is logged and handled (recorded as `DATA_STALE`), it does not crash the run.

## Scripts

| Script                  | What it does |
|-------------------------|--------------|
| `lib.sh`                | Shared helpers: `.env` loading, base-URL resolution, `aip_get` (bearer + timeout), `require_cmd`, logging, `REPORTS_DIR`. Sourced by the others; not run directly. |
| `daily-scan.sh`         | Daily Workflow: fetch `GET /api/twin/state` (fallback: per-source `/api/<svc>/state`), generate `GET /api/twin/report?type=DAILY`, write `reports/daily/<DATE>.md`, append a line to `reports/daily/INDEX.md`. Unreachable sources → `DATA_STALE` note in the report. |
| `nightly-pipeline.sh`   | Nightly chain, each stage isolated (failure logs & continues): (1) jQAssistant scan [MVP-2, skipped unless `jqassistant` on PATH]; (2) Sonar refresh via `sonar-mcp /api/sonar/state`; (3) Structurizr update [MVP-2]; (4) RAG reindex [MVP-3]; (5) reports (DAILY nightly, WEEKLY on Sundays). |
| `run-mcp-servers.sh`    | Start the 4 MVP-1 jars in the background (`java -jar mcp-servers/<m>/target/<m>.jar`); PIDs → `automation/.pids`, logs → `automation/logs/`. Checks jars exist first. |
| `stop-mcp-servers.sh`   | Stop the processes recorded in `automation/.pids` (TERM, then KILL stragglers). |
| `health-check.sh`       | Curl `/actuator/health` on 8080-8083, print an UP/DOWN table. Non-zero exit if any DOWN. |

`.pids` and `logs/` are runtime artifacts created by `run-mcp-servers.sh`.

## Prerequisites

Either run the jars locally **or** via Docker:

```bash
# Local jars
cd mcp-servers && mvn -DskipTests package
automation/run-mcp-servers.sh
automation/health-check.sh

# OR containerized (from repo root)
docker compose up -d
```

Tooling: `bash`, `curl` (required for the HTTP scripts), `jq` (optional — used to
pull the markdown out of the `McpResponse.data` field; without it the raw JSON
body is written), `java` 21 (for `run-mcp-servers.sh`).

Secrets come from the repo-root `.env` (copy from `.env.example`). Never hardcode
tokens — see [`../config/README.md`](../config/README.md).

## Cron suggestions

```cron
# m  h  dom mon dow  command  (cd to repo root so .env / relative paths resolve)
  0  7  *   *   *    cd /path/to/architecture-workspace && automation/daily-scan.sh      >> automation/logs/daily-scan.log 2>&1
  0  2  *   *   *    cd /path/to/architecture-workspace && automation/nightly-pipeline.sh >> automation/logs/nightly.log     2>&1
```

`daily-scan` at 07:00 (ready before the workday); `nightly-pipeline` at 02:00
(heavy refresh overnight; WEEKLY report auto-runs on Sundays).
