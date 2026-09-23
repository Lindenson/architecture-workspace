#!/usr/bin/env bash
# Shared helpers for AIP hooks. Source, never execute.
#
# Everything here is dependency-free by design: hooks run on every tool call and
# on every session start, on whatever machine a developer happens to have. jq is
# used when present and worked around when not.

# shellcheck shell=bash

AIP_ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
AIP_CONFIG="${AIP_CONFIG_FILE:-$AIP_ROOT/.claude/aip.config.yml}"

# cfg <section>.<key> [default]
# Reads the flat two-level YAML described in aip.config.yml. Inline lists are
# returned space-separated. Missing key or missing file → the default.
cfg() {
  local path="$1" default="${2:-}" section key value
  section="${path%%.*}"; key="${path#*.}"
  [[ -f "$AIP_CONFIG" ]] || { printf '%s' "$default"; return; }
  value="$(awk -v sec="$section" -v k="$key" '
    /^[[:space:]]*#/ { next }
    /^[A-Za-z_][A-Za-z0-9_-]*:[[:space:]]*$/ {
      cur = $0; sub(/:.*$/, "", cur); insec = (cur == sec); next
    }
    /^[A-Za-z_]/ { insec = 0 }
    insec && /^[[:space:]]+[A-Za-z_][A-Za-z0-9_-]*:/ {
      line = $0
      sub(/^[[:space:]]+/, "", line)
      name = line; sub(/:.*$/, "", name)
      if (name != k) next
      sub(/^[^:]*:[[:space:]]*/, "", line)
      sub(/[[:space:]]*#.*$/, "", line)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
      if (line ~ /^\[.*\]$/) { gsub(/^\[|\]$/, "", line); gsub(/,[[:space:]]*/, " ", line) }
      gsub(/^"|"$/, "", line)
      print line; exit
    }
  ' "$AIP_CONFIG" 2>/dev/null)"
  [[ -z "$value" ]] && value="$default"
  printf '%s' "$value"
}

# json_get <json> <jq-path> <fallback-key>
# jq when available; a grep/sed approximation for the flat string case otherwise.
json_get() {
  local json="$1" jqpath="$2" fallback="${3:-}"
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$json" | jq -r "$jqpath // empty" 2>/dev/null
  elif [[ -n "$fallback" ]]; then
    printf '%s' "$json" | grep -o "\"$fallback\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" \
      | head -1 | sed 's/.*:[[:space:]]*"//; s/"$//'
  fi
}

# aip_feature_dir — the active Spec Kit feature directory, or empty.
# Spec Kit tracks the feature in .specify/feature.json, not in the git branch.
aip_feature_dir() {
  if [[ -n "${SPECIFY_FEATURE_DIRECTORY:-}" ]]; then printf '%s' "$SPECIFY_FEATURE_DIRECTORY"; return; fi
  local f="$AIP_ROOT/.specify/feature.json"
  [[ -f "$f" ]] || return 0
  json_get "$(cat "$f")" '.featureDirectory // .feature_directory' 'featureDirectory'
}

# aip_loop — A (architecture maintenance) | B (spec implementation) | UNKNOWN
aip_loop() {
  [[ -n "$(aip_feature_dir)" ]] && { printf 'B'; return; }
  [[ -d "$AIP_ROOT/.claude" && -d "$AIP_ROOT/architecture" ]] && { printf 'A'; return; }
  printf 'UNKNOWN'
}

# aip_match_glob <path> <glob>...  — true if path matches any glob.
# Callers must pass globs as separate, already-safe arguments. Prefer
# aip_match_cfg below, which reads them from config without letting the shell
# expand them against the working directory first.
aip_match_glob() {
  local p="$1"; shift
  local g
  for g in "$@"; do
    # shellcheck disable=SC2053
    [[ "$p" == $g ]] && return 0
  done
  return 1
}

# aip_match_cfg <path> <section.key>  — true if path matches any glob in that
# config list.
#
# WHY THIS EXISTS: `aip_match_glob "$p" $(cfg foo.paths)` is a trap. The result
# of a command substitution undergoes pathname expansion, so a pattern like
# `domain/raw/**` silently expands into whatever real files sit in the current
# working directory — and then matches nothing. The bug is invisible when CWD
# happens to contain no matching files, which is exactly why it survives review.
# `set -f` disables that expansion while leaving [[ == ]] pattern matching intact.
aip_match_cfg() {
  local p="$1" key="$2" g rc=1
  local had_f=1; [[ $- == *f* ]] || had_f=0
  set -f
  for g in $(cfg "$key"); do
    # shellcheck disable=SC2053
    if [[ "$p" == $g ]]; then rc=0; break; fi
  done
  (( had_f )) || set +f
  return $rc
}

# aip_gate_log <feature-dir> <verdict> <skill> <reason>
# Append-only evidence trail. This file is what makes the harness measurable:
# without it you never learn whether a gate catches real problems or noise.
aip_gate_log() {
  local fd="$1" verdict="$2" skill="$3" reason="$4"
  [[ -z "$fd" ]] && return 0
  local log="$AIP_ROOT/$fd/$(cfg gate.log_file gate-log.md)"
  mkdir -p "$(dirname "$log")" 2>/dev/null || return 0
  if [[ ! -f "$log" ]]; then
    {
      printf '# Gate log — %s\n\n' "$fd"
      printf 'Append-only record of every phase-gate evaluation. Evidence for the PR\n'
      printf 'and the substrate for harness metrics (automation/harness-metrics.sh).\n\n'
      printf '| UTC | Verdict | Phase | Reason |\n|---|---|---|---|\n'
    } > "$log"
  fi
  printf '| %s | %s | %s | %s |\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$verdict" "$skill" "${reason//|/ }" >> "$log"
}

# aip_open_decisions <feature-dir> — count rows marked OPEN in decisions.md
aip_open_decisions() {
  local fd="$1" f="$AIP_ROOT/$1/decisions.md"
  [[ -f "$f" ]] || { printf '0'; return; }
  grep -c '^|[^|]*|.*|[[:space:]]*OPEN[[:space:]]*|' "$f" 2>/dev/null || printf '0'
}
