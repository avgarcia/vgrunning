# Controles de calidad documental

**Estado:** Vigente
**Fecha:** 2026-08-10

## Objetivo

Definir las validaciones mínimas para que la documentación de producto y diseño sea coherente, verificable y suficiente para avanzar entre fases. Estos controles no sustituyen la revisión y aprobación de la pull request requerida por el repositorio.

## Aplicación de los controles

| Control | Cuándo es obligatorio | Evidencia mínima |
| --- | --- | --- |
| Trazabilidad entre fases | Al cerrar cualquier fase | Cada riesgo, decisión o requisito relevante enlaza con su tratamiento en la fase posterior o declara explícitamente que queda pendiente. |
| Requisitos verificables | Al cerrar una fase que defina requisitos | Cada requisito Must identifica actor, comportamiento observable y resultado esperado. |
| Consistencia terminológica | Al cerrar cualquier fase | Los términos operativos se definen una vez y se emplean con el mismo significado en todos los documentos afectados. |
| Matriz de decisiones | Cuando se cree o cambie una decisión de diseño | La decisión registra motivo, alternativa descartada, impacto y fase en la que debe materializarse. |
| Preguntas abiertas bloqueantes | Al cerrar cualquier fase | Las preguntas que pueden cambiar alcance, modelo de datos o flujos están resueltas o identificadas como bloqueantes; no se cierran fases con ambigüedades ocultas. |
| Criterios de aceptación por requisito Must | Al cerrar una fase que defina requisitos funcionales | Cada Must tiene escenarios de éxito y error que permitan comprobarlo durante implementación y pruebas. |
| Control de cambios de alcance | En toda pull request documental que modifique alcance | La descripción de la pull request declara los cambios en supuestos, riesgos, decisiones o límites del MVP. |
| Validación de privacidad previa a producción | Antes de liberar a producción | Se documentan responsable, base legal, retención, acceso, borrado y tratamiento del feedback declarado. |

## Controles obligatorios para cierre de fase

Los siguientes controles deben superarse siempre antes de declarar cerrada una fase:

- Trazabilidad entre fases.
- Requisitos verificables, cuando la fase incluya requisitos.
- Consistencia terminológica.
- Preguntas abiertas bloqueantes.

No se debe declarar una fase cerrada si falta evidencia de alguno de estos controles aplicables.

## Controles condicionados por el cambio

- La matriz de decisiones es obligatoria cuando se toma o modifica una decisión con impacto de producto, datos, permisos o arquitectura.
- Los criterios de aceptación son obligatorios cuando se definen requisitos funcionales Must.
- El control de cambios de alcance es obligatorio en cualquier pull request documental que cambie alcance, supuestos, riesgos o decisiones.
- La validación de privacidad es condición de salida a producción; no bloquea las fases de descubrimiento o diseño salvo que su ausencia impida decidir el alcance.

## Criterios de revisión

Durante la revisión de una pull request documental se debe confirmar lo siguiente:

- Los enlaces, referencias de fase y nombres de documentos afectados son correctos.
- No hay contradicciones entre problema, requisitos, decisiones, supuestos y riesgos.
- Los requisitos Must no dependen de decisiones no documentadas.
- Las exclusiones de alcance son explícitas y no se presentan como comportamiento futuro garantizado.
- Se ha ejecutado `git diff --check`.

## Ejecución con Skills y revisión humana

Las Skills del plugin `documentation-quality-review` preparan evidencia y hallazgos para los ocho controles. Una Skill nunca aprueba una pull request ni sustituye a un responsable humano.

| Control | Skill | Revisor humano responsable |
| --- | --- | --- |
| Trazabilidad entre fases | `validate-phase-traceability` | Revisor de arquitectura |
| Requisitos verificables | `validate-verifiable-requirements` | Revisor de producto |
| Consistencia terminológica | `validate-terminology` | Revisor de arquitectura |
| Matriz de decisiones | `validate-design-decisions` | Revisor de arquitectura |
| Preguntas abiertas bloqueantes | `validate-blocking-questions` | Revisor de producto |
| Criterios de aceptación | `validate-acceptance-criteria` | Revisores de producto y arquitectura |
| Control de cambios de alcance | `validate-scope-changes` | Revisor de la pull request |
| Validación de privacidad | `validate-privacy-readiness` | Responsable de privacidad o DPO |

El autor ejecuta las Skills aplicables y adjunta los informes a la pull request. El responsable humano asignado revisa la evidencia, resuelve o escala los hallazgos y registra la aprobación o solicitud de cambios. Antes de abrir una pull request se debe asignar una persona concreta a cada rol de revisión aplicable.

## Límites de esta validación

Estos controles validan calidad documental; no validan que el producto sea deseable, viable técnicamente o conforme a normativa. Esas validaciones requieren revisión de las partes responsables, prototipos, análisis técnico y, antes de producción, asesoramiento de privacidad cuando corresponda.
