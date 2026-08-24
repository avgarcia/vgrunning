# Directrices del repositorio

## Estructura del proyecto y organización de módulos

El repositorio está en preparación técnica del PMV. El material normativo está en `docs/`; `docs/phase-0-problem-statement.md` recoge el problema validado, y `README.md` es el punto de entrada.

El monorepo contiene un único proyecto Gradle raíz y estas fronteras:

- `src/main/java`: producción Java, bajo `com.vgrunning`.
- `src/main/resources`: recursos del backend.
- `src/test/java`: pruebas Java.
- `frontend/`: futura SPA y sus herramientas npm.
- `api/openapi/`: contratos OpenAPI como fuente de verdad.

No mezcles código generado, credenciales locales ni notas personales con la documentación validada. Los generados permanecen bajo directorios de build y no se versionan.

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
npm ci --prefix frontend           # Instala el lockfile con Node 24.19.0 y npm 11.17.0.
git status                         # Revisa los cambios locales antes de compartirlos.
git diff --check                   # Detecta errores de espacios en blanco.
```

En macOS, Linux o Git Bash, usa `./gradlew clean build`, `./gradlew verifyJavaToolchain` y `./gradlew -q javaToolchains`.

No hay todavía runtime de aplicación, framework de pruebas, linter ni formateador. Añade cada comando en el mismo cambio que incorpore su herramienta.

## Directrices de pruebas

No hay pruebas automatizadas ni requisitos de cobertura todavía. Toda funcionalidad futura debe incluir pruebas con el framework seleccionado y nombres que describan el comportamiento, por ejemplo `workout-assignment.spec.ts`. Documenta el comando de pruebas y el umbral de cobertura cuando se adopten.

## Directrices de commits y pull requests

El historial existente usa asuntos cortos, imperativos y con guiones, por ejemplo `Add-phase-0-problem-statement`. Sigue ese patrón: `Add-phase-1-user-flows` o `Clarify-feedback-scope`.

Mantén cada commit enfocado. Los pull requests deben indicar el problema abordado, resumir los documentos o el código modificados, enlazar el issue o la decisión correspondiente cuando exista e incluir capturas para cambios de interfaz. Señala explícitamente los cambios en supuestos, alcance o riesgos.

## Flujo obligatorio de cambios

No hagas commits ni pushes directos a `main`, incluidos cambios exclusivamente documentales. Para cualquier modificación crea una rama con prefijo `feature/`, por ejemplo `feature/clarify-tag-taxonomy`; confirma el alcance, valida el cambio, haz commit y abre una PR borrador contra `main`.

Cuando haya una persona revisora independiente disponible, solo integra cambios mediante una PR aprobada por ella. En este proyecto de un único mantenedor, el autor puede fusionar su propia PR tras ejecutar las validaciones aplicables, realizar la autovalidación según los criterios documentados y dejar constancia en la PR de que no ha habido revisión independiente y de que el riesgo se acepta explícitamente. No uses cuentas alternativas para simular una aprobación independiente. Esta excepción deja de aplicar en cuanto exista una persona revisora independiente.
