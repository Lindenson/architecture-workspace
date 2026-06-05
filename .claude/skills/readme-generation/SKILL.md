---
name: readme-generation
description: Generate or refresh a README for a module/service from code reality. Trigger on "generate README", "обнови README", "сделай readme для модуля", "document this service", when a module lacks docs or its README has drifted from the code.
---

# README Generation

## Purpose
Create an accurate README for a module/service grounded in code, build files and
APIs — not in assumptions. Supports OPERATING_MODEL `KNOWLEDGE_SESSION`
(documentation gaps).

## Inputs / Sources
- github-mcp / GitLab MCP — source tree, build files (Maven `pom.xml`), configs
- OpenAPI/contract specs in the repo; ADR repo for design rationale
- jira-mcp — purpose/scope context; rag-mcp (planned) — related docs

## Procedure
1. Read the module: build coordinates, Java version, Spring Boot setup, layering
   (api/service/client/mcp), entry points.
2. Extract responsibilities, public APIs/endpoints, configuration (env vars /
   `config/*.config.yml`), and dependencies.
3. For finance/doc-processing modules, document data handled and any PII/PCI
   constraints, plus integration points (payment rails, document store, OCR).
4. Assemble: Overview · Architecture/position · Build & run · Configuration ·
   API · Dependencies · Related ADRs. Mark unverifiable claims LOW.

## Output (contract)
- **Source(s) used** · **Confidence**
- **Detected inconsistencies** (code↔existing README)
- **Linked artifacts** (Code · ADR · components · Jira)
- README draft under `knowledge/drafts/` (do not overwrite a live README)
- **Recommendations** for doc gaps (full recommendation shape).

## Guardrails
Never hardcode or expose secrets; reference env-var names only. Output is a
draft; replacing a published README needs architect approval.
