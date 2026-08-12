# ADR-0001: Registrar decisiones de arquitectura mediante ADRs

**Estado:** Aceptado
**Fecha:** 2026-08-10
**Responsable de revisión:** Revisor de arquitectura; en el flujo actual de único mantenedor, el autor asume este rol.

## Contexto

Fase 1 cerró requisitos funcionales, criterios de aceptación y decisiones de diseño con impacto funcional. Esa documentación declara que Fase 2 debe enlazar cada requisito `RF-01` a `RF-20` con su diseño, criterios de aceptación y decisión técnica correspondiente.

Si Fase 2 avanza solo con documentos narrativos, las decisiones técnicas pueden quedar mezcladas con el diseño de flujos o resolverse implícitamente durante la implementación. Eso haría más difícil revisar alternativas, detectar cambios de alcance y mantener trazabilidad entre requisitos, arquitectura y pruebas.

## Decisión

Se usarán Architecture Decision Records en `docs/adr/` para registrar las decisiones técnicas relevantes de Fase 2.

Cada ADR debe documentar:

- contexto;
- decisión;
- alternativas consideradas;
- consecuencias;
- requisitos y decisiones de Fase 1 relacionados;
- pruebas o validaciones previstas;
- decisiones pendientes, si existen.

La matriz de decisiones de Fase 1 sigue siendo la fuente de decisiones funcionales ya tomadas. Los ADRs registran la materialización técnica de esas decisiones durante Fase 2.

## Alternativas consideradas

### Mantener solo una matriz de decisiones técnica

Permitiría ver todas las decisiones en una tabla única, pero sería insuficiente para decisiones con contexto, tradeoffs, consecuencias y pruebas asociadas. La tabla puede seguir existiendo como índice, pero no como sustituto de ADRs.

### Documentar decisiones dentro de cada documento de diseño

Reduciría el número de archivos, pero dispersaría decisiones transversales como permisos, publicación atómica, versionado o privacidad. También complicaría reemplazar una decisión sin reescribir documentos históricos.

### Postergar ADRs hasta implementación

Evitaría documentación temprana, pero permitiría que decisiones estructurales se tomen por accidente en el código. Es una mala opción para este proyecto porque Fase 1 exige trazabilidad antes de cerrar Fase 2.

## Consecuencias

- Fase 2 no puede cerrarse si hay requisitos imprescindibles sin decisión técnica enlazada o sin justificación explícita de que no requieren ADR.
- Cada decisión arquitectónica aceptada debe tener propietario de revisión, aunque en el flujo actual de único mantenedor el autor asuma ese rol.
- Los ADRs deben crearse cuando la decisión sea necesaria, no todos por adelantado.
- Un ADR aceptado no se reescribe para cambiar el pasado; se reemplaza mediante otro ADR.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20` de [Requisitos funcionales y no funcionales - Fase 1](../phase-1-requirements.md).
- Todas las decisiones `D-01` a `D-08` de [Matriz de decisiones - Fase 1](../phase-1-decision-matrix.md).

## Validación prevista

Al cerrar Fase 2 se debe comprobar que:

- cada `RF` enlaza con al menos un documento de diseño;
- cada documento de diseño enlaza con los ADRs que materializan sus decisiones técnicas;
- cada ADR aceptado indica los `RF` y decisiones de Fase 1 afectados;
- `git diff --check` no detecta errores de espacios en blanco.

## Decisiones pendientes

- Stack técnico, despliegue y estrategia de persistencia. Responsable: Revisor de arquitectura. Tratamiento: resolver en ADRs específicos antes de cerrar el diseño técnico general.
