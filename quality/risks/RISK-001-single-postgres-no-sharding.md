# RISK-001: Single PostgreSQL instance, no horizontal sharding

> EXAMPLE risk record.

- **ID:** RISK-001
- **Likelihood:** Medium
- **Impact:** High
- **Exposure:** Medium × High — watch as volume grows
- **Owner:** Lead Architect / SRE
- **Status:** accepted (watched)
- **Related ADR:** [ADR-001](../../architecture/adr/ADR-001-postgresql.md)
- **Related Debt:** —

## Description

Per [ADR-001](../../architecture/adr/ADR-001-postgresql.md), the system of record
is a single PostgreSQL instance with **no horizontal sharding**. If payment and
document-metadata volume grows beyond what one instance + read replicas can
serve, we hit a write-throughput / storage ceiling that is expensive to remove
late.

## Mitigation

- **Current:** read replicas for read-heavy paths; per-context schemas to enable
  later splitting; capacity headroom monitored.
- **Planned:** partitioning of high-volume tables; offload reporting to read
  models off Kafka events ([target-state T4/T5](../../architecture/target-architecture/target-state.md)).
- **Trigger to revisit:** sustained write utilization > 60% or storage growth
  outpacing forecast (TODO: set concrete thresholds with SRE).

## Notes

Sharding / distributed SQL was deliberately deferred (team size, complexity).
Re-open ADR-001 with a superseding ADR if the trigger fires.
