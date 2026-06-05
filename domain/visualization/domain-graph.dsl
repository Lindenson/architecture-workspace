/*
 * Paydocs Domain Graph (Structurizr DSL)  [EXAMPLE / STARTER TEMPLATE]
 *
 * A DOMAIN-level view: bounded contexts as a DDD context map, not runtime
 * containers (for those see ../../architecture/c4/workspace.dsl).
 *
 * SOURCE OF TRUTH: code + scan. Regenerate from the L1–L4 pipeline; keep in
 * sync with ../model/bounded-contexts.md. TODO: replace with generated output.
 */
workspace "Paydocs Domain" "Bounded-context map (EXAMPLE template)" {

    model {
        paydocs = softwareSystem "Paydocs Domain" "DDD context map" {
            payments      = container "Payments"          "Aggregate: Payment"            "bounded-context"
            ledger        = container "Ledger"            "Aggregate: LedgerAccount"      "bounded-context"
            identityKyc   = container "Identity / KYC"    "Aggregate: Customer"           "bounded-context"
            notifications = container "Notifications"     "Aggregate: Notification"       "bounded-context"
            ingestion     = container "DocumentIngestion" "Aggregate: Document"           "bounded-context"
            storage       = container "DocumentStorage"   "Aggregate: StoredObject"       "bounded-context"
        }

        // Context relationships (allowed flows — see model/bounded-contexts.md)
        payments      -> ledger        "Posts settlement entries (in-process, same tx)"
        payments      -> identityKyc   "Checks KYC status"
        payments      -> ingestion     "Links supporting documents (event/REST)"
        ingestion     -> storage       "Stores/retrieves originals (REST + pre-signed URL)"
        payments      -> notifications "payment.settled event" "Kafka"
        ingestion     -> notifications "document.processed event" "Kafka"
        identityKyc   -> notifications "kyc.verified event" "Kafka"
    }

    views {
        container paydocs "DomainContextMap" {
            include *
            autolayout lr
            description "EXAMPLE bounded-context map of the Paydocs domain."
        }

        styles {
            element "bounded-context" { shape RoundedBox }
        }
        theme default
    }
}
