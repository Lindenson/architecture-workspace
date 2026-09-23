---
# Metadata block — indexed by rag-mcp, retrieved by the SessionStart hook (J4).
date: YYYY-MM-DD
author: <human or agent + approving architect>
type: adopted | rejected | deferred | accepted-trade-off | supersedes
bounded_context: <e.g. payments | document-ingestion | UNKNOWN>
components: []          # FQNs or Structurizr element names from impact.md
adr_ref: <ADR-00x | draft | none>
jira_ref: <KEY-123 | none>
feature: <specs/<feature> | none>
supersedes: <YYYY-MM-DD-slug | none>
confidence: HIGH | MEDIUM | LOW
---

# <Short decision title>

## Context
What situation forced a decision. Facts only — link the evidence
(impact.md, critique.md, a drift report, a Sonar finding).

## Options considered
| Option | Pros | Cons | Verdict |
|---|---|---|---|
| A | | | chosen / rejected |
| B | | | |

## Reason
Why the chosen option won. **If no reason was recorded anywhere, write
`NOT RECORDED — ask <author>`.** Never reconstruct a plausible rationale:
the next session will trust it.

## Result
What was actually done.

## Consequences
What this costs us later. Debt created, constraints now binding, migrations
owed, doors closed.

## Revisit when
The condition that would make this decision wrong — a load threshold, a
deprecation, a team-size change. This is what makes the entry useful in a year.
