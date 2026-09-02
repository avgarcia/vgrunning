package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Impide que reaparezcan raíces físicas que contradicen ADR-0026. */
@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalPackagingTest {

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
