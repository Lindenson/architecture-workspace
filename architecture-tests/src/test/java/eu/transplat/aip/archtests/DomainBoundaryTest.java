package eu.transplat.aip.archtests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Bounded-context boundary rules — the idea captured in
 * {@code ../domain/constraints/archunit-domain-rules.java} and
 * {@code ../architecture/constraints/integration.md}:
 *
 * <ul>
 *   <li>Bounded-context packages must not depend on each other's internals;
 *       cross-context interaction goes via the other context's {@code api} /
 *       {@code service} surface (or async events).</li>
 *   <li>No cross-context access to another context's {@code persistence} package
 *       (layering.md rule 3).</li>
 *   <li>Context slices must be free of cycles.</li>
 * </ul>
 *
 * <p>TEMPLATE: replace the example context names ({@code payments}, {@code ledger})
 * with the product's real bounded contexts and remove {@link Disabled}. The
 * generic {@code persistence}-isolation and cycle rules apply unchanged.
 */
@Disabled("Template: point importPackages(...) at your product code and remove this annotation")
class DomainBoundaryTest {

    private static final String BASE =
            System.getProperty("archunit.base.package", "com.yourorg.product");

    private static JavaClasses classes;

    @BeforeAll
    static void importProductClasses() {
        classes = new ClassFileImporter().importPackages(BASE);
    }

    /**
     * No context may reach into another context's persistence package. This
     * generic rule holds regardless of context names: anything in {@code ..api..}
     * or {@code ..service..} of one context must not touch {@code ..persistence..}
     * — that is only reachable within the SAME context's service layer, which the
     * layered rule in {@link LayeredArchitectureTest} already constrains.
     */
    @Test
    void contexts_must_not_access_other_context_persistence() {
        // Example pairing — replace with the product's real contexts.
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".payments..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".ledger.persistence..")
                .because("Cross-context DB access is forbidden; use service/events (integration.md)");
        rule.check(classes);
    }

    /**
     * Contexts interact only through each other's public surface ({@code api} /
     * {@code service}), never their {@code domain} internals. Expressed as: no
     * class outside a context may depend on that context's {@code domain} package.
     *
     * <p>TEMPLATE: this generic formulation guards the {@code domain} internals of
     * every context. The {@code persistence} internals are already guarded by
     * {@link #contexts_must_not_access_other_context_persistence} and by the
     * layered rule in {@link LayeredArchitectureTest}. Replace with explicit
     * per-context-pair rules if you need finer control.</p>
     */
    @Test
    void other_contexts_must_not_reach_into_a_contexts_domain() {
        // Example: nothing in 'payments' may depend on 'ledger' internals (domain).
        // Cross-context calls must target ledger's api/service surface instead.
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".payments..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".ledger.domain..")
                .because("Contexts interact only via api/service, not domain internals (integration.md)");
        rule.check(classes);
    }

    @Test
    void context_slices_are_cycle_free() {
        ArchRule rule = slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .because("Bounded contexts must form a DAG (no cyclic coupling)");
        rule.check(classes);
    }
}
