---
name: sonar-analysis
description: Pull and interpret SonarQube quality state — Quality Gate, bugs, vulnerabilities, smells, coverage, debt. Trigger on "sonar analysis", "проверь качество кода", "посмотри sonar", "quality gate status", when assessing quality of a module, PR or release.
---

# Sonar Analysis

## Purpose
Report the Quality Layer state from SonarQube and translate it into
architecture/release-relevant findings. Feeds `TECH_DEBT_SESSION`,
`RELEASE_SESSION` and `SHOW_PROJECT_STATE` (`QUALITY_STATE`).

## Inputs / Sources
- sonar-mcp (live) — Quality Gate, issues by severity/type, coverage, duplication, debt ratio
- github-mcp / GitLab MCP — map issues to changed code / authors
- jira-mcp — existing quality tickets; ADR repo — quality-related decisions

## Procedure
1. Fetch the project (or branch/PR) Quality Gate and metric breakdown via sonar-mcp.
2. Group issues by type (bug / vulnerability / smell) and severity; isolate
   new-code findings for the release path.
3. Map hotspots to modules and bounded contexts (e.g. payment validation,
   document parsers); call out security vulnerabilities on PII/PCI paths.
4. Cross-link to Jira and ADRs; separate net-new from legacy debt.

## Output (contract)
- **Source(s) used** · **Confidence** (HIGH when live snapshot ≤ 12h)
- **Detected inconsistencies** (Quality Gate vs release intent)
- **Linked artifacts** (Sonar · Code · Jira · ADR · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
Read-only. Do not mark issues resolved in Sonar or close debt (restricted).
Escalate Quality Gate failures and critical/blocker security vulnerabilities.

## Graceful degradation
If sonar-mcp is down, use the last snapshot (≤12h cache allowed), mark
`DATA_STALE`, confidence LOW.
