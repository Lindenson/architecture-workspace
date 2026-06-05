# Epics

> STATUS: starter. Epics are **mirrored from Jira** (the system of record for
> planning — level 6 in the [hierarchy](../../CLAUDE.md)); this directory is a
> read-model for governance, not the source of truth.

## How epics are mirrored

- The `jira-mcp` server pulls epics for the project (example key `PAY`) into
  lightweight local records, so the digital twin can reason offline.
- Suggested file per epic: `PAY-NNN-short-title.md` with: summary, status,
  linked stories, target release, and **architecture coverage**.

## Architecture coverage

For each epic we track its **architecture coverage** — which architectural
artifacts the epic touches or requires:

- Related ADRs / constraints / standards.
- New or affected [bounded contexts](../../domain/model/bounded-contexts.md).
- Risks / tech debt it creates or pays down.
- Whether it needs a new ADR before implementation.

This lets a release-readiness review answer "is this epic architecturally
covered?" — i.e. no un-ADR'd architectural change is shipping unreviewed.

## Epic record (EXAMPLE skeleton)

```
ID: PAY-100
Title: Payment idempotency
Status: In Progress
Stories: PAY-231, PAY-232 ...
Target release: 2026.7.0
Architecture coverage:
  - ADRs: ADR-003
  - Constraints: integration (idempotency)
  - Debt: TD-002 (paid down)
  - New ADR needed: no
```

> TODO: wire the Jira pull and pin the project key(s).
