# TD-001: Legacy `RestTemplate` usage

> EXAMPLE debt record. Numbers are placeholders.

- **ID:** TD-001
- **Source:** Sonar rule (deprecated HTTP client) + jQAssistant usage query
- **Status:** open
- **Priority:** Medium
- **Fix cost:** M
- **Related ADR:** [ADR-002](../../architecture/adr/ADR-002-spring-boot.md)
- **Related Jira:** PAY-204 (example)
- **Date raised:** 2026-05-12 (example)

## Description

Outbound HTTP calls (KYC provider, document-storage) use Spring `RestTemplate`,
which is in maintenance mode. Timeout configuration is inconsistent across call
sites. ~14 usages across `core` and `document-processing-service` (EXAMPLE
counts — TODO: confirm via scan).

## Impact

- Inconsistent timeout/retry handling risks thread exhaustion under slow
  upstreams (relates to [performance constraint](../../architecture/constraints/performance.md)).
- Diverges from the forward-looking client stack; raises onboarding friction.
- Medium maintenance drag; no current customer-facing defect.

## Suggested fix

Migrate to `WebClient` with a shared builder + standard timeouts, largely via
OpenRewrite. See the refactoring proposal:
[openrewrite-resttemplate-to-webclient](../../architecture/refactoring/openrewrite-resttemplate-to-webclient.md).

## Notes

Add an ArchUnit rule forbidding new `RestTemplate` usage once migration starts,
to prevent regression.
