# Project Overview — Paydocs Platform (EXAMPLE / STARTER TEMPLATE)

> STATUS: starter template. Replace every `TODO` and `example` marker with the
> real product facts. This file is slow-changing project context (see
> `../CLAUDE.md` → "Read first, every session").
> Source of truth for code-level claims is always the code, not this file.

## Vision

> EXAMPLE — TODO: refine with the product owner.

**Paydocs** is a payments and document-processing platform for regulated
financial workflows. It accepts payment instructions, settles them against an
internal ledger, and ingests, stores and processes the supporting documents
(invoices, KYC evidence, mandates) that finance and compliance require.

Goal: let business customers initiate payments and submit documents through one
API, while compliance and operations get a fully auditable trail.

## Scope

In scope (EXAMPLE):
- Payment initiation, validation, settlement and ledger posting.
- Document ingestion (upload + async OCR/extraction), storage and retrieval.
- Identity / KYC checks gating payment and onboarding flows.
- Customer + operator notifications.

Out of scope (EXAMPLE):
- Card scheme / acquiring integration (TODO: confirm with product).
- General-purpose BI / data-warehouse (downstream, separate system).
- Public marketing site.

## Monolith + microservices split

The system is a **modular monolith core** with a small number of **extracted
services** where load, scaling or technology differs. See
[ADR-004](../architecture/adr/ADR-004-modular-monolith-plus-services.md).

| Unit | Type | Contains (bounded contexts) | Why |
|------|------|-----------------------------|-----|
| `paydocs-core` | Modular monolith | Payments, Ledger, Identity/KYC, Notifications | Transactional consistency, one team, shared Postgres |
| `document-processing-service` | Microservice | DocumentIngestion, OCR pipeline | CPU/IO-heavy async OCR, independent scaling |
| `document-storage-service` | Microservice (thin) | DocumentStorage | Object-store gateway, see [ADR-005](../architecture/adr/ADR-005-document-storage-s3.md) |

Contexts communicate in-process via service interfaces inside the monolith, and
across service boundaries via **Kafka events + REST contracts** only — never via
shared database access (see [integration constraint](../architecture/constraints/integration.md)).

## Tech stack (EXAMPLE — TODO: pin exact versions in code/`pom.xml`)

| Layer | Choice | ADR |
|-------|--------|-----|
| Language | Java 21 (records, pattern matching, virtual threads) | — |
| App framework | Spring Boot 3.4.x | [ADR-002](../architecture/adr/ADR-002-spring-boot.md) |
| Primary datastore | PostgreSQL 16 | [ADR-001](../architecture/adr/ADR-001-postgresql.md) |
| Messaging / eventing | Apache Kafka (transactional outbox) | [ADR-003](../architecture/adr/ADR-003-kafka.md) |
| Document storage | S3-compatible object store | [ADR-005](../architecture/adr/ADR-005-document-storage-s3.md) |
| DB migrations | Flyway | [persistence standards](../architecture/standards/persistence-standards.md) |
| Build | Maven (multi-module) | — |
| API style | REST + OpenAPI 3 | [api standards](../architecture/standards/api-standards.md) |
| Arch tests | ArchUnit + jQAssistant | [../quality/archunit/README.md](../quality/archunit/README.md) |
| Observability | OpenTelemetry → (TODO: vendor) | — |

## Related context

- Domain & bounded contexts → [business-domain.md](business-domain.md)
- Who decides what → [stakeholders.md](stakeholders.md)
- Terms → [glossary.md](glossary.md)
- Where it runs → [environments.md](environments.md)
- Where it's heading → [../architecture/target-architecture/target-state.md](../architecture/target-architecture/target-state.md)

> TODO: link the real Git repositories and the Jira project key (example: `PAY`).
