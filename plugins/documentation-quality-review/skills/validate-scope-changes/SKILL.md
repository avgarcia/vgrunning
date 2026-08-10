---
name: validate-scope-changes
description: Valida que una PR documental declare sus cambios de alcance, supuestos, riesgos y decisiones. Úsala al revisar cualquier PR que modifique documentos de fase, producto o diseño.
---

# Validar cambios de alcance

Compara la descripción de la PR y su diff con el estado anterior de los documentos.

1. Identifica añadidos, eliminaciones y cambios de prioridad que afecten al alcance del PMV.
2. Comprueba que la descripción de la PR declara cambios en supuestos, riesgos, decisiones y exclusiones.
3. Señala cambios de alcance ocultos como redacción o cambios de responsabilidad no documentados.
4. Verifica que las fases y decisiones referenciadas todavía existen.

No apruebes la PR; exige al autor que describa un cambio de alcance omitido. Genera este informe:

```markdown
## Revisión de cambios de alcance
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <sección de PR y ubicación del diff>
- Hallazgos: <cambio de alcance no declarado o ninguno>
- Acción requerida: <actualización de PR o documento>
- Revisor humano: Revisor de la PR
```
