---
name: architecture-drift-analysis
description: Detect divergence between code reality and the intended architecture (ADR, Structurizr, ArchUnit rules, Jira intent). Trigger on "drift analysis", "проверь дрейф архитектуры", "разошлась ли архитектура с кодом", "architecture drift", after a large merge or before a release.
---

# Architecture Drift Analysis

## Purpose
Surface every gap between what the code does and what the architecture says it
should do. Implements the OPERATING_MODEL Architecture Drift Detection checklist
and feeds the Architecture Drift Engine in MCP_ORCHESTRATION_MAP.

## Inputs / Sources
- github-mcp / GitLab MCP — current code structure (PRIMARY)
- jqassistant-mcp (planned) — dependency graph, cycles, forbidden edges
- ArchUnit reports; Structurizr model (structurizr-mcp, planned)
- ADR repo under `architecture/adr/`; jira-mcp — intended scope; sonar-mcp

## Procedure (Drift Detection checklist)
1. Refresh dependency graph (or last snapshot) and ArchUnit results.
2. Detect: dependency cycles · layer violations · new edges between bounded
   contexts (e.g. payment-core ↔ document-store) · ADR violations · constraint
   breaches · Code↔Structurizr divergence · Code↔Jira-intent gaps.
3. Classify each drift signal by type and risk; PII/PCI boundary crossings and
   ledger-integrity edges are HIGH by default.
4. Cross-link to the ADR/constraint/component/Jira it contradicts.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** — the drift catalogue, by type
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components
- Deliverable: Architecture Drift Report in `reports/`.

## Guardrails
Read-only. Remediation that changes constraints/ADRs is a draft only. Escalate
critical drift, ADR violations, cycles and any PII/PCI boundary crossing.

## Graceful degradation
Without jqassistant-mcp/structurizr-mcp (planned), rely on Git + ArchUnit + ADR
diff, mark `DATA_STALE`, confidence ≤ MEDIUM, and note which drift classes could
not be checked (e.g. C4 divergence).
