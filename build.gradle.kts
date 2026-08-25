import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import nu.studer.gradle.jooq.JooqEdition
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("nu.studer.jooq") version "10.2.1"
    id("org.openapi.generator") version "7.24.0"
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
val openApiSpec = layout.projectDirectory.file("api/openapi/running-coach.yaml")
val generatedOpenApiServerDirectory = layout.buildDirectory.dir("generated/openapi/server")
val generatedOpenApiClientDirectory = layout.buildDirectory.dir("generated/openapi/client/typescript")
val oasdiffImage =
    "tufin/oasdiff@sha256:6065c16a4c9ce12504752f444d4981091e58c2a35436fac90b649be47d833db3"
val npmExecutable = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"

val codegen = sourceSets.create("codegen") {
    java.srcDir("src/codegen/java")
    resources.srcDir("src/main/resources")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
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
    dependsOn(generateJooqFromPostgres, "generateOpenApiServer")
}

val validateOpenApi = tasks.register<ValidateTask>("validateOpenApi") {
    group = "verification"
    description = "Valida el contrato OpenAPI 3.1 que actúa como fuente de verdad."
    inputSpec.set(openApiSpec.asFile.toURI().toString())
}

val generateOpenApiServer = tasks.register<GenerateTask>("generateOpenApiServer") {
    group = "openapi"
    description = "Genera interfaces y modelos Spring MVC a partir del contrato OpenAPI."
    generatorName.set("spring")
    inputSpec.set(openApiSpec.asFile.toURI().toString())
    outputDir.set(generatedOpenApiServerDirectory.get().asFile.absolutePath)
    apiPackage.set("org.vgrunning.generated.openapi.server.api")
    modelPackage.set("org.vgrunning.generated.openapi.server.model")
    invokerPackage.set("org.vgrunning.generated.openapi.server")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "library" to "spring-boot",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "documentationProvider" to "none",
            "useSwaggerUI" to "false",
            "openApiNullable" to "false",
            "dateLibrary" to "java8",
        ),
    )
    globalProperties.set(
        mapOf(
            "apiDocs" to "false",
            "apiTests" to "false",
            "modelDocs" to "false",
            "modelTests" to "false",
        ),
    )
    doFirst {
        project.delete(generatedOpenApiServerDirectory.get().asFile)
    }
}

val generateOpenApiClient = tasks.register<GenerateTask>("generateOpenApiClient") {
    group = "openapi"
    description = "Genera el cliente TypeScript que consumirá la futura SPA React."
    generatorName.set("typescript-axios")
    inputSpec.set(openApiSpec.asFile.toURI().toString())
    outputDir.set(generatedOpenApiClientDirectory.get().asFile.absolutePath)
    packageName.set("@vgrunning/api-client")
    configOptions.set(
        mapOf(
            "supportsES6" to "true",
            "useSingleRequestParameter" to "true",
        ),
    )
    globalProperties.set(
        mapOf(
            "apiDocs" to "false",
            "apiTests" to "false",
            "modelDocs" to "false",
            "modelTests" to "false",
        ),
    )
    doFirst {
        project.delete(generatedOpenApiClientDirectory.get().asFile)
    }
}

sourceSets.named("main") {
    java.srcDir(generatedOpenApiServerDirectory.map { it.dir("src/main/java") })
}

val installFrontendDependencies = tasks.register<Exec>("installFrontendDependencies") {
    group = "frontend"
    description = "Instala el lockfile npm reproducible del frontend."
    workingDir(file("frontend"))
    commandLine(npmExecutable, "ci")
    inputs.files("frontend/package.json", "frontend/package-lock.json", "frontend/.npmrc")
    outputs.dir("frontend/node_modules")
}

val lintOpenApi = tasks.register<Exec>("lintOpenApi") {
    group = "verification"
    description = "Aplica las reglas Spectral de ADR-0017 al contrato OpenAPI."
    dependsOn(installFrontendDependencies)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "api:lint")
    inputs.files(
        openApiSpec,
        file("api/openapi/.spectral.yaml"),
        fileTree("api/openapi") { include("*.js") },
    )
}

val verifySpectralNegativeCases = tasks.register<Exec>("verifySpectralNegativeCases") {
    group = "verification"
    description = "Demuestra que las infracciones OpenAPI bloquean Spectral."
    dependsOn(installFrontendDependencies)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "api:verify-spectral")
    inputs.files(fileTree("api/openapi/test-fixtures/spectral") { include("**/*.yaml") })
}

val typecheckGeneratedOpenApiClient = tasks.register<Exec>("typecheckGeneratedOpenApiClient") {
    group = "verification"
    description = "Comprueba el cliente TypeScript generado desde OpenAPI."
    dependsOn(installFrontendDependencies, generateOpenApiClient)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "api:typecheck")
    inputs.files(openApiSpec, file("frontend/tsconfig.api.json"))
}

fun runOasdiff(arguments: List<String>): Int {
    return ProcessBuilder(listOf("docker", "run", "--rm") + arguments)
        .directory(projectDir)
        .inheritIO()
        .start()
        .waitFor()
}

val verifyOasdiffBreakingCase = tasks.register("verifyOasdiffBreakingCase") {
    group = "verification"
    description = "Demuestra que oasdiff rechaza un cambio incompatible del contrato."
    inputs.files(
        file("api/openapi/test-fixtures/oasdiff/base.yaml"),
        file("api/openapi/test-fixtures/oasdiff/breaking.yaml"),
    )
    inputs.property("oasdiffImage", oasdiffImage)

    doLast {
        val fixturesDirectory = file("api/openapi/test-fixtures/oasdiff").absoluteFile
        val mount = "type=bind,source=${fixturesDirectory.path.replace('\\', '/')},target=/workspace,readonly"
        val exitCode =
            runOasdiff(
                listOf(
                    "--mount",
                    mount,
                    oasdiffImage,
                    "breaking",
                    "--fail-on",
                    "ERR",
                    "/workspace/base.yaml",
                    "/workspace/breaking.yaml",
                ),
            )
        check(exitCode != 0) { "oasdiff aceptó un cambio OpenAPI incompatible de prueba." }
    }
}

val checkOpenApiCompatibility = tasks.register("checkOpenApiCompatibility") {
    group = "verification"
    description = "Compara el contrato actual con main mediante oasdiff."
    dependsOn(validateOpenApi)
    inputs.file(openApiSpec)
    inputs.property("oasdiffImage", oasdiffImage)

    doLast {
        val comparisonDirectory = layout.buildDirectory.dir("tmp/oasdiff/main").get().asFile
        comparisonDirectory.mkdirs()
        val mainSpec = comparisonDirectory.resolve("main.yaml")
        val gitExitCode =
            ProcessBuilder("git", "show", "main:api/openapi/running-coach.yaml")
                .directory(projectDir)
                .redirectOutput(mainSpec)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor()
        if (gitExitCode == 0) {
            logger.lifecycle("Se compara el contrato actual contra main mediante oasdiff.")
        } else {
            logger.lifecycle("main todavía no contiene contrato OpenAPI; se establece esta revisión como línea base.")
            mainSpec.writeBytes(openApiSpec.asFile.readBytes())
        }
        openApiSpec.asFile.copyTo(comparisonDirectory.resolve("revision.yaml"), overwrite = true)
        val mount = "type=bind,source=${comparisonDirectory.absolutePath.replace('\\', '/')},target=/workspace,readonly"
        val exitCode =
            runOasdiff(
                listOf(
                    "--mount",
                    mount,
                    oasdiffImage,
                    "breaking",
                    "--fail-on",
                    "ERR",
                    "/workspace/main.yaml",
                    "/workspace/revision.yaml",
                ),
            )
        check(exitCode == 0) { "oasdiff detectó cambios incompatibles frente a main." }
    }
}

val apiCheck = tasks.register("apiCheck") {
    group = "verification"
    description = "Ejecuta la validación, generación y controles contract-first de OpenAPI."
    dependsOn(
        validateOpenApi,
        generateOpenApiServer,
        generateOpenApiClient,
        tasks.named("compileJava"),
        lintOpenApi,
        verifySpectralNegativeCases,
        typecheckGeneratedOpenApiClient,
        verifyOasdiffBreakingCase,
        checkOpenApiCompatibility,
    )
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
    dependsOn("verifyJavaToolchain", "verifyRuntimeStack", apiCheck)
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
