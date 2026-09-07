# ADR-0024: Estrategia híbrida de validación y autoridad de la IA

**Estado:** Aceptado
**Fecha:** 2026-08-28
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0013](0013-application-runtime-framework.md)

## Contexto

`ADR-0013` fija controles deterministas de compilación, pruebas, contrato, calidad y seguridad. La preparación técnica los materializa en Gradle y GitHub Actions, pero una PR de una única superficie todavía puede activar trabajo no relacionado. Al mismo tiempo, una Skill o un agente puede identificar ambigüedades, revisar trazabilidad y preparar evidencia, pero no ofrece una prueba mecánica repetible ni una autoridad de aprobación.

La reducción de trabajo irrelevante no puede disminuir la cobertura de controles normativos ni dejar rutas sin clasificar. La estrategia debe conservar el gate integral en los contextos de mayor riesgo y permitir medir la mejora antes de volver selectivos los jobs de PR.

## Decisión

La validación adoptará una estrategia híbrida con estas reglas:

- Los gates deterministas de Gradle, npm y GitHub Actions son la única autoridad automática bloqueante. Las Skills y los agentes no podrán aprobar, omitir ni declarar innecesario un gate aplicable.
- La clasificación de alcance se basará exclusivamente en rutas y configuración versionadas. Una ruta desconocida, tooling, dependencias, supply chain o una clasificación fallida elevarán la validación a `qualityGate`.
- Un agente podrá proponer controles adicionales y preparar revisión semántica, pero nunca reducir el plan calculado. La revisión semántica será evidencia pendiente de revisión humana, no una aprobación independiente.
- El inventario integral seguirá ejecutándose en `main`, de forma nocturna y antes de release, además de las rutas que requieren `qualityGate`.
- La activación de jobs dirigidos se hará primero en shadow mode. Solo podrá activarse después de observar una primera slice funcional, comprobar que no desaparece ningún control de `ADR-0013` y demostrar una reducción mediana mínima del `30 %` de runner-minutes observados en PRs de una sola superficie.
- `runner-minutes observados` será la suma de las duraciones de jobs recogidas por GitHub Actions. Es una métrica comparativa y no representa la facturación de GitHub.

## Alternativas consideradas

### Alternativa A: Mantener `qualityGate` completo en todas las PR

Se conserva la cobertura, pero se mantiene el coste de ejecutar contenedores, Playwright y análisis no relacionados con el diff. Se descarta como estado final, aunque seguirá siendo el comportamiento de seguridad durante el shadow mode.

### Alternativa B: Sustituir gates por Skills o agentes

Se descarta. El juicio semántico no es reproducible, no garantiza compilación ni seguridad y no puede sustituir la evidencia mecánica ni la responsabilidad humana.

### Alternativa C: Clasificación híbrida y conservadora

Se propone. Mantiene el gate completo ante incertidumbre y en contextos de alto riesgo, y limita los jobs dirigidos a superficies que puedan clasificarse de forma determinista y verificable.

## Consecuencias

- La configuración de rutas y gates se convierte en una frontera de seguridad que deberá probarse con fixtures mínimos.
- Habrá una fase temporal con el flujo completo y la clasificación en sombra, por lo que la optimización no será inmediata.
- Los informes de línea base y de clasificación serán artefactos efímeros; no se versionarán datos de GitHub ni credenciales.
- El resumen agregado futuro podrá simplificar la protección de `main`, pero no antes de demostrar equivalencia de controles y de validar el ruleset efectivo.
- Esta decisión no cambia umbrales, versiones ni herramientas de `ADR-0013`.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-21`, de forma indirecta mediante la preservación de los controles que protegen su implementación.

## Decisiones de Fase 1 relacionadas

- `D-03`: el PMV mantiene una unidad de despliegue y una cadena de validación común.

## Validación prevista

- Medir seis muestras completas de PR y cuatro de `main` para la línea base inicial, con duración por workflow, job y paso, runner, comandos, artefactos y estado observable de caché. Las comparaciones posteriores usarán diez PRs y cinco de `main` cuando el historial las contenga.
- Probar con fixtures que la clasificación futura conserva controles, que una ruta desconocida eleva a `qualityGate` y que un agente no puede reducir el plan.
- Ejecutar tres comparaciones limpias entre el flujo completo y el dirigido antes de activar jobs selectivos.
- Verificar que el ruleset no queda nunca sin checks requeridos durante la migración a `validation-summary`.

## Decisiones pendientes

- **Resuelto — aceptación formal.** ADR-0024 fue aceptado el 2026-08-28 por el Revisor de arquitectura. `TECH-02.2` queda autorizado respecto de esta decisión, aunque mantiene sus propias dependencias y validaciones.
- **Ampliación de la línea base inicial — no bloqueante.** Responsable: Revisor de arquitectura. Tratamiento: la muestra queda limitada a seis PRs completas y cuatro conjuntos de `main` porque GitHub solo conserva ese historial completo al 2026-08-28; la siguiente medición se ampliará a diez PRs y cinco de `main` cuando exista historial suficiente.
