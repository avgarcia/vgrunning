import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.SourceSetContainer

val validationNode = if (System.getProperty("os.name").startsWith("Windows")) "node.exe" else "node"

val classifyValidationScope = tasks.register<Exec>("classifyValidationScope") {
    group = "verification"
    description = "Calcula en shadow mode los gates aplicables al diff sin alterar la ejecución real."
    commandLine(validationNode, "scripts/classify-validation-scope.mjs", "--base", "origin/main")
    inputs.file("config/validation-matrix.json")
    inputs.file("scripts/classify-validation-scope.mjs")
    outputs.file(layout.buildDirectory.file("reports/validation/plan.json"))
}

val verifyValidationScopeClassifier = tasks.register<Exec>("verifyValidationScopeClassifier") {
    group = "verification"
    description = "Verifica con fixtures mínimos la matriz conservadora de validación."
    commandLine(validationNode, "scripts/test-classify-validation-scope.mjs")
    inputs.file("config/validation-matrix.json")
    inputs.files("scripts/classify-validation-scope.mjs", "scripts/test-classify-validation-scope.mjs")
}

val backendCheck = tasks.register("backendCheck") {
    group = "verification"
    description = "Ejecuta calidad Java/backend, arquitectura, pruebas, cobertura y PIT condicional."
    dependsOn(tasks.named("check"), tasks.named("pitest"), tasks.named("verifyCriticalQualityScope"))
}

val docsCheck = tasks.register("docsCheck") {
    group = "verification"
    description = "Ejecuta enlaces, configuración documental y verificaciones operativas locales."
    dependsOn("verifyDocumentationLinks", "verifyAiGovernance", "verifyLocalRuntimeConfiguration")
}

val supplyChainCheck = tasks.register("supplyChainCheck") {
    group = "verification"
    description = "Construye la imagen OCI y ejecuta SBOM, Trivy y reproducibilidad."
    dependsOn("trivy", "verifyOciReproducibility")
}

tasks.named<Test>("test") {
    exclude("**/SecurityAndManagementEndpointTest.class")
}

val spaDeliveryTest = tasks.register<Test>("spaDeliveryTest") {
    group = "verification"
    description = "Ejecuta las pruebas Spring que requieren los recursos SPA empaquetados."
    dependsOn("frontendBuild")
    val testSourceSet = project.extensions.getByType<SourceSetContainer>().named("test").get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    include("**/SecurityAndManagementEndpointTest.class")
}

tasks.named("verifySpaPackaging") {
    dependsOn(spaDeliveryTest)
}

tasks.named("toolingGate") {
    dependsOn("verifyQualityNegativeCases", "verifySpectralNegativeCases", "verifyOasdiffBreakingCase", verifyValidationScopeClassifier)
}

tasks.named("fastGate") {
    dependsOn(backendCheck)
}

tasks.named("qualityGate") {
    dependsOn(backendCheck, "apiCheck", "frontendCheck", docsCheck, "gitleaks", "toolingGate", supplyChainCheck, "verifySpaPackaging", classifyValidationScope)
}
