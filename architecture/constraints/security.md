# Constraint: Security, PII & PCI handling

> STATUS: enforced constraint. Owner: Security/Compliance Officer + Lead Architect
> (see [stakeholders](../../knowledge/stakeholders.md)). Critical security issues
> are an **escalate-immediately** trigger (see [`../../CLAUDE.md`](../../CLAUDE.md)).

## Data classification & handling

- Classify data as **Public / Internal / Restricted (PII) / Restricted (PCI)**.
  Payments card-like data and customer KYC evidence are **Restricted**.
- Real Restricted data lives only in prod (masked in staging) — see
  [environments](../../knowledge/environments.md).

## Secrets

1. **No secrets in code, config committed to Git, or logs.** Ever.
2. Secrets come from a **vault / environment variables** per environment
   (TODO: name the backend — e.g. HashiCorp Vault).
3. Rotate credentials; no shared long-lived static keys where avoidable.

## Encryption

- **In transit:** TLS for all external and inter-service traffic.
- **At rest:** database and object-store encryption enabled
  (documents — see [ADR-005](../adr/ADR-005-document-storage-s3.md)).

## PII / PCI in code

- Never log PII/PCI or full payment instruments. Mask/tokenize in logs and traces.
- DTOs crossing the API boundary must not leak internal identifiers or
  Restricted fields beyond what the consumer is authorized to see.
- KYC gating: regulated flows require a verified customer (see
  [domain-rules](../../domain/model/domain-rules.md)).

## Audit logging

- Every state change to a Payment, LedgerEntry or Document MUST produce an
  **immutable audit record** (who, what, when, correlation id).
- Audit logs are append-only and retained per the compliance retention policy
  (TODO: confirm period).

## Enforcement

| Rule | Enforced by |
|------|-------------|
| No secrets in code | Secret-scanning in CI (TODO: tool), Sonar security rules |
| No PII in logs | Code review + Sonar/Semgrep rules (TODO) |
| TLS / encryption | Platform config + review |

> TODO: link the threat model and the data-retention policy once written.
