# Running Coach

Repositorio de descubrimiento, diseño y preparación técnica del PMV de Running Coach.

La preparación técnica ya incluye el esqueleto ejecutable Spring Boot, ocho módulos lógicos verificados, persistencia técnica reproducible sobre PostgreSQL 18 y una SPA React mínima empaquetada. No hay todavía funcionalidad ni tablas de negocio.

## Inicio rápido

La guía completa y reproducible está en la [guía del entorno local](docs/local-development.md). No uses datos reales, proveedores externos ni credenciales reales durante esta preparación técnica.

La validación se organiza por superficie con `backendCheck`, `apiCheck`, `frontendCheck`, `docsCheck`, `toolingGate` y `supplyChainCheck`; `qualityGate` conserva el inventario integral. `classifyValidationScope` genera únicamente evidencia en shadow mode y nunca reduce los controles aplicados.

En Windows:

```powershell
Copy-Item .env.example .env
npm ci --prefix frontend
docker compose up -d --wait postgres
.\gradlew.bat bootRun
```

Detén el backend con `Ctrl+C` y PostgreSQL con `docker compose down`. Para reconstruir la base local, consulta el procedimiento de recreación de la guía.

## Base ejecutable

El backend es una única aplicación Spring MVC. Requiere PostgreSQL local antes de arrancar. El shell y las rutas de cliente de la SPA son públicos, pero `/api` permanece cerrado hasta que se implemente `identity-access`; también están abiertos los probes técnicos:

- `http://localhost:8081/actuator/health/liveness`
- `http://localhost:8081/actuator/health/readiness`

En Windows:

```powershell
docker compose up -d --wait postgres
.\gradlew.bat clean build
.\gradlew.bat bootRun
.\gradlew.bat generateJooqFromPostgres
.\gradlew.bat verifyRuntimeStack
docker compose down
```

En macOS, Linux o Git Bash, sustituye `.\gradlew.bat` por `./gradlew`.

Compose publica PostgreSQL en `localhost:5432` con la base, usuario y contraseña sintéticos `running_coach`. La aplicación admite `RUNNING_COACH_DB_URL`, `RUNNING_COACH_DB_USERNAME` y `RUNNING_COACH_DB_PASSWORD`; sus valores locales por defecto coinciden con Compose. Si `5432` ya está ocupado, se puede cambiar solo el puerto publicado con `RUNNING_COACH_DB_PORT` y ajustar `RUNNING_COACH_DB_URL` al mismo valor. Para borrar el volumen local y reconstruir la base desde todas las migraciones:

```powershell
docker compose down --volumes
docker compose up -d --wait postgres
```

Flyway conserva una única historia en `platform.flyway_schema_history`. La tarea `generateJooqFromPostgres` crea otro PostgreSQL efímero, aplica esa misma historia y escribe los tipos en `build/generated/sources/jooq/main`; esas fuentes nunca se versionan. El build completo y las pruebas requieren Docker.

## Frontend

La SPA usa React 19, TypeScript estricto y Vite. Node y Chromium solo participan en desarrollo, build y pruebas; el despliegue contiene exclusivamente los recursos Vite dentro del ejecutable Spring Boot. El cliente generado usa `/api` como ruta relativa y no existe configuración CORS.

```powershell
npm ci --prefix frontend
npm --prefix frontend run playwright:install
npm --prefix frontend run typecheck
npm --prefix frontend run lint
npm --prefix frontend run test:unit
npm --prefix frontend run build
npm --prefix frontend run test:e2e
.\gradlew.bat frontendCheck
.\gradlew.bat verifySpaPackaging
```

Vitest cubre el shell y la configuración del cliente; Playwright ejecuta un smoke sintético contra Vite preview. Las pruebas Spring verifican por separado el fallback de rutas, recursos, errores, API cerrada, ausencia de CORS y empaquetado bajo el mismo origen.

## Contrato HTTP

`api/openapi/running-coach.yaml` es la fuente de verdad del contrato OpenAPI 3.1. La API pública se reserva bajo `/api`, hereda autenticación por sesión opaca y exige además CSRF solo en operaciones protegidas que modifican estado. Todavía declara `paths: {}`: no hay endpoints de negocio ni de salud dentro de la API pública.

```powershell
.\gradlew.bat apiCheck                  # Valida el contrato, genera servidor y cliente y ejecuta sus controles.
.\gradlew.bat validateOpenApi           # Valida la especificación OpenAPI.
.\gradlew.bat generateOpenApiServer     # Genera interfaces y modelos Spring MVC bajo build/.
.\gradlew.bat generateOpenApiClient     # Genera el cliente TypeScript bajo build/.
```

La generación no se versiona. Los tipos de servidor solo podrán consumirse desde `adapter.http`; los tipos generados no son contratos entre módulos ni tipos de dominio. Spectral bloquea las infracciones automatizables de ADR-0017, entre ellas errores sin Problem Details, escrituras protegidas sin CSRF, colecciones sin paginación acotada, objetos abiertos y ejemplos con secretos plausibles. `apiCheck` requiere Docker para comparar compatibilidad con `main` mediante `oasdiff`.

## Calidad y seguridad de suministro

`check` es el gate local de compilación, formato AOSP, Error Prone, NullAway, SpotBugs, pruebas, arquitectura, cobertura, contrato y frontend. El código generado por jOOQ y OpenAPI se compila en source sets técnicos aislados: debe compilar, pero no se formatea ni se mide como código mantenido por el equipo. La suite usa JUnit 6.0.3 alineado explícitamente mediante su BOM, porque Spring Boot 4.1.1 gestiona un launcher anterior incompatible con su propia versión de Jupiter.

```powershell
.\gradlew.bat check              # Gate local de calidad.
.\gradlew.bat fastGate           # Gate de PR: check, PIT condicional y Gitleaks.
.\gradlew.bat toolingGate        # Autopruebas de los propios gates de calidad y seguridad.
.\gradlew.bat verifyAiGovernance # Verifica Skills, guidance y límites de autoridad de la IA.
.\gradlew.bat qualityGate        # Gate integral: fastGate, toolingGate, imagen, SBOM, Trivy y reproducibilidad.
.\gradlew.bat buildOciImage      # Construye la imagen linux/amd64 local.
.\gradlew.bat generateSbom       # Genera el SBOM SPDX JSON bajo build/reports/security.
.\gradlew.bat trivy              # Bloquea vulnerabilidades CRITICAL sin excepción temporal documentada.
```

Los mínimos de cobertura global son 80 % de líneas y 70 % de ramas; para paquetes futuros `domain` y `application` son 90 % y 80 %. PIT exige 70 % cuando exista código crítico. Hasta entonces, los gates críticos se registran como no aplicables. Gitleaks, Trivy y los casos negativos se ejecutan con credenciales sintéticas bajo `build/`, nunca versionadas. La imagen canónica se publicará desde `main` en GHCR identificada por commit y digest; esta tarea no despliega infraestructura.

## Gobierno de la IA

La asistencia de IA coordina el trabajo y prepara evidencia, pero no sustituye gates ejecutables ni autoridad humana. La Skill local `$implementar-slice` exige una Definition of Ready completa, validación incremental, una única ejecución final de `qualityGate` y una PR borrador; nunca autoriza fusionar. Consulta el [gobierno operativo de la IA](docs/ai-governance.md).

## Documentación

- [Enunciado del problema — Fase 0](docs/phase-0-problem-statement.md)
- [Requisitos funcionales y no funcionales — Fase 1](docs/phase-1-requirements.md)
- [Criterios de aceptación — Fase 1](docs/phase-1-acceptance-criteria.md)
- [Matriz de decisiones — Fase 1](docs/phase-1-decision-matrix.md)
- [Arranque de arquitectura — Fase 2](docs/phase-2-architecture-kickoff.md)
- [Diseño funcional y técnico de alto nivel — Fase 2](docs/phase-2-high-level-design.md)
- [Diseño detallado de identidad y acceso — Fase 2](docs/phase-2-detailed-design-identity-access.md)
- [Diseño detallado de gestión de corredores — Fase 2](docs/phase-2-detailed-design-runner-management.md)
- [Diseño detallado de clasificación y segmentación — Fase 2](docs/phase-2-detailed-design-classification-segmentation.md)
- [Diseño detallado de planificación — Fase 2](docs/phase-2-detailed-design-planning.md)
- [Diseño detallado de publicación — Fase 2](docs/phase-2-detailed-design-publication.md)
- [Diseño detallado de entrega de notificaciones — Fase 2](docs/phase-2-detailed-design-notification-delivery.md)
- [Diseño detallado de seguimiento y revisión — Fase 2](docs/phase-2-detailed-design-tracking-review.md)
- [Diseño detallado del portal del corredor — Fase 2](docs/phase-2-detailed-design-runner-portal.md)
- [Línea base de acceso y seguridad — Fase 2](docs/phase-2-access-security-baseline.md)
- [Cierre documental — Fase 2](docs/phase-2-closure.md)
- [Especificación UX-01 del portal del corredor](docs/ux-01-runner-portal-specification.md)
- [Plan de prueba de usabilidad UX-01](docs/ux-01-usability-test-plan.md)
- [Recorridos cognitivos sintéticos UX-01](docs/ux-01-cognitive-walkthroughs/README.md)
- [Guía de diseño de API HTTP](docs/api-design-guidelines.md)
- [Architecture Decision Records](docs/adr/README.md)
- [Mejoras futuras](docs/future-improvements.md)
- [Controles de calidad documental](docs/documentation-quality-gates.md)
- [Guía del entorno local](docs/local-development.md)
- [Gobierno operativo de la IA](docs/ai-governance.md)
