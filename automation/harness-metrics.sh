#!/usr/bin/env bash
# Measure the governance harness itself.
#
# Governance you do not measure becomes a ritual. These numbers answer the only
# questions that matter about a gate:
#   · does it fire?               (0 blocks in a quarter = theatre, or nothing to catch)
#   · is it overridden constantly? (high override rate = the rule is wrong, not the team)
#   · do humans actually decide?   (decisions answered vs deferred vs never closed)
#
# Reads the append-only gate logs and decision ledgers. No external dependency.
set -uo pipefail
ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
SPECS="${1:-$ROOT/specs}"

[[ -d "$SPECS" ]] || { echo "No specs/ directory at $SPECS — nothing to measure."; exit 0; }

pass=0; block=0; warn=0; override=0
declare -A block_reasons=()
while IFS= read -r line; do
  case "$line" in
    *"| PASS |"*)     pass=$((pass+1)) ;;
    *"| WARN |"*)     warn=$((warn+1)) ;;
    *"| OVERRIDE |"*) override=$((override+1)) ;;
    *"| BLOCK |"*)
      block=$((block+1))
      r="$(printf '%s' "$line" | awk -F'|' '{print $5}' | cut -c1-60 | sed 's/^ *//; s/ *$//')"
      block_reasons["$r"]=$(( ${block_reasons["$r"]:-0} + 1 )) ;;
  esac
done < <(find "$SPECS" -name 'gate-log.md' -exec cat {} + 2>/dev/null)

total=$((pass+block+warn+override))
echo "=== Gate activity ==="
if (( total == 0 )); then
  echo "  No gate evaluations recorded. Either no Loop B feature has run, or the"
  echo "  hooks are not wired — check: automation/verify-hooks.sh"
else
  printf '  evaluations: %d  ·  pass %d  ·  block %d  ·  warn %d  ·  override %d\n' \
    "$total" "$pass" "$block" "$warn" "$override"
  if (( block + override > 0 )); then
    printf '  override rate: %d%% of stops\n' $(( override * 100 / (block + override) ))
    (( override * 100 / (block + override) > 40 )) && \
      echo "  ⚠ Over 40% of stops are overridden. The rule is probably wrong, not the team."
  fi
  if (( block > 0 )); then
    echo "  top block reasons:"
    for r in "${!block_reasons[@]}"; do printf '    %3d  %s\n' "${block_reasons[$r]}" "$r"; done \
      | sort -rn | head -5
  fi
fi

echo
echo "=== Human decisions ==="
o=0; a=0; d=0
while IFS= read -r f; do
  o=$(( o + $(grep -c '|[[:space:]]*OPEN[[:space:]]*|'      "$f" 2>/dev/null || echo 0) ))
  a=$(( a + $(grep -c '|[[:space:]]*ANSWERED[[:space:]]*|'  "$f" 2>/dev/null || echo 0) ))
  d=$(( d + $(grep -c '|[[:space:]]*DEFERRED[[:space:]]*|'  "$f" 2>/dev/null || echo 0) ))
done < <(find "$SPECS" -name 'decisions.md' 2>/dev/null)
printf '  raised: %d  ·  answered %d  ·  deferred %d  ·  still open %d\n' $((o+a+d)) "$a" "$d" "$o"
(( o > 0 )) && echo "  ⚠ $o question(s) never closed — find them: grep -rl 'OPEN' $SPECS/*/decisions.md"
(( a + d > 0 && d * 100 / (a+d) > 50 )) && \
  echo "  ⚠ Most decisions are deferred. Deferral is debt with a nicer name."

echo
echo "=== Memory ==="
m=$(find "$ROOT/project-memory" -maxdepth 1 -name '2*.md' 2>/dev/null | wc -l | tr -d ' ')
# Match both the structured `type: rejected` front-matter and older free-form
# entries that predate the template.
r=$(grep -lEi 'type:[[:space:]]*rejected|^#.*rejected' "$ROOT/project-memory"/2*.md 2>/dev/null | wc -l | tr -d ' ')
printf '  entries: %s  ·  of which rejected options: %s\n' "$m" "$r"
[[ "$m" != "0" && "$r" == "0" ]] && \
  echo "  ⚠ No rejected options recorded. Only capturing what you adopted loses the expensive half."
