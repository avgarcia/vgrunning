package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Impide que los tipos jOOQ escapen de los futuros adaptadores de persistencia propietarios. */
@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class JooqBoundaryTest {

    @ArchTest
    static final ArchRule jooqMustStayInsidePersistenceAdapters =
            noClasses()
                    .that()
                    .resideOutsideOfPackages("..adapter.persistence..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.jooq..")
                    .allowEmptyShould(true);
}
