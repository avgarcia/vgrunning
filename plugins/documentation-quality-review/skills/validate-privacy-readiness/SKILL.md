---
name: validate-privacy-readiness
description: Valida que la documentación registre las evidencias de preparación para privacidad requeridas antes de producción. Úsala antes de una liberación a producción o cuando cambios de producto introduzcan datos personales, seguimiento, retención, acceso o borrado.
---

# Validar preparación para privacidad

Revisa la documentación de producto y de liberación para comprobar la evidencia requerida antes de producción: responsable, base legal, retención, acceso, borrado y tratamiento de la información de seguimiento declarada.

1. Identifica los datos personales y el tratamiento de seguimiento descritos por el cambio.
2. Comprueba si cada elemento de evidencia requerido está documentado o explícitamente pendiente.
3. Señala la preparación para producción como bloqueada cuando falte evidencia.
4. Distingue la completitud documental del asesoramiento o aprobación legal.

No proporciones asesoramiento legal ni apruebes el cumplimiento. Genera este informe:

```markdown
## Revisión de preparación para privacidad
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <controles documentados o evidencia ausente>
- Hallazgos: <evidencia de privacidad ausente o ninguna>
- Acción requerida: <acción del responsable de privacidad>
- Revisor humano: Revisor de privacidad o DPO
```
