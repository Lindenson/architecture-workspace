#!/usr/bin/env bash
# Stop hook — fires when the agent finishes a turn.
#
# Emits a reminder only when there is something to remember: product code moved
# but nothing was written to project-memory/ since. Silent otherwise — a hook
# that speaks every turn gets ignored within a day.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
FLAG="$ROOT/.claude/.drift-pending"
MEM="$ROOT/project-memory"

[[ -f "$FLAG" ]] || exit 0
[[ -d "$MEM" ]]  || exit 0

NEWEST_MEM="$(find "$MEM" -maxdepth 1 -name '2*.md' -newer "$FLAG" 2>/dev/null | head -1)"
[[ -n "$NEWEST_MEM" ]] && exit 0   # a decision was recorded after the last code change

CHANGED=$(wc -l < "$FLAG" 2>/dev/null | tr -d ' ')
printf 'Unrecorded change: %s product file(s) touched with no project-memory entry since.\n' "$CHANGED"
printf 'If a durable decision was made, run /decision-capture. If nothing was decided, ignore this.\n'
exit 0
