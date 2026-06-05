# Contributing

This repository is an **architecture workspace** (a governance layer + a digital
twin of a software system), not a typical application. Contributions therefore
fall into two tracks with different rules.

## 1. Governance artifacts (ADRs, constraints, standards, roadmap, domain model)

These encode **decisions and intent**. They change via **proposal → review →
approval by the architect**, never directly.

- Draft proposals go under the matching `drafts/` folder:
  - `architecture/adr/drafts/`
  - `knowledge/drafts/`
  - `quality/technical-debt/drafts/`
- Use the templates (`_TEMPLATE.md`) and keep the source-of-truth hierarchy:
  **Code > architecture scan + ArchUnit > Structurizr > Sonar > ADR > Jira > Wiki > notes.**
- The Claude agent may author drafts, but it **must not self-approve** restricted
  changes (changing/deleting ADRs, constraints, standards, roadmap; closing tech
  debt; promoting a Structurizr model as "approved"). See
  [`.claude/ROLE_ARCHITECT_AGENT.md`](.claude/ROLE_ARCHITECT_AGENT.md).

## 2. Code (the MCP servers, ArchUnit module, automation)

Standard open-source flow:

1. Fork & branch from `main` (`feat/…`, `fix/…`).
2. Keep modules small and layered (`api` / `service` / `client` / `domain` / `config`).
3. Conventions:
   - Java 21, Spring Boot 3.4, built from `mcp-servers/pom.xml`.
   - Every MCP tool returns the `McpResponse` shape (`data`, `status`, `source`, `confidence`).
   - **Never hardcode secrets** — read from env / `config/*.config.yml`.
   - A tool/endpoint must never throw out to the caller; degrade to
     `error` / `stale` responses.
4. Verify before opening a PR:
   ```bash
   cd mcp-servers && mvn -DskipTests package && mvn test
   ```
5. Open a PR with a clear description; link related ADRs / issues.

## Commit messages

Use imperative mood and a short scope, e.g.
`feat(sonar-mcp): add hotspot summary to getState`.

## Security

Never commit a real token, `.env`, or `config/*.config.yml`. If you find a leaked
secret, rotate it on the provider side and report it privately. See the
[Security](README.md#-security) section.

## Code of conduct

Be respectful and constructive. Assume good intent.
