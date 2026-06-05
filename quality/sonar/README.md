# SonarQube Snapshots

> STATUS: data directory. This holds **SonarQube snapshots** pulled by the
> `sonar-mcp` server / nightly pipeline. SonarQube is level 4 in the
> [source-of-truth hierarchy](../../CLAUDE.md) (below code, scans, ArchUnit,
> Structurizr). A Quality Gate failure is an **escalate-immediately** trigger.

## What lives here

Periodic snapshots of the project's Sonar state, so the digital twin can reason
over history without live calls. Suggested layout:

```
sonar/
  YYYY-MM-DD-<project-key>.json     # one snapshot per pull
  latest.json -> most recent        # (symlink or copy)
```

## Snapshot format (EXAMPLE)

```json
{
  "projectKey": "paydocs-core",
  "snapshotDate": "2026-06-05",
  "qualityGate": "PASSED",
  "measures": {
    "bugs": 3,
    "vulnerabilities": 1,
    "codeSmells": 142,
    "coverage": 76.4,
    "duplicatedLinesDensity": 2.1,
    "newCodeCoverage": 81.0
  },
  "source": "sonar-mcp",
  "confidence": "HIGH"
}
```

## Usage

- Tech-debt and release-readiness reviews read the latest snapshot
  (see [release readiness](../../delivery/releases/README.md)).
- New Sonar findings can spawn [technical-debt](../technical-debt/README.md) records.

> TODO: wire the nightly pull and pin the exact Sonar project keys per module.
