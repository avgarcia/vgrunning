# UX-01 — Recorridos cognitivos sintéticos

**Estado:** Ejecutados — listos para revisión humana

**Fecha:** 2026-08-30

**Prototipo:** Maqueta interactiva UX-01 v0.2

**Responsable:** Revisor de producto

## Naturaleza de la evidencia

Estos documentos registran una revisión experta del prototipo mediante seis perfiles sintéticos. No hubo participantes reales. Por tanto:

- no contienen nombres, citas, tiempos ni comportamientos atribuidos a personas;
- no producen tasas de éxito ni veredictos individuales `Apto` o `No apto`;
- separan evidencia observable de hipótesis que deberá contrastar la beta;
- no demuestran usabilidad, accesibilidad con tecnología de asistencia ni preparación para producción.

La decisión y el trabajo pendiente se mantienen en el [plan de prueba v0.2](../ux-01-usability-test-plan.md).

## Evidencia común de las tareas aprobadas

| Tarea | Criterios | Evidencia reproducida en la maqueta v0.2 |
| --- | --- | --- |
| `T-UX01-01` | `CA-UX01-01`, `CA-UX01-02` | El acceso muestra `Mi plan` y `24–30 ago 2026 · Semana actual`. |
| `T-UX01-02` | `CA-UX01-03`, `CA-UX01-04` | `Hoy` abre `Sábado 29 · Rodaje cómodo · Presencial · Parque del Río`. |
| `T-UX01-03` | `CA-UX01-05`, `CA-UX01-06` | No hay valores preseleccionados; `Realizado`, esfuerzo `3` y `Normal` guardan con confirmación explícita. |
| `T-UX01-04` | `CA-UX01-07` | Intentar comentar muestra información; rechazar oculta el texto y permite guardar el seguimiento estructurado. |
| `T-UX01-05` | `CA-UX01-09` | `Historial` abre `Viernes 28 · Rodaje progresivo · Individual · Pendiente de seguimiento`. |
| `T-UX01-06` | `CA-UX01-08` | La retirada enumera las tres consecuencias y cambia el estado de `Otorgado` a `Retirado`. |
| `T-UX01-07` | `CA-UX01-12` | `Cerrar sesión` vuelve al acceso. |

Cobertura transversal:

- `CA-UX01-10`: el recurso propio conserva contenido y acciones en los tamaños comprobados; `ES-UX01-08` rechaza el no disponible con un mensaje genérico sin revelar causa ni propiedad;
- `CA-UX01-11`: las comprobaciones de reflow, etiquetas, objetivos y teclado aportan evidencia parcial; zoom `400 %`, texto `200 %` y tecnologías de asistencia reales continúan pendientes.

## Evidencia técnica común

- reflow comprobado a `320` y `736` píxeles sin desbordamiento horizontal;
- cero identificadores duplicados;
- cero controles visibles sin etiqueta;
- cero objetivos de interacción visibles menores de `24` píxeles;
- orden de teclado de correo a contraseña y después a `Iniciar sesión`;
- cero errores de consola durante los recorridos;
- estados comunicados mediante texto además del tratamiento visual.

No se simuló lector de pantalla, alto contraste del sistema, zoom `400 %` ni texto `200 %`. Esa cobertura continúa pendiente de revisión específica y de la beta con configuraciones reales.

## Perfiles y rotación

| Perfil | Contexto sintético | Estados |
| --- | --- | --- |
| [P01](p01-digital-experimentado.md) | Corredor activo con experiencia en planes digitales. | `ES-UX01-02`, `ES-UX01-04`, `ES-UX01-06` |
| [P02](p02-texto-ampliado.md) | Corredor activo que utiliza ampliación y teclado. | `ES-UX01-03`, `ES-UX01-05`, `ES-UX01-07` |
| [P03](p03-pdf-mensajeria.md) | Corredor activo acostumbrado a PDF y mensajería. | `ES-UX01-08`, `ES-UX01-09`, `ES-UX01-02` |
| [P04](p04-papel-libreta.md) | Corredor activo acostumbrado a papel o libreta. | `ES-UX01-04`, `ES-UX01-06`, `ES-UX01-03` |
| [P05](p05-baja-competencia-digital.md) | Corredor activo con baja competencia digital. | `ES-UX01-05`, `ES-UX01-07`, `ES-UX01-08` |
| [P06](p06-primer-plan-estructurado.md) | Corredor activo que sigue su primer plan estructurado. | `ES-UX01-09`, `ES-UX01-06`, `ES-UX01-07` |

## Hipótesis para la beta

Estas hipótesis no son hallazgos ni requisitos:

1. personas acostumbradas a aplicaciones deportivas podrían esperar sincronización o autoguardado;
2. personas acostumbradas a PDF, mensajería o papel podrían necesitar mayor refuerzo del guardado explícito;
3. `Privacidad de comentarios` y sus consecuencias podrían requerir lenguaje adicionalmente sencillo;
4. la escala de esfuerzo y la sensación podrían necesitar ayuda contextual para corredores principiantes;
5. las configuraciones reales de ampliación, alto contraste y tecnología de asistencia podrían revelar barreras no reproducidas en esta revisión.

Integraciones, chat, offline, modo oscuro, recuperación, calendario, dashboard y edición de perfil permanecen fuera de alcance.

## Conclusión

Las tareas y los estados son técnicamente recorribles y no presentan un defecto experto `S1` o `S2` reproducible en esta maqueta. La comprensión, utilidad y confianza de corredores reales siguen sin validar y constituyen riesgo residual del MVP hasta la beta cerrada.

## Revisión de criterios de aceptación

- Estado: listo para revisión humana
- Evidencia: tareas `T-UX01-01..07`, estados `ES-UX01-02..09` y comprobaciones técnicas comunes.
- Hallazgos: cada criterio dispone de una respuesta observable en el prototipo; esta revisión no afirma que una persona pueda completarla sin ayuda.
- Acción requerida: mantener las hipótesis separadas de los hallazgos y ejecutar la beta cuando exista el primer corredor disponible.
- Revisor humano: Revisores de producto y arquitectura

## Revisión de preguntas bloqueantes

- Estado: listo para revisión humana
- Evidencia: decisión de aplazamiento registrada en el plan v0.2.
- Hallazgos: la validación con corredores tiene responsable y trigger; no existe una afirmación falsa de validación humana.
- Acción requerida: reabrir la evidencia al iniciar la beta cerrada.
- Revisor humano: Revisor de producto
