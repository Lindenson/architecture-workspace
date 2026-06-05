# Stakeholders (EXAMPLE / STARTER TEMPLATE)

> STATUS: starter template. Replace example names/handles with real people.
> This maps "who decides / who is consulted" for governance and ADR sign-off.

## Roles

| Role | Example owner | Responsibilities | Decides / approves |
|------|---------------|------------------|--------------------|
| Lead Architect | TODO (e.g. A. Architect) | Architecture vision, ADRs, constraints, drift control | Approves/supersedes ADRs, constraints, standards |
| Team Lead / EM | TODO | Delivery, capacity, technical line-management | Roadmap commitments, staffing |
| Product Owner | TODO | Backlog, scope, priorities | Scope, release content, acceptance |
| Backend Developers (~6) | TODO | Implement contexts, write tests, raise ADR drafts | Implementation details within constraints |
| QA / SDET | TODO | Test strategy, ArchUnit/integration gates | Test pyramid, quality gates |
| Security / Compliance Officer | TODO | PII/PCI, AML/KYC, audit, data retention | Security constraints, sign-off on PII flows |
| SRE / Platform | TODO | Environments, CI/CD, observability, on-call | Deploy process, infra changes |

## RACI quick reference (EXAMPLE)

| Activity | Lead Arch | Team Lead | PO | Dev | Sec/Comp | SRE |
|----------|:---------:|:---------:|:--:|:---:|:--------:|:---:|
| New ADR | A | C | C | R | C | C |
| Roadmap change | C | R | A | I | I | I |
| Security/PII flow | C | I | I | R | A | C |
| Production deploy | I | C | I | I | I | A/R |
| Tech-debt prioritization | A | R | C | R | C | I |

> R = Responsible, A = Accountable, C = Consulted, I = Informed.

## Notes

- ADR approval and constraint changes are **architect-approved only** (see
  [`../.claude/ROLE_ARCHITECT_AGENT.md`](../.claude/ROLE_ARCHITECT_AGENT.md)).
- TODO: add Slack/Teams channels and the Jira project lead.
