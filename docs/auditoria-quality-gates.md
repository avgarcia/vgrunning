# Auditoría de Quality Gates

**Estado:** Hallazgos
**Fecha:** 2026-09-09
**Alcance:** `build.gradle.kts`, `gradle/validation/quality-gates.gradle.kts`, `.github/workflows/**`, `.gitleaks.toml`, `security/trivy-exceptions.json`, `config/validation-matrix.json`, `scripts/**`

**Supuesto declarado:** no es verificable desde el repositorio qué checks son *required* en la protección de rama de `main`. Todas las afirmaciones sobre "bloquea" o "no bloquea" asumen que los jobs de `quality-gate.yml` son obligatorios y que ningún check adicional se exige fuera de los ficheros versionados.

**Censo usado en el análisis:** 113 ficheros Java en `src/main/java` (47 en `infrastructure`, 33 en `application`, 9 en `domain`, resto `api`/`package-info`), 44 en `src/test/java`, 147 commits, 2 identidades de autor.

---

## Reglas Innecesarias/Bloqueantes

### R-01 — Patrón transversal: los meta-tests pasan, el gate real no existe

Este es el hallazgo que ordena a todos los demás. El repositorio invierte un esfuerzo notable en **demostrar que las herramientas fallan ante infracciones sintéticas**, mientras que la comprobación equivalente sobre el artefacto real está ausente, deshabilitada o vacía de contenido:

| Meta-test que sí se ejecuta | Gate real equivalente | Estado real |
| --- | --- | --- |
| `verifyOasdiffBreakingCase` (fixtures `base.yaml` / `breaking.yaml`) | `checkOpenApiCompatibility` (contrato actual contra `main`) | Solo colgado de `apiCheck`, que **ningún job de CI invoca** |
| `verifyQualityNegativeCases` → fixture gitleaks `RC_SECRET_…` | `gitleaks` sobre el árbol y el historial | La config **solo** detecta `RC_SECRET_…` (ver `R-03`) |
| `verifyValidationScopeClassifier` (fixtures de la matriz) | `classifyValidationScope` | Corre en *shadow mode*; nadie consume `plan.json` (ver `S-06`) |
| `verifyDocumentationLinkChecker`, `verifyAiGovernanceChecker` | `verifyDocumentationLinks`, `verifyAiGovernance` | Solo dentro de `docsCheck`, que **ningún job de CI invoca** |

El resultado es un pipeline que se autocertifica: `toolingGate` demuestra que oasdiff rechaza un cambio incompatible de laboratorio, pero un cambio incompatible **de verdad** en `api/openapi/running-coach.yaml` entra sin oposición.

**Solución más sencilla:** que `toolingGate` no exista como job independiente. Los negative cases son test de la herramienta, no de la aplicación; muévelos al job `schedule` nocturno y haz que el gate de PR ejecute los controles reales (`apiCheck`, `frontendCheck`, `docsCheck`). Un gate que valida el artefacto vale más que diez que validan el validador.

### R-02 — `verifyTrivyExceptions` es un interbloqueo, no un control

`security/trivy-exceptions.json` se valida por **comparación literal de cadena**:

```kotlin
check(content == "{\n  \"exceptions\": []\n}" || content == "{\"exceptions\":[]}")
```

Es decir: el registro de excepciones no puede contener ninguna excepción, nunca. Al mismo tiempo, la tarea `trivy` ejecuta:

```
--scanners vuln --severity CRITICAL --exit-code 1
```

sin `--ignore-unfixed`. La primera CVE CRITICAL **sin parche disponible** en la imagen base fijada (`eclipse-temurin:25.0.4_7-jre-noble@sha256:8c6736…`) bloquea `container-security`, y con él `reproducibility` y `publish-ghcr`. La única vía de escape documentada es un fichero que el propio build prohíbe rellenar. El registro de excepciones es decorativo.

Esto se agrava porque **no hay `dependabot.yml` ni `renovate.json`** (ver `R-09`): todo está fijado por digest y lockfile, sin ningún mecanismo automatizado de actualización. La imagen base envejecerá hasta acumular la CVE que cierra el pipeline, y no existe proceso que la mueva.

**Solución más sencilla:** eliminar `verifyTrivyExceptions` y el fichero de excepciones. Sustituirlos por el mecanismo nativo de Trivy: `--ignore-unfixed` en el gate bloqueante, y `.trivyignore` (con fecha de expiración por entrada, soportado nativamente) para las excepciones conscientes. Es el estándar del mercado y no requiere código propio.

### R-03 — El escaneo de secretos no detecta secretos (verificado empíricamente)

`.gitleaks.toml` declara **una sola regla** (el marcador sintético `RC_SECRET_[A-Za-z0-9]{32}`) y **no incluye `[extend] useDefault = true`**. En gitleaks, una config propia sustituye el ruleset por defecto, no lo amplía.

Comprobado contra el digest exacto que usa el build (`zricethezav/gitleaks@sha256:c00b6bd0…`), con un fichero que contiene una AWS secret access key y un GitHub PAT con formato válido:

| Invocación | Resultado |
| --- | --- |
| `dir --config /repo/.gitleaks.toml /fixture` | `no leaks found` — **exit 0** |
| `dir /fixture` (reglas por defecto) | `leaks found: 1` — **exit 1** |
| `git /repo` sobre repo con la misma config en raíz | `no leaks found` — **exit 0** |

La tercera fila confirma que la invocación `git /repo --redact` de la tarea `gitleaks`, aunque no pasa `--config`, **autodescubre** `/repo/.gitleaks.toml` y queda igualmente neutralizada. Las dos pasadas de la tarea (historial y árbol de trabajo) detectan exclusivamente su propio marcador de prueba.

Combinado con `R-05` (Trivy sin `--scanners secret`), la cobertura efectiva de detección de secretos en todo el pipeline es **cero**.

**Solución más sencilla:** una línea.

```toml
[extend]
useDefault = true
```

Manteniendo la regla `running-coach-synthetic-secret` para que el negative case siga funcionando y el `[allowlist]` de rutas generadas ya existente.

### R-04 — Umbral JaCoCo por clase al 90 %: la configuración de máxima fricción posible

```kotlin
rule {
    element = "CLASS"
    includes = listOf("com.vgrunning.*.domain.*", "com.vgrunning.*.application.*")
    limit { counter = "LINE";   minimum = "0.90" }
    limit { counter = "BRANCH"; minimum = "0.80" }
}
```

`element = "CLASS"` aplica el umbral a **cada clase individualmente**, no al agregado. Un único value object con una rama defensiva (`if (x == null) throw …`) que no se ejercita rompe el build entero, independientemente de que el conjunto de `domain` esté al 97 %. Es el modo de configurar JaCoCo que más falsos bloqueos genera, y no aporta información que el agregado no dé mejor.

La segunda regla, la de `BUNDLE` global (`LINE ≥ 0.80`, `BRANCH ≥ 0.70`), se aplica sobre un censo donde **47 de 113 ficheros son `infrastructure`**: adaptadores jOOQ, mappers MapStruct, `@Configuration`, web mappers, `package-info`. Esto obliga a escribir tests de andamiaje sobre código sin lógica de negocio solo para levantar el porcentaje — exactamente el "exigir pruebas unitarias en DTOs o configuraciones puras" que hay que evitar.

**Solución más sencilla:** eliminar la regla `element = "CLASS"`, mantener el 90/80 como regla agregada sobre `domain`+`application`, bajar el suelo global y excluir del denominador lo que no es lógica.

```kotlin
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**", "**/*Configuration.class", "**/*Mapper*.class",
                    "**/package-info.class", "**/RunningCoachApplication.class",
                )
            }
        })
    )
    violationRules {
        rule {                       // suelo global, no aspiracional
            limit { counter = "LINE";   minimum = "0.65".toBigDecimal() }
        }
        rule {                       // el código que sí importa, agregado
            includes = listOf("com.vgrunning.*.domain.*", "com.vgrunning.*.application.*")
            limit { counter = "LINE";   minimum = "0.90".toBigDecimal() }
            limit { counter = "BRANCH"; minimum = "0.80".toBigDecimal() }
        }
    }
}
```

La prioridad "negocio sobre infraestructura" ya está bien intencionada en la configuración actual; el problema es que la regla global la contradice y la regla por clase la convierte en trampa.

### R-05 — Trivy: umbral que deja pasar todo lo que no sea CRITICAL

`--severity CRITICAL` significa que las HIGH pasan en silencio. `--scanners vuln` desactiva los escáneres `secret` y `misconfig` de Trivy, que habrían compensado parcialmente `R-03`. Y como solo se escanea la **imagen**, el árbol de dependencias npm del frontend —que nunca llega a la imagen, solo sus assets compilados— **no tiene análisis de vulnerabilidades en ningún punto del pipeline**.

**Solución más sencilla:** `--severity HIGH,CRITICAL --ignore-unfixed` en el gate bloqueante, y `--scanners vuln,secret,misconfig`. Para npm, `npm audit --audit-level=high` en `frontendCheck` cubre el hueco sin herramienta nueva.

### R-06 — Cuatro analizadores estáticos solapados sobre el mismo código Java

Sobre `src/main/java` se apilan: `javac -Xlint:all -Werror`, Error Prone, NullAway y SpotBugs. Y SpotBugs está en la combinación de máxima verbosidad:

```kotlin
effort.set(Effort.MAX)
reportLevel.set(Confidence.LOW)   // el umbral MÁS permisivo: reporta todo
ignoreFailures.set(false)         // y todo bloquea
```

Sin `excludeFilter`. `Confidence.LOW` es donde vive la mayor parte del ruido de SpotBugs, y su familia `NP_*` (null pointer dereference) **se solapa directamente** con lo que NullAway ya verifica en modo JSpecify con severidad `ERROR`. Se paga el coste de dos análisis para obtener dos veces el mismo hallazgo, uno de ellos con tasa de falsos positivos alta. A esto se suma CodeQL `java-kotlin` (`S-08`), que cubre otra vez el mismo terreno.

**Solución más sencilla:** `reportLevel.set(Confidence.MEDIUM)` y un `config/spotbugs-exclude.xml` con las categorías que NullAway ya cubre. Es el ajuste estándar y no pierde señal real:

```kotlin
spotbugs {
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs-exclude.xml"))
    ignoreFailures.set(false)
}
```

### R-07 — La cobertura se mide sobre una ejecución de tests deliberadamente reducida

```kotlin
tasks.named<Test>("test") { exclude("**/SecurityAndManagementEndpointTest.class") }
```

`jacocoTestCoverageVerification` depende de `test`. La clase excluida solo se ejecuta en `spaDeliveryTest`, alcanzable únicamente vía `verifySpaPackaging`, que **ningún job de CI invoca**. Consecuencia: el código de endpoints de seguridad y actuator cuenta como no cubierto en el denominador de un umbral que además es bloqueante, y su test nunca corre en CI.

**Solución más sencilla:** que `jacocoTestReport` agregue ambos `executionData` (`test` y `spaDeliveryTest`), y que `spaDeliveryTest` entre en el gate de PR. Alternativa aún más simple: usar una `@Tag` de JUnit en lugar de un source set de tareas paralelo.

### R-08 — Aserciones sobre `application.yaml` por comparación literal de texto

`verifyLocalRuntimeConfiguration` valida la configuración con `check("server:\n  shutdown: graceful" in configuration)`. Cualquier reordenación, cambio de indentación, comentario intercalado o migración a `application.properties` rompe el build sin que la configuración sea incorrecta. Valida el formato del fichero, no el comportamiento.

**Solución más sencilla:** un `@SpringBootTest` que inyecte `ServerProperties` y afirme sobre los valores resueltos, o simplemente borrarlo — el apagado graceful de una configuración local no es un riesgo que justifique un gate propio.

### R-09 — Sin automatización de actualización de dependencias

Todo está fijado (lockfile de Gradle, `package-lock.json`, digests SHA-256 de todas las imágenes y de todas las acciones de GitHub). Excelente para reproducibilidad, y sin ninguna contrapartida: no hay `dependabot.yml` ni `renovate.json`. Fijar sin actualizar automáticamente es cómo se llega a `R-02`.

**Solución más sencilla:** `.github/dependabot.yml`, unas 20 líneas, cubre los cuatro ecosistemas:

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: "/"
    schedule: { interval: weekly }
  - package-ecosystem: npm
    directory: "/frontend"
    schedule: { interval: weekly }
  - package-ecosystem: github-actions
    directory: "/"
    schedule: { interval: weekly }
  - package-ecosystem: docker
    directory: "/"
    schedule: { interval: weekly }
```

### R-10 — Reglas que sí aportan y deben conservarse

Para que la poda no se lea como "aflojarlo todo":

- `verifyRuntimeStack` (rechaza WebFlux/R2DBC/JPA/H2 en el classpath) codifica ADR-0012 y ADR-0013 en un control automático barato y de cero falsos positivos. Mantener.
- Los tests ArchUnit + Spring Modulith (`HexagonalLayeringTest`, `DomainIndependenceTest`, `JooqBoundaryTest`, `OpenApiBoundaryTest`, `ApplicationModularityTest`) son el gate de arquitectura correcto para ADR-0014/ADR-0026: rápidos, deterministas, no exigen cobertura artificial. Mantener.
- `verifySpaPackaging` (la SPA está en el jar, no hay `node_modules` dentro) verifica un fallo real y silencioso. Mantener — y **ejecutarlo en CI**, que hoy no ocurre.
- PIT con `mutationThreshold = 70` sobre `domain`+`application` es el umbral y el ámbito correctos. El problema es *cuándo* se ejecuta (`S-09`), no el umbral.
- El pinning por digest de acciones e imágenes es una buena práctica de cadena de suministro. Mantener, añadiendo `R-09`.

---

## Sobreingeniería en CI/CD (con su alternativa simple)

### S-01 — `qualityGate` no se ejecuta en ninguna parte

`quality-gates.gradle.kts:68` construye la tarea agregada completa:

```kotlin
tasks.named("qualityGate") {
    dependsOn(backendCheck, "apiCheck", "frontendCheck", docsCheck, "gitleaks",
              "toolingGate", supplyChainCheck, "verifySpaPackaging", classifyValidationScope)
}
```

Ningún workflow la invoca. Los jobs ejecutan `fastGate`, `toolingGate`, `trivy`, `verifyOciReproducibility` y `publishOciImage`. Lo que queda **muerto en CI**:

| Tarea | Qué deja de verificarse |
| --- | --- |
| `frontendCheck` | typecheck TS estricto, ESLint tipado + a11y, Vitest, build Vite, Playwright e2e |
| `apiCheck` | Spectral sobre el contrato, `checkOpenApiCompatibility` (breaking changes reales), typecheck del cliente generado |
| `docsCheck` | enlaces documentales, gobernanza IA, configuración runtime |
| `verifySpaPackaging` | que el jar contiene la SPA |

Es decir: **la SPA React no tiene ninguna verificación en pull request**. Ni compilación, ni lint, ni tests. Se descubrirá al construir la imagen en `main`, o en producción.

**Alternativa simple:** un único job de PR que ejecute `./gradlew qualityGate -x trivy -x verifyOciReproducibility`. Una tarea agregada, un job, sin listas paralelas que puedan divergir.

### S-02 — Pasos muertos: Playwright se instala donde nunca se usa

El job `quality-gate` ejecuta, antes de `./gradlew fastGate`:

```yaml
- run: npm ci --prefix frontend
- run: npm --prefix frontend run playwright:install -- --with-deps
```

`fastGate` = `check` + `pitest` + `verifyCriticalQualityScope` + `gitleaks`. Ninguna toca el frontend. `--with-deps` instala dependencias de sistema vía `apt` para Chromium: varios minutos por ejecución, en cada PR, para nada. El job `publish-ghcr` repite exactamente los dos pasos, y además el `npm ci` es redundante porque `bootJar` → `frontendBuild` → `installFrontendDependencies` ya lo hace desde Gradle.

**Alternativa simple:** borrar ambos pasos de `quality-gate` y `publish-ghcr`. Dejar el `setup-node` con `cache: npm` solo en los jobs que realmente construyen frontend, y que sea Gradle quien invoque `npm ci`.

### S-03 — Entre 4 y 5 construcciones Docker del mismo commit, sin reutilizar nada

Traza de un push a `main`:

| Job | Construye | Coste |
| --- | --- | --- |
| `quality-gate` | Gradle completo (test + pitest + spotbugs) | — |
| `container-security` | `trivy` → `generateSbom` → `buildOciImage` → `bootJar` → `frontendBuild` | build Gradle + frontend + **1 build Docker** |
| `reproducibility` | `verifyOciReproducibility` → `buildOciImage` + 2× `--no-cache` | build Gradle + frontend + **3 builds Docker**, dos sin caché |
| `publish-ghcr` | `publishOciImage` → `buildOciImage` → `bootJar` | build Gradle + frontend + **1 build Docker** |

Cada job hace `checkout` limpio y reconstruye desde cero: nada se pasa entre jobs. El mismo jar se compila tres veces y la misma imagen se construye cinco. `--no-daemon` en todos los `./gradlew` renuncia además a la reutilización del demonio dentro del propio job.

**Alternativa simple:** el patrón estándar de GitHub Actions — construir una vez, publicar el artefacto, consumirlo.

```yaml
# job build
- run: ./gradlew bootJar
- uses: actions/upload-artifact@…
  with: { name: app-jar, path: build/libs/*.jar }

# jobs container-security / publish
- uses: actions/download-artifact@…
  with: { name: app-jar, path: build/libs }
```

Y `docker/build-push-action` con `cache-from: type=gha` en lugar de `docker buildx build` invocado desde `ProcessBuilder` en Kotlin.

### S-04 — `verifyOciReproducibility` en cada push: dos builds `--no-cache` para volver a demostrar lo mismo

Es una propiedad valiosa (ADR-0016), pero es una propiedad del **Dockerfile**, no del commit de negocio. Reconstruir dos veces sin caché en cada push a `main` gasta el tiempo más caro del pipeline para reverificar algo que solo puede romperse cuando cambian `Dockerfile`, `.dockerignore` o la imagen base.

**Alternativa simple:** moverlo al job `schedule` nocturno (ya existe el cron `30 2 * * *`) y al filtro de paths de `Dockerfile`. Cero pérdida de señal, coste dividido entre el número de pushes diarios.

### S-05 — `verifyQualityNegativeCases`: tres builds Gradle anidados para probar que SpotBugs funciona

La tarea genera tres proyectos Gradle completos en `build/quality-fixtures/`, cada uno con su `settings.gradle.kts` y su `build.gradle.kts` embebidos como strings en Kotlin (≈180 líneas de heredoc con escapado manual `\"`), y lanza `./gradlew --no-daemon` sobre cada uno. Cada sub-build resuelve el toolchain Java 25 y descarga Error Prone, NullAway y SpotBugs desde el Plugin Portal. Más ESLint vía `npm exec` y gitleaks vía Docker.

Lo que se demuestra es que herramientas de terceros, ampliamente probadas, hacen lo que documentan. No se demuestra nada sobre este código. Y —ver `R-01`— el negative case de gitleaks pasa mientras la configuración real es incapaz de detectar un secreto de verdad: el meta-test dio confianza justamente donde no la había.

**Alternativa simple:** borrarla. Si se quiere conservar la garantía de que la configuración no se desactiva por accidente, el equivalente estándar es un fichero de test real anotado y un assert en el build de que `ignoreFailures == false`. Si se quiere conservar el enfoque completo, al job `schedule`, nunca en la ruta de PR.

### S-06 — Dos mecanismos paralelos de detección de cambios, ninguno de los dos estándar

1. El job `changes` clasifica con `git diff --name-only | grep -Eq '^(src/|frontend/|…)'` y emite tres outputs booleanos.
2. `scripts/classify-validation-scope.mjs` (92 líneas) + `config/validation-matrix.json` (7 superficies, gates declarados por superficie) + `scripts/test-classify-validation-scope.mjs` clasifican otra vez, **en shadow mode**, y suben `plan.json` como artefacto que nadie lee.

El fichero `validation-matrix.json` declara gates por superficie (`codeql-java`, `postgresql-tests`, `apiCheck`, `verifySpaPackaging`…) que **no se corresponden con lo que CI ejecuta**. Es documentación con forma de código, y ya diverge de la realidad.

Añádase `scripts/collect-validation-baseline.cjs` (197 líneas) y su test, cuyo consumidor no aparece en ningún workflow.

**Alternativa simple:** `dorny/paths-filter` o directamente los filtros `paths:` nativos del workflow, y borrar el clasificador, la matriz, sus tests y el baseline. Son ~350 líneas de JavaScript propio sustituidas por una acción estándar de 10 líneas de YAML.

### S-07 — Las tareas agregadas están definidas dos veces, en dos ficheros

`fastGate` y `qualityGate` se declaran con `register` en `build.gradle.kts` y se amplían con `named` en `quality-gates.gradle.kts`. En el caso de `fastGate` la ampliación es redundancia pura:

```kotlin
// build.gradle.kts:838
fastGate → check, pitest, verifyCriticalQualityScope, gitleaks
// quality-gates.gradle.kts:64
fastGate → backendCheck   // = check + pitest + verifyCriticalQualityScope
```

`qualityGate` recibe dependencias en cuatro puntos distintos de los dos ficheros. Para saber qué ejecuta realmente un gate hay que leer 1.356 + 71 líneas y componer mentalmente el grafo.

**Alternativa simple:** una única definición de cada tarea agregada, en un solo fichero, con la lista completa de dependencias a la vista. `gradle/validation/quality-gates.gradle.kts` es el sitio natural; `build.gradle.kts` no debería tocarlas.

### S-08 — CodeQL sin filtros de path, solapado con el análisis estático local

Los dos workflows de CodeQL se disparan en **todo** pull request y push a `main`, sin `paths:`. `codeql-javascript-typescript` se ejecuta íntegro en una PR que solo toca Java, y `codeql-java` en una PR que solo toca la SPA. Además `codeql-java` cubre terreno que ya cubren Error Prone, NullAway y SpotBugs (`R-06`) — CodeQL aporta análisis de flujo de datos entre ficheros, que los otros no dan, pero eso refuerza el argumento de bajar SpotBugs a `MEDIUM` en lugar de mantener tres capas al máximo.

**Alternativa simple:** añadir `paths:` a cada workflow (`src/**`, `**/*.java` en uno; `frontend/**` en el otro) y unificar los dos en un único workflow con matriz de lenguajes — es el ejemplo canónico de la documentación de CodeQL.

### S-09 — PIT en la ruta crítica de cada PR

`fastGate` incluye `pitest`, sin `withHistory`, con `--no-daemon`. El análisis de mutación es la técnica más lenta del stack y su valor es de tendencia, no de PR individual: un `mutationThreshold` de 70 sobre `domain`+`application` no cambia de veredicto por un commit.

**Alternativa simple:** sacar `pitest` de `fastGate`, dejarlo en el cron nocturno y en push a `main`. El umbral 70 se mantiene tal cual.

### S-10 — `quality-gate` reporta verde sin haber ejecutado nada

Cuando `needs.changes.outputs.code != 'true'`, el job se compone de un único `echo` y todos los pasos siguientes están condicionados a `false`. El check `quality-gate` aparece **en verde** en la PR habiendo ejecutado cero verificaciones. Para una PR solo documental eso es defendible, pero indistinguible en la UI de una PR de código verificada, y ninguna comprobación documental corre en su lugar (`docsCheck` no está en CI, `S-01`).

**Alternativa simple:** el patrón estándar es no condicionar los pasos dentro del job, sino el job entero con `paths:` a nivel de workflow, más un job `quality-gate-ok` que haga `needs` de los reales y sirva como *required check* único en la protección de rama. Un solo check obligatorio, que solo está verde si lo que debía ejecutarse se ejecutó.

### S-11 — Ocho skills de IA como control de calidad documental

`plugins/documentation-quality-review/skills/` define ocho skills (`validate-acceptance-criteria`, `validate-terminology`, `validate-phase-traceability`…) con sus `agents/openai.yaml`. Para un repositorio con dos identidades de autor, es una capa de gobernanza documental cuyo coste de mantenimiento probablemente supera al del código que documenta — y que, a diferencia de los gates de código, no es determinista ni reproducible.

**Alternativa simple:** conservar `verifyDocumentationLinks` (barato, determinista, detecta un fallo real) y ejecutarlo en CI. El resto de controles documentales pertenecen a la plantilla de PR y a la revisión humana, no a un pipeline.

---

## Ajustes Recomendados

### Prioridad 1 — Huecos de seguridad reales (esta semana)

| # | Acción | Fichero | Esfuerzo |
| --- | --- | --- | --- |
| 1 | Añadir `[extend] useDefault = true` | `.gitleaks.toml` | 2 líneas |
| 2 | `--severity HIGH,CRITICAL --ignore-unfixed --scanners vuln,secret,misconfig` | `build.gradle.kts` tarea `trivy` | 1 línea |
| 3 | Eliminar `verifyTrivyExceptions` y `security/trivy-exceptions.json`; adoptar `.trivyignore` | `build.gradle.kts` | −20 líneas |
| 4 | Crear `.github/dependabot.yml` (gradle, npm, github-actions, docker) | nuevo | 20 líneas |
| 5 | Añadir `npm audit --audit-level=high` a `frontendCheck` | `frontend/package.json` | 1 línea |

Los puntos 1 y 2 son el hallazgo más grave del informe: hoy no hay detección de secretos en ningún punto del pipeline, y está verificado empíricamente.

### Prioridad 2 — Que el gate ejecute lo que dice ejecutar

| # | Acción | Efecto |
| --- | --- | --- |
| 6 | El job de PR ejecuta `qualityGate -x trivy -x verifyOciReproducibility` en lugar de `fastGate` | La SPA, el contrato OpenAPI y el empaquetado pasan a verificarse en PR |
| 7 | Mover `pitest`, `toolingGate`, `verifyQualityNegativeCases` y `verifyOciReproducibility` a `schedule` + push a `main` | PR notablemente más rápida sin perder señal |
| 8 | Borrar `npm ci` + `playwright:install` de `quality-gate` y `publish-ghcr` | Varios minutos por ejecución |
| 9 | Job `quality-gate-ok` con `needs` de los reales como único *required check* | Elimina el verde vacío de `S-10` |
| 10 | `paths:` en los dos workflows de CodeQL; unificarlos en uno con matriz | Mitad de ejecuciones de CodeQL |

### Prioridad 3 — Cobertura y análisis estático proporcionados

| # | Acción | De → A |
| --- | --- | --- |
| 11 | Eliminar la regla JaCoCo `element = "CLASS"` | por clase → agregado |
| 12 | Umbral global JaCoCo | `LINE 0.80` → `LINE 0.65` |
| 13 | `classDirectories` excluyendo config, mappers, `package-info`, `RunningCoachApplication` | denominador con lógica real |
| 14 | Umbral `domain`+`application` agregado | `LINE 0.90 / BRANCH 0.80` (sin cambio, ahora alcanzable) |
| 15 | SpotBugs `Confidence.LOW` → `MEDIUM` + `excludeFilter` para la familia `NP_*` | elimina el solape con NullAway |
| 16 | Agregar `executionData` de `spaDeliveryTest` en `jacocoTestReport` y meterlo en el gate | deja de penalizar código cuyo test se excluyó |

### Prioridad 4 — Simplificación estructural

| # | Acción | Líneas eliminadas (aprox.) |
| --- | --- | --- |
| 17 | Sustituir el job `changes` + `classify-validation-scope.mjs` + `validation-matrix.json` + tests por `dorny/paths-filter` | ~350 JS/JSON + 25 YAML |
| 18 | Eliminar `verifyQualityNegativeCases` (o moverla a `schedule`) | ~180 Kotlin |
| 19 | Eliminar `scripts/collect-validation-baseline.cjs` y su test (sin consumidor en CI) | ~230 JS |
| 20 | Definición única de `fastGate` / `qualityGate` en un solo fichero | grafo legible de un vistazo |
| 21 | Sustituir `buildOciImage` / `publishOciImage` (`ProcessBuilder` + `docker buildx`) por `docker/build-push-action` con `cache-from: type=gha` | ~120 Kotlin |
| 22 | `upload-artifact` / `download-artifact` del jar entre jobs | de 3 builds Gradle a 1 |
| 23 | Reemplazar `verifyLocalRuntimeConfiguration` por un `@SpringBootTest` sobre `ServerProperties`, o eliminarlo | aserciones sobre texto → sobre comportamiento |
| 24 | Reducir `plugins/documentation-quality-review` a `verifyDocumentationLinks` | 8 skills → 1 script determinista |

### Lo que no se debe tocar

`verifyRuntimeStack`, los tests ArchUnit y de Spring Modulith, `verifySpaPackaging`, `spotlessCheck`, el pinning por digest de imágenes y acciones, el `dependencyLocking` de Gradle, `-Werror` sobre el código propio (con las fuentes generadas ya correctamente excluidas), NullAway en modo JSpecify y el umbral PIT de 70. Son controles baratos, deterministas y alineados con los ADR; el problema del pipeline no es que verifique demasiado, sino que verifica lo equivocado con demasiado esfuerzo.
