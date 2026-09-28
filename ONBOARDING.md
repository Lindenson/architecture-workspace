# ONBOARDING — from a clone to a snapshot that is actually true

The quick start in the README gets the servers running. This gets them
**telling you the truth about your project**, which is a different thing and
takes about an hour.

A fresh install answers confidently about nothing: every source points at the
placeholders in `.env.example`, and the dependency graph is empty. That state is
reported honestly — `DATA_STALE` everywhere, overall confidence `LOW` — but it
is not useful. This is the path out of it.

---

## Step 0 — Prove the machinery before pointing it anywhere

Scan **this repository's own MCP servers** first. It needs no credentials, no
product access, and gives you a non-empty graph in two minutes. If something is
broken, you find out here rather than while debugging your own project.

```bash
cd mcp-servers && mvn -B package -DskipTests && cd ..
automation/scan-graph.sh                    # no argument = scan ourselves
```

The script verifies the graph is non-empty rather than trusting the scanner's
exit code — a scan that "succeeds" and leaves an empty store is the failure this
whole step exists to catch.

Then ask the twin:

```bash
curl -s -H "Authorization: Bearer $AIP_INTERNAL_TOKEN" \
  http://localhost:8089/api/twin/state | jq '.data.architectureState'
```

`graph.labelCounts.Type` should now be in the hundreds. If it is `0`, stop here
and fix it — everything built on the graph depends on this one number.

---

## Step 1 — Credentials

```bash
cp .env.example .env
```

Fill in what you actually have. **Each source is independent**: Jira alone is
useful, and a missing source reports `DATA_STALE` without breaking the others.
Do not wait until you have all of them.

| Variable | Without it |
|---|---|
| `AIP_INTERNAL_TOKEN` | **authentication is OFF on all eight servers.** A `WARN` at startup says so — read your logs once |
| `JIRA_*` | delivery slice stale |
| `GITHUB_TOKEN`, `GIT_REPOSITORIES` | code and changeset slices stale |
| `SONAR_*`, `SONAR_PROJECT_KEYS` | quality and debt slices stale |
| `WIKI_*`, `RAG_*` | knowledge layer; off unless `KNOWLEDGE_ENABLED=true` |

**Leave `JIRA_WRITE_ENABLED` and `WIKI_WRITE_ENABLED` at `false`** until you have
decided that an agent writing to a real tracker is acceptable. Defaults are
asserted by a test, not merely documented.

---

## Step 2 — Point the graph at your product

```bash
cd /path/to/your-product && mvn -B package -DskipTests && cd -
automation/scan-graph.sh /path/to/your-product
```

For several repositories, pass them all. For the nightly run, set
`SCAN_TARGETS` in `.env`.

Then **parameterise the rules**, which is the step people skip:

```bash
$EDITOR jqassistant/rules/aip-constraints.xml
```

The package patterns default to this repository's own layout. A constraint whose
pattern matches nothing **passes silently** — that is exactly how the ArchUnit
layer rule in this repo enforced nothing for months while looking like
governance. After editing, confirm each concept matches something:

```bash
jqassistant analyze -rule-directory jqassistant/rules
# every concept should report a non-zero count
```

---

## Step 3 — Replace the C4 model

`architecture/c4/workspace.dsl` describes **Paydocs Platform**, a fictional
example. Until you replace it, `detectDrift` compares your code against a system
that does not exist and its answers are noise.

```bash
$EDITOR architecture/c4/workspace.dsl
docker run --rm -v "$PWD/architecture/c4:/usr/local/structurizr" \
  structurizr/cli:2025.11.09 validate -workspace workspace.dsl
```

Start small — systems and containers only. Components can wait; an incomplete
model that is true beats a complete one that is invented.

---

## Step 4 — Replace the example knowledge

Thirteen files are marked `EXAMPLE / STARTER TEMPLATE`. The agent reads them as
slow-changing project context, so fiction left in place becomes fiction the
agent believes.

```bash
grep -rl "EXAMPLE / STARTER TEMPLATE" knowledge/ architecture/ domain/
```

Priority order: `knowledge/project-overview.md` →
`domain/model/bounded-contexts.md` → `architecture/constraints/*` → the ADRs.
The constraints matter most: they are what `spec-critic` cites as evidence.

Leave `project-memory/` empty. It fills up by itself as decisions get made, and
a seeded memory is a memory of decisions nobody took.

---

## Step 5 — Check every slice individually

```bash
for p in 8081 8082 8083 8084 8085; do
  curl -s -H "Authorization: Bearer $AIP_INTERNAL_TOKEN" \
    "http://localhost:$p/actuator/health" -o /dev/null -w "$p: %{http_code}\n"
done
```

Then the consolidated view:

```bash
curl -s -H "Authorization: Bearer $AIP_INTERNAL_TOKEN" \
  http://localhost:8089/api/twin/state | jq '.data | {overallConfidence, staleSources}'
```

**Reading the answer:**

- `DATA_STALE` + a named reason → that source is misconfigured or down. The
  message says which; the other slices are unaffected.
- `DISABLED` → switched off on purpose. `HIGH` confidence, and it does not lower
  the overall score. This is a correct answer, not a problem.
- `OK` with zero counts → the source answers but has no data. For the graph that
  means step 2 did not run.
- Slices arriving within a second of each other → the parallel fan-out is
  working.

---

## Step 6 — Turn on the nightly pipeline

```bash
automation/nightly-pipeline.sh          # run once by hand first
```

Every stage now either does its job or reports failure. None of them returns
success while skipping the work — an earlier version did exactly that for the
graph scan, and the pipeline went green nightly with an empty store.

Once it passes by hand, schedule it.

---

## Step 7 — The first feature

Install the plugin in your product repository and run one small, ordinary
feature through both loops — not a showcase one.

```bash
/plugin marketplace add Lindenson/architecture-workspace
/plugin install architecture-intelligence@aip-marketplace
uvx --from git+https://github.com/github/spec-kit.git specify init --here
cp <workspace>/integration/specify/extensions.yml .specify/extensions.yml
cp <workspace>/integration/specify/decisions.template.md specs/<feature>/decisions.md
```

**Start with `gate.mode: warn`** in `.claude/aip.config.yml`. Read
`specs/*/gate-log.md` for two weeks, then switch to `block`. A gate that is
wrong once and cannot be overridden gets deleted along with the cases where it
was right.

Afterwards:

```bash
automation/harness-metrics.sh
```

Three numbers matter: how often gates fired, what share were overridden (above
40% means the rule is wrong, not the team), and how many questions in
`decisions.md` a human actually closed.

---

## What "working" means

- the graph has types in it, and `dependentsOf` returns a real blast radius
- `workspace.dsl` describes your system
- at least one live source is `OK` rather than `DATA_STALE`
- `automation/nightly-pipeline.sh` passes without skipping stages
- one feature has been through both loops end to end

The last one is the only one that tells you whether the design is right rather
than merely whether it runs.
