#!/usr/bin/env bash
# Stop hook — fires when the agent finishes a turn.
# Speaks only when there is something to remember. A hook that talks every turn
# is ignored within a day, which costs more than saying nothing.
set -uo pipefail
# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

FLAG="$AIP_ROOT/$(cfg drift.flag_file .claude/.drift-pending)"
MEM="$AIP_ROOT/project-memory"
[[ -f "$FLAG" ]] || exit 0
[[ -d "$MEM"  ]] || exit 0

# A memory entry newer than the last code change means the decision was captured.
[[ -n "$(find "$MEM" -maxdepth 1 -name '2*.md' -newer "$FLAG" 2>/dev/null | head -1)" ]] && exit 0

FD="$(aip_feature_dir)"
if [[ -n "$FD" && "$(aip_open_decisions "$FD")" -gt 0 ]]; then
  printf 'Open human questions in %s/decisions.md — they block /speckit-plan and /speckit-implement.\n' "$FD"
fi

printf 'Unrecorded change: %s product file(s) touched with no project-memory entry since.\n' \
  "$(wc -l < "$FLAG" 2>/dev/null | tr -d ' ')"
printf 'If a durable decision was made, run /decision-capture. If nothing was decided, ignore this.\n'
exit 0
