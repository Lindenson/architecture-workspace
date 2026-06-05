# Delivery Metrics

> STATUS: starter. Tracks delivery health metrics for the digital twin. Metrics
> are derived facts (cite source + confidence per [`../../CLAUDE.md`](../../CLAUDE.md)).

## Metrics tracked (EXAMPLE)

| Metric | Definition | Source | Target (EXAMPLE) |
|--------|-----------|--------|------------------|
| **Lead time for change** | Commit → production | Git + deploy logs | < 5 days |
| **Deployment frequency** | Releases per period | Release records / CI | weekly+ |
| **Velocity** | Story points per sprint | Jira | stable trend |
| **Change failure rate** | % deploys causing incident/rollback | CI + [incidents](../../history/incidents/README.md) | < 15% |
| **MTTR** | Mean time to restore after incident | [incidents](../../history/incidents/README.md) | < 4 h |
| **Escaped defects** | Defects found in prod per release | Jira | downward trend |

(The first four are the **DORA** metrics.)

## Notes

- Metrics inform release-readiness and weekly review, not individual performance.
- Snapshots can be stored alongside [reports](../../reports/).

> TODO: wire the metric sources and set agreed targets with the team.
