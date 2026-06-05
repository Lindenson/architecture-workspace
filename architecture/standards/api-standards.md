# API Standards (REST)

> STATUS: starter standard. EXAMPLE conventions. Relates to
> [integration constraint](../constraints/integration.md).

## REST conventions

- Resource-oriented, plural nouns: `/payments`, `/documents`, `/customers`.
- Use HTTP verbs correctly: GET (safe), POST (create/command), PUT/PATCH
  (update), DELETE. No verbs in paths.
- Status codes: 200/201/202 success, 400 validation, 401/403 auth, 404 missing,
  409 conflict (e.g. duplicate), 422 business rule, 5xx server.
- **Pagination is mandatory** on collections (`?page=&size=`, return total/links)
  — see [performance](../constraints/performance.md).
- **Idempotency:** mutating payment endpoints require an `Idempotency-Key` header
  (see [integration](../constraints/integration.md), [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md)).

## Versioning

- URI major versioning: `/api/v1/...`. Breaking changes → new major version.
- Backward-compatible changes (additive fields) do not bump the version.

## OpenAPI

- Every API is described by an **OpenAPI 3** contract committed in the producing
  repo; the contract is the source of truth for consumers.
- Generate clients/server stubs from the contract where practical (contract-first).

## Error model (EXAMPLE — RFC 9457 / Problem Details)

```json
{
  "type": "https://errors.paydocs.example/payment-duplicate",
  "title": "Duplicate payment",
  "status": 409,
  "detail": "A payment with this Idempotency-Key already exists.",
  "instance": "/api/v1/payments",
  "correlationId": "..."
}
```

- One consistent error shape across all services. No raw stack traces in responses.
- `correlationId` ties the response to logs/traces.

## DTOs

- Request/response bodies are **DTOs (records)** — never JPA entities
  (see [persistence-standards](../standards/persistence-standards.md), [layering](../constraints/layering.md)).

> TODO: link the shared error-type registry and the OpenAPI lint ruleset.
