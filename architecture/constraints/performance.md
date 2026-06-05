# Constraint: Performance & scalability

> STATUS: enforced constraint (targets are EXAMPLE — TODO: confirm SLAs with PO/SRE).

## SLA targets (EXAMPLE)

| Flow | Target |
|------|--------|
| Payment initiation (sync part) | p95 < 300 ms |
| Payment status read | p95 < 150 ms |
| Document upload (pre-signed) | p95 < 500 ms (excludes binary transfer) |
| Document OCR completion | async; p95 < 60 s, SLA-bounded, eventually consistent |
| Ledger balance read | p95 < 200 ms |

## Rules

1. **Pagination is required** on every collection endpoint — no unbounded list
   responses (see [api-standards](../standards/api-standards.md)).
2. **No N+1 queries.** Use fetch joins / batch fetching; verify with query
   counting in tests. jQAssistant/Sonar may flag suspicious access patterns.
3. **Document OCR is asynchronous** — never block a request thread on OCR; the
   request returns a tracking handle and OCR completion is an event
   (see [ADR-003](../adr/ADR-003-kafka.md), [ADR-004](../adr/ADR-004-modular-monolith-plus-services.md)).
4. **Bounded resource use:** explicit timeouts and connection-pool limits on all
   outbound calls (DB, Kafka, object store, HTTP). No unbounded retries.
5. **Heavy/blocking IO** should use virtual threads or async where measured to help
   (Java 21); do not introduce reactive complexity without evidence.
6. **Measure before optimizing.** Performance claims must cite a benchmark/trace,
   not intuition (per [`../../CLAUDE.md`](../../CLAUDE.md) output contract).

## Enforcement

| Rule | Enforced by |
|------|-------------|
| Pagination required | API review + integration test |
| No N+1 | Query-count assertions in tests; Sonar hints |
| Async OCR | Architecture review; ArchUnit (no OCR call from controller) |

> TODO: add load-test scenarios and link the results dashboard.
