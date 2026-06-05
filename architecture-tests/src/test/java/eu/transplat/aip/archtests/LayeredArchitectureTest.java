package eu.transplat.aip.archtests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces the layering constraint from
 * {@code ../architecture/constraints/layering.md}:
 *
 * <ul>
 *   <li>Controller → Service → Repository is the only downward chain.</li>
 *   <li>Controllers MUST NOT access repositories directly (rule 2).</li>
 *   <li>API is not accessed by inner layers; inward-only dependencies (rule 6).</li>
 *   <li>No cyclic dependencies between context packages (rule 4).</li>
 * </ul>
 *
 * <p>TEMPLATE: point {@link #BASE} at the product code (system property
 * {@code archunit.base.package}, default {@code com.yourorg.product}) and remove
 * the {@link Disabled} annotation to activate in the product build.
 */
@Disabled("Template: point importPackages(...) at your product code and remove this annotation")
class LayeredArchitectureTest {

    /** Configurable product base package (see pom.xml surefire systemPropertyVariables). */
    private static final String BASE =
            System.getProperty("archunit.base.package", "com.yourorg.product");

    private static JavaClasses classes;

    @BeforeAll
    static void importProductClasses() {
        classes = new ClassFileImporter().importPackages(BASE);
    }

    @Test
    void controllers_must_not_access_repositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..persistence..")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .because("Controllers must go through the service layer (layering.md rule 2)");
        rule.check(classes);
    }

    @Test
    void layers_are_strictly_respected() {
        ArchRule rule = layeredArchitecture().consideringAllDependencies()
                .layer("Api").definedBy("..api..")
                .layer("Service").definedBy("..service..")
                .layer("Persistence").definedBy("..persistence..")
                // API is the topmost layer — nothing may depend on it (rule 6).
                .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                // Services are reached only from the API layer.
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Api")
                // Persistence is reached only from services (controllers never directly).
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
                .because("Controller -> Service -> Repository is the only allowed chain (layering.md)");
        rule.check(classes);
    }

    @Test
    void bounded_context_packages_must_be_free_of_cycles() {
        // Each top-level package directly under BASE is treated as a context slice.
        ArchRule rule = slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .because("No cyclic dependencies between modules/contexts (layering.md rule 4)");
        rule.check(classes);
    }
}
