# github-mcp

Spring Boot **MCP server** that exposes Git hosting data (GitHub REST v3 or
GitLab API v4) to the AIP **digital twin**. Part of the `mcp-servers` Maven
reactor under `eu.transplat.aip`.

Every tool and endpoint returns the canonical `eu.transplat.aip.mcp.common.McpResponse`
(`data`, `status`, `source`, `confidence`, `message`, `producedAt`). The server
is resilient: tool methods never throw — upstream failures become `error` /
`stale` responses, and the server starts with placeholder credentials.

## Provider

Selected by `git.provider` (`github` | `gitlab`); a single `GitClient` bean is
wired accordingly (`GitHubClient` or `GitLabClient`). Repos are addressed as
`owner/name` (GitHub) or a project path / numeric id (GitLab).

## MCP tools

| Tool | Description |
|------|-------------|
| `readRepository(repo)` | Repo metadata: name, default branch, language, visibility, last push. |
| `repoSnapshot(repo)` | `RepoSnapshot`: default branch, last commit, branch/tag counts, open PR count. |
| `readCommits(repo, branch?, limit?)` | Recent commits (default 20, max 100). |
| `readBranches(repo)` | Branch list. |
| `readTags(repo)` | Tag list. |
| `searchPullRequests(repo, state?)` | PRs/MRs by state (`open`/`closed`/`merged`/`all`). |
| `analyzePullRequest(repo, number)` | `PRInsight`: title, author, state, changed files, additions/deletions, touched top-level dirs. |
| `extractChangesets(repo, baseRef, headRef)` | Compare `base..head`: changed files + commit messages. |
| `getState()` | Aggregate `CodeState` across all configured repos: snapshots, total open PRs, most recently active repo. The digital-twin contract. |

## REST API (mirrors the tools)

- `GET /api/github/state`
- `GET /api/github/repo/{owner}/{name}`
- `GET /api/github/repo/{owner}/{name}/snapshot`
- `GET /api/github/repo/{owner}/{name}/commits?branch=&limit=`
- `GET /api/github/pr/{owner}/{name}/{number}`

MCP SSE endpoint: `/mcp/messages`. Actuator: `/actuator/health`, `/actuator/info`.

## Environment variables

| Var | Default | Purpose |
|-----|---------|---------|
| `GIT_PROVIDER` | `github` | `github` or `gitlab`. |
| `GITHUB_API_URL` | `https://api.github.com` | GitHub REST base URL (set for GHE). |
| `GITHUB_TOKEN` | _(empty)_ | GitHub PAT (bearer). |
| `GITHUB_ORG` | _(empty)_ | Default owner for bare repo names. |
| `GITLAB_API_URL` | _(empty)_ | GitLab base URL (e.g. `https://gitlab.com`). |
| `GITLAB_TOKEN` | _(empty)_ | GitLab access token. |
| `GIT_REPOSITORIES` | _(empty)_ | Comma-separated repos tracked by `getState()`. |
| `AIP_INTERNAL_TOKEN` | _(empty)_ | Shared internal bearer; blank disables auth (dev). |
| `AIP_CONFIG_DIR` | `./config` | Dir for `github.config.yml` overrides. |

Secrets come from config only. Server port: **8082**.

## Run

```bash
java -jar target/github-mcp.jar
```
