---
name: validate-design-decisions
description: Valida decisiones documentadas de producto, datos, permisos o arquitectura. Úsala cuando una PR introduzca o modifique una decisión con impacto posterior en la implementación.
---

# Validar decisiones de diseño

Para cada decisión modificada, verifica que la documentación incluye un motivo, una alternativa considerada o descartada explícitamente, el impacto y la fase donde debe implementarse.

1. Identifica la decisión y su responsable, si se indica.
2. Comprueba que la decisión no contradice requisitos ni alcance existentes.
3. Señala decisiones presentadas como hechos sin motivo, impacto u objetivo de implementación.
4. Separa una decisión no resuelta de un aplazamiento deliberado.

No elijas una alternativa en nombre del responsable de la decisión. Genera este informe:

```markdown
## Revisión de decisiones de diseño
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <decisión y sección>
- Hallazgos: <motivo, impacto o conflicto ausente>
- Acción requerida: <acción del responsable o ninguna>
- Revisor humano: Revisor de arquitectura
```
