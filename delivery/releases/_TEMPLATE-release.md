# Release <version> — <name/date>

> TEMPLATE — copy to `YYYY.MAJOR.MINOR.md` and fill in.

- **Version:** e.g. 2026.7.0
- **Date / target date:** YYYY-MM-DD
- **Release manager:** TODO
- **Readiness verdict:** READY | READY WITH RISKS | NOT READY

## Scope

- Epics: PAY-NNN, PAY-NNN (see [epics](../epics/README.md))
- Notable changes / migrations (Flyway): …
- New/affected bounded contexts: …

## Risks

| Risk | Severity | Mitigation | Link |
|------|----------|------------|------|
| … | … | … | RISK-NNN / TD-NNN |

## Readiness checklist

- [ ] No open Critical defects
- [ ] No High/Critical [architecture violations](../../quality/architecture-violations/README.md)
- [ ] [Sonar](../../quality/sonar/README.md) Quality Gate PASSED
- [ ] Blocking [tech debt](../../quality/technical-debt/README.md) resolved or accepted
- [ ] All scoped epics architecturally covered ([epics](../epics/README.md))
- [ ] Validated on [staging](../../knowledge/environments.md)
- [ ] DB migrations reviewed + reversible plan
- [ ] Release notes drafted

## Evidence

Cite sources used for the verdict (Sonar snapshot, Jira query, scan).
