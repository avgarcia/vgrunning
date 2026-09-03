package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Protege exclusivamente los nombres de paquetes y la naturaleza de los puertos definidos por
 * ADR-0026.
 *
 * <p>Las dependencias entre capas, límites de módulos y fugas de OpenAPI o jOOQ se comprueban en
 * {@link IdentityAccessLayeringTest}, {@link OpenApiBoundaryTest}, {@link JooqBoundaryTest} y
 * {@link ApplicationModularityTest}.
 */
@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalPackageRootsTest {

    @ArchTest
    static final ArchRule noObsoleteHexagonalRootsRemain =
            classes()
                    .should()
                    .resideOutsideOfPackages(
                            "..adapter..", "..application.model..", "..application.usecase..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule applicationPortsAreInterfaces =
            classes()
                    .that()
                    .resideInAnyPackage("..application.port.in..", "..application.port.out..")
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(false);
}
