# DOMAIN INTELLIGENCE PIPELINE (DIP)

Version: 1.0 · Applies to: Architecture Agent, Domain MCP, jQAssistant, Digital Twin Core, Claude Orchestrator

## Purpose
Система построения, анализа и сопровождения **доменной модели (DDD Model)** на
основе кода, БД, миграций, ORM, API-контрактов и архитектурных правил.
Цель — построить живую эволюционирующую доменную модель и контролировать её
консистентность.

## Core Principle
Домены не описываются вручную. Домены **извлекаются, интерпретируются и валидируются**.

## Domain Intelligence Layers
- **L1 Structural (FACTS)** — jQAssistant, Spring/JPA metadata, bytecode → классы, зависимости, пакеты, связи.
- **L2 Persistence (DATA MODEL)** — PostgreSQL schema, Flyway/Liquibase → таблицы, связи, ограничения, миграции.
- **L3 Semantic (MEANING)** — naming, jMolecules (optional), аннотации, API-контракты → entities, aggregates, bounded contexts (initial).
- **L4 Domain Model (INTELLIGENCE)** — Claude reasoning + Domain MCP → bounded contexts (final), aggregates (validated), boundaries, rules.
- **L5 Governance (CONTROL)** — ADR, architecture rules, constraints, ArchUnit → enforced boundaries, allowed deps, validation rules.

## Domain Workspace Structure
```
domain/
  raw/        jqa-graph/ db-schema/ migrations/ jpa-model/
  semantic/   inferred-aggregates/ bounded-context-candidates/ entity-map.yaml
  model/      bounded-contexts.md aggregates.md domain-rules.md
  visualization/  domain-graph.dsl c4-domain-view/
  drift/      domain-drift-report.md
  constraints/ domain-rules.adoc archunit-domain-rules.java
```

## Initial Domain Extraction Pipeline
1. **Structural Scan** (jQAssistant) → structural-domain-graph.
2. **DB Model Extraction** (PostgreSQL introspection) → tables, relations, constraints.
3. **Migration Analysis** (Flyway/Liquibase) → schema evolution, domain change history.
4. **ORM Mapping Extraction** (JPA/Hibernate) → entity↔table, relationship mapping.
5. **API Boundary Extraction** (REST controllers, DTOs, OpenAPI) → exposed domain model.
6. **Domain Synthesis** (Claude) → aggregate inference, bounded context clustering,
   inconsistency detection, naming normalization → DOMAIN_STATE v1.

## Domain Graph Model
Node types: BoundedContext · Aggregate · Entity · ValueObject · Table · DTO · Mapper · Service.
Relationships: Aggregate→Entity · Entity→Table · DTO→Mapper→Entity · Service→Aggregate ·
BoundedContext→Aggregates.

## Domain Drift Detection
entity without aggregate · table without entity · DTO leaking persistence ·
cross-context dependency · inconsistent naming · orphan migrations.

## Domain Visualization
- C4 Domain View — bounded contexts, aggregates, service boundaries.
- Graph View — full dependency graph, cross-domain flows.
Tooling: Structurizr MCP, graph export from jQAssistant.

## Domain Constraints (examples)
Aggregate boundary cannot be crossed directly · Repository only via Aggregate root ·
DTO cannot reference Entity directly · Cross-context calls via service layer.

## jMolecules (optional, after stabilization)
Annotate already-validated model (@AggregateRoot, @DomainEntity, @DomainService).
Rule: **jMolecules is a stabilization tool, not a discovery tool.**

## Domain MCP Extension (`domain-mcp/`)
Build domain graph · run drift detection · merge structural+semantic+DB models ·
generate DOMAIN_STATE · feed Claude orchestrator.

## Claude Domain Command: ANALYZE_DOMAIN
jQAssistant scan → DB schema load → ORM extraction → migration scan → API scan →
domain synthesis → drift detection → update DOMAIN_STATE.
**Output DOMAIN_REPORT:** bounded contexts · aggregates · inconsistencies · risks · recommendations.

## Domain Evolution Tracking
Store versions DOMAIN_STATE_v1/v2/v3; track growth, context splits, aggregate merges.

## Governance Model
Domain model changes require architecture approval, ADR creation, impact analysis.

## Final Objective
Создать непрерывно эволюционирующую **LIVING DOMAIN MODEL OF THE SYSTEM**,
способную объяснять структуру, выявлять несогласованность, направлять разработку,
поддерживать арх. решения и безопасную эволюцию legacy.
