# Architecture Risk Register

> STATUS: governance register. Risks are accepted/mitigated with architect
> sign-off. An agent may draft and surface risks (see [`../../CLAUDE.md`](../../CLAUDE.md)).

## Risk record format

Each risk is `RISK-NNN-short-title.md`:

| Field | Meaning |
|-------|---------|
| **ID** | `RISK-NNN` |
| **Description** | The risk |
| **Likelihood** | Low / Medium / High |
| **Impact** | Low / Medium / High / Critical |
| **Exposure** | Likelihood × Impact summary |
| **Mitigation** | Current + planned mitigation |
| **Owner** | Who watches it |
| **Status** | open / mitigating / accepted / closed |
| **Related ADR / Debt** | links |

## Register (EXAMPLE)

| ID | Title | Likelihood | Impact | Status |
|----|-------|-----------|--------|--------|
| [RISK-001](RISK-001-single-postgres-no-sharding.md) | Single Postgres, no sharding | Medium | High | accepted (watched) |

> TODO: review the register at the weekly architecture cadence.
