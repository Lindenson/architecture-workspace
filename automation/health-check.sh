#!/usr/bin/env bash
# ============================================================================
#  health-check.sh — probe the MVP-1 MCP servers' actuator health
# ----------------------------------------------------------------------------
#  Curls /actuator/health on ports 8080-8083 and prints an UP/DOWN table.
#  Degrades gracefully if curl is missing. Exit code is non-zero if any DOWN.
# ============================================================================
set -euo pipefail

# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
aip_load_env

if ! require_cmd curl; then
  err "curl not found — cannot run health checks"
  exit 1
fi

# label:port
TARGETS=(
  "digital-twin-core:${PORT_DIGITAL_TWIN:-8080}"
  "jira-mcp:${PORT_JIRA_MCP:-8081}"
  "github-mcp:${PORT_GITHUB_MCP:-8082}"
  "sonar-mcp:${PORT_SONAR_MCP:-8083}"
)

printf '%-20s %-6s %-8s %s\n' "SERVER" "PORT" "STATUS" "HEALTH"
printf '%-20s %-6s %-8s %s\n' "------" "----" "------" "------"

any_down=0
for entry in "${TARGETS[@]}"; do
  label="${entry%%:*}"
  port="${entry##*:}"
  url="http://localhost:${port}/actuator/health"
  body="$(curl -sS --connect-timeout 3 --max-time 10 \
            -H "Authorization: Bearer ${AIP_INTERNAL_TOKEN:-}" \
            "${url}" 2>/dev/null || true)"
  if [[ -n "${body}" ]] && printf '%s' "${body}" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    status="UP"
    health="$(printf '%s' "${body}" | tr -d ' \n' | head -c 80)"
  else
    status="DOWN"
    health="${body:-no response}"
    any_down=1
  fi
  printf '%-20s %-6s %-8s %s\n' "${label}" "${port}" "${status}" "${health}"
done

exit "${any_down}"
