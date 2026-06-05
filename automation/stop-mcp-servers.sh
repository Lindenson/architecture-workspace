#!/usr/bin/env bash
# ============================================================================
#  stop-mcp-servers.sh — stop the locally-launched MVP-1 MCP server jars
# ----------------------------------------------------------------------------
#  Reads automation/.pids (written by run-mcp-servers.sh) and terminates each
#  process. Degrades gracefully: already-dead PIDs are reported, not fatal.
# ============================================================================
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

PIDS_FILE="${AIP_AUTOMATION_DIR}/.pids"

if [[ ! -f "${PIDS_FILE}" ]]; then
  warn "no PID file at ${PIDS_FILE} — nothing to stop (were the servers started via run-mcp-servers.sh?)"
  exit 0
fi

stopped=0
while read -r pid module; do
  [[ -z "${pid:-}" ]] && continue
  if kill -0 "${pid}" 2>/dev/null; then
    log "stopping ${module} (pid=${pid})"
    kill "${pid}" 2>/dev/null || warn "failed to signal ${module} (pid=${pid})"
    stopped=$((stopped + 1))
  else
    warn "${module} (pid=${pid}) not running — skipping"
  fi
done < "${PIDS_FILE}"

# Give them a moment, then force-kill any survivors.
if [[ "${stopped}" -gt 0 ]]; then
  for _ in 1 2 3 4 5; do
    if ! awk '{print $1}' "${PIDS_FILE}" | xargs -r -I{} kill -0 {} 2>/dev/null; then
      break
    fi
  done
  while read -r pid module; do
    [[ -z "${pid:-}" ]] && continue
    if kill -0 "${pid}" 2>/dev/null; then
      warn "force-killing ${module} (pid=${pid})"
      kill -9 "${pid}" 2>/dev/null || true
    fi
  done < "${PIDS_FILE}"
fi

rm -f "${PIDS_FILE}"
log "stop complete (${stopped} process(es) signalled); removed ${PIDS_FILE}"
