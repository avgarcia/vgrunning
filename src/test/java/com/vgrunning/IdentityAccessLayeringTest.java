package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
                            "..identityaccess.infrastructure..",
                            "jakarta.servlet..",
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
                            "..identityaccess.application..", "..identityaccess.infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructureInputMustNotUseOutputPorts =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.infrastructure.input..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..identityaccess.application.port.out..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule applicationServicesMayUseOnlyTransactionalSpring =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.security..",
                            "org.springframework.web..",
                            "org.springframework.http..",
                            "org.springframework.context..",
                            "org.springframework.boot..",
                            "org.springframework.jdbc..",
                            "org.springframework.session..",
                            "org.jooq..",
                            "org.vgrunning.generated..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule applicationMappersMustRemainPure =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.application.mapper..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.servlet..",
                            "org.jooq..",
                            "org.vgrunning.generated..",
                            "..identityaccess.infrastructure..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule applicationPortsAreTopLevelInterfaces =
            classes()
                    .that()
                    .resideInAnyPackage(
                            "..identityaccess.application.port.in..",
                            "..identityaccess.application.port.out..")
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule noObsoletePackagesRemain =
            classes()
                    .should()
                    .resideOutsideOfPackages(
                            "..identityaccess.adapter..",
                            "..identityaccess.application.model..",
                            "..identityaccess.application.usecase..",
                            "..runnerportal.adapter..")
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
