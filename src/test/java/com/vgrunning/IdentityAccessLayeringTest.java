package com.vgrunning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Fitness functions de las fronteras de identidad definidas por ADR-0015. */
@AnalyzeClasses(packages = "com.vgrunning.identityaccess", importOptions = ImportOption.DoNotIncludeTests.class)
class IdentityAccessLayeringTest {

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdaptersOrHttpSecurity =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..identityaccess.adapter..",
                            "jakarta.servlet..",
                            "org.springframework.security..",
                            "org.springframework.web..",
                            "org.jooq..",
                            "org.springframework.jdbc..")
                    .allowEmptyShould(false);

    @ArchTest
    static final ArchRule domainMustNotDependOnApplicationOrAdapters =
            noClasses()
                    .that()
                    .resideInAnyPackage("..identityaccess.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..identityaccess.application..", "..identityaccess.adapter..")
                    .allowEmptyShould(true);
}
