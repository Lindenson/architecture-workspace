---
name: jira-epic-analysis
description: Analyze a Jira epic for architectural coverage, delivery risk and code linkage. Trigger on "analyze epic", "проанализируй эпик", "архитектурное покрытие эпика", "epic coverage", when planning or grooming an epic in the payments or document-processing roadmap.
---

# Jira Epic Analysis

## Purpose
Evaluate an epic's delivery health and its architectural footprint: which
components it touches, whether decisions are recorded, and where the risk sits.
Backs OPERATING_MODEL `DELIVERY_SESSION` (Epic Coverage Analysis).

## Inputs / Sources
- jira-mcp / Atlassian MCP — epic, child stories, statuses, estimates
- github-mcp / GitLab MCP — branches/PRs/commits referencing the epic
- ADR repo — decisions the epic depends on or should produce
- jqassistant-mcp/Structurizr (planned) — components the epic affects
- sonar-mcp — quality of the touched code

## Procedure (DELIVERY_SESSION workflow)
1. Load the epic and its stories; compute scope, progress, blockers.
2. Link stories to code (PRs/commits); flag stories with no code and code with
   no story.
3. Map affected components and bounded contexts (e.g. ingestion → OCR →
   classification → ledger posting).
4. Check architectural coverage: are required ADRs present? new constraints?
   PII/PCI implications recorded?
5. Identify delivery and architecture risks.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (Jira↔Code, missing ADRs, orphan stories)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
Read-only on Jira by default; do not transition issues or change the roadmap
(restricted). Missing-ADR findings become drafts under
`architecture/adr/drafts/`. Escalate architecture-impacting epics lacking ADRs.
