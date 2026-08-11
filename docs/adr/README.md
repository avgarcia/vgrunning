# Architecture Decision Records

**Estado:** Vigente
**Fecha:** 2026-08-10

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
| [ADR-0004](0004-role-authorization-runner-isolation.md) | Autorización por roles y aislamiento de datos del corredor | Propuesto | `RF-02`, `RF-03`, `RF-05` a `RF-09`, `RF-14`, `RF-16` a `RF-19` |

## Backlog inicial de Fase 2

Estos ADRs candidatos deben confirmarse, dividirse o descartarse durante el diseño de Fase 2:

| Candidato | Decisión pendiente | Requisitos relacionados | Tratamiento |
| --- | --- | --- | --- |
| ADR-0005 | Modelo de taxonomías, etiquetas, modalidad y reglas de segmentos | `RF-02`, `RF-03`, `RF-04`, `RF-05`, `RF-06`, `RF-08` | Resolver antes de cerrar segmentación. |
| ADR-0006 | Modelo de plan semanal, entrenamientos, catálogo y objetivos | `RF-07`, `RF-11`, `RF-12`, `RF-13`, `RF-14`, `RF-16` | Resolver antes de cerrar planificación semanal. |
| ADR-0007 | Publicación atómica, versionado y destinatarios efectivos | `RF-08`, `RF-09`, `RF-10`, `RF-14`, `RF-15` | Resolver antes de cerrar publicación. |
| ADR-0008 | Republicación y envío de correo a destinatarios afectados | `RF-15`, `RF-20` | Resolver antes de cerrar notificaciones. |
| ADR-0009 | Modelo de seguimiento, historial y revisión por entrenadores | `RF-17`, `RF-18`, `RF-19` | Resolver antes de cerrar seguimiento. |
| ADR-0010 | Estrategia mínima de privacidad, retención y derechos antes de producción | Requisito no funcional de datos; `RF-17`, `RF-18`, `RF-19` | Resolver antes de salida a producción; no bloquea el arranque de Fase 2 salvo que cambie alcance o datos. |
| ADR-0011 | Estrategia de correo transaccional para acceso y publicación | `RF-01`, `RF-15`, `RF-20` | Resolver antes de implementar cualquier correo del PMV. |
