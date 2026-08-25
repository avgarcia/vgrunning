# Directrices del repositorio

## Estructura del proyecto y organización de módulos

El repositorio está en preparación técnica del PMV. El material normativo está en `docs/`; `docs/phase-0-problem-statement.md` recoge el problema validado, y `README.md` es el punto de entrada.

El monorepo contiene un único proyecto Gradle raíz y estas fronteras:

- `src/main/java`: producción Java, bajo `com.vgrunning`.
- `src/main/resources`: recursos del backend.
- `src/test/java`: pruebas Java.
- `src/codegen/java`: runner técnico aislado para generar jOOQ desde PostgreSQL efímero.
- `frontend/`: futura SPA y sus herramientas npm.
- `api/openapi/`: contratos OpenAPI como fuente de verdad.

No mezcles código generado, credenciales locales ni notas personales con la documentación validada. Los generados permanecen bajo directorios de build y no se versionan.

El backend contiene una única aplicación Spring Boot MVC y ocho módulos Spring Modulith: `identity-access`, `runner-management`, `classification-segmentation`, `planning`, `publication`, `notification-delivery`, `tracking-review` y `runner-portal`. Los paquetes Java usan identificadores válidos sin guiones y los nombres lógicos se declaran mediante Spring Modulith. Solo los paquetes `api` son contratos entre módulos; no se puede acceder a internos.

PostgreSQL 18 es el único motor admitido. Flyway mantiene una única historia en `platform.flyway_schema_history`; jOOQ genera desde una base efímera migrada y escribe solo en `build/generated`. Los tipos generados usan `org.vgrunning.generated.jooq` para quedar fuera de la detección modular y solo podrán consumirse desde el adaptador de persistencia propietario de cada módulo. `runner-portal` no posee esquema.

No crees casos de uso, dominio de plantilla, endpoints funcionales, tablas de negocio ni integraciones externas durante este scaffolding. Toda ruta de aplicación está cerrada por defecto. Los probes de liveness y readiness se sirven por el puerto técnico `8081`; el resto de Actuator permanece denegado hasta disponer de autenticación técnica.

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
docker compose up -d --wait postgres # Inicia PostgreSQL local con credenciales sintéticas.
.\gradlew.bat bootRun             # Inicia el backend en 8080 y los probes técnicos en 8081.
docker compose down               # Detiene PostgreSQL sin borrar el volumen local.
docker compose down --volumes     # Elimina la base local para recrearla desde las migraciones.
npm ci --prefix frontend           # Instala el lockfile con Node 24.19.0 y npm 11.17.0.
git status                         # Revisa los cambios locales antes de compartirlos.
git diff --check                   # Detecta errores de espacios en blanco.
```

En macOS, Linux o Git Bash, usa `./gradlew clean build`, `./gradlew verifyJavaToolchain`, `./gradlew generateJooqFromPostgres`, `./gradlew verifyRuntimeStack` y `./gradlew bootRun`.

Hay un runtime Spring Boot y pruebas iniciales de modularidad, seguridad, persistencia y arranque. El build completo requiere Docker para codegen y Testcontainers. Aún no hay linter, formateador, cobertura ni CI; añade cada comando en el mismo cambio que incorpore su herramienta.

## Directrices de pruebas

Las pruebas Java usan JUnit 5, AssertJ, Spring Boot Test, Spring Modulith, ArchUnit y Testcontainers. Verifican módulos, una infracción modular controlada, independencia del dominio, arranque, seguridad cerrada, migraciones PostgreSQL y transacciones compartidas por Spring JDBC y jOOQ. No uses H2, SQLite, Derby, HSQLDB ni drivers o pools R2DBC. jOOQ 3.21 requiere transitivamente `r2dbc-spi` para cargar su configuración incluso en modo JDBC; ese SPI pasivo es la única excepción y no autoriza un stack reactivo. Toda funcionalidad futura debe incluir pruebas que describan el comportamiento, por ejemplo `workout-assignment.spec.ts`. Documenta el umbral de cobertura cuando se adopte.

## Directrices de commits y pull requests

El historial existente usa asuntos cortos, imperativos y con guiones, por ejemplo `Add-phase-0-problem-statement`. Sigue ese patrón: `Add-phase-1-user-flows` o `Clarify-feedback-scope`.

Mantén cada commit enfocado. Los pull requests deben indicar el problema abordado, resumir los documentos o el código modificados, enlazar el issue o la decisión correspondiente cuando exista e incluir capturas para cambios de interfaz. Señala explícitamente los cambios en supuestos, alcance o riesgos.

## Flujo obligatorio de cambios

No hagas commits ni pushes directos a `main`, incluidos cambios exclusivamente documentales. Para cualquier modificación crea una rama con prefijo `feature/`, por ejemplo `feature/clarify-tag-taxonomy`; confirma el alcance, valida el cambio, haz commit y abre una PR borrador contra `main`.

Cuando haya una persona revisora independiente disponible, solo integra cambios mediante una PR aprobada por ella. En este proyecto de un único mantenedor, el autor puede fusionar su propia PR tras ejecutar las validaciones aplicables, realizar la autovalidación según los criterios documentados y dejar constancia en la PR de que no ha habido revisión independiente y de que el riesgo se acepta explícitamente. No uses cuentas alternativas para simular una aprobación independiente. Esta excepción deja de aplicar en cuanto exista una persona revisora independiente.
