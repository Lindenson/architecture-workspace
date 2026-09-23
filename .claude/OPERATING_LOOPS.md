# OPERATING_LOOPS

Version: 1.0 · Type: Contract · Applies to: Chief Agent, all subagents, all sessions

## Why this document exists

This platform runs **two different loops** with different cadences, different
write rights and different failure modes. Most damage in AI-assisted projects
comes from running them as one: an agent that is "keeping the architecture
current" starts editing product code, or an agent that is implementing a feature
quietly rewrites an ADR so the code becomes compliant.

**Before anything else in a session, establish which loop you are in.**
See [`skills/session-orientation/SKILL.md`](skills/session-orientation/SKILL.md).

---

## Loop A — ARCHITECTURE MAINTENANCE

> Keep the model of the system true. Observe, never change.

```
PRODUCT REPOS (read-only)
      │
      ▼
SCAN            jQAssistant → Neo4j · Sonar · Git · Structurizr
      │
      ▼
FACTS           domain/raw/ · quality/ · ARCHITECTURE_GRAPH
      │
      ▼
SYNTHESIS       digital-twin-core → DIGITAL_TWIN_MODEL
      │
      ▼
DRIFT           code ↔ Structurizr ↔ ADR ↔ constraints
      │
      ▼
ARTIFACTS       reports/ · domain/drift/ · quality/technical-debt/
      │
      ▼
ESCALATION      architect decides → ADR / constraint / roadmap
```

| | |
|---|---|
| **Trigger** | nightly pipeline, `SHOW_PROJECT_STATE`, `RUN_ARCHITECTURE_RESCAN`, explicit review request |
| **Cadence** | continuous / daily |
| **Scope** | the whole system |
| **May write** | `reports/`, `domain/raw/`, `domain/semantic/`, `quality/`, `project-memory/`, `*/drafts/` |
| **May NOT write** | product code, `architecture/adr/*` (non-draft), `architecture/constraints/`, `architecture/standards/`, `delivery/roadmap/`, `domain/model/` |
| **Output contract** | Source(s) · Confidence · Inconsistencies · Linked artifacts · Recommendations |
| **Failure mode to avoid** | inventing architecture; "fixing" drift by editing the diagram |

---

## Loop B — SPEC IMPLEMENTATION

> Change the system. One feature at a time, against an approved specification.

```
HUMAN INTENT
      │
      ▼
/speckit-specify        →  specs/<feature>/spec.md
      │
      ▼
/feature-impact-analysis →  specs/<feature>/impact.md      ← J1 (facts from Loop A)
      │
      ▼
/speckit-clarify
      │
      ▼
/spec-critic            →  specs/<feature>/critique.md     ← J2 (gate)
      │                    + OPEN rows in decisions.md
      ▼
              ══ HUMAN GATE 1 ══  answers land in decisions.md
      │
      ▼
/speckit-plan → /speckit-checklist → /speckit-tasks → /speckit-analyze
      │
      ▼
              ══ HUMAN GATE 2 ══
      │
      ▼
/speckit-implement  ⇄  /speckit-converge
      │
      ▼
after_converge hooks    →  drift check + decision capture  ← J3 (feedback to Loop A)
      │
      ▼
PR → independent review → ══ HUMAN GATE 3 ══ → MERGE
```

| | |
|---|---|
| **Trigger** | a human feature request |
| **Cadence** | per feature |
| **Scope** | one feature, the blast radius named in `impact.md` |
| **Lives in** | the **product repository** (`.specify/`, `specs/`), not here |
| **May write** | product code, tests, `specs/<feature>/*` |
| **May NOT write** | anything under this workspace except through J3 artifacts |
| **Output contract** | every requirement traceable to a task, a commit and a test |
| **Failure mode to avoid** | silently deciding product or architectural questions to make implementation easier |

---

## The join points

The loops **never write into each other**. They exchange four named artifacts.
This is the whole integration.

### J1 — Context injection (A → B), at spec time

Before planning, Loop B pulls a *context pack* built from Loop A's facts.

**Produced by** `feature-impact-analysis` → `specs/<feature>/impact.md`
**Contains** affected types/packages (graph blast radius) · affected bounded
contexts · applicable ADRs and constraints · existing technical debt sitting in
the blast radius · cross-service contracts touched.

Without J1 the spec is written blind and the critic has nothing to cite.

### J2 — Gate (A → B), before implementation

**Produced by** `spec-critic` → `specs/<feature>/critique.md` with
`Status: BLOCKED | NEEDS_DECISION | READY`, and — crucially — by
`specs/<feature>/decisions.md`, the **human decision ledger**.

The ledger is where the man-in-the-middle actually stands. `impact.md` and
`critique.md` append `OPEN` rows (question, who raised it, the real options); a
human fills in the answer, who answered, and when. Agents may raise rows and may
never answer them. The gate counts `OPEN` rows; CI fails a PR that still carries
one; `decision-capture` reads the answered rows as its input instead of
reconstructing intent from a diff.

Without this ledger the three HUMAN GATEs are arrows on a diagram: questions get
asked in `critique.md`, answered in a chat window, and lost. Template:
[`../integration/specify/decisions.template.md`](../integration/specify/decisions.template.md).

The critic is adversarial but never authoritative: it cannot approve itself,
cannot invent requirements, cannot edit `spec.md`. Its evidence comes from Loop A
(graph, ADRs, constraints, domain model, debt register), which is what makes it
more than a style review.

**Enforcement — and its honest limit.** `.claude/hooks/guard-speckit-phase.sh`
blocks `speckit-implement` while `critique.md` is missing or `BLOCKED`, and
while `decisions.md` still has `OPEN` rows. But `critique.md` is written by an
agent, so a gate that reads it is an **audit trail, not a lock**: an agent can
set the status itself.

What the hook actually guarantees is that every evaluation — pass, block,
warning, override — lands in `specs/<feature>/gate-log.md`, which travels into
the PR. The enforcement an agent cannot write past is
[`.github/CODEOWNERS`](../.github/CODEOWNERS) plus branch protection, and the CI
job that fails a PR carrying an `OPEN` decision row. Treat the local hook as the
fast feedback loop and the PR as the gate.

Every gate is configurable in `.claude/aip.config.yml` (`block` / `warn` / `off`)
and every block can be overridden with `AIP_GATE_OVERRIDE="<reason>"`, which is
always allowed and always logged. A gate that is wrong once and cannot be
bypassed gets deleted — along with the cases where it was right.

### J3 — Feedback (B → A), after convergence

Loop B does not update the architecture model. It emits a delta and Loop A
re-scans the blast radius.

**Mechanism:** `.specify/extensions.yml` → `hooks.after_converge`:

```yaml
hooks:
  after_converge:
    - architecture-drift-analysis   # did code and model diverge?
    - decision-capture              # what was decided, and why
    - tech-debt-delta               # what debt was closed / created
    - knowledge-reindex             # make it findable next session (optional)
```

All four exist as skills under `.claude/skills/`. The entry format above is read
from `.specify/extensions.yml` by Spec Kit after `converge`; verify the exact
schema your Spec Kit version expects before relying on it —
`integration/README.md` says how.

`decision-capture` appends to `project-memory/` and, where the decision is
durable, emits an ADR **draft** for the architect. It never writes a final ADR.

### J4 — Memory (persistent, both loops)

`project-memory/` entries are indexed by `rag-mcp` (pgvector) with metadata
`{bounded_context, adr_ref, components[], status}`. The `SessionStart` hook
retrieves the entries relevant to the active feature (from
`.specify/feature.json`) and injects them as context.

This is what makes a new session continue work instead of asking
"what were we doing?".

---

## The invariant

```
Loop A owns FACTS and the MODEL.
Loop B owns CODE.

Neither writes the other's artifacts.
They exchange impact.md · critique.md · drift report · memory entry.

A human owns every decision at the three gates.
```

Corollary — **one loop per session**. The agent that implements a feature is not
the agent that certifies the architecture afterwards. Use separate sessions or
subagents with isolated context; the hooks track the ordering so you do not have
to remember it, and the PR enforces it.

## What is a lock and what is a log

| Mechanism | Strength | Why |
|---|---|---|
| `.github/CODEOWNERS` + branch protection | **lock** | outside the working tree; an agent cannot approve a PR as the architect |
| CI `human-gate` job (no `OPEN` rows) | **lock** | runs on the merge path, not on the agent's machine |
| `guard-speckit-phase.sh` | log + fast feedback | reads agent-written files; overridable by design |
| `mark-drift-pending.sh` | log + fast feedback | blocks in Loop B, warns in Loop A |
| `gate-log.md`, `decisions.md`, `project-memory/` | evidence | append-only, travels into the PR, feeds `automation/harness-metrics.sh` |

Do not confuse the rows. Most of the value of the local hooks is that they make
the right thing the cheap thing; none of it is that they are unbreakable.

## When a change does not need Loop B

Typos, dependency patches, obvious test corrections, pure refactoring inside one
class: no spec cycle. But anything touching **domain behaviour, an API, the
database, distributed behaviour, security, or a bounded-context boundary** goes
through the full Loop B — and `feature-impact-analysis` is how you find out
which one you are in when it is not obvious.
