# Architecture Decision Records

**Estado:** Vigente
**Fecha:** 2026-08-24

## Propósito

Registrar las decisiones técnicas de arquitectura que condicionan implementación, modelo de datos, permisos, integraciones, despliegue, privacidad o pruebas del PMV.

Los ADRs no sustituyen los documentos de diseño de Fase 2. Cada ADR debe enlazarse desde el documento de diseño que lo necesite y desde la trazabilidad del requisito afectado.

## Convención

- Los ADRs viven en `docs/adr/`.
- El nombre sigue el formato `NNNN-titulo-en-kebab-case.md`.
- El identificador visible sigue el formato `ADR-NNNN`.
- Los estados permitidos son `Propuesto`, `Aceptado`, `Reemplazado` y `Descartado`.
- Un ADR aceptado solo se modifica para corregir errores menores, añadir enlaces o registrar que ha sido reemplazado. Un cambio de decisión requiere un ADR nuevo.
- Un refinamiento parcial conserva ambos ADR en estado `Aceptado`. El ADR posterior declara `Refina parcialmente`, el anterior declara `Refinado parcialmente por` y el índice registra el alcance exacto de la relación.
- El responsable de revisión por defecto es el Revisor de arquitectura. En el flujo actual de único mantenedor, el autor asume ese rol y registra la aceptación del riesgo en la PR.

## Cuándo crear un ADR

Crea un ADR durante Fase 2 cuando una decisión:

- afecte a varios requisitos `RF-01` a `RF-21`;
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
| [ADR-0021](0021-publication-editing-notification-eligibility.md) | Edición de publicaciones y elegibilidad de notificaciones | Aceptado | `RF-07`, `RF-09`, `RF-10`, `RF-14` a `RF-16`, `RF-20` |
| [ADR-0022](0022-five-point-perceived-effort-scale.md) | Escala de cinco puntos para esfuerzo percibido | Aceptado | `RF-17`, `RF-18`, `RF-19` |
| [ADR-0023](0023-recovery-objectives-independent-key-custody.md) | Objetivos de recuperación y custodia independiente de claves | Aceptado | Todos los `RF`; disponibilidad, seguridad, datos y privacidad |
| [ADR-0024](0024-hybrid-validation-ai-authority.md) | Estrategia híbrida de validación y autoridad de la IA | Propuesto | Todos los `RF`, indirectamente mediante los controles de implementación |

## Relaciones de refinamiento

| Decisión base | Decisión posterior | Alcance de la relación |
| --- | --- | --- |
| `ADR-0006` | `ADR-0020` | Concreta el ciclo de vida de grupos y planes y permite preparar grupos inactivos sin segmentos; la pertenencia exclusiva y el modelo semanal de `ADR-0006` siguen vigentes. |
| `ADR-0007` | `ADR-0021` | Sustituye el borrador persistente y los cambios pendientes posteriores a publicar por una sesión local y una republicación atómica; instantáneas y destinatarios congelados siguen vigentes. |
| `ADR-0008` | `ADR-0021` | Separa miembro efectivo de miembro elegible para envío e introduce `omitido-inactivo`; la creación transaccional de una solicitud por versión y miembro sigue vigente. |
| `ADR-0009` | `ADR-0022` | Reemplaza exclusivamente el intervalo de esfuerzo `1..10` por el catálogo `1..5`; identidad, ventana, campos, historial y revisión de `ADR-0009` siguen vigentes. |
| `ADR-0011` | `ADR-0021` | Para publicaciones, fija el correo al resolver por primera vez `active(currentVerifiedEmail)` e incorpora `omitido-inactivo` y el límite de elegibilidad; el resto de entrega sigue vigente. |
| `ADR-0014` | `ADR-0021` | Introduce el puerto de elegibilidad sin invertir dependencias entre módulos; el mapa modular y la propiedad de datos siguen vigentes. |
| `ADR-0016` | `ADR-0023` | Reemplaza los objetivos de recuperación indiferenciados, la custodia de la clave privada y la cadencia de simulacros; plataforma, copia diaria, retención y portabilidad restantes siguen vigentes. |
| `ADR-0018` | `ADR-0021` | Precisa la conservación histórica en publicaciones y la elegibilidad vigente antes del correo; el ciclo de vida y la retención de `ADR-0018` siguen vigentes. |
| `ADR-0020` | `ADR-0021` | Sustituye para planes publicados la mutabilidad de identidad, el borrador persistente, los cambios pendientes, la restauración y el historial consultable; el resto de planificación sigue vigente. |
| `ADR-0013` | `ADR-0024` | Propuesta de refinamiento de la estrategia de ejecución y autoridad de validación; conserva sus herramientas, umbrales y gates hasta una aceptación formal. |

## Resultado del backlog inicial de Fase 2

El backlog inicial y la auditoría H-01 a H-20 se materializan en `ADR-0001` a `ADR-0023`, todos aceptados. `ADR-0024` es una propuesta posterior para evolucionar la validación técnica y no modifica esas decisiones mientras no sea aceptada. Una decisión arquitectónica nueva o contradictoria deberá abrir otro ADR a partir de evidencia; no se resolverá implícitamente durante la implementación.
