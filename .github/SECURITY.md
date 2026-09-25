# Security

## What this software can reach

An AIP deployment holds credentials for Jira, GitHub/GitLab, SonarQube,
Confluence, Neo4j and Postgres, and exposes them to an LLM agent through eight
HTTP servers. Treat it as a system with the union of those privileges, not as a
documentation tool.

## The three controls, and what each actually guarantees

**Internal token.** Every server requires `Authorization: Bearer
$AIP_INTERNAL_TOKEN` on every endpoint except `/actuator/health`. Comparison is
constant-time. **If the token is unset, authentication is OFF** — the servers
log a `WARN` at startup saying so. Read your startup logs once; do not assume.

**Write gates.** `JIRA_WRITE_ENABLED` and `WIKI_WRITE_ENABLED` default to
`false`, asserted by tests. With them off, the agent can read those systems and
nothing more. Turn one on only after deciding that an agent writing there is
acceptable.

**Read-only Cypher.** `jqassistant-mcp.queryGraph` exposes arbitrary Cypher to
the agent and rejects every write keyword before reaching Neo4j. This is
asserted by `JqaServiceGuardTest`, including that the rejection happens before
any database call.

## Deployment expectations

The servers bind to localhost and are meant to stay there. They are not
hardened for a public network: there is no rate limiting, no per-user identity,
and one shared token for all callers. If you must expose them, put them behind
something that provides those.

Credentials come from `.env`, which is gitignored. `.env.example` holds
placeholders only. CI fails if a real credential file is ever committed.

## Reporting a vulnerability

Open a [security advisory](https://github.com/Lindenson/architecture-workspace/security/advisories/new)
rather than a public issue. Include the version or commit, what an attacker
gains, and a reproduction if you have one.

This is a personal open-source project; expect a best-effort response, not an
SLA. That is stated so you can plan around it.
