# Arranque de arquitectura — Fase 2

**Estado:** Propuesto
**Fecha:** 2026-08-10

## Propósito

Definir cómo se van a registrar y revisar las decisiones de arquitectura durante Fase 2 antes de empezar a diseñar en detalle los flujos, modelos e implementación del PMV.

Este documento no decide todavía el stack técnico ni la arquitectura final. Su función es evitar que Fase 2 cierre requisitos con decisiones técnicas implícitas.

## Supuestos

- Fase 1 está cerrada como fuente de requisitos funcionales imprescindibles `RF-01` a `RF-20`.
- La [Matriz de decisiones de Fase 1](phase-1-decision-matrix.md) recoge decisiones funcionales que Fase 2 debe materializar técnicamente.
- El PMV sigue limitado a un único club y a una aplicación web adaptable.
- Todavía no hay runtime, framework, base de datos, proveedor de correo ni estrategia de despliegue seleccionados.

## Regla de trabajo

Durante Fase 2, una decisión técnica relevante debe quedar registrada en un ADR antes de dar por cerrado el diseño que depende de ella.

No hace falta cerrar todos los ADRs antes de iniciar Fase 2. Sí hace falta cerrarlos antes de implementar la parte afectada.

## Orden recomendado

1. Confirmar el backlog inicial de ADRs en [Architecture Decision Records](adr/README.md).
2. Crear el documento o documentos de diseño de Fase 2 que materialicen la matriz de trazabilidad de Fase 1.
3. Para cada área de diseño, abrir ADRs en estado `Propuesto` cuando aparezca una decisión técnica no trivial.
4. Aceptar un ADR solo cuando sus alternativas, consecuencias, requisitos afectados y validación prevista estén claros.
5. Al cerrar Fase 2, comprobar que cada `RF-01` a `RF-20` enlaza con diseño, criterios de aceptación y ADR o decisión técnica equivalente.

## Criterios de cierre para esta preparación

- Existe una convención documentada para crear ADRs.
- Existe una plantilla reutilizable.
- Existe al menos un ADR aceptado que fija el uso de ADRs.
- Existe un backlog inicial de ADRs candidatos para Fase 2.
- No se han tomado decisiones técnicas de producto sin registrar su estado.
- Las decisiones pendientes tienen tratamiento y responsable de revisión.

## Riesgos

- Crear demasiados ADRs puede convertir Fase 2 en burocracia. Mitigación: solo registrar decisiones con impacto arquitectónico real.
- Crear pocos ADRs dejaría decisiones estructurales ocultas en documentos narrativos. Mitigación: revisar trazabilidad por `RF` antes de cerrar Fase 2.
- Aceptar ADRs antes de tener suficiente diseño puede bloquear malas decisiones. Mitigación: usar estado `Propuesto` hasta que las alternativas estén evaluadas.

## Siguiente paso

El siguiente documento de Fase 2 debería ser el diseño funcional y técnico de alto nivel que trace `RF-01` a `RF-20` contra flujos, modelos, ADRs candidatos y pruebas previstas.
