---
name: session-orientation
description: Read this FIRST in every session before any other skill, tool or edit. Establishes which of the two operating loops you are in — ARCHITECTURE MAINTENANCE (observe the system, keep the model true, read-only over product code) or SPEC IMPLEMENTATION (change the system through an approved specification) — and which write rights, gates and output contract apply. Trigger on session start, on "where are we", "покажи состояние", "начнём фичу", "continue the work", or whenever a request could plausibly belong to either loop.
---

# Session Orientation

## Purpose
Prevent the single most expensive failure mode of this platform: mixing the
observation loop with the change loop. Full contract in
[`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md).

**The SessionStart hook, not this skill, is the authoritative statement of which
loop you are in** — skill routing is model-driven and may not fire, whereas the
hook always runs and prints `LOOP: A` or `LOOP: B`. Read this skill for the
*rules that follow from* the loop: write rights, the legal next step, and what to
do when a task needs a write the loop forbids. If the digest is absent (hooks not
wired), fall back to step 1 below and say so.

## Procedure

### 1. Determine the loop — if the session digest did not already say

| Signal | Loop |
|---|---|
| Request is "what is the state / is this sound / analyze / review / report" | **A — Architecture Maintenance** |
| Request is "add / change / fix / implement / build <behaviour>" | **B — Spec Implementation** |
| `.specify/feature.json` exists and names an active feature | **B** |
| Working directory is this workspace and there is no product code | **A** |
| Both plausible, or unclear | **ASK THE HUMAN. Do not guess.** |

State the answer explicitly in your first response:
`LOOP: A` or `LOOP: B — feature <name>, phase <phase>`.

### 2. Load the context that loop needs — and only that

**Loop A:**
1. `.claude/ROLE_ARCHITECT_AGENT.md`, `.claude/OPERATING_MODEL.md`, `.claude/AGENT_RUNTIME.md`
2. `knowledge/` → `architecture/` (slow-changing context)
3. Live state: `digital-twin-core.showProjectState`

**Loop B:**
1. `specs/<feature>/` — `spec.md`, `impact.md`, `critique.md`, `plan.md`, `tasks.md`
2. `.specify/` constitution and feature state
3. Constraints that bind this feature: the ADRs and constraints named in `impact.md` — not the whole `architecture/` tree
4. Relevant `project-memory/` entries (injected by the SessionStart hook; retrieve more with `rag-mcp.retrieveContext` if the feature touches an area with history)

Loading the whole workspace into a Loop B session is a context leak, not
diligence. `impact.md` exists precisely so the implementer reads the ten pages
that bind this change instead of four hundred that do not.

### 3. Apply the write rights of that loop

| | Loop A may write | Loop B may write |
|---|---|---|
| product code / tests | ✗ | ✓ |
| `specs/<feature>/*` | only `impact.md`, `critique.md` | ✓ |
| `reports/`, `domain/raw/`, `domain/semantic/`, `quality/` | ✓ | ✗ |
| `project-memory/` | ✓ append | ✓ append, via `decision-capture` only |
| `architecture/adr/drafts/`, `knowledge/drafts/` | ✓ | ✓ (drafts only) |
| final ADR, `architecture/constraints/`, `architecture/standards/`, `domain/model/`, `delivery/roadmap/` | ✗ architect only | ✗ architect only |

If the task you were given requires a write the current loop does not allow:
**stop and say so.** Do not switch loops mid-session to unlock a write — that is
exactly the bias this separation exists to remove.

### 4. Know the next legal step

**Loop A:** `SHOW_PROJECT_STATE` → targeted skill (`architecture-review`,
`tech-debt-review`, `architecture-drift-analysis`, `risk-analysis`,
`release-readiness-review`) → report + escalation.

**Loop B:** the phase order is tracked by hooks and enforced on the merge path —
`specify → feature-impact-analysis → clarify → spec-critic → ⟨HUMAN answers
decisions.md⟩ → plan → checklist → tasks → analyze → ⟨HUMAN⟩ → implement ⇄
converge → after_converge → PR → independent review → ⟨HUMAN⟩ → merge`.

`after_converge` is four skills, not one (wired in `.specify/extensions.yml`):
`architecture-drift-analysis` (did code and model diverge — and it clears the
drift flag when the scan is complete) · `decision-capture` (the answered
`decisions.md` rows become durable memory) · `tech-debt-delta` (what this feature
closed, created or deferred) · `knowledge-reindex` (make it retrievable next
session — optional, needs the knowledge layer on).

If a phase artifact is missing, produce it — do not skip ahead. A blocked hook
is information, not an obstacle to route around.

## Output (contract)
Open every session with two lines:

```
LOOP: <A | B — feature <name>, phase <phase>>
CONTEXT: <files/MCP sources loaded>  ·  CONFIDENCE: <HIGH|MEDIUM|LOW>
```

Then answer the request under that loop's rules.

## Guardrails
- Never run both loops in one session. To hand over, write the artifact (report,
  `impact.md`, memory entry) and start a new session or delegate to a subagent.
- Never let Loop B edit an ADR, constraint or the C4 model to make code
  compliant. Drift is reported, not erased.
- Never let Loop A propose a product requirement. It reports evidence; the
  human decides.
- On any conflict between sources, the source-of-truth hierarchy in
  `CLAUDE.md` applies in both loops. Code always wins.
- A blocked hook is information. Produce the missing artifact rather than
  routing around it — and if the gate is genuinely wrong, use
  `AIP_GATE_OVERRIDE="<reason>"`, which is allowed and recorded, instead of
  editing the artifact the gate reads.
- Loop B: raise questions in `decisions.md`; never answer them. Filling in a
  human's answer defeats the only place a person is recorded as deciding.
