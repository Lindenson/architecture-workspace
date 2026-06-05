# Config — credentials & connection settings

**No real secret ever lives in git.** This directory ships only `*.example.*`
templates. Real files (`jira.config.yml`, `github.config.yml`, …) are matched by
`.gitignore` and stay local.

## Two ways to supply secrets

1. **`.env` (recommended, used by `.mcp.json` and `docker-compose`)**
   Copy the repository-root `.env.example` to `.env` and fill it in.
   Every MCP server reads its credentials from these environment variables.

2. **Per-server YAML overrides (optional)**
   Each Spring Boot MCP server does, at startup:
   ```
   spring.config.import=optional:file:./config/<server>.config.yml
   ```
   So if you prefer files over env vars, copy the matching
   `*.config.example.yml` to `*.config.yml` and edit it. Values here override
   the env-var defaults.

## Files

| Template                          | Real file (gitignored)      | Used by            |
|-----------------------------------|-----------------------------|--------------------|
| `jira.config.example.yml`         | `jira.config.yml`           | jira-mcp           |
| `github.config.example.yml`       | `github.config.yml`         | github-mcp         |
| `sonar.config.example.yml`        | `sonar.config.yml`          | sonar-mcp          |
| `digital-twin.config.example.yml` | `digital-twin.config.yml`   | digital-twin-core  |
| `postgres.config.example.yml`     | `postgres.config.yml`       | digital-twin, rag  |

## Rotation

Tokens are personal access tokens / API tokens. Rotate them on the provider side
and update `.env` (or the local YAML). Never paste a token into a committed file,
an ADR, a report, or a chat message.
