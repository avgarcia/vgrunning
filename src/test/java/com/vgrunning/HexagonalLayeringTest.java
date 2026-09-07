package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;

/** Fitness functions de la arquitectura hexagonal para todos los módulos de negocio. */
@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalLayeringTest {

    @ArchTest
    static final ArchRule applicationMustNotDependOnInfrastructureOrHttpSecurity =
            noClasses()
                    .that()
                    .resideInAnyPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure..",
                            "jakarta.servlet..",
                            "org.jooq..",
                            "org.vgrunning.generated..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule domainMustNotDependOnApplicationOrInfrastructure =
            noClasses()
                    .that()
                    .resideInAnyPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructureInputMustNotUseOutputPorts =
            noClasses()
                    .that()
                    .resideInAnyPackage("..infrastructure.input..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application.port.out..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule infrastructureOutputMustNotUseInputPorts =
            noClasses()
                    .that()
                    .resideInAnyPackage("..infrastructure.output..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application.port.in..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule applicationMayNotDependOnFrameworksOrPersistence =
            noClasses()
                    .that()
                    .resideInAnyPackage("..application..")
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
                    .resideInAnyPackage("..application.mapper..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.servlet..",
                            "org.jooq..",
                            "org.vgrunning.generated..",
                            "..infrastructure..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule applicationPortsAreTopLevelInterfaces =
            classes()
                    .that()
                    .resideInAnyPackage("..application.port.in..", "..application.port.out..")
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule transactionalBoundariesAreApplicationServices =
            methods()
                    .that()
                    .areAnnotatedWith(Transactional.class)
                    .should()
                    .beDeclaredInClassesThat()
                    .resideInAnyPackage("..application.service..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule noObsoletePackagesRemain =
            classes()
                    .should()
                    .resideOutsideOfPackages(
                            "..adapter..", "..application.model..", "..application.usecase..")
                    .allowEmptyShould(false);
}
