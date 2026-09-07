package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Mantiene los tipos generados desde OpenAPI dentro de las entradas web de infraestructura. */
@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class OpenApiBoundaryTest {

    @ArchTest
    static final ArchRule openApiTypesMustStayInsideWebInputs =
            noClasses()
                    .that()
                    .resideOutsideOfPackages("..infrastructure.input.web..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.vgrunning.generated.openapi..")
                    .allowEmptyShould(true);
}
