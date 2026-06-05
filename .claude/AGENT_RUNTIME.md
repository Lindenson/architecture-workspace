# AGENT_RUNTIME

Version: 1.0 · Type: Execution Layer Specification · Applies to: Chief Agent, Subagents, MCP integrations

## Purpose
Определяет реальные команды управления агентом, порядок выполнения операций,
порядок обращения к MCP, правила обновления состояния, формат записи результатов,
runtime-ограничения.

## Core Principle
Агент не рассуждает в вакууме. Агент всегда выполняет одну из команд Runtime
Layer. Каждая команда: вход · источники данных · pipeline · выходной артефакт.

## Command Interface
```
COMMAND <name>
INPUT <payload>
```

---

## COMMAND: SHOW_PROJECT_STATE
**Sources:** GitHub/GitLab, Jira, Sonar, jQAssistant, Structurizr, Wiki, RAG, Project Memory.
**Pipeline:** Git status → Jira status → Sonar metrics → architecture graph → ADR
state → knowledge base → tech-debt registry → merge into unified model → resolve
inconsistencies → generate snapshot.
**Output:** PROJECT_STATE_SNAPSHOT (architecture, delivery, quality, tech-debt,
risk, knowledge gaps, recommendations).

## COMMAND: RUN_ARCHITECTURE_RESCAN
**Sources:** Git, jQAssistant, ArchUnit, Structurizr, Sonar, ADR, Wiki.
**Pipeline:** scan source → build dependency graph (jQAssistant) → validate rules
(ArchUnit) → update Structurizr → compare with ADR → detect drift → update state.
**Output:** ARCHITECTURE_SNAPSHOT (drift report, dependency graph, violations, fixes).

## COMMAND: ANALYZE_TECH_DEBT
**Sources:** Sonar, jQAssistant, Jira, ADR, Git history.
**Pipeline:** fetch Sonar issues → map to modules → cross-check ADR compliance →
link to Jira → prioritize → cluster by domain.
**Output:** TECH_DEBT_REPORT.

## COMMAND: ANALYZE_RELEASE_READINESS
**Sources:** Jira, Git, Sonar, ADR, Architecture state.
**Pipeline:** completed issues → validate Quality Gate → validate architecture
compliance → unresolved risks → dependencies → readiness score.
**Output:** RELEASE_READINESS_REPORT — READY / READY_WITH_RISKS / NOT_READY.

## COMMAND: UPDATE_KNOWLEDGE_BASE
**Sources:** Wiki, Git, ADR, Jira, Structurizr.
**Pipeline:** fetch new documents → chunk → embeddings → update vector DB →
update Project Memory → resolve conflicts.
**Output:** KNOWLEDGE_UPDATE_REPORT.

## COMMAND: UPDATE_ARCHITECTURE_MODEL
**Sources:** jQAssistant, Structurizr, ADR, Git code structure.
**Pipeline:** extract code structure → update component model → validate against
ADR → sync Structurizr DSL → persist.
**Output:** ARCHITECTURE_MODEL_UPDATE.

## COMMAND: GENERATE_REPORT
**Types:** DAILY · WEEKLY · ARCHITECTURE · TECH_DEBT · RELEASE.
**Pipeline:** collect snapshot → normalize → extract insights → recommendations →
store in `reports/`.

---

## MCP Execution Layer — standard call order
1. Git MCP 2. Jira MCP 3. SonarQube MCP 4. jQAssistant MCP 5. Structurizr MCP
6. Wiki MCP 7. RAG MCP.

## State Model
`PROJECT_STATE` · `ARCHITECTURE_STATE` · `QUALITY_STATE` · `DELIVERY_STATE` ·
`KNOWLEDGE_STATE` · `DEBT_STATE`.

## State Update Rules
Состояние обновляется только через команды. Нельзя «предполагать» состояние.
Нельзя кешировать без обновления источников. Все изменения трассируемы.

## File Write Rules
Разрешено писать: `reports/`, `architecture/adr/drafts/`, `knowledge/drafts/`,
`quality/technical-debt/drafts/`. Запрещено: удалять ADR без подтверждения, менять
арх. решения без команды, обновлять roadmap без DELIVERY_SESSION.

## Error Handling
Если MCP недоступен: использовать кешированные данные → пометить DATA_STALE →
уровень уверенности LOW.

## Confidence Model
HIGH — данные из нескольких MCP, подтверждены кодом. MEDIUM — частично. LOW —
устаревшие/неполные.

## Event Model (future)
Команды будут вызываться автоматически: Git push · Jira update · Sonar scan ·
Architecture change.

## Final Goal
Выполнить `COMMAND SHOW_PROJECT_STATE` и вернуть полную, консистентную, актуальную
модель проекта без ручного анализа источников.
