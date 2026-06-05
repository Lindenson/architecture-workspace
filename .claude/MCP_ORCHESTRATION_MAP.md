# MCP_ORCHESTRATION_MAP

Version: 1.0 · Type: Data Orchestration Layer · Applies to: Architecture Agent, MCP Servers, Runtime Commands

## Purpose
Как MCP-системы связаны, какой источник первичен, как данные агрегируются в
цифрового двойника, как устраняются конфликты, как строится единая модель.

## Core Principle
Каждый MCP — не источник истины, а **проекция источника истины**. Истина всегда
реконструируется через синтез.

## System Of Record
- **Code Layer (PRIMARY)** — GitHub/GitLab: исходный код, структура модулей, конфиги, API-спеки.
- **Work Layer** — Jira: задачи, эпики, статусы, roadmap.
- **Quality Layer** — SonarQube: smells, bugs, vulnerabilities, coverage, debt.
- **Architecture Layer** — jQAssistant (граф зависимостей, циклы, ограничения) + Structurizr (C4).
- **Knowledge Layer** — Wiki (документация) + RAG/pgvector (семантика, история).
- **Design Contracts Layer** — OpenSpec: требования, спецификации, контракты.

## Data Flow
```
GitHub → jQAssistant (Architecture Graph) → Structurizr (C4) → Claude (Architecture State)
Jira → Delivery Model → Claude (Roadmap State)
SonarQube → Debt Model → Claude (Quality State)
Wiki + ADR → RAG Index → Claude (Knowledge State)
```
Все сходится в **DIGITAL_TWIN_MODEL** = PROJECT_STATE + ARCHITECTURE_STATE +
DELIVERY_STATE + QUALITY_STATE + KNOWLEDGE_STATE + DEBT_STATE.

## Priority Of Truth (on conflict)
1. Code (Git) + jQAssistant graph · 2. Structurizr + ArchUnit · 3. SonarQube ·
4. ADR · 5. Jira · 6. Wiki · 7. Manual notes.

## MCP Responsibility Map
- **GitHub MCP** → CODE_STATE, CHANGESET_STATE (code, PRs, commit history).
- **Jira MCP** → DELIVERY_STATE (delivery tracking, planning, epics).
- **SonarQube MCP** → QUALITY_STATE, TECH_DEBT_STATE.
- **jQAssistant MCP** → ARCHITECTURE_GRAPH, ARCHITECTURE_DRIFT_SIGNALS.
- **Structurizr MCP** → ARCHITECTURE_MODEL (C4 consistency).
- **Wiki MCP** → KNOWLEDGE_DOCUMENTS.
- **RAG MCP** → CONTEXT_PACKS (semantic retrieval, history).

## Orchestration Strategy (pull-based)
Агент всегда запрашивает MCP → синтезирует → обновляет модель.
1. **Data Acquisition** — fetch all MCP sources relevant to command.
2. **Normalization** — unified schema: components, services, tasks, risks, debts, dependencies.
3. **Cross-Linking** — Jira↔Code, ADR↔Code, Sonar↔Modules, Structurizr↔jQAssistant, Wiki↔Architecture.
4. **Conflict Detection** — drift, outdated ADR, orphan Jira, undocumented modules, hidden deps.
5. **Model Update** — DIGITAL_TWIN_MODEL, Project Memory, RAG Index.
6. **Report Generation** — insights, risks, recommendations.

## Architecture Drift Engine
Drift = любое расхождение между: Code↔Structurizr · Code↔ADR · Code↔Jira intent ·
jQAssistant↔Architecture rules · Sonar↔architectural constraints.

## Cache Strategy
Allowed: jQAssistant graph (24h), Sonar snapshot (12h), Jira snapshot (1h).
Not allowed: code state (always refresh).

## Failure Modes
- **MCP unavailable** → mark DATA_STALE, fallback to last snapshot, reduce confidence.
- **Conflicting sources** → prefer higher priority, log conflict into Project Memory,
  escalate if architecture-related.

## Output Contract
Каждый финальный вывод: Source MCPs used · Confidence level · Detected
inconsistencies · Linked artifacts (Jira, ADR, Code, Sonar) · Recommendations.

## Event Model (future)
Git Push → Trigger Scan · Jira Update → Delivery Sync · Sonar Alert → Debt
Analysis · PR Open → Architecture Check.

## Final Goal
Непрерывно реконструировать консистентного, запрашиваемого, живого цифрового
двойника системы из распределённых корпоративных инструментов.
