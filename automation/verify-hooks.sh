#!/usr/bin/env bash
# Self-test for the governance harness.
#
# Everything the hooks do rests on assumptions about the tool-call JSON schema,
# and that schema is not contractually stable across Claude Code versions. This
# script proves the hooks behave as designed against synthetic input, and with
# --live shows what your build actually sends so you can confirm the shape.
#
#   automation/verify-hooks.sh          # synthetic self-test (CI-safe)
#   automation/verify-hooks.sh --live   # print a debug hook to install temporarily
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS="$ROOT/.claude/hooks"
PASS=0; FAIL=0

if [[ "${1:-}" == "--live" ]]; then
  cat <<'DEBUG'
To capture the real tool-call schema of YOUR Claude Code build, add this hook
temporarily to .claude/settings.json, run one /speckit-* skill, then read the log:

  "PreToolUse": [{ "matcher": "*", "hooks": [{ "type": "command",
     "command": "cat >> /tmp/aip-hook-schema.jsonl; exit 0" }]}]

  tail -5 /tmp/aip-hook-schema.jsonl | jq .

Confirm which field carries the skill name, then check that
.claude/hooks/guard-speckit-phase.sh reads it (it tries .tool_input.skill,
.name, .command, .subagent_type, then .tool_name). Remove the debug hook after.
Also run `claude --debug hooks` to see which hooks are registered at all.
DEBUG
  exit 0
fi

WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/.specify" "$WORK/specs/f" "$WORK/architecture/adr/drafts" "$WORK/project-memory"
cp -r "$ROOT/.claude" "$WORK/.claude"
printf '{"featureDirectory":"specs/f"}\n' > "$WORK/.specify/feature.json"
export CLAUDE_PROJECT_DIR="$WORK"

check() { # check <name> <expected-exit> <actual-exit>
  if [[ "$2" == "$3" ]]; then printf '  PASS  %s\n' "$1"; PASS=$((PASS+1))
  else printf '  FAIL  %s (expected exit %s, got %s)\n' "$1" "$2" "$3"; FAIL=$((FAIL+1)); fi
}
skill() { printf '{"tool_name":"Skill","tool_input":{"skill":"%s"}}' "$1" \
  | "$HOOKS/guard-speckit-phase.sh" >/dev/null 2>&1; echo $?; }
edit()  { printf '{"tool_name":"Edit","tool_input":{"file_path":"%s/%s"}}' "$WORK" "$1" \
  | "$HOOKS/mark-drift-pending.sh" >/dev/null 2>&1; echo $?; }

echo "== config parses =="
# shellcheck source=../.claude/hooks/lib.sh
source "$HOOKS/lib.sh"
check "gate.mode is readable"   "block" "$(cfg gate.mode MISSING)"
check "unknown key falls back"  "X"     "$(cfg gate.nope X)"
check "loop detected as B"      "B"     "$(aip_loop)"

echo "== phase gate =="
check "impact-analysis without spec.md blocks"  2 "$(skill feature-impact-analysis)"
: > "$WORK/specs/f/spec.md"
check "impact-analysis with spec.md passes"     0 "$(skill feature-impact-analysis)"
check "critic without impact.md blocks"         2 "$(skill spec-critic)"
: > "$WORK/specs/f/impact.md"
check "critic with impact.md passes"            0 "$(skill spec-critic)"
printf '## Status\nBLOCKED\n' > "$WORK/specs/f/critique.md"
check "implement on BLOCKED critique blocks"    2 "$(skill speckit-implement)"
printf '## Status\nREADY\n' > "$WORK/specs/f/critique.md"
: > "$WORK/specs/f/plan.md"; : > "$WORK/specs/f/tasks.md"
check "implement on READY passes"               0 "$(skill speckit-implement)"

echo "== man-in-the-middle: open decisions =="
printf '| ID | Q | by | opts | OPEN | | | | |\n' > "$WORK/specs/f/decisions.md"
check "open decision row blocks implement"      2 "$(skill speckit-implement)"
printf '| ID | Q | by | opts | ANSWERED | payment-svc | @d | today | ADR |\n' > "$WORK/specs/f/decisions.md"
check "answered decision unblocks"              0 "$(skill speckit-implement)"

echo "== override and modes =="
printf '| ID | Q | by | opts | OPEN | | | | |\n' > "$WORK/specs/f/decisions.md"
check "override passes a blocked gate"          0 "$(AIP_GATE_OVERRIDE='shipping a hotfix' skill speckit-implement)"
grep -q 'OVERRIDE' "$WORK/specs/f/gate-log.md" \
  && { echo "  PASS  override recorded in gate-log.md"; PASS=$((PASS+1)); } \
  || { echo "  FAIL  override not recorded"; FAIL=$((FAIL+1)); }
sed -i 's/^  mode: block/  mode: warn/' "$WORK/.claude/aip.config.yml"
check "mode=warn does not block"                0 "$(skill speckit-implement)"
sed -i 's/^  mode: warn/  mode: off/'   "$WORK/.claude/aip.config.yml"
check "mode=off disables the gate"              0 "$(skill speckit-implement)"
sed -i 's/^  mode: off/  mode: block/'  "$WORK/.claude/aip.config.yml"

echo "== governance guard (loop-aware) =="
check "loop B blocks a numbered ADR"            2 "$(edit architecture/adr/ADR-001-postgresql.md)"
check "drafts are always writable"              0 "$(edit architecture/adr/drafts/ADR-009.md)"
check "generated facts are never editable"      2 "$(edit domain/raw/jqa-graph/x.json)"
rm "$WORK/.specify/feature.json"   # → loop A: the architect's own desk
check "loop A warns, does not block"            0 "$(edit architecture/adr/ADR-001-postgresql.md)"
printf '{"featureDirectory":"specs/f"}\n' > "$WORK/.specify/feature.json"

echo "== drift bookkeeping =="
check "product code sets the drift flag"        0 "$(edit src/main/java/Order.java)"
[[ -f "$WORK/.claude/.drift-pending" ]] \
  && { echo "  PASS  drift flag written"; PASS=$((PASS+1)); } \
  || { echo "  FAIL  drift flag missing"; FAIL=$((FAIL+1)); }
check "tests do not move the model"             0 "$(edit src/test/java/OrderTest.java)"
CLAUDE_PROJECT_DIR="$WORK" "$ROOT/automation/reset-drift-flag.sh" >/dev/null 2>&1
[[ ! -f "$WORK/.claude/.drift-pending" ]] \
  && { echo "  PASS  reset-drift-flag clears it"; PASS=$((PASS+1)); } \
  || { echo "  FAIL  drift flag not cleared"; FAIL=$((FAIL+1)); }

echo "== session digest =="
# NB: capture first, then grep. `cmd | grep -q` closes the pipe early, the
# producer takes SIGPIPE, and `pipefail` reports that as a failure — a false
# negative that looks exactly like a broken hook.
DIGEST="$("$HOOKS/session-start.sh" 2>/dev/null)"
case "$DIGEST" in
  *"LOOP: B"*) echo "  PASS  digest reports loop B"; PASS=$((PASS+1)) ;;
  *) echo "  FAIL  digest wrong"; FAIL=$((FAIL+1)) ;;
esac
case "$DIGEST" in
  *"OPEN HUMAN QUESTIONS"*) echo "  PASS  digest surfaces open decisions"; PASS=$((PASS+1)) ;;
  *) echo "  FAIL  digest hides open decisions"; FAIL=$((FAIL+1)) ;;
esac
case "$DIGEST" in
  *"newest-first"*|*"semantic retrieval"*) echo "  PASS  digest names its retrieval mode"; PASS=$((PASS+1)) ;;
  *) echo "  FAIL  digest does not state retrieval mode"; FAIL=$((FAIL+1)) ;;
esac

printf '\n%s passed, %s failed\n' "$PASS" "$FAIL"
[[ "$FAIL" == "0" ]] || exit 1
