#!/usr/bin/env bash
# SessionStart hook — J4 (memory) + loop identification.
#
# Emits a short digest to stdout, which Claude Code adds to the session context:
#   · which operating loop this working tree implies
#   · the active Spec Kit feature, if any
#   · open architectural debts and drift flags
#   · project-memory entries relevant to the active feature
#
# Contract: stdout is context, never instructions to the user. Always exit 0 —
# a failing SessionStart hook must never block a session.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
MAX_MEMORY_ENTRIES="${AIP_MEMORY_DIGEST_SIZE:-5}"

say() { printf '%s\n' "$*"; }

say "=== AIP SESSION DIGEST ==="

# ---------------------------------------------------------------- loop
if [[ -f "$ROOT/.specify/feature.json" ]]; then
  FEATURE_DIR=""
  if command -v jq >/dev/null 2>&1; then
    FEATURE_DIR="$(jq -r '.featureDirectory // .feature_directory // empty' \
      "$ROOT/.specify/feature.json" 2>/dev/null)"
  else
    FEATURE_DIR="$(grep -o '"[^"]*feature[Dd]irectory"[[:space:]]*:[[:space:]]*"[^"]*"' \
      "$ROOT/.specify/feature.json" 2>/dev/null | sed 's/.*:[[:space:]]*"//; s/"$//')"
  fi
  say "LOOP: B — SPEC IMPLEMENTATION"
  say "ACTIVE FEATURE: ${FEATURE_DIR:-<unresolved>}"

  if [[ -n "$FEATURE_DIR" && -d "$ROOT/$FEATURE_DIR" ]]; then
    for artifact in spec.md impact.md critique.md plan.md tasks.md; do
      if [[ -f "$ROOT/$FEATURE_DIR/$artifact" ]]; then
        say "  [x] $artifact"
      else
        say "  [ ] $artifact  MISSING"
      fi
    done
    if [[ -f "$ROOT/$FEATURE_DIR/critique.md" ]]; then
      STATUS="$(grep -m1 -oE '\b(BLOCKED|NEEDS_DECISION|READY)\b' \
        "$ROOT/$FEATURE_DIR/critique.md" 2>/dev/null || true)"
      say "  CRITIQUE STATUS: ${STATUS:-UNKNOWN}"
    fi
  fi
elif [[ -d "$ROOT/.claude" && -d "$ROOT/architecture" ]]; then
  say "LOOP: A — ARCHITECTURE MAINTENANCE (architect workspace)"
else
  say "LOOP: UNDETERMINED — read .claude/skills/session-orientation/SKILL.md and ask."
fi

# ---------------------------------------------------------------- drift flag
if [[ -f "$ROOT/.claude/.drift-pending" ]]; then
  say ""
  say "DRIFT PENDING: product code changed since the last architecture rescan."
  say "  Touched (most recent first):"
  tail -n 8 "$ROOT/.claude/.drift-pending" | sed 's/^/    /'
  say "  → run /architecture-drift-analysis before certifying anything."
fi

# ---------------------------------------------------------------- open debt
if [[ -d "$ROOT/quality/technical-debt" ]]; then
  DEBT_COUNT=$(find "$ROOT/quality/technical-debt" -maxdepth 1 -name 'TD-*.md' 2>/dev/null | wc -l | tr -d ' ')
  [[ "$DEBT_COUNT" != "0" ]] && say "" && say "OPEN TECHNICAL DEBT: $DEBT_COUNT item(s) registered."
fi

# ---------------------------------------------------------------- memory (J4)
# Best-effort semantic retrieval; falls back to the newest entries.
MEM_DIR="$ROOT/project-memory"
if [[ -d "$MEM_DIR" ]]; then
  say ""
  say "PROJECT MEMORY — most recent decisions (semantic retrieval: use rag-mcp.retrieveContext for more):"
  find "$MEM_DIR" -maxdepth 1 -name '2*.md' 2>/dev/null \
    | sort -r | head -n "$MAX_MEMORY_ENTRIES" \
    | while read -r f; do
        TITLE="$(grep -m1 '^# ' "$f" 2>/dev/null | sed 's/^# //')"
        say "  · $(basename "$f" .md): ${TITLE:-untitled}"
      done
fi

say ""
say "Read .claude/skills/session-orientation/SKILL.md before acting."
say "=== END DIGEST ==="
exit 0
