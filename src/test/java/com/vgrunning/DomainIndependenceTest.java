package com.vgrunning;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.vgrunning", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainIndependenceTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnFrameworksOrAdapters = noClasses()
        .that().resideInAnyPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..",
            "org.jooq..",
            "java.sql..",
            "javax.sql..",
            "io.swagger.v3..",
            "org.openapitools.."
        )
        .allowEmptyShould(true);
}
