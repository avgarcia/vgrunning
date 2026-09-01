package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Fitness functions de las fronteras de identidad definidas por ADR-0015. */
@AnalyzeClasses(
        packages = "com.vgrunning.identityaccess",
        importOptions = ImportOption.DoNotIncludeTests.class)
class IdentityAccessLayeringTest {

    @ArchTest
    static final ArchRule applicationMustNotDependOnInfrastructureOrHttpSecurity =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..identityaccess.adapter..",
                            "..identityaccess.infrastructure..",
                            "jakarta.servlet..",
                            "org.springframework..",
                            "org.jooq..",
                            "org.vgrunning.generated..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule domainMustNotDependOnApplicationOrOuterLayers =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..identityaccess.application..",
                            "..identityaccess.adapter..",
                            "..identityaccess.infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adaptersAndSpringSecurityMustUseInputPorts =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "..identityaccess.adapter..",
                            "..identityaccess.infrastructure.security..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..identityaccess.application.usecase..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule identityAccessMustNotUseSpringJdbcAsPersistenceAbstraction =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.jdbc.core..")
                    .allowEmptyShould(false);
}
