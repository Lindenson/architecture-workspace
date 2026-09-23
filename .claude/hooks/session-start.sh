#!/usr/bin/env bash
# SessionStart hook — loop identity + memory digest (J4).
#
# stdout becomes session context. This hook, not a skill, is the authoritative
# statement of which loop the session is in: skill routing is model-driven and
# may not fire, whereas this always runs.
#
# Memory retrieval is honest about its mode:
#   retrieval: rag     → semantic, scoped to the active feature (needs rag-mcp up)
#   retrieval: recent  → newest entries by date; no dependency, no relevance
# The digest states which mode produced it. A "semantic" digest that silently
# degraded to `ls | head` is exactly the drift this repository exists to prevent.
set -uo pipefail
# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

say() { printf '%s\n' "$*"; }
say "=== AIP SESSION DIGEST ==="

LOOP="$(aip_loop)"
FD="$(aip_feature_dir)"

case "$LOOP" in
  B)
    say "LOOP: B — SPEC IMPLEMENTATION"
    say "ACTIVE FEATURE: ${FD:-<unresolved>}"
    if [[ -n "$FD" && -d "$AIP_ROOT/$FD" ]]; then
      for a in spec.md impact.md decisions.md critique.md plan.md tasks.md; do
        [[ -f "$AIP_ROOT/$FD/$a" ]] && say "  [x] $a" || say "  [ ] $a  MISSING"
      done
      if [[ -f "$AIP_ROOT/$FD/critique.md" ]]; then
        say "  CRITIQUE STATUS: $(grep -m1 -oE '\b(BLOCKED|NEEDS_DECISION|READY)\b' "$AIP_ROOT/$FD/critique.md" 2>/dev/null || echo UNKNOWN)"
      fi
      OPEN="$(aip_open_decisions "$FD")"
      if [[ "${OPEN:-0}" -gt 0 ]]; then
        say "  OPEN HUMAN QUESTIONS: $OPEN — these gate /speckit-plan and /speckit-implement."
        grep '^|[^|]*|.*|[[:space:]]*OPEN[[:space:]]*|' "$AIP_ROOT/$FD/decisions.md" 2>/dev/null \
          | head -3 | cut -c1-140 | sed 's/^/    /'
      fi
    fi ;;
  A) say "LOOP: A — ARCHITECTURE MAINTENANCE (architect workspace)" ;;
  *) say "LOOP: UNDETERMINED — read .claude/skills/session-orientation/SKILL.md and ask." ;;
esac

say "GATE MODE: $(cfg gate.mode block)  ·  architect-owned writes: loop A $(cfg architect_owned.loop_a_mode warn) / loop B $(cfg architect_owned.loop_b_mode block)"

# ------------------------------------------------------------------- drift
FLAG="$AIP_ROOT/$(cfg drift.flag_file .claude/.drift-pending)"
if [[ -f "$FLAG" ]]; then
  say ""
  say "DRIFT PENDING: product code changed since the last architecture rescan."
  tail -n 6 "$FLAG" | sed 's/^/    /'
  say "  → /architecture-drift-analysis, then automation/reset-drift-flag.sh"
fi

# -------------------------------------------------------------- open debt
if [[ -d "$AIP_ROOT/quality/technical-debt" ]]; then
  N=$(find "$AIP_ROOT/quality/technical-debt" -maxdepth 1 -name 'TD-*.md' 2>/dev/null | wc -l | tr -d ' ')
  [[ "$N" != "0" ]] && { say ""; say "OPEN TECHNICAL DEBT: $N item(s)."; }
fi

# ---------------------------------------------------------------- memory J4
MEM="$AIP_ROOT/project-memory"
MODE="$(cfg memory.retrieval recent)"
N="$(cfg memory.digest_size 5)"
if [[ -d "$MEM" ]]; then
  say ""
  RESULT=""
  if [[ "$MODE" == "rag" ]] && command -v curl >/dev/null 2>&1; then
    QUERY="${FD:-project architecture decisions}"
    RESULT="$(curl -sS --max-time "$(cfg memory.rag_timeout_seconds 3)" \
      -H "Authorization: Bearer ${AIP_INTERNAL_TOKEN:-}" \
      -H 'Content-Type: application/json' \
      -d "{\"query\":\"$QUERY\",\"topK\":$N}" \
      "$(cfg memory.rag_url http://localhost:8088)/api/rag/search" 2>/dev/null)" || RESULT=""
  fi
  if [[ -n "$RESULT" && "$RESULT" != *'"status":"ERROR"'* ]]; then
    say "PROJECT MEMORY — semantic retrieval (rag-mcp), scoped to ${FD:-the project}:"
    printf '%s' "$RESULT" | grep -o '"[a-zA-Z]*[Tt]itle"[[:space:]]*:[[:space:]]*"[^"]*"' \
      | sed 's/.*:[[:space:]]*"//; s/"$//' | head -n "$N" | sed 's/^/  · /'
  else
    [[ "$MODE" == "rag" ]] \
      && say "PROJECT MEMORY — rag-mcp unavailable, DEGRADED to newest-first (no relevance ranking):" \
      || say "PROJECT MEMORY — newest-first (retrieval: recent; no relevance ranking):"
    find "$MEM" -maxdepth 1 -name '2*.md' 2>/dev/null | sort -r | head -n "$N" \
      | while read -r f; do
          say "  · $(basename "$f" .md): $(grep -m1 '^# ' "$f" 2>/dev/null | sed 's/^# //')"
        done
    say "  (ask rag-mcp.retrieveContext directly for relevance-ranked history)"
  fi
fi

say ""
say "Contract: .claude/OPERATING_LOOPS.md · Config: .claude/aip.config.yml"
say "=== END DIGEST ==="
exit 0
