import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.language.jvm.tasks.ProcessResources
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone
import info.solidsoft.gradle.pitest.PitestPluginExtension
import nu.studer.gradle.jooq.JooqEdition
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.zip.ZipFile

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("nu.studer.jooq") version "10.2.1"
    id("org.openapi.generator") version "7.24.0"
    id("com.diffplug.spotless") version "8.10.0"
    id("com.github.spotbugs") version "6.5.11"
    id("net.ltgt.errorprone") version "5.1.1"
    id("info.solidsoft.pitest") version "1.19.0"
    jacoco
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
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
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
val generatedFrontendDirectory = layout.buildDirectory.dir("generated/frontend")
val oasdiffImage =
    "tufin/oasdiff@sha256:6065c16a4c9ce12504752f444d4981091e58c2a35436fac90b649be47d833db3"
val gitleaksImage =
    "zricethezav/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f"
val trivyImage =
    "aquasec/trivy@sha256:62b1e65e8869bc4b4c6aa4fa2b21595256c7c2f6018a9d9ad61caf87187c1969"
val temurinRuntimeImage =
    "eclipse-temurin:25.0.4_7-jre-noble@sha256:8c6736fa623090b057a5bbd36d42f90c9de4c7d2d4b6c285921a4f85ce65a445"
val npmExecutable = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"
val nodeExecutable = if (System.getProperty("os.name").startsWith("Windows")) "node.exe" else "node"

val codegen = sourceSets.create("codegen") {
    java.srcDir("src/codegen/java")
    resources.srcDir("src/main/resources")
}

val jooqGenerated = sourceSets.create("jooqGenerated") {
    java.srcDir(generatedJooqDirectory)
}

val openApiGenerated = sourceSets.create("openApiGenerated") {
    java.srcDir(generatedOpenApiServerDirectory.map { it.dir("src/main/java") })
}

configurations.named(openApiGenerated.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}

configurations.named(jooqGenerated.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}

sourceSets.named("main") {
    compileClasspath += openApiGenerated.output
    compileClasspath += jooqGenerated.output
    runtimeClasspath += openApiGenerated.output
    runtimeClasspath += jooqGenerated.output
}

sourceSets.named("test") {
    compileClasspath += openApiGenerated.output
    compileClasspath += jooqGenerated.output
    runtimeClasspath += openApiGenerated.output
    runtimeClasspath += jooqGenerated.output
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(platform("org.springframework.modulith:spring-modulith-bom:2.1.0"))
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(enforcedPlatform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

    compileOnly("org.jspecify:jspecify:1.0.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.10.4")
    testCompileOnly("org.jspecify:jspecify:1.0.1")
    testCompileOnly("org.projectlombok:lombok:1.18.44")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.44")

    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.14.0")
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.36.0").aosp()
    }
}

spotbugs {
    toolVersion.set("4.10.4")
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.LOW)
    ignoreFailures.set(false)
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.create("xml") {
        required.set(true)
    }
    reports.create("html") {
        required.set(true)
    }
}

tasks.matching {
        it.name in setOf("spotbugsCodegen", "spotbugsJooqGenerated", "spotbugsOpenApiGenerated", "spotbugsTest")
    }
    .configureEach {
    enabled = false
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
        option("NullAway:JSpecifyExperimental", "false")
    }
}

jacoco {
    toolVersion = "0.8.15"
}

extensions.configure<PitestPluginExtension>("pitest") {
    pitestVersion.set("1.22.1")
    junit5PluginVersion.set("1.2.3")
    targetClasses.set(listOf("com.vgrunning.*.domain.*", "com.vgrunning.*.application.*"))
    outputFormats.set(setOf("XML", "HTML"))
    timestampedReports.set(false)
    mutationThreshold.set(70)
}

fun hasCriticalSourceFiles(): Boolean =
    fileTree("src/main/java") {
        include("com/vgrunning/**/domain/**/*.java", "com/vgrunning/**/application/**/*.java")
    }.files.isNotEmpty()

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)
    onlyIf {
        hasCriticalSourceFiles()
    }
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            includes = listOf("com.vgrunning.*.domain.*", "com.vgrunning.*.application.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("pitest") {
    onlyIf {
        hasCriticalSourceFiles()
    }
}

val verifyCriticalQualityScope = tasks.register("verifyCriticalQualityScope") {
    group = "verification"
    description = "Registra si los umbrales de cobertura y PIT aplican al código crítico existente."

    doLast {
        if (hasCriticalSourceFiles()) {
            logger.lifecycle("Se han detectado paquetes críticos: se aplican JaCoCo y PIT.")
        } else {
            logger.lifecycle("No hay paquetes domain o application: JaCoCo crítico y PIT quedan configurados y no aplican al scaffolding.")
        }
    }
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

sourceSets.named("main") {
    val generatedJooqPath = generatedJooqDirectory.get().asFile.toPath().normalize()
    java.setSrcDirs(java.srcDirs.filterNot { it.toPath().normalize() == generatedJooqPath })
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
    dependsOn(jooqGenerated.classesTaskName, openApiGenerated.classesTaskName)
}

tasks.named<JavaCompile>(jooqGenerated.compileJavaTaskName) {
    dependsOn(generateJooqFromPostgres)
    options.compilerArgs.removeAll(listOf("-Xlint:all,-processing", "-Werror"))
    options.errorprone {
        enabled.set(false)
    }
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
            "requestMappingMode" to "api_interface",
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
            "apis" to "",
            "models" to "",
            "supportingFiles" to "",
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

tasks.named<JavaCompile>(openApiGenerated.compileJavaTaskName) {
    dependsOn(generateOpenApiServer)
    options.compilerArgs.removeAll(listOf("-Xlint:all,-processing", "-Werror"))
    options.errorprone {
        enabled.set(false)
    }
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

val frontendTypecheck = tasks.register<Exec>("frontendTypecheck") {
    group = "frontend"
    description = "Comprueba en modo estricto la SPA y sus herramientas TypeScript."
    dependsOn(installFrontendDependencies, generateOpenApiClient)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "typecheck")
    inputs.files(
        fileTree("frontend/src"),
        fileTree("frontend/e2e"),
        fileTree("frontend") { include("*.config.ts", "tsconfig*.json") },
        generatedOpenApiClientDirectory,
    )
}

val frontendLint = tasks.register<Exec>("frontendLint") {
    group = "frontend"
    description = "Aplica ESLint tipado, reglas React y controles de accesibilidad."
    dependsOn(installFrontendDependencies, generateOpenApiClient)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "lint")
    inputs.files(
        fileTree("frontend/src"),
        fileTree("frontend/e2e"),
        fileTree("frontend/scripts"),
        fileTree("frontend") { include("*.config.js", "*.config.ts", "tsconfig*.json") },
        generatedOpenApiClientDirectory,
    )
}

val frontendUnitTest = tasks.register<Exec>("frontendUnitTest") {
    group = "frontend"
    description = "Ejecuta las pruebas unitarias Vitest de la SPA."
    dependsOn(installFrontendDependencies, generateOpenApiClient)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "test:unit")
    inputs.files(fileTree("frontend/src"), file("frontend/vite.config.ts"))
}

val frontendBuild = tasks.register<Exec>("frontendBuild") {
    group = "frontend"
    description = "Construye con Vite los recursos estáticos que se empaquetan en Spring Boot."
    dependsOn(installFrontendDependencies, generateOpenApiClient)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "build")
    inputs.files(
        file("frontend/index.html"),
        fileTree("frontend/src"),
        file("frontend/vite.config.ts"),
        file("frontend/tsconfig.app.json"),
        generatedOpenApiClientDirectory,
    )
    outputs.dir(generatedFrontendDirectory)
}

val installPlaywrightChromium = tasks.register<Exec>("installPlaywrightChromium") {
    group = "frontend"
    description = "Instala el Chromium fijado por la versión de Playwright del lockfile."
    dependsOn(installFrontendDependencies)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "playwright:install")
}

val frontendPlaywright = tasks.register<Exec>("frontendPlaywright") {
    group = "frontend"
    description = "Ejecuta el smoke test sintético de la SPA sobre Vite preview."
    dependsOn(frontendBuild, installPlaywrightChromium)
    workingDir(file("frontend"))
    commandLine(npmExecutable, "run", "test:e2e")
    inputs.files(
        fileTree("frontend/e2e"),
        file("frontend/playwright.config.ts"),
        generatedFrontendDirectory,
    )
}

val frontendCheck = tasks.register("frontendCheck") {
    group = "verification"
    description = "Agrega typecheck, ESLint, Vitest, build Vite y Playwright."
    dependsOn(frontendTypecheck, frontendLint, frontendUnitTest, frontendBuild, frontendPlaywright)
}

tasks.named<ProcessResources>("processResources") {
    mustRunAfter(frontendBuild)
    from(generatedFrontendDirectory) {
        into("static")
    }
}

val bootJar = tasks.named<BootJar>("bootJar")
bootJar.configure {
    dependsOn(frontendBuild)
    from(jooqGenerated.output)
    from(openApiGenerated.output)
}

val verifyGeneratedSourceIsolation = tasks.register("verifyGeneratedSourceIsolation") {
    group = "verification"
    description = "Comprueba que OpenAPI generado no entra en el source set Java propio."

    doLast {
        val buildDirectory = layout.buildDirectory.get().asFile.toPath().normalize()
        val mainSourceDirectories = sourceSets.main.get().java.srcDirs.map { it.toPath().normalize() }
        check(mainSourceDirectories.none { it.startsWith(buildDirectory) }) {
            "El source set main incluye fuentes bajo build/generated; usa openApiGenerated."
        }
    }
}

fun runCommand(arguments: List<String>, directory: File = projectDir): Int =
    ProcessBuilder(arguments)
        .directory(directory)
        .inheritIO()
        .start()
        .waitFor()

data class CommandResult(val exitCode: Int, val output: String)

fun runCommandCapturing(arguments: List<String>, directory: File = projectDir): CommandResult {
    val process =
        ProcessBuilder(arguments)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    return CommandResult(process.waitFor(), output)
}

fun ensureDockerVolume(name: String) {
    check(runCommand(listOf("docker", "volume", "create", name)) == 0) {
        "No se pudo preparar el volumen Docker de caché $name."
    }
}

val gitRevision = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }
val sourceDateEpoch = providers.exec {
    commandLine("git", "show", "-s", "--format=%ct", "HEAD")
}.standardOutput.asText.map { it.trim() }
val securityReportsDirectory = layout.buildDirectory.dir("reports/security")
val localOciImage = gitRevision.map { revision -> "vgrunning:$revision" }
val imageTar = securityReportsDirectory.map { directory -> directory.file("vgrunning-image.tar") }
val imageMetadata = securityReportsDirectory.map { directory -> directory.file("image-metadata.json") }
val sbomFile = securityReportsDirectory.map { directory -> directory.file("sbom.spdx.json") }
val trivyCacheVolume = "vgrunning-trivy-cache"

val verifyTrivyExceptions = tasks.register("verifyTrivyExceptions") {
    group = "verification"
    description = "Comprueba que el registro de excepciones Trivy conserva su formato seguro."
    val exceptions = layout.projectDirectory.file("security/trivy-exceptions.json")
    inputs.file(exceptions)

    doLast {
        val content = exceptions.asFile.readText(Charsets.UTF_8).trim()
        check(content == "{\n  \"exceptions\": []\n}" || content == "{\"exceptions\":[]}") {
            "El registro de excepciones Trivy no está vacío. Toda excepción requiere una revisión explícita."
        }
    }
}

val gitleaks = tasks.register("gitleaks") {
    group = "verification"
    description = "Busca secretos versionados o presentes en el árbol de trabajo mediante Gitleaks fijado."
    inputs.files(fileTree(projectDir) { exclude(".git/**", ".gradle/**", "build/**", "frontend/node_modules/**") })
    inputs.property("gitleaksImage", gitleaksImage)

    doLast {
        val projectMount = "type=bind,source=${projectDir.absolutePath.replace('\\', '/')},target=/repo,readonly"
        check(runCommand(listOf("docker", "run", "--rm", "--mount", projectMount, gitleaksImage, "git", "/repo", "--redact")) == 0) {
            "Gitleaks detectó un secreto o un patrón de credencial en el historial Git."
        }
        val sourceDirectory = layout.buildDirectory.dir("tmp/gitleaks-source").get().asFile
        project.delete(sourceDirectory)
        project.copy {
            from(projectDir)
            into(sourceDirectory)
            exclude(".git/**", ".gradle/**", "build/**", "frontend/node_modules/**")
        }
        val sourceMount = "type=bind,source=${sourceDirectory.absolutePath.replace('\\', '/')},target=/source,readonly"
        check(
            runCommand(
                listOf(
                    "docker", "run", "--rm", "--mount", projectMount, "--mount", sourceMount, gitleaksImage, "dir",
                    "--config", "/repo/.gitleaks.toml", "/source", "--redact",
                ),
            ) == 0,
        ) {
            "Gitleaks detectó un secreto o un patrón de credencial en el árbol de trabajo."
        }
    }
}

val buildOciImage = tasks.register("buildOciImage") {
    group = "distribution"
    description = "Construye la imagen OCI linux/amd64 a partir del bootJar."
    dependsOn(bootJar)
    mustRunAfter(tasks.named("check"))
    inputs.file(bootJar.flatMap { it.archiveFile })
    inputs.files("Dockerfile", ".dockerignore")
    inputs.property("revision", gitRevision)
    inputs.property("sourceDateEpoch", sourceDateEpoch)
    inputs.property("runtimeImage", temurinRuntimeImage)
    outputs.file(imageMetadata)

    doLast {
        val revision = gitRevision.get()
        val archive = bootJar.get().archiveFile.get().asFile
        val archivePath = archive.relativeTo(projectDir).invariantSeparatorsPath
        val image = localOciImage.get()
        val exitCode =
            runCommand(
                listOf(
                    "docker", "buildx", "build", "--load", "--platform", "linux/amd64", "--provenance=false",
                    "--build-arg", "JAR_FILE=$archivePath", "--build-arg", "VCS_REF=$revision",
                    "--build-arg", "SOURCE_DATE_EPOCH=${sourceDateEpoch.get()}", "--tag", image, ".",
                ),
            )
        check(exitCode == 0) { "No se pudo construir la imagen OCI local." }
        val digest =
            ProcessBuilder("docker", "image", "inspect", "--format={{.Id}}", image)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText().trim() }
        val metadataFile = imageMetadata.get().asFile
        metadataFile.parentFile.mkdirs()
        metadataFile.writeText(
            """{
  "image": "$image",
  "revision": "$revision",
  "digest": "$digest",
  "platform": "linux/amd64"
}
""",
            Charsets.UTF_8,
        )
    }
}

val verifyOciReproducibility = tasks.register("verifyOciReproducibility") {
    group = "verification"
    description = "Comprueba dos construcciones OCI sin caché con el mismo digest local."
    dependsOn(buildOciImage)
    inputs.file(bootJar.flatMap { it.archiveFile })
    inputs.files("Dockerfile", ".dockerignore")
    inputs.property("revision", gitRevision)
    inputs.property("sourceDateEpoch", sourceDateEpoch)
    outputs.file(securityReportsDirectory.map { it.file("oci-reproducibility.txt") })

    doLast {
        val revision = gitRevision.get()
        val archive = bootJar.get().archiveFile.get().asFile
        val archivePath = archive.relativeTo(projectDir).invariantSeparatorsPath
        val firstImage = "vgrunning-repro-a:$revision"
        val secondImage = "vgrunning-repro-b:$revision"

        fun buildWithoutCache(tag: String) {
            check(
                runCommand(
                    listOf(
                        "docker", "buildx", "build", "--load", "--no-cache", "--platform", "linux/amd64",
                        "--provenance=false", "--build-arg", "JAR_FILE=$archivePath", "--build-arg", "VCS_REF=$revision",
                        "--build-arg", "SOURCE_DATE_EPOCH=${sourceDateEpoch.get()}", "--tag", tag, ".",
                    ),
                ) == 0,
            ) { "No se pudo construir la imagen OCI sin caché para comprobar reproducibilidad." }
        }

        fun imageId(image: String): String =
            ProcessBuilder("docker", "image", "inspect", "--format={{.Id}}", image)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText().trim() }

        buildWithoutCache(firstImage)
        buildWithoutCache(secondImage)
        val firstDigest = imageId(firstImage)
        val secondDigest = imageId(secondImage)
        check(firstDigest == secondDigest) {
            "Las construcciones OCI sin caché no son reproducibles: $firstDigest frente a $secondDigest."
        }
        val evidence = securityReportsDirectory.get().file("oci-reproducibility.txt").asFile
        evidence.parentFile.mkdirs()
        evidence.writeText("digest=$firstDigest\nplatform=linux/amd64\n", Charsets.UTF_8)
    }
}

val publishOciImage = tasks.register("publishOciImage") {
    group = "publishing"
    description = "Publica la imagen OCI canónica de GHCR para el commit actual."
    dependsOn(buildOciImage)
    inputs.file(imageMetadata)
    inputs.property("revision", gitRevision)
    doLast {
        val revision = gitRevision.get()
        val repository = System.getenv("GITHUB_REPOSITORY") ?: "avgarcia/vgrunning"
        val remoteImage = "ghcr.io/$repository:$revision"
        val localImage = localOciImage.get()
        check(runCommand(listOf("docker", "tag", localImage, remoteImage)) == 0) {
            "No se pudo etiquetar la imagen canónica de GHCR."
        }
        check(runCommand(listOf("docker", "push", remoteImage)) == 0) {
            "No se pudo publicar la imagen OCI en GHCR."
        }
        val digest =
            ProcessBuilder("docker", "image", "inspect", "--format={{index .RepoDigests 0}}", remoteImage)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText().trim() }
        check("@sha256:" in digest) { "GHCR no devolvió un digest inmutable para la imagen publicada." }
        imageMetadata.get().asFile.writeText(
            """{
  "image": "$remoteImage",
  "revision": "$revision",
  "digest": "$digest",
  "platform": "linux/amd64"
}
""",
            Charsets.UTF_8,
        )
    }
}

val generateSbom = tasks.register("generateSbom") {
    group = "verification"
    description = "Genera el SBOM SPDX JSON de la imagen OCI local mediante Trivy fijado."
    dependsOn(buildOciImage)
    inputs.file(imageMetadata)
    inputs.property("trivyImage", trivyImage)
    outputs.file(sbomFile)

    doLast {
        val reportsDirectory = securityReportsDirectory.get().asFile
        reportsDirectory.mkdirs()
        ensureDockerVolume(trivyCacheVolume)
        val image = localOciImage.get()
        check(runCommand(listOf("docker", "image", "save", "--output", imageTar.get().asFile.absolutePath, image)) == 0) {
            "No se pudo exportar la imagen OCI para analizarla sin socket Docker."
        }
        val mount = "type=bind,source=${reportsDirectory.absolutePath.replace('\\', '/')},target=/reports"
        val cacheMount = "type=volume,source=$trivyCacheVolume,target=/root/.cache/trivy"
        check(
            runCommand(
                listOf(
                    "docker", "run", "--rm", "--mount", mount, "--mount", cacheMount, trivyImage, "image", "--timeout",
                    "30m", "--input", "/reports/vgrunning-image.tar", "--format", "spdx-json", "--output", "/reports/sbom.spdx.json",
                ),
            ) == 0,
        ) { "Trivy no pudo generar el SBOM SPDX de la imagen." }
    }
}

val trivy = tasks.register("trivy") {
    group = "verification"
    description = "Bloquea vulnerabilidades CRITICAL en la imagen OCI local mediante Trivy fijado."
    dependsOn(generateSbom, verifyTrivyExceptions)
    inputs.file(imageTar)
    inputs.file(layout.projectDirectory.file("security/trivy-exceptions.json"))
    inputs.property("trivyImage", trivyImage)

    doLast {
        val reportsDirectory = securityReportsDirectory.get().asFile
        ensureDockerVolume(trivyCacheVolume)
        val mount = "type=bind,source=${reportsDirectory.absolutePath.replace('\\', '/')},target=/reports,readonly"
        val cacheMount = "type=volume,source=$trivyCacheVolume,target=/root/.cache/trivy"
        val result =
            runCommandCapturing(
                listOf(
                    "docker", "run", "--rm", "--mount", mount, "--mount", cacheMount, trivyImage, "image", "--timeout",
                    "30m", "--input", "/reports/vgrunning-image.tar", "--scanners", "vuln", "--severity", "CRITICAL",
                    "--exit-code", "1", "--no-progress",
                ),
            )
        check(
            result.exitCode == 0,
        ) {
            "Trivy no pudo completar el análisis de vulnerabilidades CRITICAL (exit ${result.exitCode}).\n${result.output}"
        }
    }
}

val qualityGate = tasks.register("qualityGate") {
    group = "verification"
    description = "Ejecuta todos los controles obligatorios de calidad, contrato y seguridad de suministro."
}

val fastGate = tasks.register("fastGate") {
    group = "verification"
    description = "Ejecuta los controles obligatorios de PR sin autopruebas de tooling ni análisis de imagen OCI."
    dependsOn(
        tasks.named("check"),
        tasks.named("pitest"),
        verifyCriticalQualityScope,
        gitleaks,
    )
}

val toolingGate = tasks.register("toolingGate") {
    group = "verification"
    description = "Ejecuta las autopruebas de las herramientas de calidad y seguridad."
}

qualityGate.configure {
    dependsOn(
        fastGate,
        trivy,
        verifyOciReproducibility,
    )
}

val verifyQualityNegativeCases = tasks.register("verifyQualityNegativeCases") {
    group = "verification"
    description = "Demuestra con proyectos fixture mínimos que las infracciones de calidad bloquean sus controles."
    dependsOn(installFrontendDependencies)
    inputs.dir("src/quality-fixtures")
    inputs.file("frontend/eslint.config.js")
    inputs.property("gitleaksImage", gitleaksImage)

    doLast {
        fun expectFailure(arguments: List<String>, name: String, directory: File = projectDir) {
            check(runCommand(arguments, directory) != 0) { "$name aceptó una infracción controlada." }
        }

        val wrapper =
            if (System.getProperty("os.name").startsWith("Windows")) {
                projectDir.resolve("gradlew.bat").absolutePath
            } else {
                projectDir.resolve("gradlew").absolutePath
            }
        val fixtureRoot = layout.buildDirectory.dir("quality-fixtures").get().asFile
        check(fixtureRoot.toPath().startsWith(layout.buildDirectory.get().asFile.toPath())) {
            "La ruta de fixtures debe permanecer dentro de build/."
        }
        fixtureRoot.deleteRecursively()
        fixtureRoot.mkdirs()

        fun prepareJavaFixture(name: String, buildScript: String): File {
            val fixtureDirectory = fixtureRoot.resolve(name)
            fixtureDirectory.resolve("settings.gradle.kts").apply {
                parentFile.mkdirs()
                writeText(
                    """
                    plugins {
                        id(\"org.gradle.toolchains.foojay-resolver-convention\") version \"1.0.0\"
                    }

                    rootProject.name = \"running-coach-quality-fixture-$name\"
                    """.trimIndent() + "\\n",
                    Charsets.UTF_8,
                )
            }
            fixtureDirectory.resolve("build.gradle.kts").writeText(buildScript.trimIndent() + "\\n", Charsets.UTF_8)
            copy {
                from(projectDir.resolve("src/quality-fixtures/$name"))
                into(fixtureDirectory.resolve("src/main/java"))
            }
            return fixtureDirectory
        }

        val werrorFixture =
            prepareJavaFixture(
                "werror",
                """
                import org.gradle.api.tasks.compile.JavaCompile
                import org.gradle.jvm.toolchain.JavaLanguageVersion
                import org.gradle.jvm.toolchain.JvmVendorSpec

                plugins {
                    java
                }

                repositories {
                    mavenCentral()
                }

                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(25))
                        vendor.set(JvmVendorSpec.ADOPTIUM)
                    }
                }

                tasks.withType<JavaCompile>().configureEach {
                    options.encoding = \"UTF-8\"
                    options.release.set(25)
                    options.compilerArgs.addAll(listOf(\"-Xlint:all,-processing\", \"-Werror\"))
                }
                """,
            )
        expectFailure(
            listOf(wrapper, "-p", werrorFixture.absolutePath, "compileJava", "--no-daemon"),
            "javac -Werror",
        )

        val nullAwayFixture =
            prepareJavaFixture(
                "nullaway",
                """
                import net.ltgt.gradle.errorprone.errorprone
                import org.gradle.api.tasks.compile.JavaCompile
                import org.gradle.jvm.toolchain.JavaLanguageVersion
                import org.gradle.jvm.toolchain.JvmVendorSpec

                plugins {
                    java
                    id(\"net.ltgt.errorprone\") version \"5.1.1\"
                }

                repositories {
                    mavenCentral()
                }

                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(25))
                        vendor.set(JvmVendorSpec.ADOPTIUM)
                    }
                }

                dependencies {
                    compileOnly(\"org.jspecify:jspecify:1.0.1\")
                    errorprone(\"com.google.errorprone:error_prone_core:2.50.0\")
                    errorprone(\"com.uber.nullaway:nullaway:0.14.0\")
                }

                tasks.withType<JavaCompile>().configureEach {
                    options.encoding = \"UTF-8\"
                    options.release.set(25)
                    options.errorprone {
                        check(\"NullAway\", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
                        option(\"NullAway:OnlyNullMarked\", \"true\")
                        option(\"NullAway:JSpecifyMode\", \"true\")
                        option(\"NullAway:JSpecifyExperimental\", \"false\")
                    }
                }
                """,
            )
        expectFailure(
            listOf(wrapper, "-p", nullAwayFixture.absolutePath, "compileJava", "--no-daemon"),
            "NullAway",
        )

        val spotBugsFixture =
            prepareJavaFixture(
                "spotbugs",
                """
                import com.github.spotbugs.snom.Confidence
                import com.github.spotbugs.snom.Effort
                import com.github.spotbugs.snom.SpotBugsTask
                import org.gradle.api.tasks.compile.JavaCompile
                import org.gradle.jvm.toolchain.JavaLanguageVersion
                import org.gradle.jvm.toolchain.JvmVendorSpec

                plugins {
                    java
                    id(\"com.github.spotbugs\") version \"6.5.11\"
                }

                repositories {
                    mavenCentral()
                }

                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(25))
                        vendor.set(JvmVendorSpec.ADOPTIUM)
                    }
                }

                spotbugs {
                    toolVersion.set(\"4.10.4\")
                    effort.set(Effort.MAX)
                    reportLevel.set(Confidence.LOW)
                    ignoreFailures.set(false)
                }

                tasks.withType<JavaCompile>().configureEach {
                    options.encoding = \"UTF-8\"
                    options.release.set(25)
                }

                tasks.withType<SpotBugsTask>().configureEach {
                    reports.create(\"xml\") {
                        required.set(true)
                    }
                }
                """,
            )
        expectFailure(
            listOf(wrapper, "-p", spotBugsFixture.absolutePath, "spotbugsMain", "--no-daemon"),
            "SpotBugs",
        )

        val eslintFixture = layout.buildDirectory.file("quality-fixtures/eslint-invalid.ts").get().asFile
        eslintFixture.parentFile.mkdirs()
        eslintFixture.writeText("const intentionallyUnused: string = 1;\n", Charsets.UTF_8)
        expectFailure(
            listOf(npmExecutable, "exec", "--prefix", "frontend", "eslint", "--", "--no-ignore", eslintFixture.absolutePath),
            "ESLint",
        )

        val gitleaksFixture = layout.buildDirectory.file("quality-fixtures/gitleaks-invalid.txt").get().asFile
        gitleaksFixture.writeText("RC_" + "SECRET_ABCDEFGHIJKLMNOPQRSTUVWXYZ123456", Charsets.UTF_8)
        val fixtureMount = "type=bind,source=${gitleaksFixture.parentFile.absolutePath.replace('\\', '/')},target=/fixture,readonly"
        val projectMount = "type=bind,source=${projectDir.absolutePath.replace('\\', '/')},target=/repo,readonly"
        expectFailure(
            listOf(
                "docker", "run", "--rm", "--mount", fixtureMount, "--mount", projectMount, gitleaksImage, "dir",
                "--config", "/repo/.gitleaks.toml", "/fixture", "--redact",
            ),
            "Gitleaks",
        )
    }
}

toolingGate.configure {
    dependsOn(verifyQualityNegativeCases)
}

qualityGate.configure {
    dependsOn(toolingGate)
}
val verifySpaPackaging = tasks.register("verifySpaPackaging") {
    group = "verification"
    description = "Comprueba que bootJar contiene la SPA y no contiene dependencias Node."
    dependsOn(bootJar)
    inputs.file(bootJar.flatMap { it.archiveFile })

    doLast {
        val archive = bootJar.get().archiveFile.get().asFile
        ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            check("BOOT-INF/classes/static/index.html" in entries) {
                "bootJar no contiene BOOT-INF/classes/static/index.html."
            }
            check(entries.any { it.matches(Regex("BOOT-INF/classes/static/assets/.+-[A-Za-z0-9_-]+\\.(?:js|css)")) }) {
                "bootJar no contiene recursos Vite con hash."
            }
            check(entries.none { "/node_modules/" in it || it.startsWith("node_modules/") }) {
                "bootJar contiene dependencias Node."
            }
        }
    }
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
        typecheckGeneratedOpenApiClient,
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

tasks.register<Exec>("verifyDocumentationLinks") {
    group = "verification"
    description = "Comprueba enlaces Markdown locales y anclas GFM de la documentación versionada."
    commandLine(nodeExecutable, "scripts/verify-documentation-links.cjs")
    inputs.dir("docs")
    inputs.file("README.md")
    inputs.file("AGENTS.md")
    inputs.file("scripts/verify-documentation-links.cjs")
}

val verifyDocumentationLinkChecker = tasks.register<Exec>("verifyDocumentationLinkChecker") {
    group = "verification"
    description = "Demuestra que el verificador documental rechaza enlaces rotos y acepta anclas válidas."
    commandLine(nodeExecutable, "scripts/test-verify-documentation-links.cjs")
    inputs.file("scripts/verify-documentation-links.cjs")
    inputs.file("scripts/test-verify-documentation-links.cjs")
}

val verifyAiGovernance = tasks.register<Exec>("verifyAiGovernance") {
    group = "verification"
    description = "Comprueba la política versionada de autoridad y límites operativos de la IA."
    commandLine(nodeExecutable, "scripts/verify-ai-governance.cjs")
    inputs.file("AGENTS.md")
    inputs.file("docs/ai-governance.md")
    inputs.dir(".agents/skills/implementar-slice")
    inputs.dir("config/linear-agent")
    inputs.file("scripts/verify-ai-governance.cjs")
}

val verifyAiGovernanceChecker = tasks.register<Exec>("verifyAiGovernanceChecker") {
    group = "verification"
    description = "Demuestra con fixtures mínimos que la política rechaza invocación implícita y autorización de merge."
    commandLine(nodeExecutable, "scripts/test-verify-ai-governance.cjs")
    inputs.file("scripts/verify-ai-governance.cjs")
    inputs.file("scripts/test-verify-ai-governance.cjs")
    inputs.file("AGENTS.md")
    inputs.file("docs/ai-governance.md")
    inputs.dir(".agents/skills/implementar-slice")
    inputs.dir("config/linear-agent")
}

toolingGate.configure {
    dependsOn(verifyDocumentationLinkChecker, verifyAiGovernanceChecker)
}

val verifyLocalRuntimeConfiguration = tasks.register("verifyLocalRuntimeConfiguration") {
    group = "verification"
    description = "Comprueba el apagado graceful y su límite explícito para desarrollo local."
    inputs.file("src/main/resources/application.yaml")

    doLast {
        val configuration = file("src/main/resources/application.yaml").readText()
        check("server:\n  shutdown: graceful" in configuration) {
            "El runtime local debe configurar server.shutdown=graceful."
        }
        check("lifecycle:\n    timeout-per-shutdown-phase: 30s" in configuration) {
            "El runtime local debe limitar cada fase de apagado graceful a 30 segundos."
        }
        check("config:\n    import: optional:file:.env[.properties]" in configuration) {
            "El runtime local debe importar opcionalmente la configuración sintética de .env."
        }
    }
}

tasks.named("check") {
    dependsOn(
        "verifyJavaToolchain",
        "verifyRuntimeStack",
        "spotlessCheck",
        "spotbugsMain",
        "jacocoTestReport",
        "jacocoTestCoverageVerification",
        verifyGeneratedSourceIsolation,
    )
}

apply(from = "gradle/validation/quality-gates.gradle.kts")

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
