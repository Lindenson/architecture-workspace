---
name: traceability-matrix
description: Generate the chain from requirement to evidence for a feature — REQ id to task to commit to the component it touched in the dependency graph to the binding ADR to the test that covers it. Produces specs/<feature>/traceability.md and names every requirement with no test, no commit or no task. Run after /speckit-converge and before opening the PR. Trigger on "traceability", "прослеживаемость", "is requirement X actually implemented", "what covers REQ-3", "покажи покрытие требований", or when Definition of Done needs to be demonstrated rather than asserted.
---

# Traceability Matrix — evidence, not assertion

> **Loop B → Loop A bridge (join point J3).** Runs after `/speckit-converge`.
> Read-only over code and history; writes only `specs/<feature>/traceability.md`.
> Never edits product code, a numbered ADR, a constraint or the C4 model.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
The Definition of Done in `OPERATING_LOOPS.md` is a checklist a human ticks.
This turns it into a claim a machine can refute.

The links already exist, but only pairwise and only in prose: `tasks.md` knows
requirements, `impact.md` knows components, `decisions.md` knows ADRs, git knows
commits, the graph knows types. Nobody walks the whole chain, so "requirement 3
is done" stays an assertion. The useful output is not the matrix — it is the
**gaps**: a requirement with no test, a commit touching a component nobody
declared, an ADR that binds code the feature changed.

## Inputs / Sources
- `specs/<feature>/spec.md` — the requirement ids. If the spec has no stable
  ids, say so and stop: nothing downstream can be traced to prose.
- `specs/<feature>/tasks.md` — task-to-requirement mapping
- `specs/<feature>/impact.md` — the blast radius *predicted* before implementation
- `specs/<feature>/decisions.md` — answered rows and the ADRs they produced
- `github-mcp` — `extractChangesets`, `readCommits`, `analyzePullRequest`
- `jqassistant-mcp` — `queryGraph`, `dependentsOf` to resolve changed files to
  types and to find what now depends on them
- `architecture/adr/`, `architecture/constraints/`
- test sources in the product repo

## Procedure

1. **Collect requirement ids** from `spec.md`. A requirement without an id
   cannot be traced — list those separately as `UNTRACEABLE` rather than
   silently dropping them.

2. **REQ → task.** From `tasks.md`. A requirement with no task is a gap that
   `speckit-analyze` should have caught; report it as one, and say so.

3. **Task → commit.** `github-mcp.extractChangesets` over the feature branch.
   Match by task id in the commit message where the convention exists; where it
   does not, match by touched path and mark the link `INFERRED`, not `STATED`.
   Never present an inferred link as a recorded one.

4. **Commit → component.** Resolve changed files to fully-qualified types, then
   ask the graph what depends on them (`dependentsOf`). Two questions matter:
   - did the change land **inside** the blast radius `impact.md` predicted?
   - did it land **outside** it? An out-of-radius change is the interesting
     finding: either the impact analysis was wrong, or the implementation went
     somewhere it was not meant to.

5. **Component → ADR.** Which binding ADRs and constraints cover the touched
   components (from `impact.md`, re-checked against `architecture/adr/`). Flag
   any component the feature changed that is governed by an ADR the spec never
   mentioned.

6. **REQ → test.** Find tests covering each requirement: by id in the test name
   or comment, by the acceptance criterion's wording, by the touched component.
   Mark the match `STATED` (an explicit id reference) or `INFERRED` (a
   same-component test). **A requirement covered only by an inferred test is
   reported as untested**, because a test that does not name what it protects
   will not survive the next refactor.

7. **Emit the matrix and, more importantly, the gaps.**

## Output (contract)
Write `specs/<feature>/traceability.md`:

```markdown
# Traceability — <feature>

## Coverage
N of M requirements have a task, a commit and a named test.

| REQ | Task | Commit | Component | ADR | Test | Link quality |
|-----|------|--------|-----------|-----|------|--------------|
| REQ-1 | T-3 | a1b2c3d | com.acme.order.OrderService | ADR-007 | OrderCancelTest#refunds | STATED |
| REQ-2 | T-5 | e4f5g6h | com.acme.pay.RefundService | — | — | **NO TEST** |

## Gaps
- REQ-2: implemented, no test names it
- REQ-4: no task, therefore no implementation
- T-7: a task with no requirement — scope that nobody asked for

## Outside the predicted blast radius
| Component changed | Predicted in impact.md? | Governed by |
| com.acme.ledger.Posting | NO | ADR-004 (ledger ownership) |

## Untraceable
Requirements stated in prose with no id: ...

## Sources used / Confidence
...
```

## Guardrails
- Read-only. This skill measures; it does not fix. A missing test becomes a
  finding and, if the human accepts it, a `tech-debt-delta` entry — never a test
  written by this skill to close its own gap.
- Never infer that a requirement is covered because a nearby test passes.
  `INFERRED` is a weaker claim and is presented as one.
- No graph available → resolve components by package structure, mark the whole
  "outside blast radius" section `LOW` confidence, and say the radius was not
  measured. A matrix that looks complete but was guessed is worse than an
  obviously partial one.
- If `spec.md` has no requirement ids, stop and say so. Recommend adding them;
  do not invent them, because ids you invent will not match the ones a human
  adds later and the next run will contradict this one.
