---
name: wiki-synchronization
description: Reconcile the team Wiki/Confluence with code, ADRs and the digital twin, flagging stale or contradictory pages. Trigger on "sync wiki", "синхронизируй wiki", "проверь актуальность wiki", "wiki out of date", during the weekly/monthly knowledge cadence.
---

# Wiki Synchronization

> **MVP status:** wiki-mcp is *planned*. Until live, operate via the Atlassian
> Confluence MCP if present, else over exported/markdown docs in the repo.

## Purpose
Keep the Knowledge Layer aligned with the source-of-truth: detect Wiki pages that
contradict code/ADRs and surface documentation gaps. Backs `KNOWLEDGE_SESSION`
and `UPDATE_KNOWLEDGE_BASE`.

## Inputs / Sources
- wiki-mcp (planned) / Atlassian Confluence MCP — pages, spaces, history
- ADR repo + Structurizr model; github-mcp / GitLab MCP — code reality
- rag-mcp (planned) — semantic match of pages to components

## Procedure
1. Enumerate relevant Wiki pages (architecture, runbooks, payment/document flows).
2. Compare each against code, ADRs and the model in trust order — code wins.
3. Flag: stale pages, contradictions, orphan pages, and missing pages for
   existing components (e.g. no runbook for the document-ingestion pipeline).
4. Draft corrected content; never silently edit a live page.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (Wiki↔code↔ADR), gaps catalogue
- **Linked artifacts** (Wiki · ADR · Code · components · Jira)
- Drafts under `knowledge/drafts/`
- **Recommendations**: Problem · Evidence · Impact · Recommendation · Priority ·
  Related ADR · Related Jira · Related Components

## Guardrails
No autonomous edits/publishes to the Wiki — drafts only, architect approves.
Never expose secrets or PII pulled from pages.

## Graceful degradation
With no wiki-mcp and no Confluence MCP in session, restrict scope to repo-local
docs, mark `DATA_STALE`, confidence LOW, and report that live Wiki state could
not be verified.
