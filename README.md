<div align="center">

# 🧠 Architecture Intelligence Platform (AIP)

**A living digital twin of your software system — and the workspace that turns Claude into a Chief Enterprise Architect.**

[![Status](https://img.shields.io/badge/status-MVP--3%20working-success)](#-roadmap)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0%20MCP-blue)](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
[![MCP](https://img.shields.io/badge/protocol-MCP%20over%20SSE-9cf)](https://modelcontextprotocol.io/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

</div>

> **What is this?** A standalone repository that turns [Claude](https://claude.ai/code)
> into a **Chief Enterprise Architecture Agent** and team-lead assistant for a
> ~10-person modern-Java team building **monolithic and microservice systems for
> finance and document processing**.
>
> It is the *governance layer* and *digital twin* of a product: it continuously
> reconstructs a consistent, queryable model of the system from your distributed
> enterprise tools (Git, Jira, SonarQube, jQAssistant, Structurizr, Wiki, OpenSpec)
> and keeps the AI grounded in **facts, not hallucinations**.

> ⚠️ This repo contains **no product source code**. It is the *architect's
> workspace*. Developers contribute here only via proposals; the architect approves.

---

## Table of contents

- [The problem this solves](#-the-problem-this-solves)
- [How it works](#-how-it-works)
- [Source-of-truth hierarchy](#-source-of-truth-hierarchy)
- [Knowledge hierarchy](#-knowledge-hierarchy-the-four-levels-of-truth)
- [Repository layout](#-repository-layout)
- [Quick start](#-quick-start)
- [MCP servers](#-mcp-servers)
- [Skills & subagents](#-skills--subagents)
- [Governance documents](#-governance-documents)
- [Roadmap](#-roadmap)
- [Security](#-security)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 The problem this solves

Three months into any AI-assisted project the same rot sets in:

- the README is stale,
- the Wiki has drifted,
- Jira only half-matches reality,
- architecture lives in someone's head,
- Sonar debt is tracked somewhere else entirely.

The AI then sees **contradictory data** and starts to hallucinate. AIP fixes this
with an explicit **Source-of-Truth hierarchy** and a synthesis layer that
*reconstructs* truth instead of trusting any single tool. The agent never argues
with code, never invents architecture, and always cites its source and a
**confidence level** (`HIGH` / `MEDIUM` / `LOW`).

The end goal: at any moment the architect can ask

```
SHOW PROJECT STATE
```

…and get a consolidated picture of architecture, quality, technical debt,
delivery, Jira, ADR compliance, risks, and recommendations — **without manually
checking each tool.**

---

## 🏗 How it works

```
                         ┌──────────────────────────────┐
                         │          Claude Agent         │
                         │  (Chief Architecture Brain)   │
                         └───────────────┬───────────────┘
                                         │  MCP (SSE)
                         ┌───────────────▼───────────────┐
                         │      digital-twin-core :8080   │  ← orchestrator
                         │  SHOW_PROJECT_STATE, RESCAN…   │
                         └───┬──────────┬──────────┬──────┘
                  /api/state │          │          │ /api/state
              ┌──────────────▼┐   ┌─────▼──────┐  ┌▼─────────────┐
              │  jira-mcp :8081│   │github :8082│  │ sonar  :8083 │   + jqassistant :8085,
              │  DELIVERY_STATE│   │ CODE_STATE │  │QUALITY/DEBT  │     structurizr :8084,
              └───────┬────────┘   └─────┬──────┘  └──────┬───────┘     rag :8088 + wiki :8086
                                                                        (optional); planned: openspec
                      │                  │                │
                 ┌────▼───┐         ┌────▼────┐      ┌─────▼─────┐
                 │  Jira  │         │ GitHub/ │      │ SonarQube │
                 │        │         │ GitLab  │      │           │
                 └────────┘         └─────────┘      └───────────┘

  Everything merges into the DIGITAL_TWIN_MODEL — the queryable state of the system.
```

Each MCP server is a small **Spring Boot** app exposing tools to Claude over the
**Model Context Protocol (MCP) via SSE** (using Spring AI's MCP server). The
`digital-twin-core` orchestrator fans out to the others, merges their state
slices, resolves conflicts by the truth hierarchy, and returns a single
consolidated model with provenance and confidence.

---

## 🔝 Source-of-truth hierarchy

On any conflict, the **higher** source wins:

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

---

## 🧱 Knowledge hierarchy (the four levels of truth)

| Level | What | Where | Changes |
|------|------|-------|---------|
| **L0** | Code | product repos (read via MCP) | constantly |
| **L1** | Auto-extracted facts | `quality/`, `domain/raw/`, `jqassistant/` | every scan |
| **L2** | Architecture decisions | `architecture/adr/`, `architecture/constraints/` | by architect |
| **L3** | Intentions | `delivery/roadmap/`, `architecture/target-architecture/` | by architect |

Plus **Project Memory** (`project-memory/`) — the undervalued history of *why*
decisions were made (and rejected).

---

## 📁 Repository layout

```
architecture-workspace/
├── .claude/              Agent governance contract
│   ├── *.md                ROLE, OPERATING_MODEL, AGENT_RUNTIME, MCP_ORCHESTRATION_MAP …
│   ├── agents/             7 subagents (architecture, debt, delivery, …)
│   └── skills/             16 skills (architecture-review, release-readiness, …)
├── knowledge/            L0-ish Project knowledge (vision, domain, glossary, stakeholders)
├── architecture/         L2 ADR, constraints, standards, C4 (Structurizr DSL), target arch
├── delivery/             L3 roadmap, epics, releases, metrics (Jira sync)
├── quality/              L1 tech debt, risks, violations, sonar, archunit
├── history/              Project memory: decisions, incidents, lessons-learned
├── domain/               Living DDD model (raw → semantic → model → drift)
├── reports/              Generated daily/weekly/release/architecture reports
├── rag/                  RAG layer (sources → chunks → embeddings → index) — MVP-3
├── project-memory/       Decision journal (why, including rejected options)
├── automation/           Nightly pipeline & scan scripts
├── config/              Credential templates (real files gitignored)
├── jqassistant/          Scan rules & reports — MVP-2
├── mcp-servers/          Java Spring Boot MCP servers
│   ├── mcp-common/         Shared MCP protocol layer + auth + REST client factory
│   ├── jira-mcp/           Delivery source
│   ├── github-mcp/         Code source (GitHub & GitLab)
│   ├── sonar-mcp/          Quality source
│   └── digital-twin-core/  Orchestrator → DIGITAL_TWIN_MODEL
├── architecture-tests/   ArchUnit enforcement module (template for product repos)
├── db/                   pgvector schema (db/init.sql)
├── .mcp.json             MCP wiring (all servers; secrets via ${ENV})
├── .env.example          Credential template (copy to .env)
└── docker-compose.yml    Postgres+pgvector, Neo4j, MCP servers
```

---

## 🚀 Quick start

**Prerequisites:** JDK 21+, Maven 3.9+, Docker (optional), and a Claude Code client.

```bash
# 1. Secrets — copy the template and fill in your tokens
cp .env.example .env && $EDITOR .env

# 2. Infra (Postgres+pgvector for RAG/knowledge, Neo4j for jQAssistant graph)
docker compose up -d postgres neo4j

# 3. Build the MVP-1 MCP servers
cd mcp-servers && mvn -DskipTests package && cd ..

# 4. Run the servers (locally or via docker)
./automation/run-mcp-servers.sh        # or: docker compose up -d
./automation/health-check.sh           # verify :8080-:8083 are UP

# 5. Open this folder in Claude — .mcp.json wires the servers automatically.
```

Then ask the agent:

```
SHOW PROJECT STATE
```

---

## 🔌 MCP servers

All servers speak **MCP over SSE** and require `Authorization: Bearer ${AIP_INTERNAL_TOKEN}`.
Secrets come from `.env` / `config/*.config.yml` (gitignored). See
[`.claude/MCP_SERVERS.md`](.claude/MCP_SERVERS.md) for full details.

| Server             | Port | Status      | Source              | Contributes            |
|--------------------|------|-------------|---------------------|------------------------|
| `digital-twin-core`| 8080 | ✅ MVP-1    | orchestrator        | `DIGITAL_TWIN_MODEL`   |
| `jira-mcp`         | 8081 | ✅ MVP-1    | Jira REST           | `DELIVERY_STATE`       |
| `github-mcp`       | 8082 | ✅ MVP-1    | GitHub / GitLab REST| `CODE_STATE`           |
| `sonar-mcp`        | 8083 | ✅ MVP-1    | SonarQube Web API   | `QUALITY_STATE`, debt  |
| `structurizr-mcp`  | 8084 | ✅ MVP-2    | Structurizr DSL (C4) | `ARCHITECTURE_MODEL`  |
| `jqassistant-mcp`  | 8085 | ✅ MVP-2    | Neo4j (jQAssistant) | `ARCHITECTURE_GRAPH`   |
| `rag-mcp`          | 8088 | ✅ MVP-3 *(optional)* | Postgres + pgvector | `CONTEXT_PACKS`     |
| `wiki-mcp`         | 8086 | ✅ MVP-3 *(optional)* | Confluence / Wiki   | `KNOWLEDGE_DOCUMENTS` |
| `openspec-mcp`     | 8087 | 🔜 MVP-4    | OpenSpec repo       | `DESIGN_CONTRACTS`     |

---

## 🧩 Skills & subagents

**16 skills** (`.claude/skills/`) — business scenarios the agent runs:
`architecture-review` · `adr-review` · `architecture-drift-analysis` ·
`tech-debt-review` · `release-readiness-review` · `jira-epic-analysis` ·
`release-notes-generation` · `readme-generation` ·
`architecture-documentation-update` · `wiki-synchronization` · `sonar-analysis` ·
`jqassistant-analysis` · `structurizr-analysis` · `project-health-review` ·
`risk-analysis` · `project-state-review`.

**7 subagents** (`.claude/agents/`) the chief agent delegates to —
`architecture` · `technical-debt` · `delivery` · `documentation` · `knowledge` ·
`release` · `quality`. Each returns *result + data sources + confidence + recommendations*.

---

## 📚 Governance documents

The agent contract lives in `.claude/`:

| Document | Purpose |
|----------|---------|
| [`ROLE_ARCHITECT_AGENT.md`](.claude/ROLE_ARCHITECT_AGENT.md) | Role, authority, allowed/restricted actions |
| [`OPERATING_MODEL.md`](.claude/OPERATING_MODEL.md) | Operating modes & daily/weekly/monthly procedures |
| [`AGENT_RUNTIME.md`](.claude/AGENT_RUNTIME.md) | Runtime commands (`SHOW_PROJECT_STATE`, …) |
| [`MCP_ORCHESTRATION_MAP.md`](.claude/MCP_ORCHESTRATION_MAP.md) | How sources synthesize into the digital twin |
| [`DOMAIN_INTELLIGENCE_BOOTSTRAP.md`](.claude/DOMAIN_INTELLIGENCE_BOOTSTRAP.md) | Living DDD domain-model pipeline |
| [`BOOTSTRAP_ARCHITECT_AGENT.md`](.claude/BOOTSTRAP_ARCHITECT_AGENT.md) | Mission & workspace spec |
| [`MCP_SERVERS.md`](.claude/MCP_SERVERS.md) | Server status, ports, build & run |

---

## 🗺 Roadmap

### ✅ Done — MVP-1 (this release)
- Full governance contract, 16 skills, 7 subagents
- Knowledge / architecture / quality / delivery / history / domain hierarchy (seeded templates)
- **Working** Spring Boot MCP servers: `jira-mcp`, `github-mcp`, `sonar-mcp`, `digital-twin-core`
- `mcp-common` shared protocol layer (confidence model, internal-token auth, REST client factory)
- ArchUnit enforcement module, automation scripts, `docker-compose` (pgvector + Neo4j + servers), pgvector schema
- All secrets externalized; verified: reactor builds, tests pass, servers boot and serve MCP/SSE

### ✅ Done — MVP-2 (architecture graph)
- `jqassistant-mcp` — read-only Cypher over the Neo4j bytecode graph: cycles,
  layering violations, coupling/god-classes, blast-radius, `getState → ARCHITECTURE_GRAPH`
- `structurizr-mcp` — parse/validate `workspace.dsl`, list C4 elements/views,
  `detectDrift`, `getState → ARCHITECTURE_MODEL`
- `digital-twin-core.runArchitectureRescan` now performs a **real** scan
  (jQAssistant + Structurizr) and surfaces drift signals; architecture state is
  live in `SHOW_PROJECT_STATE`

### ✅ Done — MVP-3 (knowledge & RAG — **optional, configurable**)
- `rag-mcp` — Postgres + pgvector retrieval over ADRs, wiki, code docs, project
  memory. **Pluggable embeddings**: `local` (offline ONNX all-MiniLM-L6-v2,
  no API key — default), `openai` (any OpenAI-compatible endpoint), or `none`
  (Postgres full-text only). Owns its dimension-sized table.
- `wiki-mcp` — Confluence read/search/(gated)update.
- **Optional by design**: off by default. The whole platform runs fully without
  it; when off, the knowledge slice is reported `DISABLED` and never lowers
  confidence. Enable per-project with `KNOWLEDGE_ENABLED=true` +
  `docker compose --profile knowledge up -d`.

### 🔜 MVP-4 — Event-driven twin
- Event bridge: `Git push → scan → jQAssistant → Sonar → Structurizr → RAG reindex → report → Jira update`
- `openspec-mcp`, full automation, scheduled nightly digital-twin refresh

---

## 🔒 Security

- **No secret ever lives in git.** The repo ships only `*.example` templates.
  Real credentials go in `.env` / `config/*.config.yml`, both gitignored.
  See [`config/README.md`](config/README.md).
- All internal MCP traffic is authenticated with a single
  `AIP_INTERNAL_TOKEN` bearer; the auth filter uses constant-time comparison.
- The dedicated `architecture_ai` Postgres database is **separate** from any
  product/business data.
- Never paste a token into an ADR, report, commit, or chat message.

---

## 🤝 Contributing

This is an *architecture workspace* — see [`CONTRIBUTING.md`](CONTRIBUTING.md).
In short: governance artifacts (ADRs, constraints, standards, roadmap) change via
**proposals/drafts** that the architect approves; code (the MCP servers) follows
standard PR review. The AI agent may author drafts but never self-approves
restricted changes.

---

## 📄 License

Licensed under the **Apache License 2.0** — see [`LICENSE`](LICENSE).
