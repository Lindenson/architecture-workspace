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

## Step 3 — wire the feedback hook (J3)

```bash
cp integration/specify/extensions.yml <product-repo>/.specify/extensions.yml
```

This is what makes convergence trigger a drift check and a memory write instead
of ending at a green checkmark.

## Step 4 — set up the graph for that repo

```bash
jqassistant scan -f target/classes
jqassistant analyze -rule-directory <workspace>/jqassistant/rules
```

Without a populated graph, `feature-impact-analysis` degrades to package
structure and caps its confidence at MEDIUM. It still works — it just guesses
the blast radius instead of measuring it.

## Step 5 — copy the CI gate

Take the `archunit` and `contract-integrity` jobs from
[`../.github/workflows/architecture-gate.yml`](../.github/workflows/architecture-gate.yml)
into the product repo's workflow and point them at its modules. Add:

```yaml
- name: Spec coverage
  run: |
    # every requirement ID in spec.md appears in at least one test
    ...
```

---

## The resulting feature cycle

```
product repo:   /speckit-specify   "users can cancel an order"
                /feature-impact-analysis      → impact.md      [J1, reads the graph]
                /speckit-clarify
new session:    /spec-critic                  → critique.md    [J2]
                ══ HUMAN ══
new session:    /speckit-plan → checklist → tasks → analyze
                ══ HUMAN ══
new session:    /speckit-implement ⇄ /speckit-converge
                after_converge: drift + decision-capture       [J3 → workspace]
                PR → independent review → ══ HUMAN ══ → merge
next session:   SessionStart injects the decisions just made   [J4]
```

Each arrow into `══ HUMAN ══` is a hook-enforced stop, not a convention.

## What NOT to do

- **Do not copy `architecture/`, `domain/` or `quality/` into product repos.**
  One model, many consumers. Copies drift, and drift between copies is invisible.
- **Do not let a product repo write here directly.** J3 artifacts only.
- **Do not run Loop A and Loop B in one session** to save time. It is the one
  shortcut that reliably produces an agent that rewrites the ADR to match the
  code it just wrote.
