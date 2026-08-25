# Running Coach

Repositorio de descubrimiento, diseño y preparación técnica del PMV de Running Coach.

La preparación técnica ya incluye el esqueleto ejecutable Spring Boot, con ocho módulos lógicos verificados y sin funcionalidad de negocio ni persistencia.

## Base ejecutable

El backend es una única aplicación Spring MVC. Todas las rutas de aplicación quedan cerradas hasta que se implemente `identity-access`; las únicas rutas abiertas son los probes técnicos:

- `http://localhost:8081/actuator/health/liveness`
- `http://localhost:8081/actuator/health/readiness`

En Windows:

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
.\gradlew.bat verifyRuntimeStack
```

En macOS, Linux o Git Bash, sustituye `.\gradlew.bat` por `./gradlew`.

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
