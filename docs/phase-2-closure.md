# Cierre documental — Fase 2

**Estado:** Validado — diseño cerrado; únicamente autorizada la preparación técnica con datos sintéticos
**Fecha:** 2026-08-24
**Responsables de revisión:** Revisor de arquitectura y Revisor de producto
**Evidencia de autorrevisión:** Registrada en la PR por la persona mantenedora que actúa como autora y revisora, incluida la ausencia de revisión independiente y la aceptación expresa de ese riesgo
**Alcance de autorización:** Se autoriza únicamente la preparación técnica con datos sintéticos; no se autorizan funcionalidades de negocio, tratamiento de datos personales reales, proveedor de correo ni producción

Los documentos normativos conservan responsables por rol para no vincular decisiones duraderas a una persona concreta. La evidencia de cada cambio identifica en la PR a la persona que ejerció esos roles; mantener solo roles anónimos o duplicar nombres personales en cada ADR queda descartado.

## Alcance de la revisión

Esta revisión comprueba que Fase 2 materializa los requisitos imprescindibles `RF-01` a `RF-21`, las decisiones `D-01` a `D-11`, los riesgos activos y los límites del PMV mediante diseños detallados y `ADR-0001` a `ADR-0023`.

Se mantienen los ocho módulos de `ADR-0014`: `identity-access`, `runner-management`, `classification-segmentation`, `planning`, `publication`, `notification-delivery`, `tracking-review` y `runner-portal`. La cobertura semanal se coordina desde `publication`; no crea un noveno módulo, una proyección persistente ni un buscador global.

## Resultado validado

- Los ocho módulos tienen propietario, dependencias, datos, autorización, API prevista y validación documentados.
- Cada requisito `RF-01` a `RF-21` tiene dos criterios de aceptación con identificador estable y una fila única en la [trazabilidad de requisitos](phase-2-high-level-design.md#trazabilidad-de-requisitos).
- Cada decisión `D-01` a `D-11` tiene tratamiento en la [trazabilidad de decisiones](phase-2-high-level-design.md#trazabilidad-de-decisiones-de-fase-1).
- Los `23` ADR están aceptados y sus nueve relaciones parciales están registradas simétricamente en ambos documentos y en el índice.
- Las decisiones de producto y arquitectura examinadas tienen tratamiento explícito. Los artefactos contract-first, las evidencias de privacidad, la restauración externa y la operación real siguen pendientes.
- El estado `Validado` se apoya en las puertas documentales repetidas sobre el cambio final y en la autorrevisión y aceptación del riesgo registradas en la PR; cualquier modificación material posterior deberá reabrir la revisión afectada.

## Decisiones incorporadas durante la auditoría

1. Las solicitudes de publicación nacen sin correo. En su primer procesamiento se resuelven conjuntamente actividad y correo verificado vigente; el destino se fija para todos sus intentos y cambios posteriores solo afectan a solicitudes futuras.
2. `retry-later` de elegibilidad aplica backoff de `5` segundos a `5` minutos y termina a creación `+120` minutos como `fallo-definitivo/elegibilidad-no-resuelta`, con alerta y liberación del orden. Una reconciliación ya iniciada conserva su ventana sin nuevo envío.
3. Los eventos internos durables usan `EventPublicationRegistry` JDBC de Spring Modulith, tablas gestionadas por Flyway, consumidores idempotentes, reintento de publicaciones incompletas y alerta por antigüedad. No se introduce broker.
4. Tras restaurar, se recalculan vencimientos y se reaplican primero las supresiones del registro externo de privacidad; solo después se reanudan consumidores y trabajo incompleto.
5. Las excepciones de grupo cambian membresía, no contenido. Todos los miembros reciben el mismo plan; la personalización individual se aplaza a `MF-006`.
6. `RF-21` y `D-09` incorporan cobertura semanal informativa para todos los corredores `active`: `cubierto`, `sin-grupo`, `grupo-sin-plan`, `plan-en-borrador` o `fuera-de-publicacion`, con `sin-modalidad` independiente.
7. Los secretos de activación, reactivación, recuperación y verificación de correo viajan en fragmento HTTPS, se eliminan antes de renderizar o cargar recursos y permanecen solo en memoria hasta enviarlos en el cuerpo HTTPS.
8. El entrenador opera globalmente solo sobre corredores `active`. Pendientes, inactivos y cancelados quedan ocultos en listas, detalles, conteos y seguimiento; el administrador conserva acceso auditado.
9. `ADR-0009` permanece aceptado y `ADR-0022` reemplaza únicamente su intervalo. La escala única es `1 Muy suave`, `2 Suave`, `3 Moderado`, `4 Intenso`, `5 Muy intenso`, con la pregunta «¿Cuánto esfuerzo te supuso este entrenamiento?» y sin valor por defecto.
10. La edición publicada se conserva sin retirada. Se acepta que errores de grupo, semana, miembros, hoy o pasado no puedan corregirse retroactivamente ni cancelar correos pendientes o en vuelo.
11. El PMV admite únicamente adultos. El administrador declara la mayoría de edad al invitar y la persona la confirma en la activación inicial; se conserva evidencia mínima, no fecha de nacimiento, documento o copia acreditativa.
12. Modalidad de corredor y de entrenamiento son informativas e independientes. Mezclarlas no bloquea, excluye, advierte ni exige confirmación. La ubicación es opcional e informativa y, cuando existe, pertenece a un entrenamiento presencial.
13. La revisión de seguimiento significa consultar y analizar. No existe estado revisado, titularidad, prioridad, nota, respuesta, SLA ni prueba de lectura; se acepta que haya consultas duplicadas u omisiones humanas.
14. `RF-01` separa aceptación durable de la solicitud, aceptación del proveedor y entrega al servidor receptor. No promete entrega física ni lectura; la entrega se verifica con un buzón controlado.
15. `RF-15` enumera los cambios canónicos visibles futuros que exigen republicación y excluye no-op, metadatos técnicos, hoy y pasado.
16. Todo el PMV web debe cumplir WCAG `2.2 AA`, incluidas pruebas a `320` CSS px, zoom `400 %`, texto `200 %`, teclado, foco, errores y objetivos de puntero de `24 × 24` CSS px o excepción oficial documentada.
17. `ADR-0023` fija recuperación por escenario: PITR Azure `RPO <= 15 min` y `RTO <= 4 h`; pérdida total de Azure `RPO <= 24 h` y `RTO <= 24 h`; migración planificada con objetivo de pérdida cero y `RTO <= 4 h`. La clave privada y las credenciales externas se custodian fuera de Azure.
18. Los objetivos se muestran como «Zx según las zonas que utilizas con tu entrenador» o «ritmo de distancia +/− segundos por km, usando tu marca de referencia acordada con tu entrenador». No se almacenan, calculan o validan referencias personales; desconocerlas no bloquea y se resuelve fuera del producto.
19. Se normaliza el lenguaje a «miembro efectivo del segmento» y «entrenamiento publicado para el corredor».
20. La línea base de acceso y seguridad queda enlazada desde `README.md`; las fechas históricas no se reescriben de forma masiva.

## Trazabilidad desde Fase 0

| Riesgo, supuesto o límite | Tratamiento en Fase 2 | Trabajo posterior |
| --- | --- | --- |
| La personalización individual podía convertir los grupos en una distinción cosmética. | Segmentos, grupos y excepciones gobiernan membresía; el plan es común para el grupo. | Medir uso real; cualquier personalización parte de `MF-006` y exige revisar datos, UX y arquitectura. |
| El seguimiento era demasiado vago. | Estados, escala, ventana, historial y consulta de solo lectura son observables. | Validar usabilidad con datos sintéticos y completar privacidad. |
| Correo y WhatsApp podían actuar como sistema de gestión. | La aplicación es fuente de verdad; el correo solo avisa. WhatsApp queda fuera. | Probar proveedor, dominio, buzón controlado, supresión y operación. |
| El entrenador necesitaba visibilidad sin una bandeja de trabajo. | `RF-21` muestra cobertura semanal y `RF-19` permite consulta de seguimiento de activos, sin workflow. | Medir utilidad antes de añadir estados, SLA o asignaciones. |
| Una pérdida total de Azure podía inutilizar recuperación y claves. | `ADR-0023` separa backup, credenciales y clave privada del proveedor principal. | Completar restauración externa antes de producción y repetirla semestralmente. |
| El Retiro o la modalidad podían convertirse en restricciones accidentales. | Ubicación y modalidades son informativas; mezclar modalidades es válido. | Ninguno dentro del PMV. |

## Puertas de calidad documental

| Puerta | Evidencia esperada | Estado actual |
| --- | --- | --- |
| Trazabilidad entre fases | `RF-01..RF-21`, `D-01..D-11`, riesgos, ADR y mejoras futuras enlazados. | Autorrevisión del mantenedor único: `21` filas de requisitos, `11` de decisiones, `23` ADR indexados y enlaces locales válidos; evidencia reproducible en la PR. |
| Requisitos verificables | Actor, condición, comportamiento y resultado observable por requisito. | Autorrevisión completada sobre los `21` requisitos; evidencia en la PR. |
| Criterios de aceptación | Éxito y error o límite para los `21` requisitos imprescindibles. | Autorrevisión: `21/21` secciones contienen ambos escenarios y `42/42` identificadores son únicos; evidencia en la PR. |
| Terminología | Cuenta, corredor, segmento, grupo, miembro efectivo, publicación, seguimiento y revisión usados sin significados incompatibles. | Autorrevisión terminológica completada; no aparecen los dos términos obsoletos buscados; evidencia en la PR. |
| Decisiones de diseño | Alternativas, consecuencias, trazabilidad y validación de `ADR-0001..ADR-0023`. | Auditoría estricta: `23 ADR(s), 0 error(es), 0 aviso(s)`; nueve relaciones parciales simétricas y evidencia en la PR. |
| Preguntas bloqueantes | Decisiones no resueltas clasificadas por implementación, datos reales o producción, con tratamiento. | Autovalidación: sin decisión de producto o arquitectura implícita; artefactos y bloqueos posteriores conservan responsable y tratamiento. |
| Cambios de alcance | `RF-21`, `D-09..D-11`, `ADR-0023` y `MF-006` declarados; no se añade módulo. | Autorrevisión del diff completada; alcance y riesgo sin revisión independiente aceptados expresamente en la PR. |
| API HTTP | Recursos y semántica previstos; OpenAPI `3.1` todavía inexistente. | Bloquea cada slice antes de implementar su HTTP. |
| Privacidad y producción | Evidencias de `ADR-0010`, `ADR-0018`, proveedores, EIPD, restauración y runbooks. | No satisfecha; bloquea datos reales y producción. |

## Preparación técnica posterior

Durante la revisión puede ejecutarse únicamente la preparación técnica: proyecto Gradle, esqueleto Spring Boot y Spring Modulith, frontend React, pipeline, OpenAPI, Flyway, generación jOOQ y cliente, y fixtures sintéticos. Tras cerrar la fase se creará en Linear el backlog de funcionalidades verticales y podrá comenzar su implementación de negocio.

Esta separación permite producir y revisar los artefactos contract-first que cada slice necesita sin convertir el cierre pendiente en una formalidad vacía. Se descartan tanto bloquear esos artefactos hasta cerrar la fase como implementar funcionalidades de negocio antes de completar la revisión humana.

Cada funcionalidad deberá completar y aprobar antes de implementar su caso de uso:

1. contrato OpenAPI y compatibilidad;
2. catálogo de Problem Details;
3. modelo, migraciones Flyway, restricciones e índices;
4. autorización, alcance y transacciones;
5. pruebas de contrato, dominio, arquitectura, concurrencia, accesibilidad, privacidad y operación aplicables.

Esta secuencia no autoriza código con datos reales, proveedor de correo real, staging productivo ni producción.

## Bloqueos antes de datos reales o producción

- Completar responsable, bases, información, consentimientos, retención, derechos, encargados, EIPD y riesgo residual de `ADR-0010` y `ADR-0018` a `ADR-0023`.
- Aprobar dominio, remitente, `Reply-To`, DPA de Brevo, webhook, entregabilidad, supresión, alertas y runbooks de `ADR-0011` y `ADR-0016`.
- Demostrar aislamiento, eventos durables, restauración PITR, restauración externa completa fuera de Azure, custodia de claves, telemetría sin datos personales y reaplicación ordenada de vencimientos y supresiones.

## Condición de vigencia del cierre

Fase 2 conserva el estado `Validado` mientras las puertas aplicables correspondan a la versión vigente, no aparezcan contradicciones o bloqueos documentales sin tratamiento y la PR mantenga la evidencia de que no hubo revisión independiente y de que el mantenedor aceptó expresamente ese riesgo. Un cambio material en alcance, decisiones o trazabilidad deberá reabrir la revisión correspondiente.
