# Architecture Workspace — Architecture Intelligence Platform (AIP)

A standalone repository that turns Claude into a **Chief Enterprise Architecture
Agent** and team-lead assistant for a ~10-person modern-Java team building
**monolithic and microservice systems for finance and document processing**.

It is the *digital twin* and *nervous system* of the product: a governance layer
that continuously reconstructs a consistent, queryable model of the system from
distributed enterprise tools (Git, Jira, SonarQube, jQAssistant, Structurizr,
Wiki, OpenSpec) and keeps the AI grounded in **facts, not hallucinations**.

> This repo contains **no product source code**. It is the architect's workspace.
> Developers contribute here only via proposals; the architect approves.

---

## Why this exists

Three months into any AI-assisted project the README rots, the wiki drifts, Jira
half-matches reality, architecture lives in someone's head, and Sonar debt is
tracked elsewhere. The AI then sees contradictory data and starts hallucinating.

AIP fixes that with an explicit **Source-of-Truth hierarchy** and a synthesis
layer that reconstructs truth instead of trusting any single tool.

### Priority of truth (on conflict)

```
1. Source code (Git)            ← absolute
2. Architecture scan (jQAssistant) + ArchUnit
3. Structurizr model
4. SonarQube
5. ADR
6. Jira
7. Wiki
8. Manual notes                 ← lowest
```

The agent never argues with code, never invents architecture, and always cites
its source and a confidence level (HIGH / MEDIUM / LOW).

---

## Knowledge hierarchy (the 4 levels of truth)

| Level | What | Where | Changes |
|------|------|-------|---------|
| L0 | Code | the product repos (read via MCP) | constantly |
| L1 | Auto-extracted facts | `quality/`, `domain/raw/`, `jqassistant/` | every scan |
| L2 | Architecture decisions | `architecture/adr/`, `architecture/constraints/` | by architect |
| L3 | Intentions | `delivery/roadmap/`, `architecture/target-architecture/` | by architect |

Plus **Project Memory** (`project-memory/`) — the undervalued history of *why*
decisions were made (and rejected). And the **knowledge contexts** the agent
reads first: `knowledge/`, `architecture/`, `delivery/`, `quality/`, `history/`.

---

## Repository layout

```
architecture-workspace/
├── .claude/              Agent governance: role, operating model, runtime,
│   ├── agents/             7 subagents (architecture, debt, delivery, …)
│   └── skills/             16 skills (architecture-review, release-readiness, …)
├── knowledge/            L? Project knowledge (vision, domain, glossary)
├── architecture/         L2 ADR, constraints, standards, C4, target arch
├── delivery/             L3 roadmap, epics, releases, metrics (Jira sync)
├── quality/              L1 tech debt, risks, violations, sonar, archunit
├── history/              Project memory: decisions, incidents, lessons
├── domain/               Living DDD model (raw → semantic → model → drift)
├── reports/              Generated daily/weekly/release/architecture reports
├── rag/                  RAG layer (sources → chunks → embeddings → index)
├── project-memory/       Decision journal
├── automation/           Nightly pipeline & scan scripts
├── config/              Credential templates (real files gitignored)
├── jqassistant/          Scan rules & reports
├── mcp-servers/          Java Spring Boot MCP servers (MVP-1)
│   ├── mcp-common/         Shared MCP protocol layer
│   ├── jira-mcp/           Delivery source
│   ├── github-mcp/         Code source
│   ├── sonar-mcp/          Quality source
│   └── digital-twin-core/  Orchestrator → DIGITAL_TWIN_MODEL
├── architecture-tests/   ArchUnit enforcement module
├── .mcp.json             MCP wiring (all servers; secrets via ${ENV})
├── .env.example          Credential template (copy to .env)
└── docker-compose.yml    Postgres+pgvector, Neo4j, MCP servers
```

---

## Quick start

```bash
# 1. Secrets
cp .env.example .env && $EDITOR .env

# 2. Infra (Postgres+pgvector, Neo4j)
docker compose up -d postgres neo4j

# 3. Build + run the MVP-1 MCP servers
cd mcp-servers && mvn -q -DskipTests package
# then `docker compose up -d` (or run each jar) to start jira/github/sonar/twin

# 4. Open this folder in Claude — .mcp.json wires the servers automatically.
```

Then ask the agent:

```
SHOW PROJECT STATE
```

…and get a consolidated picture of architecture, quality, debt, delivery, Jira,
ADR compliance, risks, and recommendations — without manually checking each tool.

---

## Governance documents

Read these (in `.claude/`) to understand the agent contract:

- `BOOTSTRAP_ARCHITECT_AGENT.md` — mission & workspace spec
- `ROLE_ARCHITECT_AGENT.md` — role, authority, allowed/restricted actions
- `OPERATING_MODEL.md` — operating modes & daily/weekly/monthly procedures
- `AGENT_RUNTIME.md` — runtime commands (SHOW_PROJECT_STATE, …)
- `MCP_ORCHESTRATION_MAP.md` — how sources synthesize into the digital twin
- `DOMAIN_INTELLIGENCE_BOOTSTRAP.md` — living DDD domain model pipeline
- `MCP_SERVERS.md` — server status, ports, build/run

## MVP roadmap

- **MVP-1 (this repo):** jira-mcp, github-mcp, sonar-mcp, digital-twin-core
- **MVP-2:** jqassistant-mcp, structurizr-mcp
- **MVP-3:** rag-mcp (pgvector), wiki-mcp
- **MVP-4:** event bridge (Git push → scan → reindex → report), full orchestration

## Security

All credentials live in `.env` / `config/*.config.yml` — **gitignored**. The repo
ships only `*.example` templates. See `config/README.md`. Never commit a token.
