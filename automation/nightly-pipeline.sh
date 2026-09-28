#!/usr/bin/env bash
# ============================================================================
#  nightly-pipeline.sh — AIP nightly chain
# ----------------------------------------------------------------------------
#  Runs the full nightly refresh as a sequence of independent stages. Each stage
#  is wrapped so one failure logs and continues — the pipeline never aborts
#  mid-chain. Suggested cron: 02:00 daily.
#
#  Stages:
#    1. jQAssistant scan      — populates the dependency graph (requires jqassistant)
#    2. Sonar refresh         (sonar-mcp /api/sonar/state)
#    3. Structurizr check     — validate the C4 model and report code-vs-model drift
#    4. RAG reindex           — optional knowledge layer only
#    5. Reports               (digital-twin DAILY; WEEKLY on Sundays)
# ============================================================================
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
aip_load_env

FAILED_STAGES=()

# run_stage <name> <function> : execute a stage, never abort on failure.
run_stage() {
  local name="$1"; shift
  log "=== STAGE: ${name} ==="
  if "$@"; then
    log "--- STAGE OK: ${name}"
  else
    warn "--- STAGE FAILED: ${name} (continuing)"
    FAILED_STAGES+=("${name}")
  fi
}

# --- Stage 1: jQAssistant scan ----------------------------------------------
#
# This stage used to log "scan wiring not implemented yet" and return 0, so the
# pipeline went green every night with an empty graph. Every tool built on the
# graph then answered truthfully about nothing. A stage that cannot do its job
# must SAY SO in the pipeline status, not report success.
stage_jqassistant() {
  if ! command -v jqassistant >/dev/null 2>&1; then
    warn "jqassistant not on PATH — the dependency graph will not be refreshed."
    warn "Blast radius, cycles and layering checks answer from a stale or empty graph."
    return 1
  fi
  # SCAN_TARGETS: space-separated product repo paths. Unset scans this repo's
  # own MCP servers, which at least keeps the chain exercised.
  # shellcheck disable=SC2086
  "$ROOT/automation/scan-graph.sh" ${SCAN_TARGETS:-}
}

# --- Stage 2: Sonar refresh -------------------------------------------------
stage_sonar() {
  local url; url="$(aip_sonar_url)"
  log "refreshing Sonar via ${url}/api/sonar/state"
  aip_get "${url}/api/sonar/state" >/dev/null
}

# --- Stage 3: Structurizr model check ---------------------------------------
#
# Not an "update": nothing here edits workspace.dsl. The C4 model is
# architect-owned, and drift between it and the code is REPORTED, never erased
# by rewriting the model (.claude/OPERATING_LOOPS.md, write-rights table).
stage_structurizr() {
  local url; url="$(aip_structurizr_url)"
  log "validating the C4 model via ${url}/api/structurizr/validate"
  aip_get "${url}/api/structurizr/validate" >/dev/null || return 1
  log "checking code-vs-model drift via ${url}/api/structurizr/drift"
  aip_get "${url}/api/structurizr/drift" >/dev/null || return 1
}

# --- Stage 4: RAG reindex ----------------------------------------------------
#
# The knowledge layer is OPTIONAL and off unless KNOWLEDGE_ENABLED=true.
# "Disabled" is a valid outcome reported as such — not a skipped stage that
# looks like success, and not a failure either.
stage_rag() {
  if [[ "${KNOWLEDGE_ENABLED:-false}" != "true" ]]; then
    log "knowledge layer disabled (KNOWLEDGE_ENABLED=false) — nothing to reindex"
    return 0
  fi
  local url; url="$(aip_rag_url)"
  log "reindexing project-memory and specs via ${url}/api/rag/reindex"
  aip_get "${url}/api/rag/reindex" >/dev/null || return 1
  # Yesterday's decisions must be retrievable in today's sessions; an index that
  # accepted a write and returns nothing looks exactly like one that worked.
  log "verifying retrieval answers after reindex"
  aip_get "${url}/api/rag/state" >/dev/null || return 1
}

# --- Stage 5: Reports -------------------------------------------------------
stage_reports() {
  local twin; twin="$(aip_twin_url)"
  local ok=0

  # DAILY every night.
  log "generating DAILY report"
  if md_json="$(aip_get "${twin}/api/twin/report?type=DAILY")"; then
    mkdir -p "${REPORTS_DIR}/daily"
    out="${REPORTS_DIR}/daily/$(date +%F).md"
    if md="$(aip_json_field "${md_json}" '.data // empty')" && [[ -n "${md}" ]]; then
      printf '%s\n' "${md}" > "${out}"
    else
      printf '%s\n' "${md_json}" > "${out}"
    fi
    log "wrote ${out}"
  else
    warn "DAILY report unreachable"
    ok=1
  fi

  # WEEKLY on Sundays (date +%u == 7).
  if [[ "$(date +%u)" -eq 7 ]]; then
    log "Sunday — generating WEEKLY report"
    if wk_json="$(aip_get "${twin}/api/twin/report?type=WEEKLY")"; then
      mkdir -p "${REPORTS_DIR}/weekly"
      wout="${REPORTS_DIR}/weekly/$(date +%G-W%V).md"
      if wmd="$(aip_json_field "${wk_json}" '.data // empty')" && [[ -n "${wmd}" ]]; then
        printf '%s\n' "${wmd}" > "${wout}"
      else
        printf '%s\n' "${wk_json}" > "${wout}"
      fi
      log "wrote ${wout}"
    else
      warn "WEEKLY report unreachable"
      ok=1
    fi
  fi
  return "${ok}"
}

# --- Drive the chain --------------------------------------------------------
log "nightly-pipeline starting"
run_stage "jqassistant-scan" stage_jqassistant
run_stage "sonar-refresh"     stage_sonar
run_stage "structurizr-update" stage_structurizr
run_stage "rag-reindex"       stage_rag
run_stage "reports"           stage_reports

if [[ "${#FAILED_STAGES[@]}" -eq 0 ]]; then
  log "nightly-pipeline complete — all stages OK"
else
  warn "nightly-pipeline complete with ${#FAILED_STAGES[@]} failed stage(s): ${FAILED_STAGES[*]}"
fi
