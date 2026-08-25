import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(platform("org.springframework.modulith:spring-modulith-bom:2.1.0"))
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-restclient")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
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
val forbiddenRuntimeModules = setOf(
    "org.springframework:spring-webflux",
    "org.springframework.boot:spring-boot-starter-webflux",
    "org.springframework.boot:spring-boot-r2dbc",
    "org.springframework.boot:spring-boot-starter-data-r2dbc",
    "org.springframework.data:spring-data-r2dbc",
    "org.springframework.boot:spring-boot-jpa",
    "org.springframework.boot:spring-boot-starter-data-jpa",
    "org.hibernate.orm:hibernate-core",
    "org.springframework.boot:spring-boot-starter-data-jdbc",
    "org.springframework.data:spring-data-jdbc",
)

tasks.register("verifyRuntimeStack") {
    group = "verification"
    description = "Rechaza dependencias no permitidas en el runtime imperativo."

    doLast {
        val runtimeModules = configurations.runtimeClasspath.get().incoming.resolutionResult.allComponents
            .mapNotNull { component ->
                component.moduleVersion?.let { module -> "${module.group}:${module.name}" }
            }
            .toSet()
        val forbiddenFound = runtimeModules.intersect(forbiddenRuntimeModules)

        check(forbiddenFound.isEmpty()) {
            "El runtime contiene dependencias no permitidas: ${forbiddenFound.sorted().joinToString()}."
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
