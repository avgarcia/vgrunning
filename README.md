# Running Coach

Repositorio de descubrimiento, diseño y preparación técnica del PMV de Running Coach.

La preparación técnica ya incluye el esqueleto ejecutable Spring Boot, ocho módulos lógicos verificados y persistencia técnica reproducible sobre PostgreSQL 18. No hay todavía funcionalidad ni tablas de negocio.

## Base ejecutable

El backend es una única aplicación Spring MVC. Requiere PostgreSQL local antes de arrancar. Todas las rutas de aplicación quedan cerradas hasta que se implemente `identity-access`; las únicas rutas abiertas son los probes técnicos:

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

## Contrato HTTP

`api/openapi/running-coach.yaml` es la fuente de verdad del contrato OpenAPI 3.1. La API pública se reserva bajo `/api`, hereda autenticación por sesión opaca y exige además CSRF solo en operaciones protegidas que modifican estado. Todavía declara `paths: {}`: no hay endpoints de negocio ni de salud dentro de la API pública.

```powershell
.\gradlew.bat apiCheck                  # Valida el contrato, genera servidor y cliente y ejecuta sus controles.
.\gradlew.bat validateOpenApi           # Valida la especificación OpenAPI.
.\gradlew.bat generateOpenApiServer     # Genera interfaces y modelos Spring MVC bajo build/.
.\gradlew.bat generateOpenApiClient     # Genera el cliente TypeScript bajo build/.
```

La generación no se versiona. Los tipos de servidor solo podrán consumirse desde `adapter.http`; los tipos generados no son contratos entre módulos ni tipos de dominio. Spectral bloquea las infracciones automatizables de ADR-0017, entre ellas errores sin Problem Details, escrituras protegidas sin CSRF, colecciones sin paginación acotada, objetos abiertos y ejemplos con secretos plausibles. `apiCheck` requiere Docker para comparar compatibilidad con `main` mediante `oasdiff`.

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
- [Guía de diseño de API HTTP](docs/api-design-guidelines.md)
- [Architecture Decision Records](docs/adr/README.md)
- [Mejoras futuras](docs/future-improvements.md)
- [Controles de calidad documental](docs/documentation-quality-gates.md)
