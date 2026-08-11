---
name: gestionar-adrs
description: Crea, actualiza y audita Architecture Decision Records del proyecto Running Coach. Úsala cuando se proponga, acepte, reemplace, descarte o revise un ADR en docs/adr/, cuando haya que mantener el índice de ADRs o cuando se necesite comprobar su trazabilidad con RF-01 a RF-20 y las decisiones de Fase 1.
---

# Gestión de ADRs

Usa `docs/adr/README.md` como índice y convención canónica, y `docs/adr/adr-template.md` como plantilla. Los ADRs documentan decisiones técnicas de Fase 2; no sustituyen los documentos de diseño ni deciden arquitectura sin evidencia.

## Flujo

1. Lee `AGENTS.md`, `docs/adr/README.md`, la plantilla y los documentos de Fase 1 y Fase 2 afectados.
2. Clasifica la petición: crear, actualizar, auditar, aceptar, reemplazar o descartar un ADR.
3. Ejecuta `python .agents/skills/gestionar-adrs/scripts/audit_adrs.py` antes y después de modificar ADRs. Usa `--strict` al preparar una PR.
4. Separa siempre errores mecánicos, decisiones pendientes y riesgos. El resultado del auditor no aprueba una PR ni sustituye a un revisor.

## Crear o actualizar

- Crea un ADR solo para una decisión con impacto arquitectónico. No lo uses para preferencias locales o detalles reversibles.
- Asigna el siguiente identificador libre con formato `NNNN-titulo-en-kebab-case.md` y el título `ADR-NNNN` correspondiente.
- Conserva todas las secciones de la plantilla, incluido responsable de revisión, requisitos, decisiones de Fase 1, validación y decisiones pendientes.
- Registra el ADR nuevo en la tabla `## ADRs` del índice. Mantén el backlog como candidatos hasta que exista un ADR real.
- No conviertas un ADR en `Aceptado` si no concreta la decisión, alternativas, consecuencias, requisitos afectados y validación prevista.
- No reescribas una decisión ya aceptada. Crea otro ADR, márcalo como `Reemplazado` cuando corresponda y enlaza la sustitución.
- Si una pregunta puede cambiar alcance, modelo de datos o flujo, declárala bloqueante y define responsable y tratamiento.

## Revisar

- Para una decisión nueva o modificada, usa `validate-design-decisions`, `validate-scope-changes`, `validate-phase-traceability` y `validate-blocking-questions` cuando estén disponibles.
- Usa `validate-terminology` al introducir o renombrar conceptos operativos.
- Antes de cerrar Fase 2, comprueba que `RF-01` a `RF-20` enlazan con diseño, criterios de aceptación y ADR o una justificación explícita.
- Declara en la PR los cambios de alcance, supuestos, riesgos y la ausencia de revisión independiente cuando aplique.

## Límites

No aceptes un ADR, no cierres Fase 2 y no fusiones una PR solo porque el auditor no encuentre errores. El auditor valida estructura y trazabilidad básica; no puede determinar si la decisión técnica es correcta.
