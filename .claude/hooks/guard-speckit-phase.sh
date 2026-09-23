#!/usr/bin/env bash
# PreToolUse:Skill hook — enforces Loop B phase order (join point J2).
#
# Hook input is JSON on stdin. Exit code 2 blocks the tool call and returns
# stderr to the agent as the reason; exit 0 allows it.
#
# Rules enforced:
#   feature-impact-analysis  requires  spec.md
#   spec-critic              requires  spec.md + impact.md
#   speckit-plan             requires  critique.md, status != BLOCKED
#   speckit-implement        requires  critique.md READY|NEEDS_DECISION + plan.md + tasks.md
#   speckit-converge         requires  tasks.md
#
# The gate is about artifact existence and status, never about content quality.
# A human can always override by producing the missing artifact deliberately.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
INPUT="$(cat)"

extract() { # extract <json> <key-regex>
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$1" | jq -r "$2 // empty" 2>/dev/null
  else
    printf '%s' "$1" | grep -o "\"$3\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" \
      | head -1 | sed 's/.*:[[:space:]]*"//; s/"$//'
  fi
}

SKILL="$(extract "$INPUT" '.tool_input.skill // .tool_input.name // .tool_input.command' 'skill')"
[[ -z "$SKILL" ]] && exit 0

# Normalize: speckit.plan / speckit-plan / /speckit-plan → speckit-plan
SKILL="$(printf '%s' "$SKILL" | tr '.' '-' | sed 's#^/##' | tr '[:upper:]' '[:lower:]')"

# ------------------------------------------------------------- feature dir
FEATURE_DIR=""
if [[ -n "${SPECIFY_FEATURE_DIRECTORY:-}" ]]; then
  FEATURE_DIR="$SPECIFY_FEATURE_DIRECTORY"
elif [[ -f "$ROOT/.specify/feature.json" ]]; then
  if command -v jq >/dev/null 2>&1; then
    FEATURE_DIR="$(jq -r '.featureDirectory // .feature_directory // empty' \
      "$ROOT/.specify/feature.json" 2>/dev/null)"
  else
    FEATURE_DIR="$(grep -o '"[^"]*feature[Dd]irectory"[[:space:]]*:[[:space:]]*"[^"]*"' \
      "$ROOT/.specify/feature.json" | head -1 | sed 's/.*:[[:space:]]*"//; s/"$//')"
  fi
fi

# No active feature → nothing to gate (e.g. /speckit-constitution, Loop A skills).
[[ -z "$FEATURE_DIR" ]] && exit 0
FD="$ROOT/$FEATURE_DIR"

have() { [[ -f "$FD/$1" ]]; }

critique_status() {
  have critique.md || { echo "MISSING"; return; }
  grep -m1 -oE '\b(BLOCKED|NEEDS_DECISION|READY)\b' "$FD/critique.md" 2>/dev/null || echo "UNKNOWN"
}

block() {
  printf 'BLOCKED by Loop B phase gate (.claude/hooks/guard-speckit-phase.sh).\n%s\n' "$1" >&2
  printf 'Phase order: specify → feature-impact-analysis → clarify → spec-critic → [HUMAN] → plan → checklist → tasks → analyze → [HUMAN] → implement ⇄ converge.\n' >&2
  printf 'See .claude/OPERATING_LOOPS.md. Produce the missing artifact; do not route around the gate.\n' >&2
  exit 2
}

case "$SKILL" in
  feature-impact-analysis)
    have spec.md || block "spec.md is missing in $FEATURE_DIR. Run /speckit-specify first."
    ;;

  spec-critic)
    have spec.md   || block "spec.md is missing in $FEATURE_DIR."
    have impact.md || block "impact.md is missing. Run /feature-impact-analysis first — a critique without a measured blast radius is opinion, not evidence."
    ;;

  speckit-plan)
    have impact.md || block "impact.md is missing. Run /feature-impact-analysis before planning."
    case "$(critique_status)" in
      MISSING) block "critique.md is missing. Run /spec-critic before planning." ;;
      BLOCKED) block "critique.md status is BLOCKED. Resolve the critical issues and re-run /spec-critic; a human must approve before planning." ;;
    esac
    ;;

  speckit-implement)
    case "$(critique_status)" in
      MISSING) block "critique.md is missing. Loop B does not implement an uncriticized spec." ;;
      BLOCKED) block "critique.md status is BLOCKED. Implementation must not start." ;;
    esac
    have plan.md  || block "plan.md is missing. Run /speckit-plan."
    have tasks.md || block "tasks.md is missing. Run /speckit-tasks."
    ;;

  speckit-converge)
    have tasks.md || block "tasks.md is missing — converge runs only after tasks and an implement pass."
    ;;
esac

exit 0
