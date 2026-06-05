#!/usr/bin/env bash
# ============================================================================
#  automation/lib.sh — shared helpers for AIP automation scripts
# ----------------------------------------------------------------------------
#  Source this from the other scripts:  source "$(dirname "$0")/lib.sh"
#  Provides: .env loading, base-URL resolution, an authenticated curl helper,
#  command guards, logging, and REPORTS dir resolution.
#
#  Everything degrades gracefully: missing tools/servers are reported, not fatal
#  (callers decide whether to continue). Secrets come only from the environment.
# ============================================================================

# --- Path resolution --------------------------------------------------------
# Directory of this lib (automation/) and the repo root (its parent).
AIP_AUTOMATION_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AIP_REPO_ROOT="$(cd "${AIP_AUTOMATION_DIR}/.." && pwd)"
REPORTS_DIR="${AIP_REPORTS_DIR:-${AIP_REPO_ROOT}/reports}"

# --- Logging ----------------------------------------------------------------
log()  { printf '[%s] %s\n'  "$(date +'%F %T')" "$*"; }
warn() { printf '[%s] WARN: %s\n' "$(date +'%F %T')" "$*" >&2; }
err()  { printf '[%s] ERROR: %s\n' "$(date +'%F %T')" "$*" >&2; }

# --- .env loading -----------------------------------------------------------
# Load repo-root .env if present (export every assignment). Never fatal.
aip_load_env() {
  local env_file="${AIP_REPO_ROOT}/.env"
  if [[ -f "${env_file}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
    log "loaded env from ${env_file}"
  else
    warn ".env not found at ${env_file} — using process env / defaults (copy .env.example → .env)"
  fi
}

# --- Command guards ---------------------------------------------------------
# require_cmd <cmd> : returns non-zero (and warns) if <cmd> is missing.
require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    warn "required command '$1' not found on PATH"
    return 1
  fi
  return 0
}

# --- Base URL resolution ----------------------------------------------------
# Resolve each MCP server base URL from env with localhost defaults.
aip_twin_url()   { echo "${PORT_DIGITAL_TWIN_URL:-http://localhost:${PORT_DIGITAL_TWIN:-8080}}"; }
aip_jira_url()   { echo "${PORT_JIRA_URL:-http://localhost:${PORT_JIRA_MCP:-8081}}"; }
aip_github_url() { echo "${PORT_GITHUB_URL:-http://localhost:${PORT_GITHUB_MCP:-8082}}"; }
aip_sonar_url()  { echo "${PORT_SONAR_URL:-http://localhost:${PORT_SONAR_MCP:-8083}}"; }

# --- Authenticated GET ------------------------------------------------------
# aip_get <url> : GET with bearer token + timeout. Echoes body on HTTP 2xx and
# returns 0; otherwise warns (with the status code) and returns non-zero.
# Degrades gracefully when curl is missing.
aip_get() {
  local url="$1"
  require_cmd curl || return 2
  local token="${AIP_INTERNAL_TOKEN:-}"
  local tmp http
  tmp="$(mktemp)"
  http="$(curl -sS \
            --connect-timeout "${AIP_CURL_CONNECT_TIMEOUT:-5}" \
            --max-time "${AIP_CURL_MAX_TIME:-30}" \
            -H "Authorization: Bearer ${token}" \
            -H "Accept: application/json" \
            -o "${tmp}" -w '%{http_code}' \
            "${url}" 2>/dev/null || echo "000")"
  if [[ "${http}" =~ ^2 ]]; then
    cat "${tmp}"
    rm -f "${tmp}"
    return 0
  fi
  rm -f "${tmp}"
  warn "GET ${url} failed (HTTP ${http})"
  return 1
}

# --- JSON field extraction --------------------------------------------------
# aip_json_field <json> <jq-filter> : extract a field with jq if available,
# else echo nothing and return 1 (caller falls back to the raw body).
aip_json_field() {
  local json="$1" filter="$2"
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "${json}" | jq -r "${filter}" 2>/dev/null
    return $?
  fi
  warn "jq not found — cannot extract '${filter}'"
  return 1
}
