---
name: validate-phase-traceability
description: Valida la trazabilidad entre fases de producto y diseño. Úsala al cerrar una fase o revisar documentación de fase que cambie riesgos, decisiones, requisitos o trabajo posterior previsto.
---

# Validar trazabilidad entre fases

Revisa los documentos de fase relevantes y el diff de la PR. Usa `docs/documentation-quality-gates.md` cuando exista en el espacio de trabajo.

1. Enumera cada riesgo, decisión y requisito imprescindible activo o modificado.
2. Identifica su tratamiento en la fase siguiente, o verifica que se registra explícitamente como pendiente o fuera de alcance.
3. Señala enlaces ausentes, ambiguos o contradictorios entre fases.

No apruebes la PR ni declares correcta una decisión de producto. Genera este informe:

```markdown
## Revisión de trazabilidad
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <documento y sección de origen>
- Hallazgos: <enlaces ausentes o contradicciones concretas>
- Acción requerida: <acción o ninguna>
- Revisor humano: Revisor de arquitectura
```
