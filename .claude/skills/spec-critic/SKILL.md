---
name: spec-critic
description: Adversarially review a feature specification BEFORE implementation and produce specs/<feature>/critique.md with a BLOCKED / NEEDS_DECISION / READY verdict. Checks requirement completeness, edge cases, domain ownership, distributed-systems semantics, API and data contracts, and testability against the architecture graph, ADRs, constraints and the debt register. Trigger on "critique the spec", "раскритикуй спеку", "review this specification", "is this spec ready", or before /speckit-plan on any non-trivial feature.
---

# Spec Critic — join point J2

> **Loop B — spec implementation.** Runs in the PRODUCT repository against an
> active feature. Writes only `specs/<feature>/` artifacts; never product code,
> and never this workspace's governance files.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
Find the problems in a specification while they are still cheap. The critic is
**adversarial but not authoritative**: it challenges decisions, it never makes
them.

Run in a session separate from the one that wrote the spec. Ideally delegate to a
subagent so the critique is not contaminated by the authoring context.

## Hard boundaries — the critic MUST NOT
- choose product requirements or invent missing ones
- edit `spec.md`, `plan.md`, ADRs, constraints or the C4 model
- write implementation code
- approve its own recommendations or mark a spec READY to unblock a pipeline

## Inputs / Sources
`spec.md` · `impact.md` (**required** — if missing, run
`feature-impact-analysis` first; a critique without measured impact is opinion) ·
`plan.md` if it exists · the project constitution · `architecture/adr/` ·
`architecture/constraints/` · `architecture/standards/` ·
`domain/model/bounded-contexts.md` · `quality/technical-debt/` ·
`project-memory/` (was this decided and rejected before?) ·
`jqassistant-mcp` and `structurizr-mcp` for evidence · existing code and tests.

## Procedure

Work through the checks below. **Every finding must carry evidence** — a file,
an ADR ID, a graph query result, a memory entry. A finding without evidence is a
question, and belongs under Ambiguities, not Critical Issues.

### Requirements
Unambiguous behaviour? Actors, inputs, outputs identified? State transitions
defined? Failure states defined? Is there a requirement that cannot be tested?

### Edge cases
Empty input · duplicate request · retry · timeout · concurrent request · partial
failure · downstream unavailable · database failure · network failure ·
reconnect · reordering · stale data · authorization failure.

### Domain
Who owns the entity? Who owns the state transition? Which service is the source
of truth? Does the feature cross a bounded context (check `impact.md`
classification)? Are ownership boundaries explicit, or assumed?

### Distributed behaviour (when `impact.md` says CROSS-SERVICE)
Idempotency · ordering · delivery semantics · retry semantics · timeouts · dead
letters · duplicate events · event versioning · consistency model · concurrency ·
recovery. Each unanswered one is at least `NEEDS_DECISION`.

### API and data
Request / response / error contracts · backward compatibility · versioning ·
validation · authorization · schema changes · migration **and rollback** ·
retention · indexes · constraints · transaction boundaries.

### Memory — does this contradict a decision already made?

**Run this check explicitly; do not fold it into "architecture compliance".**
A memory nobody argues with is an archive.

`project-memory/` holds entries of type `rejected` and `accepted trade-off`
alongside the adopted ones. That is the expensive half: ADRs record what was
chosen, memory records what was *considered and turned down*, and why. It is
worth nothing if a feature can quietly propose the same thing six months later
and no one raises a hand.

1. Extract the approach the spec proposes — technology, pattern, boundary,
   storage choice, integration style.
2. Search `project-memory/` for entries touching the same subject. Use
   `rag-mcp.retrieveContext` when the knowledge layer is on; otherwise grep the
   directory. Read the `type:` and `bounded_context:` metadata, not only titles.
3. For each `rejected` or `superseded` entry that matches:
   - If the spec proposes it again, raise a **Critical Issue** citing the entry
     by filename and date. The finding is not "this is wrong" — it is "this was
     rejected on <date> for <reason>; the spec must say what changed."
   - Reversing an old decision is legitimate and often correct. What is not
     legitimate is reversing it *without noticing*. Force the argument into the
     open, then let the human decide.
4. Check `Revisit when` on matching adopted entries. If the stated condition has
   now come true, say so: the earlier decision may be due for review regardless
   of this feature.
5. If memory holds nothing about the subject, say that too. "No prior decision
   found on X" is information — it tells the human they are deciding this for
   the first time.

Never overrule memory yourself, and never treat an old entry as binding. The
critic surfaces the contradiction; the human resolves it, and
`decision-capture` records the resolution as a new entry superseding the old.

### Architecture compliance
Does the spec require anything an ADR or constraint forbids? Does it introduce an
undocumented cross-service dependency? Does it deepen debt listed in `impact.md`?
Was this approach already rejected in `project-memory/`? — cite the entry.

### Testability
Reject acceptance criteria like "works correctly", "fast", "secure", "reliable"
unless converted into measurable criteria. Name the measurement for each.

## Output (contract)
Write `specs/<feature>/critique.md`:

```markdown
# Specification Critique — <feature>

## Status
BLOCKED | NEEDS_DECISION | READY

## Sources used / Confidence
...  ·  HIGH | MEDIUM | LOW

## Critical Issues
### C1 — <title>
Problem: ...
Why it matters: ...
Evidence: <file / ADR-00x / cypher result / memory entry>
Required human decision: ...

## Ambiguities
### A1 — Question: ...

## Missing Requirements
### M1 — ...

## Domain / Architecture Concerns
### D1 — ...

## Contradicts a Prior Decision
### P1 — <subject>
Prior entry: project-memory/YYYY-MM-DD-slug.md (type: rejected, <date>)
Then: <what was rejected and why>
Now: <what this spec proposes>
What must change to justify reversing it: ...
(or: "No prior decision found on <subject> — this is being decided for the first time.")

## Failure Modes
### F1 — ...

## Contract Concerns
### API-1 — ...

## Testing Concerns
### T1 — ...

## Human Decisions Required
Each of these is ALSO appended to `specs/<feature>/decisions.md` as an `OPEN`
row — that ledger is what the human answers, what the gate counts, and what
`decision-capture` reads afterwards. Listing a question only here means it gets
answered in a chat window and lost.

1. ...
2. ...

## Recommendation
<what the critic would ask the human to do next — never an approval>
```

### Status semantics
- **BLOCKED** — an ADR/constraint violation, an unowned state transition, or a
  missing failure-mode definition on a CROSS-SERVICE feature. Implementation must
  not start. The hook enforces this.
- **NEEDS_DECISION** — the spec is coherent but contains open product or
  architectural choices only the human can make.
- **READY** — no critical issue found. This is a *recommendation to the human*,
  not an approval, and never a self-issued green light.

## Guardrails
- One loop per session: the critic does not implement, and the implementer does
  not critique its own feature.
- Empty critique is suspicious. If you find nothing, say what you checked and
  with what confidence — silence is not evidence of quality.
- If `impact.md` is missing or its confidence is LOW, say so in the header and
  cap the critique's confidence accordingly.
- Append to `decisions.md`; never fill its `Answer` / `Answered by` / `Status`
  columns. You raise questions; humans close them.
- A prior rejection is evidence, not a veto. Cite it, state what would have to
  be true to reverse it, and stop there.
- `READY` is a recommendation, not an approval. The real approval is a human on
  the PR (CODEOWNERS); every gate evaluation is recorded in `gate-log.md` so a
  reviewer can see whether a status was earned or asserted.
