# Directrices del repositorio

## Estructura del proyecto y organización de módulos

Este repositorio está actualmente en fase de descubrimiento de producto. El material de referencia está en `docs/`; `docs/phase-0-problem-statement.md` recoge el problema validado, el alcance, las decisiones, los supuestos y los riesgos. `README.md` es el punto de entrada del proyecto.

Cuando comience la implementación, mantén el código de producción, las pruebas y los recursos estáticos en directorios raíz claramente nombrados, por ejemplo: `src/`, `tests/` y `assets/`. No mezcles archivos generados, credenciales locales ni notas personales con la documentación de producto validada.

## Convenciones de documentación

Redacta los documentos de producto y técnicos en Markdown y en castellano. Conserva en su idioma original los identificadores técnicos, rutas, comandos, claves de configuración, nombres de Skills y URLs. Usa nombres descriptivos en kebab-case, con un prefijo de fase o tema, como `docs/phase-1-user-flows.md` o `docs/architecture-overview.md`. Inicia cada documento con un título claro y, cuando sea útil, su estado y fecha.

Mantén la distinción entre decisiones, supuestos, preguntas abiertas y riesgos. Actualiza el enunciado del problema solo si cambia el entendimiento del producto; crea un documento nuevo para trabajo de diseño posterior, en lugar de reescribir el contexto histórico.

## Comandos de compilación, pruebas y desarrollo

Todavía no hay runtime de aplicación, gestor de paquetes, linter, formateador ni framework de pruebas configurados. No añadas comandos a esta guía hasta que exista la herramienta correspondiente. Para el repositorio actual, solo de documentación, usa:

```powershell
git status       # Revisa los cambios locales antes de compartirlos
git diff --check # Detecta errores de espacios en blanco
```

Cuando se elija una tecnología, añade aquí sus comandos de instalación, ejecución local, lint, formato y pruebas en el mismo cambio que la incorpore.

## Directrices de pruebas

No hay pruebas automatizadas ni requisitos de cobertura todavía. Toda funcionalidad futura debe incluir pruebas con el framework seleccionado y nombres que describan el comportamiento, por ejemplo `workout-assignment.spec.ts`. Documenta el comando de pruebas y el umbral de cobertura cuando se adopten.

## Directrices de commits y pull requests

El historial existente usa asuntos cortos, imperativos y con guiones, por ejemplo `Add-phase-0-problem-statement`. Sigue ese patrón: `Add-phase-1-user-flows` o `Clarify-feedback-scope`.

Mantén cada commit enfocado. Los pull requests deben indicar el problema abordado, resumir los documentos o el código modificados, enlazar el issue o la decisión correspondiente cuando exista e incluir capturas para cambios de interfaz. Señala explícitamente los cambios en supuestos, alcance o riesgos.

## Flujo obligatorio de cambios

No hagas commits ni pushes directos a `main`, incluidos cambios exclusivamente documentales. Para cualquier modificación crea una rama con prefijo `feature/`, por ejemplo `feature/clarify-tag-taxonomy`; confirma el alcance, valida el cambio, haz commit y abre una PR borrador contra `main`.

Cuando haya una persona revisora independiente disponible, solo integra cambios mediante una PR aprobada por ella. En este proyecto de un único mantenedor, el autor puede fusionar su propia PR tras ejecutar las validaciones aplicables, realizar la autovalidación según los criterios documentados y dejar constancia en la PR de que no ha habido revisión independiente y de que el riesgo se acepta explícitamente. No uses cuentas alternativas para simular una aprobación independiente. Esta excepción deja de aplicar en cuanto exista una persona revisora independiente.
