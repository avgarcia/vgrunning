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
- Un ADR aceptado solo se modifica para corregir errores menores, añadir enlaces o registrar que ha sido reemplazado. Un cambio de decisión requiere un ADR nuevo — esto incluye actualizar en el sitio un árbol de paquetes, una tabla de estados o cualquier otro contenido que un ADR posterior refine: el ADR base conserva lo que decía cuando se aceptó, y el ADR posterior es la única fuente del estado vigente. (Precedente corregido: ver nota de refinamiento en `ADR-0014`.)
- Un refinamiento parcial conserva ambos ADR en estado `Aceptado`. El ADR posterior declara `Refina parcialmente`, el anterior declara `Refinado parcialmente por` y el índice registra el alcance exacto de la relación.
- El responsable de revisión por defecto es el Revisor de arquitectura. En el flujo actual de único mantenedor, el autor asume ese rol y registra la aceptación del riesgo en la PR.
- `Fecha` es la fecha de propuesta y, salvo que se indique lo contrario, también la de aceptación. `Fecha de aceptación` solo se añade cuando la aceptación ocurre en una fecha distinta; no es obligatoria si coincide con `Fecha`. `Validación documental` es un campo narrativo adicional, no sustitutivo, para cuando la aceptación lleva condiciones, evidencias o caveats que una fecha sola no transmite (por ejemplo, `ADR-0010`, con evidencias jurídicas pendientes).

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
| [ADR-0024](0024-hybrid-validation-ai-authority.md) | Estrategia híbrida de validación y autoridad de la IA | Aceptado | Todos los `RF`, indirectamente mediante los controles de implementación |
| [ADR-0025](0025-spring-session-jdbc-local-login-rate-limit.md) | Spring Session JDBC y límite local de intentos de acceso | Aceptado | `RF-01`, `RF-02`, `RF-16`, `RF-18`, `RF-19` |
| [ADR-0026](0026-hexagonal-packaging-under-infrastructure.md) | Paquetería hexagonal bajo infraestructura | Aceptado | Todos los `RF` |
| [ADR-0027](0027-login-attempt-consumption-policy.md) | Consumo de todos los intentos de inicio de sesión | Aceptado | `RF-01`, `RF-02`, `RF-16`, `RF-18`, `RF-19` |
| [ADR-0028](0028-mapstruct-mapping-boundaries.md) | MapStruct en las fronteras de representaciones | Aceptado | Todos los `RF` implementados mediante contratos HTTP o persistencia |
| ADR-0029 | *(número quemado)* CQRS para queries de solo lectura | Descartado | — Un borrador se descartó antes de mergear; su regla útil se incorpora como refinamiento en `ADR-0030`. El número no se reutiliza para evitar confundir dos decisiones distintas si se consulta una rama que aún lo referencie. |
| [ADR-0030](0030-publication-jsonb-snapshot-model.md) | Instantáneas de publicación en JSONB y modelo relacional mínimo | Aceptado | `RF-08` a `RF-10`, `RF-14` a `RF-16`, `RF-20`, `RF-21` |
| [ADR-0031](0031-notification-delivery-provider-delegation.md) | Delegación de supresión, eventos y operación en el proveedor de correo | Aceptado | `RF-01`, `RF-15`, `RF-20` |

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
| `ADR-0013` | `ADR-0024` | Refinamiento de la estrategia de ejecución y autoridad de validación; conserva sus herramientas, umbrales y gates. |
| `ADR-0003` | `ADR-0025` | Sustituye el token opaco y verificador propios por una sesión HTTP gestionada por Spring Session JDBC; conserva identidad local, credenciales e invitación. |
| `ADR-0013` | `ADR-0025` | Sustituye el repositorio propio de sesiones por Spring Session JDBC y concreta Bucket4j local para la topología inicial de un nodo. |
| `ADR-0015` | `ADR-0025` | Sustituye el `SecurityContextRepository` propio por la integración de Spring Security y Spring Session; conserva `ActorContext` y autorización por recurso. |
| `ADR-0014` | `ADR-0026` | Reúne adaptadores de entrada y salida bajo infraestructura, reserva `application.port` para interfaces, mantiene `api` como contrato intermodular y elimina `application.model`. |
| `ADR-0025` | `ADR-0027` | Sustituye el conteo de fallos por el consumo de todos los intentos válidos de login en ambos buckets locales. |
| `ADR-0026` | `ADR-0028` | Añade `application.mapper` para MapStruct puro y obliga a usar MapStruct en conversiones entre representaciones. |
| `ADR-0007` | `ADR-0030` | Sustituye el modelo relacional completo de instantáneas (9 tablas) por 3 tablas más un documento `JSONB` para el contenido congelado; la congelación de destinatarios en la primera publicación y la inmutabilidad de versiones siguen vigentes. |
| `ADR-0012` | `ADR-0030` | Concreta qué invariantes de `publication` se expresan como restricción física (unicidad de plan, versión, destinatario) y cuáles pasan a validación de código; la estrategia transaccional general sigue vigente. |
| `ADR-0021` | `ADR-0030` | El `ETag` deja de ser una columna de revisión propia y pasa a derivarse de `active_version_number`, resuelto mediante una sentencia CAS; miembro efectivo, miembro elegible y `omitido-inactivo` siguen vigentes. |
| `ADR-0010` | `ADR-0031` | Retira el tratamiento de la huella HMAC de supresión y su bloqueante de producción; la supresión pasa a ser responsabilidad del proveedor de correo como encargado del tratamiento. |
| `ADR-0011` | `ADR-0031` | Sustituye supresión local, inbox de eventos, ledger de transiciones, pausa persistida y cuatro calendarios de reintento por delegación en el proveedor, proyección directa, circuit breaker en memoria y backoff único; la creación transaccional, prioridad y orden siguen vigentes. |
| `ADR-0012` | `ADR-0031` | Retira de `notification-delivery` la coordinación de inbox y ledger; lease y `SKIP LOCKED` siguen vigentes. |
| `ADR-0016` | `ADR-0031` | Reduce el alcance de Key Vault en este módulo a la clave del payload, la API key de Brevo y el Bearer del webhook; retira la rotación de clave HMAC. |

## Bloqueantes activos para producción

Consolida los 18 bloqueantes declarados en «Decisiones pendientes» de los 6 ADRs que los tienen, para no tener que releerlos completos. Cada fila remite a su ADR de origen (enlazado en el índice superior); esta tabla no sustituye su texto completo, solo evita perder la vista de conjunto.

| ADR | Bloqueante | Alcance | Responsable |
| --- | --- | --- | --- |
| `ADR-0010` | Documentar identidad y contacto del responsable; adquirir dominio y crear `privacidad@` | Producción | Responsable del tratamiento |
| `ADR-0010` | Revisión especializada de bases jurídicas, consentimiento explícito, interés legítimo y plazos | Producción | Responsable del tratamiento con asesoramiento de privacidad |
| `ADR-0010` | Inventariar y aprobar DPA/subencargados/regiones/transferencias de Azure, GHCR, Scaleway, Grafana Cloud, Brevo y buzón | Producción | Responsable del tratamiento y Revisor de arquitectura |
| `ADR-0010` | Análisis de riesgos y EIPD aprobada con stack, escala y proveedores reales | Producción | Responsable del tratamiento con asesoramiento de privacidad o DPO |
| `ADR-0010` | Aprobar información de privacidad, RAT, procedimiento de derechos, automatización de retención/bloqueo/destrucción, brechas, medidas y simulacros | Producción | Responsable del tratamiento |
| `ADR-0011` | Adquirir y controlar dominio; definir y autenticar remitente con Brevo | Producción | Propietario del servicio |
| `ADR-0011` | Aprobar Brevo como encargado (DPA, subencargados, ubicaciones, retención, transferencias) | Producción | Responsable del tratamiento con asesoramiento de privacidad |
| `ADR-0016` | Adquirir dominio, configurar DNS y completar TLS | Producción | Propietario del servicio |
| `ADR-0016` | Aprobar Azure, GHCR, Scaleway y Grafana Cloud como encargados/subencargados | Producción | Responsable del tratamiento |
| `ADR-0016` | Escribir y probar runbooks de despliegue, rollback, restauración, rotación, incidentes, saturación, caída y salida de proveedor | Producción | Persona operadora y Revisor de arquitectura |
| `ADR-0018` | Confirmar base jurídica y proporcionalidad de conservar cuenta/perfil/clasificación 24 meses tras finalizar la relación | Datos personales reales y producción | Responsable del tratamiento con Revisor de privacidad o DPO |
| `ADR-0018` | Usar solo datos ficticios/sintéticos/anonimizados mientras el punto anterior no se resuelva | Desarrollo y pruebas (restricción vigente) | Revisor de arquitectura |
| `ADR-0023` | Designar nominalmente a la persona custodio y documentar aceptación, sustitución y acceso de emergencia | Producción | Propietario del servicio |
| `ADR-0023` | Seleccionar y versionar herramienta de cifrado híbrido, formato de sobre y comandos de recuperación | Producción | Revisor de arquitectura |
| `ADR-0023` | Crear identidades separadas de Azure y Scaleway, MFA, Object Lock, retención y runbooks | Producción | Persona operadora |
| `ADR-0023` | Ejecutar con éxito la primera restauración externa completa y corregir desviaciones | Producción | Persona operadora y Revisor de arquitectura |
| `ADR-0025` | Concretar y probar la invalidación de todas las sesiones de una cuenta mediante Spring Session | Recuperación, cambio de contraseña y desactivación | Revisor de arquitectura |
| `ADR-0025` | Definir proxies confiables y su configuración antes de interpretar `Forwarded`/`X-Forwarded-For` | Confiar en cabeceras de origen | Responsable de plataforma |

Ecos que remiten a estos mismos bloqueantes sin añadir uno nuevo: `ADR-0004:128`, `ADR-0009:132` y `ADR-0015:130` (→ `ADR-0010`); `ADR-0011:191` y `ADR-0013:159` (→ `ADR-0016`).

Bloqueantes adicionales, declarados en los diseños detallados de Fase 2 (no en ADRs): la tabla de `phase-2-detailed-design-notification-delivery.md` («Decisiones pendientes») y los bullets de la misma sección en `phase-2-detailed-design-publication.md`.

## Resultado del backlog inicial de Fase 2

El backlog inicial y la auditoría H-01 a H-20 se materializan en `ADR-0001` a `ADR-0023`, todos aceptados. `ADR-0024` a `ADR-0028` son decisiones posteriores aceptadas que refinan implementación, sesión, paquetería y mapeo sin sustituir los umbrales, herramientas ni el mapa modular de sus ADR base. `ADR-0029` fue descartado antes de mergear y su número no se reutiliza. `ADR-0030` y `ADR-0031` refinan el modelo persistente de `publication` y `notification-delivery` antes de su implementación, aprovechando que ninguno de los dos módulos tiene código ni migraciones propias más allá de una tabla. Una decisión arquitectónica nueva o contradictoria deberá abrir otro ADR a partir de evidencia; no se resolverá implícitamente durante la implementación.
