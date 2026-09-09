# Controles de calidad documental

**Estado:** Vigente
**Fecha:** 2026-08-15
**Última actualización:** 2026-09-08 — simplificada la gobernanza de revisión para el flujo de único mantenedor (Bloque 4.1 del plan de corrección de la auditoría documental): se elimina el reparto de roles por control y la declaración de ausencia de revisión independiente pasa a hacerse una sola vez, en este documento

## Objetivo

Definir las validaciones mínimas para que la documentación de producto y diseño sea coherente, verificable y suficiente para avanzar entre fases. Estos controles no sustituyen el proceso de revisión de PR definido por el repositorio.

## Aplicación de los controles

| Control | Cuándo es obligatorio | Evidencia mínima |
| --- | --- | --- |
| Trazabilidad entre fases | Al cerrar cualquier fase | Cada riesgo, decisión o requisito relevante enlaza con su tratamiento en la fase posterior o declara explícitamente que queda pendiente. |
| Requisitos verificables | Al cerrar una fase que defina requisitos | Cada requisito imprescindible identifica actor, comportamiento observable y resultado esperado. |
| Consistencia terminológica | Al cerrar cualquier fase | Los términos operativos se definen una vez y se emplean con el mismo significado en todos los documentos afectados. |
| Matriz de decisiones | Cuando se cree o cambie una decisión de diseño | La decisión registra motivo, alternativa descartada, impacto y fase en la que debe materializarse. |
| Preguntas abiertas bloqueantes | Al cerrar cualquier fase | Las preguntas que pueden cambiar alcance, modelo de datos o flujos están resueltas o identificadas como bloqueantes; no se cierran fases con ambigüedades ocultas. |
| Criterios de aceptación por requisito imprescindible | Al cerrar una fase que defina requisitos funcionales | Cada requisito imprescindible tiene escenarios de éxito y error que permitan comprobarlo durante implementación y pruebas. |
| Control de cambios de alcance | En toda PR documental que modifique alcance | La descripción de la PR declara los cambios en supuestos, riesgos, decisiones o límites del PMV. |
| Validación de privacidad previa a producción | Antes de liberar a producción | Se documentan responsable, base legal, retención, acceso, borrado y tratamiento de la información de seguimiento declarada. |
| Diseño de API HTTP | En toda PR que cree o modifique una operación HTTP propia | Cada operación identifica un recurso real y cumple `ADR-0017`, la guía de API, OpenAPI contract-first y los controles de compatibilidad. |

## Controles obligatorios para cierre de fase

Los siguientes controles deben superarse siempre antes de declarar cerrada una fase:

- Trazabilidad entre fases.
- Requisitos verificables, cuando la fase incluya requisitos.
- Consistencia terminológica.
- Preguntas abiertas bloqueantes.

No se debe declarar una fase cerrada si falta evidencia de alguno de estos controles aplicables.

## Controles condicionados por el cambio

- La matriz de decisiones es obligatoria cuando se toma o modifica una decisión con impacto de producto, datos, permisos o arquitectura.
- Los criterios de aceptación son obligatorios cuando se definen requisitos funcionales imprescindibles.
- El control de cambios de alcance es obligatorio en cualquier PR documental que cambie alcance, supuestos, riesgos o decisiones.
- La validación de privacidad es condición de salida a producción; no bloquea las fases de descubrimiento o diseño salvo que su ausencia impida decidir el alcance.
- El diseño de API HTTP es obligatorio cuando una PR crea, cambia o elimina rutas, representaciones, métodos, estados o errores del contrato.

## Criterios de revisión

Durante la revisión de una PR documental se debe confirmar lo siguiente:

- Los enlaces, referencias de fase y nombres de documentos afectados son correctos.
- No hay contradicciones entre problema, requisitos, decisiones, supuestos y riesgos.
- Los requisitos imprescindibles no dependen de decisiones no documentadas.
- Las exclusiones de alcance son explícitas y no se presentan como comportamiento futuro garantizado.
- Las operaciones HTTP no son acciones nominalizadas, no codifican roles en sus rutas y justifican recurso, método, estado, seguridad e idempotencia.
- Se ha ejecutado `git diff --check`.

## Ejecución con Skills

Las Skills del complemento `documentation-quality-review` preparan evidencia y hallazgos para los ocho controles documentales generales:

| Control | Skill o herramienta |
| --- | --- |
| Trazabilidad entre fases | `validate-phase-traceability` |
| Requisitos verificables | `validate-verifiable-requirements` |
| Consistencia terminológica | `validate-terminology` |
| Matriz de decisiones | `validate-design-decisions` |
| Preguntas abiertas bloqueantes | `validate-blocking-questions` |
| Criterios de aceptación | `validate-acceptance-criteria` |
| Control de cambios de alcance | `validate-scope-changes` |
| Validación de privacidad | `validate-privacy-readiness` |
| Diseño de API HTTP | Spectral, `oasdiff` y revisión de API HTTP definida en la guía de API |

Ninguna Skill ni herramienta aprueba una PR ni sustituye el criterio de una persona.

## Revisión

El proyecto tiene un único mantenedor: el autor de cada PR documental ejecuta las Skills aplicables, revisa la evidencia que producen, resuelve o descarta cada hallazgo con su propio criterio y deja constancia de esa conclusión en la PR. No hay reparto de roles por control ni una persona revisora distinta del autor que asignar.

La ausencia de revisión independiente se declara **una sola vez, aquí**: mientras el proyecto tenga un único mantenedor, ninguna PR documental cuenta con una segunda persona que confirme la evidencia, y ese riesgo se acepta como condición de trabajo del proyecto, no como una excepción a repetir en cada PR. No se permite usar una cuenta alternativa para aparentar independencia.

Esta sección deja de aplicar en cuanto exista una segunda persona manteniendo el proyecto: en ese momento se debe volver a repartir responsabilidad de revisión por control y exigir de nuevo su declaración explícita por PR.

## Límites de esta validación

Estos controles validan calidad documental; no validan que el producto sea deseable, viable técnicamente o conforme a normativa. Esas validaciones requieren revisión de las partes responsables, prototipos, análisis técnico y, antes de producción, asesoramiento de privacidad cuando corresponda. El flujo de un único mantenedor no sustituye la intervención de un responsable de privacidad o DPO cuando sea necesaria.
