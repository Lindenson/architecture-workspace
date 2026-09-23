# v2.0.0 — Two-loop operating contract

The agent contract changed incompatibly. Read the Migration section before
upgrading an in-flight feature.

---

## The idea

AI-assisted delivery fails in a specific way: the agent that implements a
feature also decides whether the architecture is still sound. It always
concludes that it is.

v2 separates those jobs into two loops that never write into each other:

| | **Loop A — architecture maintenance** | **Loop B — spec implementation** |
|---|---|---|
| Goal | keep the model of the system true | change the system |
| Over product code | read-only | writes it |
| Lives in | this workspace | the product repo (`.specify/`, `specs/`) |
| Cadence | continuous / nightly | per feature |

They exchange four named artifacts — `impact.md` (J1), `critique.md` +
`decisions.md` (J2), drift report + memory entry + debt delta (J3), retrieved
memory (J4) — and a human owns every gate.

Full contract: `.claude/OPERATING_LOOPS.md`.

## Breaking

- **Loop B features now require `impact.md` and `decisions.md`.** Phase gates
  block `plan` and `implement` until both exist and no decision row is `OPEN`.
  In-flight features need the two files added, or `gate.mode: warn` in
  `.claude/aip.config.yml` while you migrate.
- **`JqaService.queryGraph` gained a third parameter** (named Cypher
  parameters). The REST `GET /api/jqassistant/cypher` form is unchanged; the
  POST body now accepts `"params"`. Any direct caller of the two-argument
  method must be updated.
- **`openspec-mcp` removed** from `.mcp.json` and the roadmap. Spec Kit
  artifacts are read directly from the product repository instead.
- **`.claude/settings.json` rewritten** with `$CLAUDE_PROJECT_DIR` in place of
  hardcoded absolute paths. Anyone who copied the old file has permissions that
  silently never matched.

## Added

- **Two-loop operating contract** — `.claude/OPERATING_LOOPS.md`, with the
  write-rights table and the four join points.
- **`decisions.md`, the human decision ledger** — `impact.md` and `spec-critic`
  append `OPEN` rows with real options; humans answer. Agents may raise rows and
  may never answer them. Template: `integration/specify/decisions.template.md`.
- **Six loop skills** — `session-orientation`, `feature-impact-analysis` (graph
  blast radius → bounded contexts → binding ADRs → existing debt), `spec-critic`
  (adversarial pre-implementation review), `decision-capture` (decisions,
  including rejected options, into long-term memory), `tech-debt-delta`,
  `knowledge-reindex`.
- **Configurable hooks** — phase gate, loop-aware governance guard, drift
  bookkeeping, session memory digest. All modes and paths live in
  `.claude/aip.config.yml`; `AIP_GATE_OVERRIDE="<reason>"` always works and is
  always logged to `specs/<feature>/gate-log.md`.
- **CODEOWNERS + `human-gate` CI job** — the enforcement an agent cannot write
  its way past. No PR carrying an unanswered decision reaches a merge; every
  gate override is surfaced as a reviewer warning.
- **Plugin packaging** — `/plugin install architecture-intelligence@aip-marketplace`
  brings the whole contract into any product repository.
- **`automation/verify-hooks.sh`** (26 assertions) and
  **`automation/harness-metrics.sh`** (gate activity, override rate, decisions
  answered / deferred / never closed).
- `reports/`, `jqassistant/`, `specs/`, `integration/` — directories the README
  advertised but the tree did not contain.

## Fixed

- **Glob lists underwent pathname expansion.** `$(cfg architect_owned.paths)`
  unquoted meant `domain/raw/**` expanded against the working directory, so the
  guard silently matched nothing — and only from some directories. Found by the
  new self-test, invisible to review.
- **The governance guard blocked the architect** in their own workspace. Now
  loop-aware: warn in Loop A, block in Loop B, both configurable.
- **`queryGraph` accepted no parameters**, making every set-based Cypher query
  in impact analysis unexecutable as documented.
- **`.drift-pending` was a one-way latch** and was not gitignored: it nagged
  forever and would have been committed. `automation/reset-drift-flag.sh`
  clears it after a real rescan.
- **The session digest claimed semantic retrieval while running a filename
  sort.** It now attempts rag-mcp and states which mode produced it —
  `semantic retrieval (rag-mcp)` or `DEGRADED to newest-first`.
- `extensions.yml` referenced two skills that did not exist; both now ship and
  CI asserts every reference resolves.
- A stray empty skill directory created by a failed brace expansion.
- Pinned `structurizr/cli` to `v2025.11.09`. Note that the upstream repository
  was archived on 2026-02-04; that is its final release.

## Honesty about what is a lock

Local hooks read files that an agent writes, so they are **fast feedback and an
audit trail, not a lock**. The locks are `.github/CODEOWNERS` with branch
protection, and the `human-gate` CI job. Every gate evaluation is appended to
`gate-log.md` and travels into the PR. `.claude/OPERATING_LOOPS.md` has the
full table; nothing in the docs calls a log a lock.

## Migration

```bash
# 1. product repo: the feedback loop and the decision ledger
cp integration/specify/extensions.yml        <product-repo>/.specify/extensions.yml
cp integration/specify/decisions.template.md <product-repo>/specs/<feature>/decisions.md

# 2. the actual lock
cp .github/CODEOWNERS <product-repo>/.github/CODEOWNERS   # edit the owners
# then: Settings → Branches → Require a pull request → Require review from Code Owners

# 3. verify before trusting
./automation/verify-hooks.sh          # 26 assertions
./automation/verify-hooks.sh --live   # capture your build's real hook schema
claude --debug hooks
```

Start the team on `gate.mode: warn`. Read `gate-log.md` for two weeks. Then
switch to `block`. A gate that is wrong once and cannot be overridden gets
deleted along with the cases where it was right.

## Known limits

- The tool-call JSON schema the phase gate reads is not a stable contract across
  Claude Code versions. The gate no-ops on an unrecognised shape, so a mismatch
  shows up as "the gate never fires", not as a broken session. Verify with
  `verify-hooks.sh --live`.
- Plugin component-path conventions differ between versions; see
  `.claude-plugin/README.md` for how to check and what to change.
- `mcp-servers` has not been built against a full dependency set in this
  release's verification environment. The `queryGraph` change was compiled and
  behaviourally tested in isolation (write-guard, blank input, null params,
  defensive copy); run `mvn -B verify` before tagging.
- SSE remains the MCP transport for all nine servers. Streamable HTTP is the
  current direction and is deferred to a later release.
