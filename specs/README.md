# specs/ — Loop B artifacts

> Feature specifications live in the **product repository**, not here. This
> directory exists so the workspace can hold reference material, and so
> `automation/harness-metrics.sh` has somewhere to look when specs are mirrored.

Per feature, in the product repo:

```
specs/<feature>/
├── spec.md        WHAT / WHY / WHO / behaviour / constraints / acceptance criteria
├── impact.md      J1 — blast radius, contexts, binding ADRs, existing debt
├── decisions.md   the human decision ledger — agent raises, human answers
├── critique.md    J2 — adversarial review, BLOCKED / NEEDS_DECISION / READY
├── gate-log.md    append-only record of every gate evaluation (evidence for the PR)
├── plan.md        technical plan
├── checklist.md   requirements-quality gate (NOT an implementation checklist)
└── tasks.md       small, ordered, dependency-aware, testable
```

Templates: [`../integration/specify/decisions.template.md`](../integration/specify/decisions.template.md).
Contract: [`../.claude/OPERATING_LOOPS.md`](../.claude/OPERATING_LOOPS.md).

`spec.md`, `plan.md`, `checklist.md` and `tasks.md` are produced by Spec Kit.
`impact.md`, `decisions.md`, `critique.md` and `gate-log.md` are this harness's
additions — they are what connect a feature to the architecture model, to a
human decision, and to an audit trail.
