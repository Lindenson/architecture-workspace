#!/usr/bin/env bash
# Clear the drift-pending flag after an architecture rescan has actually run.
#
# Without this the flag is a one-way latch: one code edit and the session digest
# nags forever, which trains everyone to ignore it. `architecture-drift-analysis`
# calls this on completion; the nightly pipeline calls it after a full rescan.
set -euo pipefail
ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
# shellcheck source=../.claude/hooks/lib.sh
source "$ROOT/.claude/hooks/lib.sh"

FLAG="$AIP_ROOT/$(cfg drift.flag_file .claude/.drift-pending)"
ARCHIVE="$AIP_ROOT/reports/drift/.scanned"

[[ -f "$FLAG" ]] || { echo "No drift flag set — nothing to clear."; exit 0; }
COUNT=$(wc -l < "$FLAG" | tr -d ' ')
mkdir -p "$(dirname "$ARCHIVE")"
{ printf '# scanned %s (%s entries)\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$COUNT"; cat "$FLAG"; } >> "$ARCHIVE"
rm -f "$FLAG"
echo "Cleared drift flag: $COUNT entries archived to ${ARCHIVE#"$AIP_ROOT"/}"
