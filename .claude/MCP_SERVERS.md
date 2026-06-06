# MCP_SERVERS — status, ports, build & run

All AIP MCP servers are Spring Boot apps exposing tools over **MCP SSE** (Spring AI
MCP server). The Claude agent connects via `.mcp.json`. Secrets come from `.env` /
`config/*.config.yml` (gitignored). Internal calls carry `Authorization: Bearer
${AIP_INTERNAL_TOKEN}`.

| Server             | Port | Status      | Source                       | Outputs to twin            |
|--------------------|------|-------------|------------------------------|----------------------------|
| digital-twin-core  | 8080 | **MVP-1 ✅** | orchestrator (fan-out)       | DIGITAL_TWIN_MODEL         |
| jira-mcp           | 8081 | **MVP-1 ✅** | Jira REST                    | DELIVERY_STATE             |
| github-mcp         | 8082 | **MVP-1 ✅** | GitHub/GitLab REST           | CODE_STATE, CHANGESET_STATE|
| sonar-mcp          | 8083 | **MVP-1 ✅** | SonarQube Web API            | QUALITY_STATE, DEBT_STATE  |
| structurizr-mcp    | 8084 | **MVP-2 ✅** | Structurizr DSL (workspace.dsl) | ARCHITECTURE_MODEL    |
| jqassistant-mcp    | 8085 | **MVP-2 ✅** | Neo4j (jQAssistant graph)    | ARCHITECTURE_GRAPH        |
| rag-mcp            | 8088 | **MVP-3 ✅ (optional)** | Postgres + pgvector  | CONTEXT_PACKS              |
| wiki-mcp           | 8086 | **MVP-3 ✅ (optional)** | Confluence API       | KNOWLEDGE_DOCUMENTS        |
| openspec-mcp       | 8087 | MVP-4 (planned) | OpenSpec repo            | DESIGN_CONTRACTS           |

> **The knowledge layer (rag-mcp + wiki-mcp) is OPTIONAL and OFF by default.**
> The orchestrator's `digital-twin.features.knowledge.enabled` is `false` unless
> `KNOWLEDGE_ENABLED=true`; while off, the knowledge slice is reported as
> `DISABLED` (a first-class state that does **not** lower confidence). Enable it
> per-project with `KNOWLEDGE_ENABLED=true` + `docker compose --profile knowledge up -d`.
> `rag-mcp` embeddings are pluggable: `local` (offline ONNX, default, no key),
> `openai` (any OpenAI-compatible endpoint), or `none` (Postgres full-text only).

> Planned servers are already wired in `.mcp.json` (per spec). Until their Spring
> Boot apps are running on the listed port, the agent will see a connection error
> for that server only — the live MVP-1 servers are unaffected. Disable a planned
> server in `.claude/settings.json` (`enabledMcpjsonServers`) to silence it.

## Build
```bash
cd mcp-servers
mvn -q -DskipTests package        # builds mcp-common + all MVP-1 & MVP-2 servers
```

## Run (local jars)
```bash
java -jar jira-mcp/target/jira-mcp.jar                      # :8081
java -jar github-mcp/target/github-mcp.jar                  # :8082
java -jar sonar-mcp/target/sonar-mcp.jar                    # :8083
java -jar jqassistant-mcp/target/jqassistant-mcp.jar       # :8085 (needs Neo4j)
java -jar structurizr-mcp/target/structurizr-mcp.jar       # :8084 (reads workspace.dsl)
java -jar digital-twin-core/target/digital-twin-core.jar   # :8080
# Optional knowledge layer:
java -jar rag-mcp/target/rag-mcp.jar                       # :8088 (needs Postgres+pgvector)
java -jar wiki-mcp/target/wiki-mcp.jar                     # :8086 (Confluence)
```

## Run (docker)
```bash
docker compose up -d                              # postgres, neo4j, MVP-1 + MVP-2 servers
docker compose --profile knowledge up -d          # + optional rag-mcp & wiki-mcp
```

## Tool surface (MVP-1)
- **jira-mcp:** searchIssues, getIssue, getEpics, transitionIssue, createIssue,
  updateIssue, linkIssue, linkToArchitecture *(writes gated by `jira.write-enabled`)*
- **github-mcp:** readRepository, repoSnapshot, readCommits, readBranches, readTags,
  searchPR, analyzePR, extractChangesets
- **sonar-mcp:** qualityGate, technicalDebt, codeSmells, securityIssues, coverage, fetchReport
- **jqassistant-mcp:** queryGraph (read-only Cypher), findCycles, findLayeringViolations,
  dependenciesOf, dependentsOf (blast radius), godClasses, runScan, getState
- **structurizr-mcp:** readWorkspace, validateModel, listElements, listRelationships,
  getViews, detectDrift, getState
- **rag-mcp** *(optional):* indexPath, reindexAll, search, retrieveContext,
  updateEmbeddings, getState *(returns DISABLED when `rag.enabled=false`)*
- **wiki-mcp** *(optional):* searchPages, getPage, listPages, readPages,
  updatePage *(write-gated)*, getState *(DISABLED when `wiki.enabled=false`)*
- **digital-twin-core:** showProjectState, analyzeTechDebt, analyzeReleaseReadiness,
  runArchitectureRescan *(real — fans out to jQAssistant + Structurizr)*,
  updateKnowledgeBase *(real when knowledge enabled, else DISABLED)*, generateReport
