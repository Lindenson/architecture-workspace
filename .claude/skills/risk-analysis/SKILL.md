---
name: risk-analysis
description: Identify and rank cross-cutting project risks — architecture, delivery, quality, security, compliance. Trigger on "risk analysis", "проанализируй риски", "какие риски проекта", "risk register", before a release or a roadmap decision.
---

# Risk Analysis

## Purpose
Build a ranked risk register from objective signals across all state domains,
with each risk traced to evidence. Supports `RELEASE_SESSION`,
`DELIVERY_SESSION` (Roadmap Risk Analysis) and the monthly assessment.

## Inputs / Sources
- jqassistant-mcp/ArchUnit (planned) — architecture violations, cycles
- sonar-mcp — vulnerabilities, reliability/security hotspots
- jira-mcp — slipping epics, open critical defects, scope risk
- github-mcp/GitLab MCP — change concentration, risky areas; ADR repo

## Procedure
1. Collect signals: architecture drift/cycles · Quality Gate & security ·
   delivery slippage · undocumented decisions · knowledge gaps.
2. Classify by category (Architecture / Quality / Security / Delivery /
   Compliance) and weight likelihood × impact; finance/document-processing
   amplifiers — PII/PCI exposure, payment-integrity, data-retention — raise impact.
3. Trace each risk to concrete evidence and the artifact it threatens.
4. Rank into a register; propose mitigations.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** feeding risk
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Risk register + Recommendations**: Problem · Evidence · Impact ·
  Recommendation · Priority · Related ADR · Related Jira · Related Components
- Deliverable: Risk Analysis in `reports/`.

## Guardrails
Analysis only; mitigations that change constraints/roadmap/ADRs are drafts.
Escalate high-risk architecture violations and critical security/compliance risks
immediately per OPERATING_MODEL escalation rules.
