---
name: tech-debt-review
description: Inventory, prioritize and link technical debt across Sonar and architecture violations. Trigger on "tech debt review", "проанализируй техдолг", "оцени технический долг", "analyze technical debt", during the weekly cadence.
---

# Technical Debt Review

## Purpose
Build a prioritized technical-debt registry that fuses code smells, bugs,
vulnerabilities and architecture violations, mapped to modules, ADRs and Jira.
Backs OPERATING_MODEL `TECH_DEBT_SESSION` and AGENT_RUNTIME `ANALYZE_TECH_DEBT`
(digital-twin-core `analyzeTechDebt`).

## Inputs / Sources
- sonar-mcp — smells, bugs, vulnerabilities, coverage, debt ratio
- jqassistant-mcp (planned) + ArchUnit — architecture violations, cycles
- ADR repo — decisions the debt contradicts; jira-mcp — existing debt tickets
- github-mcp / GitLab MCP — Git history / churn for risk weighting

## Procedure (TECH_DEBT_SESSION workflow)
1. `analyzeTechDebt` / fetch Sonar issues → map to modules and bounded contexts.
2. Add architecture violations from jQAssistant + ArchUnit.
3. Cross-check ADR compliance; link each item to its Jira ticket (or flag as
   untracked debt).
4. Cluster by domain (e.g. payment reconciliation, OCR/document parsing) and
   prioritize by risk × reach; PII/PCI and reliability debt rank highest.
5. Produce a Debt Prioritization Matrix and refactoring recommendations.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (debt contradicting ADRs; untracked debt)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components
- Deliverables: Technical Debt Report + Debt Prioritization Matrix in `reports/`.

## Guardrails
Closing/resolving tech debt is restricted — propose, do not close. Write debt
proposals as drafts under `quality/technical-debt/drafts/`. Escalate critical
security vulnerabilities and Quality Gate failures.
