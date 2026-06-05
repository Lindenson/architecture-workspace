# Project Memory — Decision Journal

> STATUS: starter. The **decision journal**: a chronological, append-only record
> of every significant decision and *why* — especially **rejected options**
> ("why not X"), which ADRs usually don't capture. Project Memory is the lowest
> level in the [source-of-truth hierarchy](../CLAUDE.md), but it's the *richest*
> for context. An agent **may append** here autonomously (see [`../CLAUDE.md`](../CLAUDE.md)).

## Entry format

Per the [bootstrap spec](../.claude/BOOTSTRAP_ARCHITECT_AGENT.md), each entry records:

**Date · Author · Context · Reason · Result · Consequences · Related ADR · Related Jira**

One file per entry: `YYYY-MM-DD-short-title.md`. Keep the running index in
[`journal.md`](journal.md).

## Why a separate journal from ADRs?

- ADRs are formal, immutable, and record decisions we **adopted**.
- Project Memory also records decisions we **rejected or deferred**, debates, and
  the reasoning that future-us will want when revisiting a choice.

## Entries (EXAMPLE)

- [2026-03-10 — Rejected Kafka Streams](2026-03-10-rejected-kafka-streams.md)
- [2026-05-20 — Rejected MongoDB](2026-05-20-rejected-mongodb.md)

> TODO: append entries as decisions are made; never rewrite history — add a new entry.
