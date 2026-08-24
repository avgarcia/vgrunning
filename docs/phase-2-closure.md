# Cierre documental — Fase 2

**Estado:** Validado — Fase 2 cerrada
**Fecha:** 2026-08-24
**Responsables de revisión:** Revisor de arquitectura y Revisor de producto
**Restricción:** Este cierre no autoriza implementación, tratamiento de datos personales reales ni producción

## Alcance del cierre

Este documento registra la comprobación final de que Fase 2 materializa los requisitos imprescindibles `RF-01` a `RF-20`, las decisiones `D-01` a `D-08`, los riesgos activos y los límites del PMV mediante diseño detallado y decisiones de arquitectura trazables.

El cierre comprende ocho módulos confirmados por `ADR-0014`: `identity-access`, `runner-management`, `classification-segmentation`, `planning`, `publication`, `notification-delivery`, `tracking-review` y `runner-portal`. No crea un noveno módulo ni amplía el alcance del PMV.

## Resultado

- Los ocho módulos tienen diseño detallado validado y propietario, dependencias, datos, autorización, API prevista y validación documentados.
- Cada requisito `RF-01` a `RF-20` tiene una fila única en la [trazabilidad de requisitos](phase-2-high-level-design.md#trazabilidad-de-requisitos), criterios de aceptación y ADR o decisión técnica equivalente.
- Cada decisión `D-01` a `D-08` tiene tratamiento y ADR relacionado en la [trazabilidad de decisiones](phase-2-high-level-design.md#trazabilidad-de-decisiones-de-fase-1).
- Los `21` ADRs están aceptados y el backlog inicial de Fase 2 no conserva candidatos pendientes.
- No quedan preguntas de producto o arquitectura abiertas dentro del diseño de los ocho módulos.
- Las obligaciones anteriores a implementación, datos reales y producción permanecen explícitas y no se presentan como trabajo resuelto.

## Trazabilidad desde Fase 0

| Riesgo, supuesto o límite de Fase 0 | Tratamiento confirmado en Fase 2 | Trabajo posterior |
| --- | --- | --- |
| La personalización individual podía convertir los grupos en una distinción cosmética. | `classification-segmentation` y `planning` separan segmentos reutilizables, grupos exclusivos y excepciones persistentes; los planes pertenecen al grupo, no a listas semanales reconstruidas. | Medir durante la validación operativa si el modelo reduce trabajo manual; cualquier cambio de alcance exige revisar `ADR-0005`, `ADR-0006` y `ADR-0020`. |
| La información de seguimiento era demasiado vaga para resultar operativa. | `tracking-review` define estados, valores, ventana, historial y consultas globales; `runner-portal` define captura y presentación. | Validar usabilidad con datos sintéticos y completar privacidad antes de datos reales. |
| Correo y WhatsApp podían seguir actuando como sistema de gestión. | La aplicación es la fuente de verdad; `publication` y `notification-delivery` usan el correo únicamente como aviso de disponibilidad o cambio. WhatsApp queda fuera del PMV. | Probar entregabilidad y operación antes de habilitar el proveedor. |
| Los grupos debían poder expresarse mediante criterios operativos útiles. | Las taxonomías controladas, segmentos dinámicos, excepciones y grupos de planificación materializan el supuesto sin introducir un motor genérico. | Medir límites de lote, página y rendimiento antes de cerrar los contratos. |
| El entrenador debía revisar seguimiento sin intervención administrativa. | `tracking-review` concede lectura global a administrador y entrenador, sin asignación de titularidad ni edición de la declaración del corredor. | Mantener autorización, auditoría y revisión de privacidad del acceso global. |
| El Retiro podía convertirse en una restricción accidental. | `D-04`, `planning` y `runner-portal` usan ubicación opcional de texto libre solo para entrenamientos presenciales. | Ninguno dentro del PMV. |

## Correcciones de coherencia realizadas al cerrar

- `RF-02` y sus criterios se alinean con el rol inicial único e inmutable decidido por `ADR-0004`.
- `RF-08` y sus criterios se alinean con `ADR-0006`: cada plan pertenece a un grupo de planificación y no se asigna directamente a segmentos o corredores.
- Los criterios de `RF-17` distinguen `realizado`, que exige esfuerzo y sensación, de `no-realizado`, que los prohíbe; el comentario requiere consentimiento vigente.
- Los estados y referencias que todavía describían `runner-portal`, los ADRs o los diseños detallados como pendientes se actualizan al resultado ya validado.

Estas correcciones no cambian el alcance: eliminan contradicciones con decisiones aceptadas y con documentos detallados ya fusionados.

## Revisión de trazabilidad

- Estado: listo para revisión humana
- Evidencia: matrices de `RF-01` a `RF-20` y `D-01` a `D-08` en [Diseño de alto nivel](phase-2-high-level-design.md), diseños detallados de los ocho módulos, índice de ADRs y tabla de trazabilidad desde Fase 0 de este documento.
- Hallazgos: se corrigieron estados heredados que aún decían `Pendiente de diseño`, la consulta de `RF-04` pendiente del portal y dos referencias que trataban diseños ya validados como trabajo futuro. No queda un requisito, decisión o riesgo activo sin tratamiento o aplazamiento explícito.
- Acción requerida: enlazar OpenAPI, migraciones y pruebas posteriores con estas matrices; ninguna acción adicional para el cierre documental.
- Revisor humano: Revisor de arquitectura

## Revisión de verificabilidad de requisitos

- Estado: listo para revisión humana
- Evidencia: definiciones únicas de `RF-01` a `RF-20` en [Requisitos de Fase 1](phase-1-requirements.md), criterios observables y validación prevista por módulo.
- Hallazgos: los veinte requisitos identifican actor o capacidad autorizada, comportamiento y resultado; se corrigieron las combinaciones no observables o contradictorias de `RF-02`, `RF-08` y `RF-17`.
- Acción requerida: ninguna para cerrar; los contratos y pruebas deberán conservar las restricciones documentadas.
- Revisor humano: Revisor de producto

## Revisión de terminología

- Estado: listo para revisión humana
- Evidencia: lenguaje ubicuo de los diseños detallados y mapa modular de `ADR-0014`.
- Hallazgos: `cuenta` designa identidad de acceso, `corredor` el perfil operativo, `segmento` una clasificación dinámica, `grupo de planificación` la cohorte exclusiva y `publicación` la versión visible. `Alumno` permanece únicamente en el contexto histórico de Fase 0. No se detectan significados incompatibles después de las correcciones.
- Acción requerida: usar estos términos canónicos en OpenAPI, código, migraciones y pruebas.
- Revisor humano: Revisor de arquitectura

## Revisión de decisiones de diseño

- Estado: listo para revisión humana
- Evidencia: [Matriz de decisiones de Fase 1](phase-1-decision-matrix.md), `ADR-0001` a `ADR-0021` y secciones de alternativas, consecuencias y validación de cada diseño detallado.
- Hallazgos: las decisiones activas tienen motivo, alternativas descartadas, impacto y materialización. La auditoría estructural estricta informa `21 ADR(s), 0 error(es), 0 aviso(s)`; esa evidencia no sustituye la revisión humana.
- Acción requerida: crear un ADR nuevo si OpenAPI, migraciones, pruebas o validación operativa contradicen una decisión aceptada; ninguna para el cierre actual.
- Revisor humano: Revisor de arquitectura

## Revisión de preguntas bloqueantes

- Estado: listo para revisión humana
- Evidencia: secciones `Decisiones pendientes` de los ocho diseños y tabla de decisiones y bloqueos posteriores del diseño de alto nivel.
- Hallazgos: no quedan preguntas de producto o arquitectura sin responder. OpenAPI, migraciones, límites medidos y pruebas bloquean implementación; privacidad, dominio, proveedor y operación bloquean datos reales o producción. Todos tienen tratamiento y responsables documentados.
- Acción requerida: no reinterpretar esos bloqueos como decisiones opcionales ni resolverlos silenciosamente durante el desarrollo.
- Revisor humano: Revisor de producto

## Revisión de criterios de aceptación

- Estado: listo para revisión humana
- Evidencia: [Criterios de aceptación de Fase 1](phase-1-acceptance-criteria.md) y secciones `Validación prevista` de cada diseño detallado.
- Hallazgos: cada `RF-01` a `RF-20` conserva un escenario de éxito y otro de error o límite. Se alinearon `RF-02`, `RF-08` y `RF-17` con los ADRs aceptados; los diseños amplían pruebas de seguridad, concurrencia, atomicidad, privacidad y límites.
- Acción requerida: convertir estos criterios en pruebas trazables sin afirmar que ya están ejecutadas.
- Revisor humano: Revisores de producto y arquitectura

## Revisión de cambios de alcance

- Estado: listo para revisión humana
- Evidencia: diff de la rama de cierre, exclusiones de [Requisitos de Fase 1](phase-1-requirements.md), [Mejoras futuras](future-improvements.md) y secciones de cambios de alcance de publicación, notificaciones, seguimiento y portal.
- Hallazgos: el cierre no añade capacidades, actores, datos ni módulos. Las modificaciones semánticas reflejan refinamientos ya aceptados por `ADR-0004`, `ADR-0006` y `ADR-0009`.
- Acción requerida: la PR debe declarar que no cambia el alcance, enumerar estas correcciones y mantener fuera del PMV `MF-001` a `MF-005`.
- Revisor humano: Revisor de la PR

## Revisión de API HTTP

- Estado: requiere artefacto antes de implementar
- Evidencia: secciones de API HTTP de los ocho diseños, `ADR-0017` y [Guía de diseño de API HTTP](api-design-guidelines.md).
- Hallazgos: las operaciones previstas identifican recursos en plural bajo `/api`, métodos, actores, concurrencia, idempotencia y errores; no introducen verbos, prefijos por rol ni secretos en rutas o consultas. Todavía no existe el contrato OpenAPI `3.1`, por lo que no se han podido ejecutar Spectral, `oasdiff`, generación ni pruebas de contrato.
- Acción requerida: crear y revisar OpenAPI antes de implementar cualquier controlador o consumidor; una contradicción debe corregir el diseño o abrir un ADR, no adaptar el contrato silenciosamente.
- Revisor humano: Revisor de arquitectura

La validación de privacidad previa a producción no es un gate de cierre de Fase 2. Permanece expresamente sin satisfacer y bloquea datos personales reales y producción.

## Condiciones posteriores al cierre

### Antes de implementar cualquier módulo

1. Crear y revisar el contrato OpenAPI `3.1` como fuente de verdad, con recursos, representaciones, seguridad, CSRF, estados, idempotencia, precondiciones y compatibilidad conforme a `ADR-0013`, `ADR-0017` y la guía de API.
2. Fijar el catálogo común versionado de Problem Details y medir límites de páginas y lotes; no inventar códigos o límites durante la implementación.
3. Crear migraciones Flyway, restricciones, índices y generación jOOQ respetando propiedad de esquemas y transacciones de `ADR-0012` y `ADR-0014`.
4. Trazar pruebas de contrato, arquitectura, autorización, concurrencia, transacción, accesibilidad y retención hacia los criterios y diseños aplicables.
5. Incorporar los comandos reales de instalación, ejecución, lint, formato y pruebas a `AGENTS.md` en el mismo cambio que introduzca el runtime.

### Antes de tratar datos personales reales o salir a producción

- Completar las evidencias de responsable, bases, información, consentimiento, retención, derechos, encargados, EIPD y riesgo residual de `ADR-0010` y `ADR-0018` a `ADR-0021`.
- Aprobar dominio, remitente, `Reply-To`, DPA de Brevo, webhook, entregabilidad, supresión, alertas y runbooks exigidos por `ADR-0011` y `ADR-0016`.
- Demostrar aislamiento, seguridad, restauración, telemetría sin datos personales y destrucción o reaplicación de supresiones mediante pruebas en el stack real.

## Declaración de cierre

Fase 2 queda cerrada como diseño funcional, técnico y arquitectónico del PMV. Este cierre confirma que no deben tomarse decisiones de producto o arquitectura durante la codificación de los ocho módulos.

El cierre no equivale a preparación para implementar: el primer trabajo posterior es producir y revisar los artefactos contract-first y de persistencia indicados. Tampoco autoriza datos personales reales, habilitación del proveedor de correo, staging productivo ni producción.

Las Skills documentales preparan evidencia, pero no aprueban la PR. En el flujo de único mantenedor, antes de fusionar la PR de cierre debe registrarse expresamente que no hubo revisión independiente y que el mantenedor acepta ese riesgo.
