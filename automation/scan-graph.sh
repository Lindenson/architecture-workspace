#!/usr/bin/env bash
#
# scan-graph.sh — populate the dependency graph in Neo4j.
#
#   automation/scan-graph.sh [target-repo ...]
#
# With no argument it scans THIS repository's own MCP servers, which is the
# fastest way to get a non-empty graph and to check the whole chain works before
# pointing it at a product.
#
# WHY THIS EXISTS
# jqassistant-mcp answers queries about a graph it does not build. Until this
# script runs, that graph is empty and every tool built on it — blast radius,
# cycles, layering violations, feature-impact-analysis — answers truthfully
# about nothing. The old nightly pipeline had a stage that logged
# "scan wiring not implemented yet" and returned 0, so the pipeline went green
# with no graph.
#
# Requires: jqassistant on PATH, a JDK, and a reachable Neo4j.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/automation/lib.sh"
aip_load_env

RULES_DIR="$ROOT/jqassistant/rules"
NEO4J_URI="${NEO4J_URI:-bolt://localhost:7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-}"

require_cmd jqassistant || {
  err "jqassistant is not on PATH."
  err "Install: https://jqassistant.github.io/jqassistant/current/#_command_line_distribution"
  err "The graph is the most precise source this platform has; without it"
  err "feature-impact-analysis falls back to package structure at MEDIUM confidence."
  exit 1
}

# ---------------------------------------------------------------- targets
targets=("$@")
if [[ ${#targets[@]} -eq 0 ]]; then
  log "No target given — scanning this repository's own MCP servers."
  log "This is the self-check: a non-empty graph here proves the whole chain works."
  targets=("$ROOT/mcp-servers")
fi

# Staging directory: scanned artifacts are copied here under a .jar name.
# See scan_one() for why that copy is not optional.
STAGE="$(mktemp -d -t aip-scan-XXXXXX)"
trap 'rm -rf "$STAGE"' EXIT

scan_one() {
  local target="$1"
  [[ -d "$target" ]] || { warn "not a directory, skipping: $target"; return 1; }

  # WHAT TO SCAN, AND WHY IT IS NOT target/classes
  #
  # jQAssistant picks its scanner plugin by FILE EXTENSION. Pointed at a
  # target/classes DIRECTORY it walks the tree with the generic file scanner and
  # records :File and :Directory nodes — no :Type, no :Package, no :DEPENDS_ON —
  # and reports success. That is how this script "completed" against an empty
  # graph: 562 classes on disk, 0 types in Neo4j, exit code 0.
  #
  # Nor the Spring Boot fat jar: target/<module>.jar after repackage contains
  # every dependency, so scanning it yields ~36k types of which a few hundred
  # are yours. A blast radius computed over Spring and Netty is noise.
  #
  # The right artifact is target/<module>.jar.original — the plain jar Spring
  # Boot renames during repackage, holding only the module's own classes. But
  # jQAssistant does not recognise the `.original` extension and silently scans
  # NOTHING: no "Entering", no "Leaving", no error. Hence the copy to
  # "<module>.jar" below. It looks superfluous; it is the whole fix.
  local jars=()
  mapfile -t jars < <(find "$target" -type f -path '*/target/*.jar.original' 2>/dev/null)

  if [[ ${#jars[@]} -eq 0 ]]; then
    # Non-Spring-Boot projects have no .jar.original; a plain jar is correct there.
    mapfile -t jars < <(find "$target" -type f -path '*/target/*.jar' \
                          ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null)
  fi

  if [[ ${#jars[@]} -eq 0 ]]; then
    warn "No jars under $target/*/target — nothing packaged."
    warn "Run 'mvn -B package -DskipTests' first: jQAssistant reads bytecode, not sources."
    warn "A missing build scans to an empty graph WITHOUT failing, which is why this stops here."
    return 1
  fi

  log "Scanning ${#jars[@]} artifact(s) under $target"
  for jar in "${jars[@]}"; do
    local module staged
    module="$(basename "$(dirname "$(dirname "$jar")")")"
    staged="$STAGE/${module}.jar"
    cp "$jar" "$staged" || { err "could not stage $jar"; return 1; }

    log "  scan $module  (${jar#"$ROOT"/})"
    local output
    output="$(jqassistant scan -f "$staged" 2>&1)" || { err "scan failed: $jar"; return 1; }

    # A scan that entered nothing produced nothing. Catch it here rather than at
    # the final count, where it is one number among several and easy to miss.
    if ! printf '%s' "$output" | grep -q 'Leaving'; then
      err "scan of $module entered no archive — jQAssistant did not recognise it."
      err "Check the artifact is a real jar: unzip -l $jar | head"
      return 1
    fi
  done
  return 0
}

failed=0
for t in "${targets[@]}"; do
  scan_one "$t" || failed=1
done

# ---------------------------------------------------------------- analyze
if [[ -d "$RULES_DIR" ]] && compgen -G "$RULES_DIR/*" >/dev/null; then
  log "Analyzing with rules from ${RULES_DIR#"$ROOT"/}"
  jqassistant analyze -Djqassistant.analyze.rule.directory="$RULES_DIR" || warn "analyze reported violations or failed — see output above"
else
  warn "No rules in ${RULES_DIR#"$ROOT"/} — running concept extraction only."
  warn "Constraints in architecture/constraints/ have no machine counterpart until rules exist."
  jqassistant analyze || true
fi

# ------------------------------------------------------- verify, don't assume
# A scan that "succeeded" and left an empty graph is the failure mode this
# whole script exists to prevent, so check rather than trust the exit code.
log "Verifying the graph is actually populated"

# Over HTTP, not cypher-shell. cypher-shell is a separate install that most
# people do not have, so the old check degraded to a WARNING — and a scan that
# silently recorded zero types sailed past it reporting success. The HTTP
# endpoint is always there if Neo4j is running at all.
NEO4J_HTTP="${NEO4J_HTTP_URL:-http://localhost:7474}"
count=""
if command -v curl >/dev/null 2>&1; then
  response="$(curl -s --max-time 10 -X POST "$NEO4J_HTTP/db/neo4j/tx/commit" \
      -H 'Content-Type: application/json' \
      ${NEO4J_PASSWORD:+-u "${NEO4J_USER:-neo4j}:$NEO4J_PASSWORD"} \
      -d '{"statements":[{"statement":"MATCH (t:Type) RETURN count(t) AS n"}]}' 2>/dev/null)"
  count="$(printf '%s' "$response" | grep -oE '"row":\[[0-9]+\]' | grep -oE '[0-9]+' | head -1)"
fi

if [[ -z "$count" ]]; then
  err "Could not query Neo4j at $NEO4J_HTTP to verify the graph."
  err "The scan may have written nothing. Check by hand:"
  err "  curl -X POST $NEO4J_HTTP/db/neo4j/tx/commit -H 'Content-Type: application/json' \\"
  err "    -d '{\"statements\":[{\"statement\":\"MATCH (t:Type) RETURN count(t)\"}]}'"
  failed=1
elif [[ "$count" -gt 0 ]]; then
  log "Graph contains $count types."
else
  err "Graph is EMPTY after a scan that reported success."
  err "Most likely causes, in order:"
  err "  1. nothing was packaged — run 'mvn -B package -DskipTests'"
  err "  2. jqassistant wrote to its embedded store instead of $NEO4J_HTTP"
  err "     (check .jqassistant.yml -> jqassistant.store.uri)"
  err "  3. the artifact was not recognised as a jar"
  failed=1
fi

# ------------------------------------------------- drift flag is now answered
if [[ "$failed" -eq 0 && -x "$ROOT/automation/reset-drift-flag.sh" ]]; then
  "$ROOT/automation/reset-drift-flag.sh" || true
fi

[[ "$failed" -eq 0 ]] && log "scan-graph complete" || err "scan-graph finished with errors"
exit "$failed"
