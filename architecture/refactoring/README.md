# Refactoring Proposals

> STATUS: starter. This directory holds **refactoring proposals** — structured
> arguments for a code-wide change, often automatable (e.g. via OpenRewrite).
> An agent may draft these autonomously; execution requires team agreement
> (see [`../../CLAUDE.md`](../../CLAUDE.md)).

## What goes here

Each proposal is one file describing a single refactoring, using this shape:

- **Problem** — what is wrong / what we want to change.
- **Evidence** — facts: counts from Sonar/jQAssistant/grep, affected modules.
- **Impact** — blast radius, risk, who is affected.
- **Plan** — concrete steps (recipe, PR sequencing, validation).
- **Risks** — what could go wrong + mitigations / rollback.

Proposals should link the driving [tech debt](../../quality/technical-debt/README.md),
[ADRs](../adr/README.md) and Jira keys.

## Examples

- [openrewrite-resttemplate-to-webclient.md](openrewrite-resttemplate-to-webclient.md)

> TODO: add proposals as they arise; close them out by linking the merged PRs.
