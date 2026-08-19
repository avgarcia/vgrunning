# Architecture Decision Records

**Estado:** Vigente
**Fecha:** 2026-08-15

## Propósito

Registrar las decisiones técnicas de arquitectura que condicionan implementación, modelo de datos, permisos, integraciones, despliegue, privacidad o pruebas del PMV.

Los ADRs no sustituyen los documentos de diseño de Fase 2. Cada ADR debe enlazarse desde el documento de diseño que lo necesite y desde la trazabilidad del requisito afectado.

## Convención

- Los ADRs viven en `docs/adr/`.
- El nombre sigue el formato `NNNN-titulo-en-kebab-case.md`.
- El identificador visible sigue el formato `ADR-NNNN`.
- Los estados permitidos son `Propuesto`, `Aceptado`, `Reemplazado` y `Descartado`.
- Un ADR aceptado solo se modifica para corregir errores menores, añadir enlaces o registrar que ha sido reemplazado. Un cambio de decisión requiere un ADR nuevo.
- El responsable de revisión por defecto es el Revisor de arquitectura. En el flujo actual de único mantenedor, el autor asume ese rol y registra la aceptación del riesgo en la PR.

## Cuándo crear un ADR

Crea un ADR durante Fase 2 cuando una decisión:

- afecte a varios requisitos `RF-01` a `RF-20`;
- condicione el modelo de datos, permisos, transacciones o integraciones;
- cierre una alternativa técnica relevante;
- cambie alcance, riesgo, coste operativo o estrategia de pruebas;
- sea necesaria para evitar que la implementación resuelva arquitectura de forma implícita.

No crees un ADR para decisiones locales de interfaz, nombres internos, detalles mecánicos reversibles o preferencias sin impacto arquitectónico.

## ADRs

| ID | Título | Estado | Requisitos relacionados |
| --- | --- | --- | --- |
| [ADR-0001](0001-record-architecture-decisions.md) | Registrar decisiones de arquitectura mediante ADRs | Aceptado | Todos los `RF` |
| [ADR-0002](0002-architecture-single-club.md) | Arquitectura general del PMV y límites single-club | Aceptado | Todos los `RF`; especialmente `RF-02`, `RF-18`, `RF-19` |
| [ADR-0003](0003-identity-authentication-invitation.md) | Identidad, autenticación, invitación y recuperación de acceso | Aceptado | `RF-01`, `RF-02`, `RF-16`, `RF-18`, `RF-19` |
| [ADR-0004](0004-role-authorization-runner-isolation.md) | Autorización por roles y aislamiento de datos del corredor | Aceptado | `RF-02`, `RF-03`, `RF-05` a `RF-09`, `RF-14`, `RF-16` a `RF-19` |
| [ADR-0005](0005-controlled-taxonomies-dynamic-segments.md) | Taxonomías controladas y segmentos dinámicos | Aceptado | `RF-02` a `RF-06`, `RF-08`, `RF-09`, `RF-10` |
| [ADR-0006](0006-weekly-plan-training-model.md) | Modelo de grupos de planificación, planes semanales y entrenamientos | Aceptado | `RF-04`, `RF-07`, `RF-08`, `RF-11` a `RF-14`, `RF-16` |
| [ADR-0007](0007-atomic-publication-versioning-recipients.md) | Publicación atómica, versionado y destinatarios efectivos | Aceptado | `RF-08` a `RF-10`, `RF-14` a `RF-16`, `RF-20` |
| [ADR-0008](0008-transactional-publication-notifications.md) | Solicitud transaccional de notificaciones de publicación | Aceptado | `RF-15`, `RF-20` |
| [ADR-0009](0009-training-feedback-history-review.md) | Seguimiento por entrenamiento, historial y revisión | Aceptado | `RF-17`, `RF-18`, `RF-19` |
| [ADR-0010](0010-privacy-retention-rights-readiness.md) | Preparación para privacidad, retención y derechos | Aceptado | Todos los `RF`; requisito no funcional de datos y privacidad |
| [ADR-0011](0011-transactional-email-delivery-strategy.md) | Entrega de correo transaccional | Aceptado | `RF-01`, `RF-15`, `RF-20` |
| [ADR-0012](0012-relational-persistence-transaction-strategy.md) | Persistencia relacional y estrategia transaccional | Aceptado | Todos los `RF`; especialmente `RF-03`, `RF-05` a `RF-10`, `RF-14`, `RF-15` y `RF-17` a `RF-20` |
| [ADR-0013](0013-application-runtime-framework.md) | Runtime imperativo, framework de aplicación y contrato API | Aceptado | Todos los `RF` |
| [ADR-0014](0014-modular-hexagonal-ddd-architecture.md) | Arquitectura modular, hexagonal y DDD selectivo | Aceptado | Todos los `RF` |
| [ADR-0015](0015-application-authorization-enforcement.md) | Aplicación de autorización y alcance por recurso | Aceptado | `RF-02`, `RF-03`, `RF-05` a `RF-09`, `RF-14` y `RF-16` a `RF-19` |
| [ADR-0016](0016-deployment-platform-operations.md) | Plataforma de despliegue y operación | Aceptado | Todos los `RF`; requisito no funcional de datos y privacidad |
| [ADR-0017](0017-resource-oriented-http-api.md) | API HTTP orientada a recursos y semántica REST | Aceptado | Todos los `RF` expuestos mediante HTTP |
| [ADR-0018](0018-runner-lifecycle-inactivity-reactivation.md) | Ciclo de vida, inactividad y reactivación del corredor | Aceptado | `RF-01`, `RF-02`, `RF-03`, `RF-16` a `RF-19`; datos y privacidad |
| [ADR-0019](0019-classification-coordination-lifecycle-history.md) | Coordinación, ciclo de vida e historial de clasificación | Aceptado | `RF-02` a `RF-06`, `RF-08` a `RF-10`; datos y privacidad |
| [ADR-0020](0020-planning-lifecycle-objectives-history.md) | Ciclo de vida, objetivos e historial de planificación | Aceptado | `RF-04`, `RF-07` a `RF-16`; especialmente `RF-08`, `RF-12`, `RF-14`; datos y privacidad |

## Backlog inicial de Fase 2

Estos ADRs candidatos deben confirmarse, dividirse o descartarse durante el diseño de Fase 2:

| Candidato | Decisión pendiente | Requisitos relacionados | Tratamiento |
| --- | --- | --- | --- |
