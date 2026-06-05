/*
 * Paydocs Platform — C4 model (Structurizr DSL)  [EXAMPLE / STARTER TEMPLATE]
 *
 * SOURCE OF TRUTH: the code. This model should be REGENERATED from the codebase
 * via jQAssistant scan -> Structurizr export, then validated against ArchUnit.
 * Hand edits here are a stop-gap; on conflict, the code wins (see ../../CLAUDE.md).
 * See README.md in this directory for the generation/validation flow.
 *
 * TODO: replace example containers/relationships with generated facts once the
 *       jQAssistant -> Structurizr pipeline is wired.
 */
workspace "Paydocs Platform" "Payments + document-processing platform (EXAMPLE template)" {

    model {
        // --- People ---
        customer   = person "Customer" "Initiates payments and uploads supporting documents."
        operator   = person "Operations Operator" "Monitors payments and document processing."
        compliance = person "Compliance Officer" "Reviews KYC, audit trails and flagged payments."

        // --- Software system ---
        paydocs = softwareSystem "Paydocs Platform" "Accepts payments, settles to a ledger, and ingests/stores documents." {

            apiGateway = container "API Gateway" "Routes external traffic, authn/z, rate limiting." "Spring Cloud Gateway (example)" "gateway"

            // Modular monolith core (see ADR-004)
            core = container "Paydocs Core" "Modular monolith: Payments, Ledger, Identity/KYC, Notifications." "Java 21 / Spring Boot" {
                payments      = component "Payments" "Validates and orchestrates payments (idempotent)."
                ledger        = component "Ledger" "Double-entry, append-only ledger entries and balances."
                identityKyc   = component "Identity / KYC" "Customer identity and KYC verification gating."
                notifications = component "Notifications" "Reacts to domain events; sends customer/operator notifications."
            }

            docProcessing = container "Document Processing Service" "Async OCR / data extraction pipeline." "Java 21 / Spring Boot" "service"
            docStorage    = container "Document Storage Service" "Thin gateway to object storage; metadata index." "Java 21 / Spring Boot" "service"

            // Infrastructure containers
            db        = container "PostgreSQL" "System of record: payments, ledger, KYC, document metadata, pgvector." "PostgreSQL 16" "database"
            kafka     = container "Kafka" "Async event backbone between contexts/services (outbox)." "Apache Kafka" "messaging"
            objStore  = container "Object Storage" "Immutable document binaries." "S3-compatible" "storage"
        }

        // --- Relationships: people -> system ---
        customer   -> apiGateway "Initiates payments, uploads documents" "HTTPS/REST"
        operator   -> apiGateway "Monitors and operates" "HTTPS/REST"
        compliance -> apiGateway "Reviews KYC & audit" "HTTPS/REST"

        // --- Gateway -> containers ---
        apiGateway -> core          "Routes API calls" "REST/JSON"
        apiGateway -> docProcessing "Routes document API calls" "REST/JSON"
        apiGateway -> docStorage    "Pre-signed upload/download" "REST/JSON"

        // --- Core internal (in-process, see layering/integration constraints) ---
        payments -> ledger      "Posts settlement entries (same tx)"
        payments -> identityKyc "Checks KYC status before settlement"

        // --- Data + messaging ---
        core          -> db    "Reads/writes (per-context schema)" "JDBC"
        docProcessing -> db    "Reads/writes document metadata" "JDBC"
        docStorage    -> db    "Reads/writes object metadata" "JDBC"
        docStorage    -> objStore "Stores/retrieves binaries" "S3 API"

        core          -> kafka "Publishes domain events (outbox)" "Kafka"
        docProcessing -> kafka "Publishes/consumes document events" "Kafka"
        notifications -> kafka "Consumes domain events" "Kafka"
        docProcessing -> docStorage "Fetches originals for OCR" "REST + pre-signed URL"
    }

    views {
        systemContext paydocs "SystemContext" {
            include *
            autolayout lr
            description "EXAMPLE system context — Paydocs and its users."
        }

        container paydocs "Containers" {
            include *
            autolayout lr
            description "EXAMPLE container view — modular monolith core + document services + infra."
        }

        component core "CoreComponents" {
            include *
            autolayout lr
            description "EXAMPLE component view — bounded contexts inside the modular monolith."
        }

        styles {
            element "Person"    { shape Person }
            element "database"  { shape Cylinder }
            element "storage"   { shape Folder }
            element "messaging" { shape Pipe }
        }

        theme default
    }
}
