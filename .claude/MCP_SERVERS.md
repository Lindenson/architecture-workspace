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
| structurizr-mcp    | 8084 | MVP-2 (planned) | Structurizr DSL/API      | ARCHITECTURE_MODEL         |
| jqassistant-mcp    | 8085 | MVP-2 (planned) | Neo4j (jQAssistant)      | ARCHITECTURE_GRAPH         |
| rag-mcp            | 8088 | MVP-3 (planned) | Postgres + pgvector      | CONTEXT_PACKS              |
| wiki-mcp           | 8086 | MVP-3 (planned) | Confluence/Wiki API      | KNOWLEDGE_DOCUMENTS        |
| openspec-mcp       | 8087 | MVP-4 (planned) | OpenSpec repo            | DESIGN_CONTRACTS           |

> Planned servers are already wired in `.mcp.json` (per spec). Until their Spring
> Boot apps are running on the listed port, the agent will see a connection error
> for that server only — the live MVP-1 servers are unaffected. Disable a planned
> server in `.claude/settings.json` (`enabledMcpjsonServers`) to silence it.

## Build
```bash
cd mcp-servers
mvn -q -DskipTests package        # builds mcp-common + all MVP-1 servers
```

## Run (local jars)
```bash
java -jar jira-mcp/target/jira-mcp.jar          # :8081
java -jar github-mcp/target/github-mcp.jar      # :8082
java -jar sonar-mcp/target/sonar-mcp.jar        # :8083
java -jar digital-twin-core/target/digital-twin-core.jar   # :8080
```

## Run (docker)
```bash
docker compose up -d            # postgres, neo4j, and all MVP-1 MCP servers
```

## Tool surface (MVP-1)
- **jira-mcp:** searchIssues, getIssue, getEpics, transitionIssue, createIssue,
  updateIssue, linkIssue, linkToArchitecture *(writes gated by `jira.write-enabled`)*
- **github-mcp:** readRepository, repoSnapshot, readCommits, readBranches, readTags,
  searchPR, analyzePR, extractChangesets
- **sonar-mcp:** qualityGate, technicalDebt, codeSmells, securityIssues, coverage, fetchReport
- **digital-twin-core:** showProjectState, analyzeTechDebt, analyzeReleaseReadiness,
  runArchitectureRescan, generateReport
