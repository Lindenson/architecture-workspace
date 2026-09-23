#!/usr/bin/env bash
# PreToolUse hook — Loop B phase order and the man-in-the-middle check (J2).
#
# Three honest limitations, stated up front because the rest of the harness
# depends on understanding them:
#
#   1. critique.md is written by the agent. A gate that reads it is an AUDIT,
#      not a lock: a determined agent can set the status itself. The real lock
#      is CODEOWNERS + branch protection on the PR (see .github/CODEOWNERS).
#      What this hook guarantees is that every pass leaves a row in gate-log.md,
#      which travels into the PR and is reviewable by a human.
#   2. The tool schema for skill invocation is not contractually stable. This
#      hook reads several shapes and no-ops when it recognises none. Verify
#      against your Claude Code build with: automation/verify-hooks.sh --live
#   3. Overrides are always possible via $AIP_GATE_OVERRIDE and always logged.
#
# Exit 2 blocks and returns stderr to the agent; exit 0 allows.
set -uo pipefail
# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

INPUT="$(cat)"
MODE="$(cfg gate.mode block)"
[[ "$MODE" == "off" ]] && exit 0

# --------------------------------------------------------------- which phase
SKILL="$(json_get "$INPUT" '.tool_input.skill // .tool_input.name // .tool_input.command // .tool_input.subagent_type' 'skill')"
[[ -z "$SKILL" ]] && SKILL="$(json_get "$INPUT" '.tool_name' 'tool_name')"
[[ -z "$SKILL" ]] && exit 0
# /speckit.plan · speckit-plan · SpecKit.Plan → speckit-plan
SKILL="$(printf '%s' "$SKILL" | tr '.' '-' | sed 's#^/##; s#^mcp__##' | tr '[:upper:]' '[:lower:]')"

aip_match_cfg "$SKILL" gate.phases || exit 0

FD="$(aip_feature_dir)"
# No active feature → this is not Loop B. Trivial fixes and Loop A work are
# deliberately ungated (see OPERATING_LOOPS.md, "When a change does not need Loop B").
[[ -z "$FD" ]] && exit 0
FDIR="$AIP_ROOT/$FD"

have()  { [[ -f "$FDIR/$1" ]]; }
status() {
  have critique.md || { echo MISSING; return; }
  grep -m1 -oE '\b(BLOCKED|NEEDS_DECISION|READY)\b' "$FDIR/critique.md" 2>/dev/null || echo UNKNOWN
}

# --------------------------------------------------------------- verdict
REASON=""
case "$SKILL" in
  feature-impact-analysis)
    have spec.md || REASON="spec.md missing — run /speckit-specify first." ;;
  spec-critic)
    have spec.md   || REASON="spec.md missing."
    [[ -z "$REASON" ]] && { have impact.md || REASON="impact.md missing — run /feature-impact-analysis; a critique without a measured blast radius is opinion, not evidence."; } ;;
  speckit-plan)
    have impact.md || REASON="impact.md missing — run /feature-impact-analysis before planning."
    if [[ -z "$REASON" ]]; then
      case "$(status)" in
        MISSING) REASON="critique.md missing — run /spec-critic before planning." ;;
        BLOCKED) REASON="critique.md is BLOCKED — resolve the critical issues, re-run /spec-critic, get human sign-off." ;;
      esac
    fi ;;
  speckit-implement)
    case "$(status)" in
      MISSING) REASON="critique.md missing — Loop B does not implement an uncriticized spec." ;;
      BLOCKED) REASON="critique.md is BLOCKED — implementation must not start." ;;
    esac
    [[ -z "$REASON" ]] && { have plan.md  || REASON="plan.md missing — run /speckit-plan."; }
    [[ -z "$REASON" ]] && { have tasks.md || REASON="tasks.md missing — run /speckit-tasks."; } ;;
  speckit-converge)
    have tasks.md || REASON="tasks.md missing — converge runs only after tasks and an implement pass." ;;
esac

# --------------------------------- man-in-the-middle: unanswered human questions
if [[ -z "$REASON" && "$(cfg gate.require_decisions_closed true)" == "true" ]]; then
  case "$SKILL" in
    speckit-plan|speckit-implement)
      OPEN="$(aip_open_decisions "$FD")"
      if [[ "${OPEN:-0}" -gt 0 ]]; then
        REASON="$OPEN unanswered question(s) in decisions.md — these are human decisions, not agent decisions. Answer them (or mark DEFERRED with a reason) before proceeding."
      fi ;;
  esac
fi

[[ -z "$REASON" ]] && { aip_gate_log "$FD" PASS "$SKILL" "prerequisites satisfied"; exit 0; }

# --------------------------------------------------------------- override
OVERRIDE_VAR="$(cfg gate.override_env AIP_GATE_OVERRIDE)"
OVERRIDE="${!OVERRIDE_VAR:-}"
if [[ -n "$OVERRIDE" ]]; then
  aip_gate_log "$FD" OVERRIDE "$SKILL" "$REASON || override: $OVERRIDE"
  printf 'Gate overridden for %s. Reason recorded in %s/%s: %s\n' \
    "$SKILL" "$FD" "$(cfg gate.log_file gate-log.md)" "$OVERRIDE" >&2
  exit 0
fi

if [[ "$MODE" == "warn" ]]; then
  aip_gate_log "$FD" WARN "$SKILL" "$REASON"
  printf 'GATE WARNING (mode=warn, not blocking): %s\n' "$REASON" >&2
  exit 0
fi

aip_gate_log "$FD" BLOCK "$SKILL" "$REASON"
cat >&2 <<MSG
BLOCKED by the Loop B phase gate.
$REASON

Phase order: specify → feature-impact-analysis → clarify → spec-critic → [HUMAN]
             → plan → checklist → tasks → analyze → [HUMAN] → implement ⇄ converge
Produce the missing artifact rather than routing around the gate.
To proceed deliberately: set $OVERRIDE_VAR="<why>" — it is allowed and logged.
Contract: .claude/OPERATING_LOOPS.md · Config: .claude/aip.config.yml
MSG
exit 2
