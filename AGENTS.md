# Directrices del repositorio

## Estructura del proyecto y organización de módulos

El repositorio está en preparación técnica del PMV. El material normativo está en `docs/`; `docs/phase-0-problem-statement.md` recoge el problema validado, y `README.md` es el punto de entrada.

El monorepo contiene un único proyecto Gradle raíz y estas fronteras:

- `src/main/java`: producción Java, bajo `com.vgrunning`.
- `src/main/resources`: recursos del backend.
- `src/test/java`: pruebas Java.
- `src/codegen/java`: runner técnico aislado para generar jOOQ desde PostgreSQL efímero.
- `frontend/`: SPA React, pruebas y herramientas npm.
- `api/openapi/`: contratos OpenAPI como fuente de verdad.

No mezcles código generado, credenciales locales ni notas personales con la documentación validada. Los generados permanecen bajo directorios de build y no se versionan.

El backend contiene una única aplicación Spring Boot MVC y ocho módulos Spring Modulith: `identity-access`, `runner-management`, `classification-segmentation`, `planning`, `publication`, `notification-delivery`, `tracking-review` y `runner-portal`. Los paquetes Java usan identificadores válidos sin guiones y los nombres lógicos se declaran mediante Spring Modulith. Solo los paquetes `api` son contratos entre módulos; no se puede acceder a internos.

PostgreSQL 18 es el único motor admitido. Flyway mantiene una única historia en `platform.flyway_schema_history`; jOOQ genera desde una base efímera migrada y escribe solo en `build/generated`. Los tipos generados usan `org.vgrunning.generated.jooq` para quedar fuera de la detección modular y solo podrán consumirse desde el adaptador de persistencia propietario de cada módulo. `runner-portal` no posee esquema.

`api/openapi/running-coach.yaml` es la fuente de verdad OpenAPI 3.1. Los tipos generados de servidor y cliente se escriben solo bajo `build/generated/openapi`; los primeros solo podrán consumirse desde `adapter.http`. La API inicial permanece bajo `/api`, no contiene `paths` funcionales ni endpoints de salud públicos y declara sesión opaca como seguridad heredada. CSRF se exige además, en la misma alternativa de seguridad, solo en operaciones protegidas que modifican estado.

No crees casos de uso, dominio de plantilla, endpoints funcionales, tablas de negocio ni integraciones externas durante este scaffolding. El shell y las rutas de cliente de la SPA admiten `GET` y `HEAD`; `/api`, métodos inseguros y el resto de Actuator permanecen denegados hasta disponer de autenticación técnica. Los probes de liveness y readiness se sirven por el puerto técnico `8081`.

## Convenciones de documentación

Redacta los documentos de producto y técnicos en Markdown y en castellano. Conserva en su idioma original los identificadores técnicos, rutas, comandos, claves de configuración, nombres de Skills y URLs. Usa nombres descriptivos en kebab-case, con un prefijo de fase o tema, como `docs/phase-1-user-flows.md` o `docs/architecture-overview.md`. Inicia cada documento con un título claro y, cuando sea útil, su estado y fecha.

Mantén la distinción entre decisiones, supuestos, preguntas abiertas y riesgos. Actualiza el enunciado del problema solo si cambia el entendimiento del producto; crea un documento nuevo para trabajo de diseño posterior, en lugar de reescribir el contexto histórico.

## Convenciones de API HTTP

Toda operación HTTP pública debe diseñarse contract-first con OpenAPI y cumplir `docs/adr/0017-resource-oriented-http-api.md` y `docs/api-design-guidelines.md`. Antes de implementar o revisar una operación, identifica el recurso, representación, método, estado HTTP, seguridad, idempotencia y compatibilidad. No introduzcas verbos, acciones nominalizadas, prefijos por rol o secretos en rutas o parámetros de consulta. Una excepción requiere una decisión arquitectónica explícita.

## Comandos de compilación, pruebas y desarrollo

Gradle Wrapper es la entrada canónica; no hace falta instalar Gradle globalmente. Requiere una JVM 17 o superior para arrancar Gradle y resuelve automáticamente Java 25 Temurin para compilar.

```powershell
.\gradlew.bat clean build         # Compila y ejecuta los controles disponibles en Windows.
.\gradlew.bat verifyJavaToolchain # Comprueba Java 25 Temurin.
.\gradlew.bat -q javaToolchains   # Muestra toolchains detectados y descargados.
.\gradlew.bat test                # Ejecuta pruebas con PostgreSQL 18 efímero; requiere Docker.
.\gradlew.bat generateJooqFromPostgres # Migra PostgreSQL efímero y genera jOOQ bajo build/generated.
.\gradlew.bat verifyRuntimeStack  # Rechaza motores alternativos, implementaciones R2DBC, JPA, Hibernate y Spring Data JDBC.
.\gradlew.bat apiCheck            # Valida, genera y comprueba la compatibilidad del contrato OpenAPI; requiere Docker.
.\gradlew.bat validateOpenApi     # Valida api/openapi/running-coach.yaml.
.\gradlew.bat generateOpenApiServer # Genera interfaces y modelos Spring MVC bajo build/generated/openapi/server.
.\gradlew.bat generateOpenApiClient # Genera el cliente TypeScript bajo build/generated/openapi/client/typescript.
.\gradlew.bat frontendCheck      # Ejecuta typecheck, ESLint, Vitest, build Vite y Playwright.
.\gradlew.bat verifySpaPackaging # Comprueba la SPA dentro de bootJar y la ausencia de node_modules.
docker compose up -d --wait postgres # Inicia PostgreSQL local con credenciales sintéticas.
.\gradlew.bat bootRun             # Inicia el backend en 8080 y los probes técnicos en 8081.
docker compose down               # Detiene PostgreSQL sin borrar el volumen local.
docker compose down --volumes     # Elimina la base local para recrearla desde las migraciones.
npm ci --prefix frontend           # Instala el lockfile con Node 24.19.0 y npm 11.17.0.
npm --prefix frontend run playwright:install # Instala el Chromium fijado por Playwright.
npm --prefix frontend run typecheck # Comprueba TypeScript estricto para SPA y herramientas.
npm --prefix frontend run lint      # Aplica ESLint tipado, React Hooks y accesibilidad.
npm --prefix frontend run test:unit # Ejecuta las pruebas Vitest.
npm --prefix frontend run build     # Genera la SPA bajo build/generated/frontend.
npm --prefix frontend run test:e2e  # Ejecuta el smoke Playwright sobre Vite preview.
git status                         # Revisa los cambios locales antes de compartirlos.
git diff --check                   # Detecta errores de espacios en blanco.
```

En macOS, Linux o Git Bash, usa `./gradlew clean build`, `./gradlew verifyJavaToolchain`, `./gradlew generateJooqFromPostgres`, `./gradlew verifyRuntimeStack`, `./gradlew apiCheck`, `./gradlew frontendCheck` y `./gradlew bootRun`.

Hay un runtime Spring Boot y pruebas de modularidad, seguridad, persistencia, arranque y entrega de la SPA. El build completo requiere Docker para codegen y Testcontainers, además del Chromium fijado por Playwright. Spectral aplica los controles automatizables de ADR-0017: rutas y versiones, secretos en URL, `operationId`, semántica HTTP mínima, Problem Details, seguridad de sesión y CSRF, parámetros de ruta, paginación acotada, objetos cerrados y ejemplos sin secretos. La revisión humana sigue siendo obligatoria para semántica de recursos, idempotencia, compatibilidad y excepciones justificadas. Aún no hay formateador, cobertura ni CI; añade cada comando en el mismo cambio que incorpore su herramienta.

## Directrices de pruebas

Las pruebas Java usan JUnit 5, AssertJ, Spring Boot Test, Spring Modulith, ArchUnit y Testcontainers. Verifican módulos, una infracción modular controlada, independencia del dominio, arranque, seguridad, entrega de la SPA, migraciones PostgreSQL y transacciones compartidas por Spring JDBC y jOOQ. El frontend usa Vitest, Testing Library y Playwright; no mezcles sus conjuntos de pruebas ni uses datos reales. No uses H2, SQLite, Derby, HSQLDB ni drivers o pools R2DBC. jOOQ 3.21 requiere transitivamente `r2dbc-spi` para cargar su configuración incluso en modo JDBC; ese SPI pasivo es la única excepción y no autoriza un stack reactivo. Toda funcionalidad futura debe incluir pruebas que describan el comportamiento, por ejemplo `workout-assignment.spec.ts`. Documenta el umbral de cobertura cuando se adopte.

## Directrices de commits y pull requests

El historial existente usa asuntos cortos, imperativos y con guiones, por ejemplo `Add-phase-0-problem-statement`. Sigue ese patrón: `Add-phase-1-user-flows` o `Clarify-feedback-scope`.

Mantén cada commit enfocado. Los pull requests deben indicar el problema abordado, resumir los documentos o el código modificados, enlazar el issue o la decisión correspondiente cuando exista e incluir capturas para cambios de interfaz. Señala explícitamente los cambios en supuestos, alcance o riesgos.

## Flujo obligatorio de cambios

No hagas commits ni pushes directos a `main`, incluidos cambios exclusivamente documentales. Para cualquier modificación crea una rama con prefijo `feature/`, por ejemplo `feature/clarify-tag-taxonomy`; confirma el alcance, valida el cambio, haz commit y abre una PR borrador contra `main`.

Cuando haya una persona revisora independiente disponible, solo integra cambios mediante una PR aprobada por ella. En este proyecto de un único mantenedor, el autor puede fusionar su propia PR tras ejecutar las validaciones aplicables, realizar la autovalidación según los criterios documentados y dejar constancia en la PR de que no ha habido revisión independiente y de que el riesgo se acepta explícitamente. No uses cuentas alternativas para simular una aprobación independiente. Esta excepción deja de aplicar en cuanto exista una persona revisora independiente.
