# Releases

> STATUS: starter. Holds **release records** and feeds the **release-readiness**
> review (see [role doc](../../.claude/ROLE_ARCHITECT_AGENT.md) → Modes).

## Release record

One file per release, `YYYY.MAJOR.MINOR.md` (or your scheme), from
[`_TEMPLATE-release.md`](_TEMPLATE-release.md): version, scope, risks, readiness.

## Release readiness (verdict)

A release-readiness review combines:
- Critical defects (Jira) and [architecture violations](../../quality/architecture-violations/README.md).
- [Technical debt](../../quality/technical-debt/README.md) blocking the scope (e.g. [TD-002](../../quality/technical-debt/TD-002-missing-idempotency-on-payment-endpoint.md)).
- [Sonar](../../quality/sonar/README.md) Quality Gate.
- Epic status + architecture coverage ([epics](../epics/README.md)).
- Validation on [staging](../../knowledge/environments.md).

Verdict is one of: **READY / READY WITH RISKS / NOT READY**, with cited evidence.

> TODO: link the release pipeline and the changelog source.
