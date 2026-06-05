/*
 * Domain ArchUnit rules (L5 — Governance)  [EXAMPLE / STARTER TEMPLATE]
 *
 * STATUS: template. These rules enforce aggregate + bounded-context boundaries
 * and persistence/API separation derived from domain/model/domain-rules.md and
 * domain/constraints/domain-rules.adoc.
 *
 * This file is a SNIPPET to be copied into the product's `architecture-tests`
 * module (see quality/archunit/README.md). It will NOT compile standalone here —
 * it documents the intended rules. TODO: adapt package names to the real product
 * and wire into the build so the ArchUnit gate fails on violation.
 *
 * Assumed package layout (EXAMPLE):
 *   eu.transplat.product.<context>.api          // controllers, DTOs
 *   eu.transplat.product.<context>.service      // application/domain services
 *   eu.transplat.product.<context>.domain       // aggregates, entities, value objects
 *   eu.transplat.product.<context>.persistence  // JPA entities, repositories
 */
package eu.transplat.product.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "eu.transplat.product")
class DomainArchitectureRules {

    private static final String BASE = "eu.transplat.product";

    // Controllers must NOT access repositories directly (layering constraint rule 2).
    @ArchTest
    static final ArchRule controllers_must_not_use_repositories =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..")
            .because("Controllers must go through the service layer (layering.md)");

    // Layered architecture: api -> service -> persistence only.
    @ArchTest
    static final ArchRule layered =
        layeredArchitecture().consideringAllDependencies()
            .layer("Api").definedBy("..api..")
            .layer("Service").definedBy("..service..")
            .layer("Persistence").definedBy("..persistence..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Api")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service");

    // A DTO must not reference a persistence Entity (no persistence leak across API).
    @ArchTest
    static final ArchRule dto_must_not_reference_entity =
        noClasses().that().haveSimpleNameEndingWith("Dto")
            .should().dependOnClassesThat().areAnnotatedWith("jakarta.persistence.Entity")
            .because("DTOs must not leak JPA entities (domain-rules.adoc)");

    // Controllers must not expose entities.
    @ArchTest
    static final ArchRule controllers_must_not_return_entities =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().areAnnotatedWith("jakarta.persistence.Entity")
            .because("Entities must not cross the API boundary (persistence-standards.md)");

    // Bounded contexts must be free of cycles (each top-level package is a context slice).
    @ArchTest
    static final ArchRule contexts_should_be_cycle_free =
        slices().matching(BASE + ".(*)..")
            .should().beFreeOfCycles();

    // No cross-context access to another context's persistence package.
    // TODO: generate one rule per context pair, or use a slices() isolation rule.
    @ArchTest
    static final ArchRule no_cross_context_persistence_access =
        noClasses().that().resideInAPackage(BASE + ".payments..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".ledger.persistence..")
            .because("Cross-context DB access is forbidden; use service/events (integration.md)");

    // Example of running rules programmatically (if not using @AnalyzeClasses).
    static void example() {
        JavaClasses classes = new ClassFileImporter().importPackages(BASE);
        controllers_must_not_use_repositories.check(classes);
    }
}
