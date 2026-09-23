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
      │
      ▼
              ══ HUMAN GATE 1 ══
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
`Status: BLOCKED | NEEDS_DECISION | READY`.

The critic is adversarial but never authoritative: it cannot approve itself,
cannot invent requirements, cannot edit `spec.md`. Its evidence comes from Loop A
(graph, ADRs, constraints, domain model, debt register), which is what makes it
more than a style review.

**Enforcement:** `.claude/hooks/guard-speckit-phase.sh` blocks
`speckit-implement` while `critique.md` is missing or `BLOCKED`.

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
```

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
subagents with isolated context; the hooks enforce the ordering, not your memory.

## When a change does not need Loop B

Typos, dependency patches, obvious test corrections, pure refactoring inside one
class: no spec cycle. But anything touching **domain behaviour, an API, the
database, distributed behaviour, security, or a bounded-context boundary** goes
through the full Loop B — and `feature-impact-analysis` is how you find out
which one you are in when it is not obvious.
