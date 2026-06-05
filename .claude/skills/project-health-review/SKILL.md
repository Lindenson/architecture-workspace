---
name: project-health-review
description: Produce a consolidated project-health snapshot across architecture, quality, debt, delivery, knowledge and risk. Trigger on "project health", "оцени здоровье проекта", "общее состояние проекта", "health check", for the monthly cadence or an executive summary.
---

# Project Health Review

## Purpose
Give a single consolidated health picture of the digital twin spanning all state
domains, with the top risks and recommendations. Aligns with the monthly
procedure and the Project Health Snapshot of `ARCHITECTURE_RESCAN`.

## Inputs / Sources
- digital-twin-core: `showProjectState`, `generateReport`, `analyzeTechDebt`,
  `analyzeReleaseReadiness`
- jira-mcp · github-mcp/GitLab MCP · sonar-mcp (live)
- jqassistant-mcp · structurizr-mcp · wiki-mcp · rag-mcp (planned)

## Procedure
1. Gather each state domain: `ARCHITECTURE_STATE`, `QUALITY_STATE`,
   `DEBT_STATE`, `DELIVERY_STATE`, `KNOWLEDGE_STATE`.
2. Score each domain (health + trend); note finance/document-processing hotspots
   (e.g. reconciliation reliability, ingestion throughput, PII/PCI posture).
3. Consolidate the top risks across domains and rank them.
4. Summarize with explicit confidence per domain (degraded where a planned MCP
   is offline).

## Output (contract)
- **Per-domain health** (Architecture · Quality · Debt · Delivery · Knowledge)
- **Source(s) used** · **Confidence** (per domain)
- **Detected inconsistencies** (cross-domain)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Top risks + Recommendations**: Problem · Evidence · Impact · Recommendation ·
  Priority · Related ADR · Related Jira · Related Components
- Deliverable: Project Health Snapshot in `reports/`.

## Guardrails
Read/report only. Escalate any critical signal (drift, ADR violation, Quality
Gate failure, cycles, security). For full per-system depth, delegate to the
relevant skill/subagent rather than guessing.
