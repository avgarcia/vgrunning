---
name: validate-blocking-questions
description: Valida que las preguntas abiertas que pueden cambiar alcance, modelos de datos o flujos de usuario estén resueltas o tratadas explícitamente como bloqueantes. Úsala antes de cerrar una fase o aprobar documentación relacionada.
---

# Validar preguntas bloqueantes

Revisa los supuestos, preguntas abiertas, riesgos y decisiones de los documentos de fase modificados.

1. Identifica preguntas sin responder que puedan alterar alcance, datos, permisos, flujos o cumplimiento.
2. Clasifica cada una como resuelta, aplazada deliberadamente con responsable y fecha, o bloqueante.
3. Señala supuestos presentados como decisiones sin validación.
4. Bloquea el cierre de fase cuando una pregunta sin responsable pueda cambiar la fase siguiente.

No resuelvas silenciosamente una pregunta de producto. Genera este informe:

```markdown
## Revisión de preguntas bloqueantes
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <pregunta, supuesto o riesgo>
- Hallazgos: <clasificación y motivo>
- Acción requerida: <decisión del responsable o ninguna>
- Revisor humano: Revisor de producto
```
