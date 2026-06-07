#!/usr/bin/env bash
# ============================================================================
#  AIP doctor — preflight self-diagnosis for a fresh checkout.
#  Verifies the governance contract, MCP wiring, secrets, and built jars are
#  all present. Run from anywhere inside the repo:
#      ./automation/doctor.sh
#  Exit code 0 = ready; 1 = something is missing (details printed).
# ============================================================================
set -uo pipefail

# Resolve repo root (this script lives in <root>/automation/).
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

red()   { printf '\033[31m%s\033[0m\n' "$*"; }
grn()   { printf '\033[32m%s\033[0m\n' "$*"; }
ylw()   { printf '\033[33m%s\033[0m\n' "$*"; }
fail=0

check_file() {
  if [ -f "$1" ]; then grn "  ✓ $1"; else red "  ✗ MISSING: $1"; fail=1; fi
}

echo "AIP doctor — checking checkout at: $ROOT"
echo
echo "1) Governance contract (.claude/ — a HIDDEN directory):"
for f in \
  .claude/ROLE_ARCHITECT_AGENT.md \
  .claude/OPERATING_MODEL.md \
  .claude/AGENT_RUNTIME.md \
  .claude/MCP_ORCHESTRATION_MAP.md \
  .claude/BOOTSTRAP_ARCHITECT_AGENT.md \
  .claude/DOMAIN_INTELLIGENCE_BOOTSTRAP.md \
  .claude/MCP_SERVERS.md \
  .claude/settings.json \
  CLAUDE.md
do check_file "$f"; done

skills=$(find .claude/skills -name SKILL.md 2>/dev/null | wc -l | tr -d ' ')
agents=$(find .claude/agents -name '*.md' 2>/dev/null | wc -l | tr -d ' ')
[ "$skills" = "16" ] && grn "  ✓ skills: $skills/16" || { red "  ✗ skills: $skills/16"; fail=1; }
[ "$agents" = "7" ]  && grn "  ✓ subagents: $agents/7" || { red "  ✗ subagents: $agents/7"; fail=1; }

echo
echo "2) MCP wiring & templates (HIDDEN dot-files):"
for f in .mcp.json .env.example .gitignore docker-compose.yml; do check_file "$f"; done

echo
echo "3) Secrets:"
if [ -f .env ]; then grn "  ✓ .env present (gitignored)"; else
  ylw "  ! .env not found — copy it:  cp .env.example .env  (then edit)"; fi

echo
echo "4) Built MCP server jars (run: cd mcp-servers && mvn -DskipTests package):"
jars=0
for m in jira-mcp github-mcp sonar-mcp jqassistant-mcp structurizr-mcp rag-mcp wiki-mcp digital-twin-core; do
  if [ -f "mcp-servers/$m/target/$m.jar" ]; then jars=$((jars+1)); fi
done
if [ "$jars" -eq 8 ]; then grn "  ✓ all 8 server jars built"; else
  ylw "  ! $jars/8 jars built — run: (cd mcp-servers && mvn -DskipTests package)"; fi

echo
if [ "$fail" -eq 0 ]; then
  grn "READY: the governance contract and MCP wiring are all present."
  echo "Open this folder in Claude and ask:  SHOW PROJECT STATE"
else
  red "INCOMPLETE CHECKOUT — contract files are missing."
  echo
  echo "Almost always this means the code was obtained in a way that drops hidden"
  echo "dot-files/dot-dirs (a ZIP unpacked by a tool that hides them, or 'cp *')."
  echo "Fix it with a full git clone:"
  echo "    git clone https://github.com/Lindenson/architecture-workspace.git"
  exit 1
fi
