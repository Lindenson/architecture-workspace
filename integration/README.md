# Integration — wiring the workspace into product repositories

This workspace holds no product code. The architecture loop (A) lives here; the
implementation loop (B) lives in each product repo. This directory is the seam.

Read [`../.claude/OPERATING_LOOPS.md`](../.claude/OPERATING_LOOPS.md) first — it
defines both loops and the four join points this guide installs.

---

## Step 1 — publish the workspace as a plugin

`.claude/` is invisible to a session opened in a product repository. Packaging it
as a Claude Code plugin makes the skills, agents, hooks and MCP wiring available
everywhere, as one versioned unit.

```bash
# once, from any machine
/plugin marketplace add Lindenson/architecture-workspace
```

```bash
# in each product repository
/plugin install architecture-intelligence@aip-marketplace
/reload-plugins
```

The plugin ships:
- **skills** — `session-orientation`, `spec-critic`, `feature-impact-analysis`,
  `decision-capture`, plus the 16 Loop A analysis skills
- **agents** — the 7 subagents
- **hooks** — phase-order gate, governance guard, drift bookkeeping, memory digest
- **MCP servers** — `.mcp.json` (still requires `AIP_INTERNAL_TOKEN` in the
  product repo's environment)

Version the plugin deliberately: a governance contract that changes silently
underneath ten developers is worse than no contract.

## Step 2 — install Spec Kit in the product repository

```bash
uvx --from git+https://github.com/github/spec-kit.git specify init --here
```

Then, once per project:

```
/speckit-constitution   # project-wide principles — the architect writes these,
                        # derived from architecture/constraints/ + standards/
```

The constitution is **not** a copy of this workspace's constraints. It is the
subset that binds day-to-day implementation, phrased so `speckit-analyze` can
evaluate against it.

## Step 2b — verify the hook schema against YOUR build

The gate reads the skill name out of the tool-call JSON. That shape is not a
stable contract across Claude Code versions, so confirm it once rather than
trusting it forever:

```bash
./automation/verify-hooks.sh          # 26 assertions against the hook scripts
./automation/verify-hooks.sh --live   # prints a debug hook to capture real input
claude --debug hooks                  # shows which hooks are actually registered
```

If the live capture shows the skill name in a field the guard does not read, add
it to the `json_get` chain at the top of `guard-speckit-phase.sh`. The guard
no-ops on an unrecognised shape, so a mismatch shows up as "the gate never
fires", not as a broken session.

## Step 3 — wire the feedback hook (J3) and the decision ledger

```bash
cp integration/specify/extensions.yml <product-repo>/.specify/extensions.yml
# and, at the start of every feature:
cp integration/specify/decisions.template.md <product-repo>/specs/<feature>/decisions.md
```

`extensions.yml` makes convergence trigger a drift check and a memory write
instead of ending at a green checkmark. `decisions.md` is where the human
actually stands: agents append `OPEN` questions, humans answer, the gate counts
open rows, and CI fails a PR that still carries one.

## Step 4 — set up the graph for that repo

```bash
jqassistant scan -f target/classes
jqassistant analyze -rule-directory <workspace>/jqassistant/rules
```

Without a populated graph, `feature-impact-analysis` degrades to package
structure and caps its confidence at MEDIUM. It still works — it just guesses
the blast radius instead of measuring it.

## Step 5 — install the actual lock

Everything above is fast feedback. The enforcement an agent cannot write past
lives outside the working tree:

```bash
cp .github/CODEOWNERS <product-repo>/.github/CODEOWNERS   # then edit the owners
```

Then, in GitHub: **Settings → Branches → Require a pull request → Require review
from Code Owners**. Without branch protection, CODEOWNERS is documentation.

Take the `archunit`, `contract-integrity`, `harness` and `human-gate` jobs from
[`../.github/workflows/architecture-gate.yml`](../.github/workflows/architecture-gate.yml)
into the product repo and point them at its modules. `human-gate` is the one
that matters most: it fails any PR whose `decisions.md` still has an `OPEN` row,
and raises a reviewer warning for every gate override recorded in `gate-log.md`.

Add a spec-coverage step once you have a requirement-ID convention:

```yaml
- name: Spec coverage
  run: |
    # every requirement ID in spec.md appears in at least one test
    ...
```

## Step 6 — measure the harness

```bash
./automation/harness-metrics.sh
```

Reports gate activity, override rate, and how many human questions were raised,
answered, deferred or never closed. Read it monthly. Zero blocks in a quarter
means the gate is theatre; an override rate above 40% means the rule is wrong,
not the team; a pile of `DEFERRED` rows is debt with a nicer name.

---

## The resulting feature cycle

```
product repo:   /speckit-specify   "users can cancel an order"
                /feature-impact-analysis   → impact.md + OPEN rows in decisions.md
                /speckit-clarify
new session:    /spec-critic               → critique.md + more OPEN rows   [J2]
                ══ HUMAN ══  answers land in decisions.md, with a name and a date
new session:    /speckit-plan → checklist → tasks → analyze
                ══ HUMAN ══
new session:    /speckit-implement ⇄ /speckit-converge
                after_converge: drift-analysis · decision-capture ·
                                tech-debt-delta · knowledge-reindex          [J3]
                PR: decisions.md + gate-log.md + debt delta are the evidence
                    CI human-gate fails on any OPEN row
                    CODEOWNERS requires the architect      ══ HUMAN ══ → merge
next session:   SessionStart injects the decisions just made                 [J4]
```

The stops marked `══ HUMAN ══` are enforced on the **merge path** (CODEOWNERS +
the `human-gate` CI job). The local hooks make the right order the cheap order
and record every deviation in `gate-log.md`; they are not the lock. Keeping that
distinction straight is the difference between a harness people trust and one
they route around.

## What NOT to do

- **Do not copy `architecture/`, `domain/` or `quality/` into product repos.**
  One model, many consumers. Copies drift, and drift between copies is invisible.
- **Do not let a product repo write here directly.** J3 artifacts only.
- **Do not run Loop A and Loop B in one session** to save time. It is the one
  shortcut that reliably produces an agent that rewrites the ADR to match the
  code it just wrote.
