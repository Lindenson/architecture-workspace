# Glossary (EXAMPLE / STARTER TEMPLATE)

> STATUS: starter template. Add/curate terms as the domain stabilizes. Keep
> aligned with `../domain/semantic/entity-map.yaml` and `domain/model/`.

## Domain terms

| Term | Definition |
|------|-----------|
| **Payment** | An instruction to move funds between parties; aggregate root in the Payments context. |
| **Settlement** | The act of finalizing a payment and posting balanced entries to the Ledger. |
| **Ledger** | Double-entry record of all financial movements; append-only. |
| **Ledger Entry** | A single immutable debit or credit line belonging to a ledger account. |
| **Ledger Account** | An account against which entries are posted; has a derived balance. |
| **KYC** | Know Your Customer — identity verification required before regulated flows. |
| **AML** | Anti-Money-Laundering controls applied to payments and onboarding. |
| **Mandate** | Customer authorization document permitting recurring/initiated payments. |
| **Document** | Any ingested file (invoice, mandate, KYC evidence); stored immutably. |
| **OCR** | Optical Character Recognition — extracts structured data from documents. |
| **ADF** | Automatic Document Feed — example batch source of scanned documents (TODO: confirm). |
| **Retention** | The minimum period a document/record must be kept for compliance. |

## Technical / architecture terms

| Term | Definition |
|------|-----------|
| **Bounded Context** | A boundary within which a domain model and its language are consistent (DDD). |
| **Aggregate** | A cluster of domain objects treated as a unit; changed only via its root. |
| **Idempotency Key** | Client-supplied key making a repeated request (e.g. payment) a no-op. |
| **Saga** | A sequence of local transactions coordinated via events to keep contexts consistent. |
| **Outbox** | Pattern: write events to a DB table in the same tx, relay to Kafka reliably. See [ADR-003](architecture/adr/ADR-003-kafka.md). |
| **ADR** | Architecture Decision Record. See [`../architecture/adr/README.md`](../architecture/adr/README.md). |
| **ArchUnit** | Java library that asserts architecture rules as unit tests. |
| **jQAssistant** | Scans bytecode/Git/DB into a graph for architecture analysis. |
| **Structurizr / C4** | Diagrams-as-code modeling of System Context / Containers / Components. |
| **Drift** | Divergence between intended architecture (ADR/Structurizr) and actual code. |
| **Technical Debt (TD)** | Known suboptimal solution tracked for later fix. See [`../quality/technical-debt/README.md`](../quality/technical-debt/README.md). |
| **Quality Gate** | SonarQube pass/fail threshold for a build. |
| **pgvector** | Postgres extension for vector similarity; backs the [RAG layer](../rag/README.md). |
| **DTO** | Data Transfer Object — API boundary type; must not expose JPA entities. |

> TODO: add product-specific acronyms and any regulator-mandated terms.
