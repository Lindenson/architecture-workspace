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

scan_one() {
  local target="$1"
  [[ -d "$target" ]] || { warn "not a directory, skipping: $target"; return 1; }

  # Bytecode, not sources: jQAssistant reads compiled classes, so an unbuilt
  # project produces an empty graph and no error. Build first, and say so.
  local classes
  mapfile -t classes < <(find "$target" -type d -path '*/target/classes' 2>/dev/null)
  if [[ ${#classes[@]} -eq 0 ]]; then
    warn "No */target/classes under $target — nothing compiled."
    warn "Build it first (mvn -B package -DskipTests), then re-run."
    warn "Scanning sources instead of bytecode is not possible: the graph is built from class files."
    return 1
  fi

  log "Scanning ${#classes[@]} output director(ies) under $target"
  for dir in "${classes[@]}"; do
    log "  scan $dir"
    jqassistant scan -f "$dir" || { err "scan failed: $dir"; return 1; }
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
  jqassistant analyze -rule-directory "$RULES_DIR" || warn "analyze reported violations or failed — see output above"
else
  warn "No rules in ${RULES_DIR#"$ROOT"/} — running concept extraction only."
  warn "Constraints in architecture/constraints/ have no machine counterpart until rules exist."
  jqassistant analyze || true
fi

# ------------------------------------------------------- verify, don't assume
# A scan that "succeeded" and left an empty graph is the failure mode this
# whole script exists to prevent, so check rather than trust the exit code.
log "Verifying the graph is actually populated"
if require_cmd cypher-shell >/dev/null 2>&1; then
  count=$(cypher-shell -a "$NEO4J_URI" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" \
            --format plain "MATCH (t:Type) RETURN count(t) AS n" 2>/dev/null | tail -1 | tr -d ' ')
  if [[ "${count:-0}" -gt 0 ]]; then
    log "Graph contains $count types."
  else
    err "Graph is EMPTY after a scan that reported success."
    err "Most likely the scanned directories held no .class files, or jqassistant"
    err "is writing to a different store than \$NEO4J_URI points at."
    failed=1
  fi
else
  warn "cypher-shell not on PATH — cannot verify the graph is non-empty."
  warn "Check by hand: jqassistant-mcp.queryGraph 'MATCH (t:Type) RETURN count(t)'"
fi

# ------------------------------------------------- drift flag is now answered
if [[ "$failed" -eq 0 && -x "$ROOT/automation/reset-drift-flag.sh" ]]; then
  "$ROOT/automation/reset-drift-flag.sh" || true
fi

[[ "$failed" -eq 0 ]] && log "scan-graph complete" || err "scan-graph finished with errors"
exit "$failed"
