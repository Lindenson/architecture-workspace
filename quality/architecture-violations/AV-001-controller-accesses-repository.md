# AV-001: Controller accesses Repository directly

> EXAMPLE violation record. Placeholder location.

- **ID:** AV-001
- **Source:** ArchUnit `layeredArchitecture()` rule "Controllers may not access Repositories"
- **Violated constraint/ADR:** [layering constraint](../../architecture/constraints/layering.md) (rule 2)
- **Location:** `eu.transplat.product.payments.api.PaymentController` → `PaymentRepository` (EXAMPLE)
- **Detected:** 2026-05-30, build #1234 (example)
- **Severity:** High
- **Status:** open (example)
- **Related Debt / Jira:** PAY-240 (example)

## Description

`PaymentController` calls `PaymentRepository` directly, bypassing the service
layer. This violates the [layering constraint](../../architecture/constraints/layering.md):
the only allowed downward path is Controller → Service → Repository.

## Why it matters

- Business rules and transaction boundaries that belong in the Service are
  bypassed — risk of inconsistent payment state.
- Erodes the modular-monolith boundaries that [ADR-004](../../architecture/adr/ADR-004-modular-monolith-plus-services.md)
  depends on.

## Remediation

Introduce/route through `PaymentService`; remove the repository dependency from
the controller. Re-run the ArchUnit gate to confirm the rule passes.

> TODO: link the failing build log and the fixing PR once available.
