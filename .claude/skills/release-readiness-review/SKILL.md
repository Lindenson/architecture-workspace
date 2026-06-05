---
name: release-readiness-review
description: Assess whether a release is ready, with risks, and produce a READY / READY_WITH_RISKS / NOT_READY verdict. Trigger on "release readiness", "готов ли релиз", "оцени готовность релиза", "can we release", before cutting a release of the payments or document-processing services.
---

# Release Readiness Review

## Purpose
Decide release readiness from objective signals: completed scope, open defects,
Quality Gate, architecture compliance and unresolved risks. Backs
OPERATING_MODEL `RELEASE_SESSION` and AGENT_RUNTIME `ANALYZE_RELEASE_READINESS`
(digital-twin-core `analyzeReleaseReadiness`).

## Inputs / Sources
- jira-mcp — epic/story status, open defects, release scope
- github-mcp / GitLab MCP — merged changes, open PRs, pipelines
- sonar-mcp — Quality Gate, new-code coverage, blockers
- ADR repo + jqassistant-mcp/ArchUnit (planned) — architecture compliance

## Procedure (RELEASE_SESSION workflow)
1. `analyzeReleaseReadiness` for the target version/milestone.
2. Completed vs open issues; flag unfinished scope and open critical defects.
3. Validate Quality Gate and new-code coverage from Sonar.
4. Validate architecture compliance (no new violations/cycles/ADR breaches).
5. Collect unresolved risks and external dependencies (e.g. payment-rail
   certification, KYC/PII handling sign-off).
6. Compute readiness and assign the verdict.

## Output (contract)
- **Verdict**: READY / READY_WITH_RISKS / NOT_READY
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (scope vs code, Jira vs Git)
- **Linked artifacts** (Jira · ADR · Code · Sonar · components)
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components
- Deliverable: Release Readiness Report in `reports/`.

## Guardrails
Advisory only — the agent does not authorize the release. Escalate Quality Gate
failures, critical defects, security issues and high-risk architecture
violations. Pair with `release-notes-generation` for the notes draft.
