# jqassistant-mcp

Spring Boot MCP server that exposes the **jQAssistant bytecode dependency graph**
(stored in Neo4j) to the Claude architect agent and the AIP digital twin. It is
the **ARCHITECTURE_GRAPH** source (MVP-2).

Because it reads compiled bytecode (real types, packages, artifacts and their
`:DEPENDS_ON` relationships), it gives more precise Java architecture analysis
than text-level scanning: dependency cycles, layering violations, coupling /
god-class detection, blast-radius (who depends on a type) and arbitrary
read-only Cypher.

## How the graph is populated

This server **reads** a Neo4j database that jQAssistant has populated. It does
not require jQAssistant to run. Populate the graph by one of:

1. **jQAssistant CLI** (externally, or via the `runScan` tool if configured):
   ```
   jqassistant scan -f <dir-of-compiled-classes-or-jars>
   jqassistant analyze
   ```
   This writes the `:Type` / `:Package` / `:Artifact` nodes and `:DEPENDS_ON`
   relationships into the embedded/remote Neo4j.

2. **Docker Neo4j** + jQAssistant pointed at it, e.g.:
   ```
   docker run -p 7474:7474 -p 7687:7687 \
     -e NEO4J_AUTH=neo4j/password neo4j:5
   ```
   then run `jqassistant scan/analyze` against that bolt endpoint, or load an
   existing jQAssistant store. See the repo's `jqassistant/` dir for project
   conventions.

The server boots fine even when Neo4j is **down** — the driver is created
without connecting, and every tool degrades to an `ERROR` / `DATA_STALE`
response instead of failing.

## Tools (MCP) and REST endpoints

Every tool/endpoint returns the canonical `McpResponse` envelope
(`data`, `status`, `source`, `confidence`, `message`, `producedAt`).
`source = "jqassistant-mcp:Neo4j"`.

| Tool | Description | REST |
| --- | --- | --- |
| `queryGraph(cypher, limit)` | Arbitrary **read-only** Cypher. Write/DDL keywords are rejected. Appends `LIMIT` when missing. | `GET /api/jqassistant/cypher?q=&limit=` or `POST /api/jqassistant/cypher` `{cypher, limit}` |
| `findCycles()` | Package-level dependency cycles (bounded path length). | `GET /api/jqassistant/cycles` |
| `findLayeringViolations()` | Controller types depending directly on repository types (bypassing the service layer). | `GET /api/jqassistant/violations` |
| `dependenciesOf(packageOrType)` | Outgoing `:DEPENDS_ON` of a package/type. | — |
| `dependentsOf(typeFqn)` | Incoming transitive dependents (blast radius, depth 1..5). | — |
| `godClasses(threshold)` | Types with fan-out ≥ threshold (default 30). | — |
| `runScan()` | Run jQAssistant `scan` + `analyze` via the configured CLI; no-op (`DATA_STALE`) when not configured. | — |
| `getState()` | ARCHITECTURE_GRAPH summary: label counts, dependency count, cycle count, layering-violation count. | `GET /api/jqassistant/state` |

### Schema assumptions

The Cypher targets the jQAssistant Java plugin schema: nodes labelled `:Type`,
`:Package`, `:Artifact`; dependency edges `:DEPENDS_ON`; fully-qualified name on
`fqn` (with `coalesce(n.fqn, n.name)` as a fallback). If the actual schema
differs, queries return empty results rather than failing.

Package globs (e.g. `*..controller..`) are reduced pragmatically to a
significant substring (`controller`) matched with `CONTAINS` on the fqn.

## Environment variables

| Var | Default | Meaning |
| --- | --- | --- |
| `NEO4J_URI` | `bolt://localhost:7687` | Neo4j bolt URI |
| `NEO4J_USER` | `neo4j` | Neo4j user |
| `NEO4J_PASSWORD` | *(blank)* | Neo4j password |
| `JQA_SCAN_DIR` | *(blank)* | Dir of compiled classes/jars for `runScan` |
| `JQA_CLI` | *(blank)* | Path to a jQAssistant executable for `runScan` |
| `JQA_CONTROLLER_PACKAGE` | `*..controller..` | Controller-layer glob |
| `JQA_SERVICE_PACKAGE` | `*..service..` | Service-layer glob |
| `JQA_REPOSITORY_PACKAGE` | `*..repository..` | Repository-layer glob |
| `AIP_INTERNAL_TOKEN` | *(blank)* | Shared internal bearer token (auth disabled when blank) |

Config can also be supplied via `config/jqassistant.config.yml` (gitignored).
Secrets come from config only — never hardcoded.

## Run

```
java -jar target/jqassistant-mcp.jar
```

Server listens on port **8085**. Health: `GET /actuator/health`.
