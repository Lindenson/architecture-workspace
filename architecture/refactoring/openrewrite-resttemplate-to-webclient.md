# Refactoring Proposal: Migrate `RestTemplate` → `WebClient` via OpenRewrite

> STATUS: EXAMPLE proposal (draft). Illustrative; numbers are placeholders.
> Drives: [TD-001](../../quality/technical-debt/TD-001-resttemplate-usage.md).
> Related Jira: PAY-204 (example).

## Problem

The codebase uses Spring's `RestTemplate` for outbound HTTP (document-storage
calls, KYC provider). `RestTemplate` is in maintenance mode; `WebClient` is the
forward-looking client (works with virtual threads / reactive, better timeout and
connection control — relevant to [performance constraint](../constraints/performance.md)).

## Evidence

> EXAMPLE — TODO: replace with real scan output.

- ~`14` usages of `RestTemplate` across `core` and `document-processing-service`
  (source: jQAssistant query / `grep -r "RestTemplate"`).
- Sonar rule flagging deprecated/legacy HTTP client usage: `javaS....` (TODO: exact rule).
- Inconsistent timeout configuration across call sites.

## Impact

- Affected modules: Payments (KYC client), Document Processing (storage client).
- Blast radius: medium — outbound integration only; no API contract change.
- Touches code paths covered by integration tests (Testcontainers + WireMock).

## Plan

1. Introduce a shared `WebClient` builder bean with standard timeouts/retries.
2. Apply an **OpenRewrite** recipe to mechanically migrate call sites
   (`org.openrewrite.java.spring.*` — TODO: pin the exact recipe/version).
3. Manually port any streaming/error-handling that the recipe can't cover.
4. Run ArchUnit + integration tests; verify no `RestTemplate` remains via an
   ArchUnit "no classes should use RestTemplate" rule.
5. Sequence as small PRs per module; update [TD-001](../../quality/technical-debt/TD-001-resttemplate-usage.md).

## Risks

- Reactive/back-pressure semantics differ — mitigate by using `WebClient`
  blocking where appropriate (virtual threads) and reviewing error mapping.
- Hidden timeout behavior changes — mitigate with explicit timeouts + load test.
- Rollback: revert per-module PR; recipe changes are isolated and reviewable.

> TODO: attach the OpenRewrite recipe YAML and the before/after Sonar deltas.
