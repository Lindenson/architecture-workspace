#!/usr/bin/env bash
# ============================================================================
#  run-mcp-servers.sh — start the 4 MVP-1 MCP server jars locally
# ----------------------------------------------------------------------------
#  Launches each jar in the background, recording PIDs to automation/.pids and
#  logs to automation/logs/<module>.log. Stop them with stop-mcp-servers.sh.
#
#  Requires the jars to be built first:  cd mcp-servers && mvn -DskipTests package
#  For a containerized run instead, use:  docker compose up -d
# ============================================================================
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
aip_load_env

require_cmd java || { err "java not found — install a JDK 21 runtime"; exit 1; }

PIDS_FILE="${AIP_AUTOMATION_DIR}/.pids"
LOG_DIR="${AIP_AUTOMATION_DIR}/logs"
mkdir -p "${LOG_DIR}"
: > "${PIDS_FILE}"   # truncate / start fresh

# module:port pairs
MODULES=(
  "jira-mcp:8081"
  "github-mcp:8082"
  "sonar-mcp:8083"
  "digital-twin-core:8080"
)

# Pre-flight: ensure every jar exists before starting any.
missing=0
for entry in "${MODULES[@]}"; do
  module="${entry%%:*}"
  jar="${AIP_REPO_ROOT}/mcp-servers/${module}/target/${module}.jar"
  if [[ ! -f "${jar}" ]]; then
    err "missing jar: ${jar}"
    missing=1
  fi
done
if [[ "${missing}" -eq 1 ]]; then
  err "build the jars first:  (cd ${AIP_REPO_ROOT}/mcp-servers && mvn -DskipTests package)"
  exit 1
fi

for entry in "${MODULES[@]}"; do
  module="${entry%%:*}"
  port="${entry##*:}"
  jar="${AIP_REPO_ROOT}/mcp-servers/${module}/target/${module}.jar"
  logfile="${LOG_DIR}/${module}.log"
  log "starting ${module} on :${port} (log: ${logfile})"
  # Inherit the loaded .env so each server reads its secrets from the environment.
  nohup java -jar "${jar}" > "${logfile}" 2>&1 &
  pid=$!
  printf '%s %s\n' "${pid}" "${module}" >> "${PIDS_FILE}"
  log "  ${module} pid=${pid}"
done

log "all MVP-1 servers launched. PIDs in ${PIDS_FILE}."
log "check health with: automation/health-check.sh"
