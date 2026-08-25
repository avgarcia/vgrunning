import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import nu.studer.gradle.jooq.JooqEdition

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("nu.studer.jooq") version "10.2.1"
}

group = "com.vgrunning"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

dependencyLocking {
    lockAllConfigurations()
}

configurations.configureEach {
    exclude(group = "org.mockito", module = "mockito-core")
    exclude(group = "org.mockito", module = "mockito-junit-jupiter")
}

val postgresImage =
    "postgres@sha256:1957b2ff3137e4ef7f3bc813e74fff50b1e1ffddc85c8b9d6f14ade972be8687"
val generatedJooqDirectory = layout.buildDirectory.dir("generated/sources/jooq/main")

val codegen = sourceSets.create("codegen") {
    java.srcDir("src/codegen/java")
    resources.srcDir("src/main/resources")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(platform("org.springframework.modulith:spring-modulith-bom:2.1.0"))
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-restclient")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    "codegenImplementation"("org.flywaydb:flyway-core")
    "codegenImplementation"("org.flywaydb:flyway-database-postgresql")
    "codegenImplementation"("org.jooq:jooq-codegen:3.21.7")
    "codegenImplementation"("org.postgresql:postgresql")
    "codegenImplementation"("org.testcontainers:testcontainers-postgresql")
    "codegenRuntimeOnly"("org.slf4j:slf4j-simple")
}

jooq {
    version.set("3.21.7")
    edition.set(JooqEdition.OSS)
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.generator.target
                .withPackageName("org.vgrunning.generated.jooq")
                .withDirectory(generatedJooqDirectory.get().asFile.absolutePath)
        }
    }
}

val generateJooqFromPostgres = tasks.register<JavaExec>("generateJooqFromPostgres") {
    group = "jooq"
    description = "Genera los tipos jOOQ desde un PostgreSQL 18 efímero y migrado con Flyway."
    dependsOn(tasks.named(codegen.classesTaskName))
    mainClass.set("org.vgrunning.codegen.JooqCodeGenerator")
    classpath = codegen.runtimeClasspath
    args(generatedJooqDirectory.get().asFile.absolutePath)
    inputs.files(fileTree("src/main/resources/db/migration") { include("**/*.sql") })
    inputs.property("postgresImage", postgresImage)
    outputs.dir(generatedJooqDirectory)
}

tasks.named("compileJava") {
    dependsOn(generateJooqFromPostgres)
}

tasks.named("generateJooq") {
    enabled = false
    description = "Deshabilitada: usa generateJooqFromPostgres para generar desde PostgreSQL migrado."
}

val javaToolchains = extensions.getByType<JavaToolchainService>()
val temurin25Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}

tasks.register("verifyJavaToolchain") {
    group = "verification"
    description = "Comprueba que el build usa Java 25 de Eclipse Adoptium."
    inputs.property("toolchainLanguageVersion", 25)
    inputs.property("toolchainVendor", "Eclipse Adoptium")

    doLast {
        val metadata = temurin25Launcher.get().metadata
        check(metadata.languageVersion.asInt() == 25) {
            "Se esperaba Java 25 y se resolvió Java ${metadata.languageVersion}."
        }
        check(JvmVendorSpec.ADOPTIUM.matches(metadata.vendor)) {
            "Se esperaba Eclipse Adoptium y se resolvió ${metadata.vendor}."
        }
        logger.lifecycle(
            "Java toolchain verificado: ${metadata.vendor} ${metadata.javaRuntimeVersion} (${metadata.installationPath})",
        )
    }
}

tasks.named("check") {
    dependsOn("verifyJavaToolchain", "verifyRuntimeStack")
}

// Este control inspecciona los artefactos resueltos, incluso si nadie usa aún sus APIs.
// forbidden-apis analiza referencias de bytecode y sería complementario, no un sustituto de esta política.
// jOOQ 3.21 carga r2dbc-spi al construir DefaultConfiguration incluso en modo JDBC; se permite solo ese SPI pasivo.
val forbiddenRuntimeModules = setOf(
    "org.springframework:spring-webflux",
    "org.springframework.boot:spring-boot-starter-webflux",
    "org.springframework.boot:spring-boot-r2dbc",
    "org.springframework.boot:spring-boot-starter-data-r2dbc",
    "org.springframework.data:spring-data-r2dbc",
    "org.springframework:spring-r2dbc",
    "io.r2dbc:r2dbc-pool",
    "org.postgresql:r2dbc-postgresql",
    "io.asyncer:r2dbc-mysql",
    "org.mariadb:r2dbc-mariadb",
    "org.springframework.boot:spring-boot-jpa",
    "org.springframework.boot:spring-boot-starter-data-jpa",
    "org.hibernate.orm:hibernate-core",
    "org.springframework.boot:spring-boot-starter-data-jdbc",
    "org.springframework.data:spring-data-jdbc",
    "com.h2database:h2",
    "org.xerial:sqlite-jdbc",
    "org.apache.derby:derby",
    "org.hsqldb:hsqldb",
)

tasks.register("verifyRuntimeStack") {
    group = "verification"
    description = "Rechaza dependencias no permitidas en el runtime imperativo."

    doLast {
        val checkedConfigurations = listOf(configurations.runtimeClasspath.get(), configurations.testRuntimeClasspath.get())
        val runtimeModules = checkedConfigurations
            .flatMap { configuration ->
                configuration.incoming.resolutionResult.allComponents.mapNotNull { component ->
                    component.moduleVersion?.let { module -> "${module.group}:${module.name}" }
                }
            }
            .toSet()
        val forbiddenFound = runtimeModules.intersect(forbiddenRuntimeModules)
        val unexpectedR2dbc = runtimeModules
            .filter { module -> "r2dbc" in module && module != "io.r2dbc:r2dbc-spi" }
            .toSet()

        check(forbiddenFound.isEmpty() && unexpectedR2dbc.isEmpty()) {
            val violations = (forbiddenFound + unexpectedR2dbc).sorted().joinToString()
            "El runtime contiene dependencias no permitidas: $violations."
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
