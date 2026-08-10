---
name: validate-verifiable-requirements
description: Valida que los requisitos funcionales imprescindibles sean observables y verificables. Úsala al redactar o revisar requisitos, criterios de aceptación o cambios de alcance en documentación de producto.
---

# Validar requisitos verificables

Revisa cada requisito imprescindible modificado. Un requisito es verificable solo cuando indica el actor, el comportamiento observable, el resultado esperado y las restricciones o condiciones de error aplicables.

1. Cita el requisito e identifica el elemento ausente, si lo hay.
2. Rechaza expresiones aspiracionales como "básico", "fácil" o "rápido" salvo que tengan una definición observable.
3. Comprueba que las exclusiones y las notificaciones se declaran como comportamiento, no como supuestos.

No reescribas la intención de negocio sin identificarla como un cambio propuesto. Genera este informe:

```markdown
## Revisión de verificabilidad de requisitos
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <requisito y sección>
- Hallazgos: <requisito no verificable o ninguno>
- Acción requerida: <aclaración precisa>
- Revisor humano: Revisor de producto
```
