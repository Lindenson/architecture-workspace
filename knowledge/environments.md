# Environments (EXAMPLE / STARTER TEMPLATE)

> STATUS: starter template. Replace URLs, owners and data-classification with
> real values. Security constraints apply per
> [`../architecture/constraints/security.md`](../architecture/constraints/security.md).

## Environment matrix

| Env | Purpose | Data classification | Data source | Who deploys | Notes |
|-----|---------|---------------------|-------------|-------------|-------|
| **dev** | Local + shared dev integration | Synthetic only | Generated fixtures | Any dev (CI on merge to `develop`) | No real PII. Ephemeral. |
| **test** | Automated test / QA verification | Synthetic + masked | Masked snapshot | CI pipeline | ArchUnit + integration gates run here. |
| **staging** | Pre-prod, release rehearsal, UAT | Masked production-like | Masked prod copy (TODO: confirm masking) | SRE via release pipeline | Mirrors prod topology. Release readiness checked here. |
| **prod** | Live customer traffic | **Restricted (PII + PCI)** | Real | SRE only, approved release | Full audit logging; secrets via vault. |

## Rules (EXAMPLE)

- Real PII/PCI data **only** in prod (and tightly masked in staging). Never in dev/test.
- Secrets are injected via vault/env per env; never committed. See security constraint.
- Promotion path: `dev → test → staging → prod`. No skipping; staging is mandatory
  for release readiness (see [`../delivery/releases/README.md`](../delivery/releases/README.md)).
- Each env has its own Kafka cluster/topic prefix and Postgres instance.

> TODO: add concrete hostnames, namespaces/clusters, and the secrets backend
> (e.g. HashiCorp Vault / cloud secrets manager).
