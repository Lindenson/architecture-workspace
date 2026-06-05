#!/usr/bin/env bash
# ============================================================================
#  nightly-pipeline.sh — AIP nightly chain
# ----------------------------------------------------------------------------
#  Runs the full nightly refresh as a sequence of independent stages. Each stage
#  is wrapped so one failure logs and continues — the pipeline never aborts
#  mid-chain. Suggested cron: 02:00 daily.
#
#  Stages:
#    1. jQAssistant scan      [MVP-2 placeholder] — skipped unless `jqassistant`
#    2. Sonar refresh         (sonar-mcp /api/sonar/state)
#    3. Structurizr update    [MVP-2 placeholder]
#    4. RAG reindex           [MVP-3 placeholder]
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

# --- Stage 1: jQAssistant scan (MVP-2 placeholder) --------------------------
stage_jqassistant() {
  if ! command -v jqassistant >/dev/null 2>&1; then
    log "jqassistant not on PATH — skipping (MVP-2; jqassistant-mcp planned)"
    return 0
  fi
  log "jqassistant found — MVP-2 scan wiring not implemented yet; skipping body"
  return 0
}

# --- Stage 2: Sonar refresh -------------------------------------------------
stage_sonar() {
  local url; url="$(aip_sonar_url)"
  log "refreshing Sonar via ${url}/api/sonar/state"
  aip_get "${url}/api/sonar/state" >/dev/null
}

# --- Stage 3: Structurizr update (MVP-2 placeholder) ------------------------
stage_structurizr() {
  log "Structurizr update is MVP-2 (structurizr-mcp planned) — skipping"
  return 0
}

# --- Stage 4: RAG reindex (MVP-3 placeholder) -------------------------------
stage_rag() {
  log "RAG reindex is MVP-3 (rag-mcp + pgvector) — skipping"
  return 0
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
