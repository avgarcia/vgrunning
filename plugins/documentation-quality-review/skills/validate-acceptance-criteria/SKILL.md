---
name: validate-acceptance-criteria
description: Valida criterios de aceptación para requisitos funcionales imprescindibles. Úsala cuando los requisitos se preparen para implementación, diseño de flujos de usuario, planificación de pruebas o revisión de PR.
---

# Validar criterios de aceptación

Para cada requisito imprescindible modificado, verifica que sus criterios de aceptación describen al menos un escenario de éxito y el comportamiento relevante de error o límite.

1. Enlaza el requisito con sus criterios; señala los enlaces ausentes.
2. Comprueba que los criterios usan entradas, acciones y resultados observables.
3. Identifica criterios que dependen de datos, permisos o decisiones no documentados.
4. Señala ejemplos presentados como criterios exhaustivos.

No afirmes que los criterios están probados; esta habilidad valida solo la documentación. Genera este informe:

```markdown
## Revisión de criterios de aceptación
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <requisito y criterios>
- Hallazgos: <escenario o dependencia ausente>
- Acción requerida: <criterio o decisión necesaria>
- Revisor humano: Revisores de producto y arquitectura
```
