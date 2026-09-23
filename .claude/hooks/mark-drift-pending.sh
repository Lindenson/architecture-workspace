#!/usr/bin/env bash
# PostToolUse hook — governance guard + drift bookkeeping.
#
# The guard is LOOP-AWARE. This workspace is the architect's own desk: in Loop A
# they approve ADRs and edit constraints, and a hook that blocks that blocks
# their primary job — after which the hook gets deleted along with its useful
# half. So: warn in Loop A, block in Loop B. Both modes are configurable.
set -uo pipefail
# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

INPUT="$(cat)"
FILE="$(json_get "$INPUT" '.tool_input.file_path // .tool_input.path' 'file_path')"
[[ -z "$FILE" ]] && exit 0
REL="${FILE#"$AIP_ROOT"/}"

LOOP="$(aip_loop)"
case "$LOOP" in
  B) MODE="$(cfg architect_owned.loop_b_mode block)" ;;
  *) MODE="$(cfg architect_owned.loop_a_mode warn)" ;;
esac

# Drafts are always writable — that is the whole point of the draft path.
if aip_match_cfg "$REL" architect_owned.draft_paths; then
  exit 0
fi

# Auto-extracted facts: never hand-editable in any loop. Re-run the extraction.
if aip_match_cfg "$REL" architect_owned.generated_paths; then
  printf 'BLOCKED: %s holds auto-extracted FACTS and is never hand-edited.\n' "$REL" >&2
  printf 'Re-run the extraction (jqassistant-mcp.runScan) instead.\n' >&2
  exit 2
fi

if aip_match_cfg "$REL" architect_owned.paths; then
  MSG=$(cat <<TXT
$REL is architect-owned (.claude/OPERATING_LOOPS.md, write-rights table).
Emit a draft instead: architecture/adr/drafts/ · knowledge/drafts/ · quality/technical-debt/drafts/
Drift between code and the model is REPORTED, never erased by editing the model.
TXT
)
  case "$MODE" in
    block)
      printf 'BLOCKED (loop %s): %s\n' "$LOOP" "$MSG" >&2
      printf 'Config: architect_owned.loop_%s_mode in .claude/aip.config.yml\n' "$(printf '%s' "$LOOP" | tr 'AB' 'ab')" >&2
      exit 2 ;;
    warn)
      # Loop A: the architect is allowed to do this. Say it once, do not block.
      printf 'NOTE (loop %s): editing an architect-owned artifact — %s\n' "$LOOP" "$REL"
      printf 'If this is an approval, record it in project-memory/ so the reason survives.\n'
      exit 0 ;;
    *) exit 0 ;;
  esac
fi

# ------------------------------------------------------------ drift bookkeeping
EXT="${FILE##*.}"
aip_match_cfg "$EXT" drift.code_extensions || exit 0
aip_match_cfg "$REL" drift.ignore_globs && exit 0

FLAG="$AIP_ROOT/$(cfg drift.flag_file .claude/.drift-pending)"
mkdir -p "$(dirname "$FLAG")" 2>/dev/null
printf '%s  %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$REL" >> "$FLAG"
MAX="$(cfg drift.max_entries 200)"
tail -n "$MAX" "$FLAG" > "$FLAG.tmp" 2>/dev/null && mv "$FLAG.tmp" "$FLAG"
exit 0
