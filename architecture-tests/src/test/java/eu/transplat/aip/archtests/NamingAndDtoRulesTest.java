package eu.transplat.aip.archtests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Naming, DTO and injection conventions derived from
 * {@code ../architecture/constraints/layering.md} (rule 5),
 * {@code ../architecture/standards/} and {@code ../domain/constraints/}:
 *
 * <ul>
 *   <li>A {@code *Dto} must not reference a JPA {@code @Entity} (no persistence leak).</li>
 *   <li>Entities reside in the {@code ..domain..} package.</li>
 *   <li>Repositories are interfaces.</li>
 *   <li>No field injection — {@code @Autowired} on fields is forbidden
 *       (use constructor injection).</li>
 * </ul>
 *
 * <p>TEMPLATE: adjust the annotation FQNs / package names to the product, then
 * remove the {@link Disabled} annotation to activate.
 */
@Disabled("Template: point importPackages(...) at your product code and remove this annotation")
class NamingAndDtoRulesTest {

    private static final String BASE =
            System.getProperty("archunit.base.package", "com.yourorg.product");

    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";
    private static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";

    private static JavaClasses classes;

    @BeforeAll
    static void importProductClasses() {
        classes = new ClassFileImporter().importPackages(BASE);
    }

    @Test
    void dto_must_not_reference_entity() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Dto")
                .should().dependOnClassesThat().areAnnotatedWith(ENTITY_ANNOTATION)
                .because("DTOs must not leak JPA entities across the API boundary (layering.md rule 5)");
        rule.check(classes);
    }

    @Test
    void entities_reside_in_domain() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(ENTITY_ANNOTATION)
                .should().resideInAPackage("..domain..")
                .because("Entities (aggregates) live in the domain layer (domain-rules.adoc)");
        rule.check(classes);
    }

    @Test
    void repositories_are_interfaces() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces()
                .because("Repositories are abstractions; the implementation is provided by the framework");
        rule.check(classes);
    }

    @Test
    void no_field_injection() {
        ArchRule rule = noFields()
                .should().beAnnotatedWith(AUTOWIRED_ANNOTATION)
                .because("Field injection is forbidden — use constructor injection (api-standards.md)");
        rule.check(classes);
    }

    @Test
    void domain_must_not_depend_on_spring() {
        // Keep the domain layer framework-agnostic (template — adjust as needed).
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                .should().notBeAnnotatedWith(AUTOWIRED_ANNOTATION)
                .because("The domain layer must stay free of Spring wiring (clean domain model)");
        rule.check(classes);
    }
}
