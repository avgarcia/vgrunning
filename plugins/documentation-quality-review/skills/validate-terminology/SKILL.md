---
name: validate-terminology
description: Valida la consistencia terminológica entre documentos de producto y diseño. Úsala al cerrar una fase o cuando una PR añada, renombre o cambie conceptos operativos.
---

# Validar terminología

Revisa los documentos modificados y los documentos de fase que referencian para detectar términos que describen el mismo concepto de forma distinta o un mismo término con significados incompatibles.

1. Elabora una lista breve de los términos operativos modificados.
2. Compara cada término con las definiciones y usos existentes.
3. Señala sinónimos, términos no definidos y definiciones contradictorias.
4. Recomienda un término canónico y el documento que debe definirlo.

No inventes una entrada de glosario cuando el responsable de producto no haya elegido el concepto. Genera este informe:

```markdown
## Revisión de terminología
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <término y secciones de origen>
- Hallazgos: <conflicto o ninguno>
- Acción requerida: <redacción canónica o decisión>
- Revisor humano: Revisor de arquitectura
```
