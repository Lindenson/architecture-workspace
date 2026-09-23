#!/usr/bin/env bash
# PostToolUse:Edit|Write|MultiEdit hook.
#
# Two jobs:
#   1. Protect governance artifacts from Loop B. Editing a numbered ADR, a
#      constraint, a standard, the validated domain model or the C4 workspace is
#      an architect decision — the agent emits a draft instead. Exit 2 blocks.
#   2. Record that product code changed, so the next session and the nightly
#      pipeline know the architecture model may have drifted.
#
# Always cheap, never chatty: no stdout on the happy path.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
INPUT="$(cat)"

if command -v jq >/dev/null 2>&1; then
  FILE="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // .tool_input.path // empty' 2>/dev/null)"
else
  FILE="$(printf '%s' "$INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' \
    | head -1 | sed 's/.*:[[:space:]]*"//; s/"$//')"
fi
[[ -z "$FILE" ]] && exit 0

REL="${FILE#"$ROOT"/}"

# ------------------------------------------------------- 1. governance guard
deny() {
  printf 'BLOCKED: %s is architect-owned (see .claude/OPERATING_LOOPS.md, write-rights table).\n' "$REL" >&2
  printf 'Write a draft instead: architecture/adr/drafts/ · knowledge/drafts/ · quality/technical-debt/drafts/\n' >&2
  printf 'Drift between code and the model is REPORTED, never erased by editing the model.\n' >&2
  exit 2
}

case "$REL" in
  architecture/adr/drafts/*|knowledge/drafts/*|quality/technical-debt/drafts/*)
    : ;;                                        # drafts are always allowed
  architecture/adr/ADR-*.md)              deny ;;
  architecture/constraints/*)             deny ;;
  architecture/standards/*)               deny ;;
  architecture/c4/workspace.dsl)          deny ;;
  architecture/target-architecture/*)     deny ;;
  domain/model/*)                         deny ;;
  domain/constraints/*)                   deny ;;
  delivery/roadmap/*)                     deny ;;
  domain/raw/*)
    printf 'BLOCKED: domain/raw/ holds auto-extracted FACTS and is never hand-edited.\n' >&2
    printf 'Re-run the extraction (jqassistant-mcp.runScan) instead.\n' >&2
    exit 2 ;;
esac

# ------------------------------------------------------- 2. drift bookkeeping
case "$FILE" in
  *.java|*.kt|*.kts|*.go|*.py|*.ts|*.tsx|*.js|*.cs|*.rb|*.rs|*.sql|*.proto|*.yaml|*.yml)
    case "$REL" in
      */test/*|*/tests/*|*.test.*|*.spec.*) exit 0 ;;   # tests do not move the model
      .claude/*|reports/*|specs/*)          exit 0 ;;
    esac
    mkdir -p "$ROOT/.claude"
    printf '%s  %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$REL" >> "$ROOT/.claude/.drift-pending"
    # keep the flag file bounded
    tail -n 200 "$ROOT/.claude/.drift-pending" > "$ROOT/.claude/.drift-pending.tmp" 2>/dev/null \
      && mv "$ROOT/.claude/.drift-pending.tmp" "$ROOT/.claude/.drift-pending"
    ;;
esac

exit 0
