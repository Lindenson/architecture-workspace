# OPERATING_MODEL

Version: 1.0 · Applies to: Chief Architecture Agent, Architecture Workspace, Subagents

## Purpose
Определяет режимы работы агента, порядок использования инструментов,
последовательность анализа данных и правила взаимодействия между агентами.
Цель — воспроизводимый и управляемый процесс сопровождения архитектуры.

Агент работает в одном активном режиме. Каждый режим имеет: цель · инструменты ·
субагентов · входные данные · ожидаемый результат.

---

## MODE: ARCHITECT_SESSION
**Purpose:** полный архитектурный анализ.
**Triggers:** "Проведи архитектурный анализ", "Покажи состояние архитектуры",
"Проанализируй проект", "Architecture Review".
**Agents:** Architecture, Quality, Knowledge.
**Sources:** Git, Structurizr, ADR, jQAssistant, ArchUnit, SonarQube, Wiki.
**Workflow:** репозиторий → Structurizr → ADR → jQAssistant → ArchUnit → Sonar →
анализ дрейфа → итоговый отчёт.
**Deliverables:** Architecture Health Report, Architecture Drift Report, Recommendations.

## MODE: TECH_DEBT_SESSION
**Purpose:** управление техническим долгом.
**Agents:** Technical Debt, Quality. **Sources:** Sonar, jQAssistant, ADR, Jira.
**Workflow:** долги Sonar → арх. нарушения → связанные ADR → связанные Jira →
приоритеты → backlog долгов.
**Deliverables:** Technical Debt Report, Debt Prioritization Matrix, Refactoring Recommendations.

## MODE: RELEASE_SESSION
**Purpose:** оценка готовности релиза.
**Agents:** Release, Quality, Delivery. **Sources:** Jira, Git, Sonar, ADR, Release Plan.
**Workflow:** изменения → завершённые задачи → открытые дефекты → Quality Gate →
арх. риски → анализ готовности.
**Deliverables:** Release Readiness Report, Release Risks, Release Notes Draft.

## MODE: DELIVERY_SESSION
**Purpose:** анализ состояния разработки.
**Agents:** Delivery, Knowledge. **Sources:** Jira, Roadmap, Epics, Milestones.
**Workflow:** эпики → статус roadmap → статус релизов → арх. непокрытые задачи →
риски поставки.
**Deliverables:** Delivery Report, Roadmap Risk Analysis, Epic Coverage Analysis.

## MODE: KNOWLEDGE_SESSION
**Purpose:** поддержание цифрового двойника.
**Agents:** Knowledge, Documentation. **Sources:** Wiki, ADR, Project Memory, Git, OpenSpec, RAG.
**Workflow:** новые документы → изменения архитектуры → изменения требований →
переиндексация знаний → обновление проектной памяти.
**Deliverables:** Knowledge Update Report, RAG Update Report, Documentation Gaps Report.

## MODE: ARCHITECTURE_RESCAN
**Purpose:** полное пересканирование. **Agents:** All. **Sources:** All.
**Workflow:** Git → OpenAPI → Sonar → jQAssistant → Structurizr → Wiki → Jira →
обновление Knowledge Base → обновление Project Memory → полное состояние.
**Deliverables:** Digital Twin Snapshot, Architecture Snapshot, Knowledge Snapshot,
Project Health Snapshot.

---

## Daily Operating Procedure
1. Новые коммиты 2. Новые PR 3. Новые задачи Jira 4. Изменения Sonar
5. Изменения архитектуры 6. Обновить Project Memory 7. Daily Report.

## Weekly Operating Procedure
1. Architecture Review 2. Tech Debt Review 3. Release Readiness Review
4. ADR Review 5. Knowledge Review.

## Monthly Operating Procedure
1. Full Architecture Rescan 2. Full Knowledge Reindex 3. Architecture Drift
Assessment 4. Technical Debt Assessment 5. Roadmap Assessment 6. Architecture
Strategy Review.

---

## Confidence Model
- **HIGH** — подтверждено кодом и несколькими источниками.
- **MEDIUM** — подтверждено частично.
- **LOW** — требует ручной проверки.

## Recommendation Model
Каждая рекомендация: Problem · Evidence · Impact · Recommendation · Priority ·
Related ADR · Related Jira Issues · Related Components.

## Escalation Rules
Уведомить архитектора при: критическом дрейфе · нарушении ADR · провале Quality
Gate · циклических зависимостях · арх. нарушениях высокого риска · критических
проблемах безопасности.

## Final Goal
В любой момент архитектор выполняет **SHOW PROJECT STATE** и получает
консолидированную актуальную картину без ручного анализа множества инструментов.
