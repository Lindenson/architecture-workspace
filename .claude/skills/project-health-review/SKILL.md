---
name: project-health-review
description: OVER TIME — a scored health assessment comparing the current state against previous snapshots in reports/, for the monthly cadence or an executive summary: are architecture, quality, debt, delivery, knowledge and risk getting better or worse, and what changed. Trigger on "project health", "оцени здоровье проекта", "health check", "стало лучше или хуже", "monthly review", "executive summary". For the live picture right now use project-state-review instead.
---

# Project Health Review

> **Loop A — architecture maintenance.** Read-only over product code.
> May write: `reports/`, `quality/`, `domain/raw/`, `domain/semantic/`,
> `project-memory/` (append), and drafts under `architecture/adr/drafts/`,
> `knowledge/drafts/`, `quality/technical-debt/drafts/`.
> Architect-owned and blocked for you: numbered ADRs, `architecture/constraints/`,
> `architecture/standards/`, `architecture/c4/workspace.dsl`, `domain/model/`,
> `delivery/roadmap/`. Emit a draft and escalate instead of editing them —
> drift is REPORTED, never erased by editing the model.
> Contract: [`../../OPERATING_LOOPS.md`](../../OPERATING_LOOPS.md)

## Purpose
Score the digital twin across all state domains **and compare against history**,
so the team sees direction, not just position. Aligns with the monthly procedure
and the Project Health Snapshot of `ARCHITECTURE_RESCAN`.

**Not the same as [`project-state-review`](../project-state-review/SKILL.md).**
That one answers *what is true now* and is the default for any on-demand
question. This one answers *which way are we moving*: it reads previous reports
under `reports/` (daily, weekly, architecture, tech-debt), computes the delta per
domain, and names what improved and what decayed. **Without at least one prior
snapshot there is no trend** — say so and fall back to a single-point score
rather than presenting a position as a direction.

## Inputs / Sources
- digital-twin-core: `showProjectState`, `generateReport`, `analyzeTechDebt`,
  `analyzeReleaseReadiness`
- jira-mcp · github-mcp/GitLab MCP · sonar-mcp (live)
- jqassistant-mcp · structurizr-mcp (live) · wiki-mcp · rag-mcp (live, optional knowledge layer)

## Procedure
1. Gather each state domain: `ARCHITECTURE_STATE`, `QUALITY_STATE`,
   `DEBT_STATE`, `DELIVERY_STATE`, `KNOWLEDGE_STATE`.
2. Score each domain (health + trend); note finance/document-processing hotspots
   (e.g. reconciliation reliability, ingestion throughput, PII/PCI posture).
3. **Sweep the revisit conditions in `project-memory/`.** Every entry carries a
   `Revisit when` line — the condition that would make that decision wrong: a
   load threshold, a deprecation, a team-size change, a date. Nobody checks
   them, which is how a decision that was right in March quietly becomes the
   reason something is broken in November.

   For each entry, compare its condition against the state you just gathered:
   - **Condition met** → list the decision as due for review, with the entry,
     the condition, and the measurement that now satisfies it. This is a
     finding, not a task: whether to actually revisit is the architect's call.
   - **Condition approaching** (within reach of the current trend) → mention it
     once, without alarm.
   - **No `Revisit when` recorded** → count these. A memory where most entries
     have no expiry condition is a memory that can only grow, never be pruned;
     report the count so the gap is visible.

   Nothing here is ever edited. Memory is append-only; a revisited decision
   becomes a new entry that supersedes the old one, written by whoever makes it.

4. Consolidate the top risks across domains and rank them.
5. Summarize with explicit confidence per domain (degraded where an MCP
   is offline).

## Output (contract)
- **Per-domain health** (Architecture · Quality · Debt · Delivery · Knowledge)
- **Source(s) used** · **Confidence** (per domain)
- **Detected inconsistencies** (cross-domain)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Top risks + Recommendations**: Problem · Evidence · Impact · Recommendation ·
  Priority · Related ADR · Related Jira · Related Components
- Deliverable: Project Health Snapshot in `reports/`.

```markdown
## Decisions due for review
| Entry | Decided | Revisit condition | Why it is met now |
| project-memory/2026-05-20-rejected-mongodb.md | 2026-05-20 | "if document volume exceeds 50M" | volume is 61M as of this scan |

Approaching: ...
Entries with no revisit condition recorded: N of M
```

## Guardrails
Read/report only. Escalate any critical signal (drift, ADR violation, Quality
Gate failure, cycles, security). For full per-system depth, delegate to the
relevant skill/subagent rather than guessing.
