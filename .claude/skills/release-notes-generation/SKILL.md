---
name: release-notes-generation
description: Draft release notes from merged changes and completed Jira scope. Trigger on "release notes", "сгенерируй release notes", "подготовь заметки к релизу", "changelog for release", after a release readiness check or when cutting a version.
---

# Release Notes Generation

## Purpose
Produce a factual, audience-ready release-notes draft for a version, derived from
Git history and completed Jira scope, with architecture-relevant changes called
out. Part of OPERATING_MODEL `RELEASE_SESSION`; `GENERATE_REPORT` type RELEASE.

## Inputs / Sources
- github-mcp / GitLab MCP — merged PRs/MRs, commits, tags between versions
- jira-mcp / Atlassian MCP — completed issues, fix-versions, defects fixed
- ADR repo — decisions shipped in this release; sonar-mcp — quality context

## Procedure
1. Determine version range (previous tag → target) from Git.
2. Collect merged changes and map to completed Jira issues (group: Features,
   Fixes, Security, Architecture/Tech-Debt, Breaking changes).
3. Highlight finance/document-processing impact: payment-flow changes, document
   pipeline changes, and any PII/PCI or data-migration notes.
4. Note shipped ADRs and any breaking API/contract changes.
5. Write notes; flag entries with no Jira/PR backing as LOW confidence.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (commits without issues, scope mismatch)
- **Linked artifacts** (Jira · ADR · Code · components)
- Release-notes draft in `reports/` (or `knowledge/drafts/` if doc-bound)
- **Recommendations** where follow-up is needed (Problem · Evidence · Impact ·
  Recommendation · Priority · Related ADR · Related Jira · Related Components)

## Guardrails
Draft only — publishing is the architect's/release owner's call. Never invent
changes not backed by Git/Jira. Flag undocumented breaking changes for escalation.
