# v2.1.0 — What the platform says about itself, it can now prove

v2.0.0 shipped the two-loop operating contract: architecture maintenance and
spec implementation, separated, joined by four named artifacts, with a human at
every gate.

v2.1.0 is the release where the platform was held to its own standard.

Every claim in the agent contract was checked against the code. Most held. The
ones that did not are what this release is made of — and each was the exact
failure this platform exists to detect, sitting inside the platform.

No breaking changes. 65 files, +2022 / −305.

---

## The theme: a system that states its real condition

Four defects, one shape. The data existed; nothing acted on it, or what acted on
it was told something false.

### The contract told the agent to avoid its best tool

37 mentions across 14 skills and 6 subagents described jqassistant-mcp,
structurizr-mcp, rag-mcp and wiki-mcp as *planned*, with instructions to work
around them and cap confidence. All four shipped in MVP-2 and MVP-3. Two skills
opened with a banner telling the agent the server did not exist.

So the agent avoided the dependency graph — the most precise source available,
and the one `feature-impact-analysis` is built on — and reported `DATA_STALE`
while the data was fine.

Fixed, with the optional-knowledge-layer distinction preserved rather than
flattened: rag and wiki ship but run only with `KNOWLEDGE_ENABLED=true`, and
`DISABLED` remains a confident answer that does not lower confidence.

### The contract named eight tools that do not exist

`searchPR`, `linkIssue`, `updatePages`, `searchKnowledge`, `indexDocuments`,
`generateDiagrams`, `runAnalysis`, `readReports` — plus two phantom servers.
Written from memory rather than from the code.

Worse than the previous one: there the agent avoided a working tool; here it
called something absent and failed without being able to diagnose why, because
the contract is all it has.

Every surface is now taken from the `@Tool` methods. **A CI gate extracts them
from the source and fails the build on any documented name that does not
resolve** — verified in both directions.

### Untested guarantees

60 tools had 16 test methods, all `contextLoads`. The guarantees the agent is
told to rely on were asserted nowhere.

**39 behavioural tests**, none needing Neo4j, Postgres or Jira:

- the `McpResponse` envelope — `stale()` *keeps* the data and drops to LOW (a
  dead upstream degrades an answer, it does not delete it); `disabled()` is HIGH
  confidence, because "the layer is off" is known exactly
- the internal-token filter — wrong, same-length (the case that exercises the
  constant-time loop), prefix and non-Bearer tokens rejected; `/actuator/health`
  exempt by prefix only
- the read-only Cypher guard — every write keyword rejected **before** the
  database is touched, proven by a recording fake
- the Jira write gate — default-false asserted, not merely documented, with a
  client that throws on any call so a write slipping past fails loudly
- `VectorStore` — identifier validation at construction, pgvector literal format,
  count mismatch caught before any SQL
- the HTTP timeouts — a deliberately hanging server must be abandoned, not waited on

They immediately found two real bugs: a source-attribution convention nobody had
written down, and an input check running after a database call.

### Resilience that only covered half the failure

"A server answers `DATA_STALE` rather than failing when its upstream is down"
held for a **refused** connection. Not for a **hanging** one — Jira under load,
Confluence in a GC pause — which is the more common production failure.

No timeouts existed anywhere. With an infinite read timeout the thread waits
forever, and because digital-twin-core fanned out to seven sources
**sequentially**, one hung upstream stalled `SHOW_PROJECT_STATE` entirely: no
answer, no `DATA_STALE`, no error. A tool that never returns is worse than one
that returns bad news, because nothing downstream can react.

Now: 5s connect, 15s read, enforced in a factory no method can bypass; the
fan-out runs in parallel on virtual threads, giving the command a ceiling of
`max(timeout)` instead of `sum(latency)`.

---

## Memory became a mechanism

The part worth the release on its own.

**It objects.** `project-memory/` holds `rejected` entries as well as adopted
ones — ADRs record what was chosen, only memory records what was turned down.
`spec-critic` now runs an explicit contradiction check: a spec proposing
something a prior entry rejected gets a Critical Issue citing it by date, asking
what changed. Reversing an old decision is legitimate; reversing it *without
noticing* is not. A prior rejection is evidence, never a veto.

**It expires.** Every entry carries a `Revisit when` condition. Nothing read
them — which is how a decision that was right in March becomes the reason
something breaks in November. `project-health-review` now sweeps them monthly
against the live state and lists the decisions whose condition has come true.

**It proves.** New `traceability-matrix` walks
`REQ → task → commit → component in the graph → ADR → the test that names it`.
The matrix is not the point; the gaps are: a requirement with no test that names
it, a task with no requirement, and a change that landed **outside the blast
radius `impact.md` predicted** — meaning either the analysis was wrong or the
implementation went somewhere it was not meant to. CI fails a merge where a
requirement has no test.

Definition of Done stopped being a checklist that proves someone ticked it.

---

## Also

- **The build did not compile, and had not for some time.** An unanchored
  `store/` in `.gitignore` — meant for a Neo4j dump — silently swallowed
  `rag-mcp`'s entire `VectorStore` package. The sources existed on one machine,
  `git status` said clean, and the reactor failed at module 8 of 10. Package
  restored, rule anchored, and a CI step now fails the build when a source file
  exists on disk but is ignored by git: this class of bug is invisible to the
  person who wrote the file.
- **Namespace unbound from a client.** `eu.transplat.aip` → `com.wolper.aip`
  across 120 files. Including the FQCN held as a *string* in
  `AutoConfiguration.imports`, which a `sed` over `.java` would have missed, the
  compiler would not have flagged, and whose failure mode is authentication
  silently never loading.
- **One name for the persistence layer.** It went by three — `repository`,
  `store`, `persistence` — and the ArchUnit layer rule matched the one used by
  nobody, so it passed vacuously. It looked like enforcement and enforced
  nothing. One name now, checked by CI.
- **`db/init.sql`: 124 lines → 40.** Six tables no code ever touched, three of
  which implied that ADRs, debt and memory live in the database. They live in
  Markdown, because they are reviewed in pull requests and a table row cannot be.
- **Fail-open security became loud.** Correct binding, correct constant-time
  comparison — and with no token, a filter registered disabled and *nothing
  logged*. A server that believes it is protected and is not is worse than one
  knowingly open.
- **`digital-twin-core` left port 8080** — the most contested port on a
  developer machine. Now 8089.
- Dependabot, SECURITY.md, healthchecks on all containers, and `.env.example`
  documenting the write gates that let an agent modify a real tracker.

## Honesty where it costs something

- `architecture-tests/` is a **template**: its rules are `@Disabled` and it
  imports no product code. The CI job is now called "ArchUnit template
  compiles" and prints what it did and did not check. Calling it a gate would
  have been the self-deception this repository exists to prevent.
- Local hooks read files an agent writes, so they are an **audit trail, not a
  lock**. The locks are CODEOWNERS with branch protection and the `human-gate`
  CI job. `OPERATING_LOOPS.md` carries the table; nothing calls a log a lock.
- The session digest says which retrieval mode produced it — `semantic
  retrieval (rag-mcp)` or `DEGRADED to newest-first`. It previously claimed
  pgvector and ran a filename sort.
- The README no longer quotes a test count. It had already drifted by one.

## Still unproven

No feature has yet gone through both loops end to end. Everything here shows the
harness behaves as designed. Whether the design is right is a different
question, and only a real feature answers it.

Start on `gate.mode: warn`. Read `gate-log.md` for two weeks. Then `block`.
