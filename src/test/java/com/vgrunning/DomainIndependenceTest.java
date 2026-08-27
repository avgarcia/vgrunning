package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Protege la independencia del futuro dominio frente a frameworks y adaptadores de infraestructura.
 *
 * <p>El catálogo de reglas de arquitectura transversal se ampliará en RC-7 cuando existan capas y
 * adaptadores reales que validar. Esta regla ya es efectiva para cualquier paquete {@code domain}
 * que se incorpore.
 */
@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainIndependenceTest {

    /** Impide que el dominio dependa de Spring, OpenAPI, jOOQ o JDBC. */
    @ArchTest
    static final ArchRule domainMustNotDependOnFrameworksOrAdapters =
            noClasses()
                    .that()
                    .resideInAnyPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "org.jooq..",
                            "java.sql..",
                            "javax.sql..",
                            "io.swagger.v3..",
                            "org.openapitools..")
                    .allowEmptyShould(true);
}
