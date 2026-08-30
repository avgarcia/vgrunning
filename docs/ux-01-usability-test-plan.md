# UX-01 — Plan de prueba de usabilidad

**Estado:** Aprobado — revisión experta ejecutada; validación con corredores aplazada
**Versión:** 0.2
**Fecha:** 2026-08-30
**Responsable:** Revisor de producto
**Ámbito:** Prototipo interactivo del portal web de un corredor `active`

## Propósito

Este plan define una primera ronda moderada para comprobar si corredores reales o potenciales pueden completar los recorridos críticos de [UX-01](ux-01-runner-portal-specification.md) y comprender sus estados sin ayuda del equipo.

La prueba genera evidencia cualitativa para corregir la experiencia antes de implementar. No demuestra conformidad WCAG, significación estadística, licitud del tratamiento ni preparación para producción. La evaluación con participantes complementa, pero no sustituye, los gates técnicos y la revisión de accesibilidad exigidos por el proyecto.

## Decisión de ejecución para el MVP

El Revisor de producto aprobó la especificación y este plan el `2026-08-30`, y confirmó que no dispone actualmente de seis corredores para ejecutar la ronda moderada. La validación con corredores se aplaza deliberadamente hasta la primera beta cerrada del MVP y conserva al Revisor de producto como responsable.

Para no fabricar evidencia, los seis perfiles previstos se utilizan antes de la beta únicamente en [recorridos cognitivos sintéticos](ux-01-cognitive-walkthroughs/README.md) ejecutados por revisión experta contra la maqueta v0.2. Estos recorridos:

- comprueban que tareas, estados, controles y respuestas observables existen y pueden recorrerse;
- registran evidencia reproducible del prototipo y riesgos como hipótesis;
- no inventan tiempos, citas, preferencias, errores humanos ni porcentajes de éxito;
- no se presentan como sesiones con participantes ni validan comprensión o utilidad real;
- no sustituyen pruebas con tecnologías de asistencia reales ni autorizan producción.

El riesgo residual de usabilidad se acepta exclusivamente para continuar la preparación del MVP. La experiencia no podrá declararse validada con corredores hasta disponer de evidencia de la beta cerrada.

Fuentes metodológicas de referencia:

- [Using moderated usability testing — GOV.UK](https://www.gov.uk/service-manual/user-research/using-moderated-usability-testing);
- [Finding participants for user research — GOV.UK](https://www.gov.uk/service-manual/user-research/find-user-research-participants);
- [Involving Users in Evaluating Web Accessibility — W3C WAI](https://www.w3.org/WAI/test-evaluate/involving-users/).

## Preguntas de investigación

1. ¿Identifica el corredor `Mi plan` como destino inicial y comprende qué semana está consultando?
2. ¿Localiza el entrenamiento de hoy, su modalidad y el lugar publicado sin confundir huecos con entrenamientos?
3. ¿Comprende los estados `Pendiente de seguimiento`, `Sin seguimiento`, `Realizado`, `No realizado` y `Retirado`?
4. ¿Registra un seguimiento válido y distingue `Guardar` de `Cancelar` sin esperar autoguardado?
5. ¿Entiende que el comentario es opcional y que rechazar el consentimiento no impide guardar los campos estructurados?
6. ¿Encuentra un entrenamiento anterior mediante `Historial` y entiende la carga progresiva?
7. ¿Encuentra `Privacidad de comentarios` y puede explicar las tres consecuencias de retirar el consentimiento?
8. ¿Reconoce los estados sin plan, fecha futura, sesión caducada, conflicto, ventana cerrada y recurso no disponible?
9. ¿Puede cerrar la sesión sin confundir la acción con cerrar el menú o abandonar la página?
10. ¿La experiencia se percibe sobria, clara, confiable y deportiva sin sacrificar legibilidad?

## Método previsto para la beta cerrada

- Prueba de usabilidad moderada, individual y basada en tareas.
- `6` participantes en la primera ronda.
- Sesiones de `45` a `55` minutos.
- Móvil como superficie principal; al menos dos sesiones repetirán una tarea crítica en escritorio.
- Pensamiento en voz alta durante las tareas.
- Una persona moderadora y una persona tomando notas.
- Prototipo y credenciales exclusivamente sintéticos.
- Sin grabación por defecto. Grabar audio, vídeo o pantalla exige aprobación previa, consentimiento informado y tratamiento documentado del material.
- Un piloto interno previo comprueba tiempos, consignas y estados, pero sus resultados no cuentan como evidencia con usuarios.

Una ronda pequeña sirve para localizar problemas, no para inferir porcentajes sobre toda la población. Los resultados se expresarán como conteos y evidencia observada, no como significación estadística.

## Revisión experta previa al MVP

El `2026-08-30` se ejecutaron seis recorridos cognitivos sintéticos sobre una única apertura controlada de la maqueta v0.2:

1. las siete tareas `T-UX01-01..07` completaron sus resultados observables;
2. los estados `ES-UX01-02..09` reprodujeron el mensaje y las acciones definidos;
3. el reflow a `320` y `736` píxeles no presentó desbordamiento horizontal;
4. no se detectaron identificadores duplicados, controles visibles sin etiqueta ni objetivos de interacción menores de `24` píxeles;
5. el orden de teclado avanzó desde correo a contraseña y después a `Iniciar sesión`;
6. no se registraron errores de consola.

Esta evidencia demuestra que el instrumento y el prototipo son recorribles. No demuestra que un corredor encuentre, comprenda o acepte esos recorridos sin ayuda.

## Participantes

### Criterios de inclusión

- persona de `18` años o más;
- corredor actual o potencial que consulta o podría consultar entrenamientos mediante una web;
- uso habitual de teléfono móvil;
- capacidad para participar en castellano;
- aceptación informada de la sesión y de la toma de notas.

### Variación buscada

- experiencia deportiva y digital diversa;
- personas que ya usan alguna herramienta digital de entrenamiento y personas que no;
- diferentes tamaños de pantalla y sistemas móviles;
- cuando sea viable, personas que utilicen zoom, ampliación, teclado u otra tecnología de asistencia en su configuración habitual.

No se concluirá que una observación de una persona con discapacidad representa a todas las personas con la misma discapacidad. La cobertura con tecnologías de asistencia se documentará y continuará en rondas específicas si no puede incluirse adecuadamente en esta.

### Exclusiones de la muestra

- personas que hayan diseñado o implementado UX-01;
- participantes cuya relación directa con el equipo les impida criticar con libertad;
- menores de edad;
- uso de planes, comentarios, credenciales o información de salud reales durante las tareas.

El canal de reclutamiento queda por confirmar. Antes de contactar participantes, el responsable debe documentar información, consentimiento, acceso y conservación de los datos de investigación. La aplicación y el prototipo nunca almacenarán esos datos.

## Roles

| Rol | Responsabilidad |
| --- | --- |
| Moderador | Presentar la sesión, leer consignas sin pistas, observar, intervenir solo según el protocolo y cerrar la sesión. |
| Persona de notas | Registrar acciones, frases, dudas, ayudas, resultado y tiempo orientativo mediante identificadores `P01..P06`. |
| Observador | Solo cuando aporte valor; permanece en silencio y no contacta al participante durante la sesión. |
| Revisor de producto | Aprueba el protocolo, clasifica hallazgos y decide si una propuesta cambia alcance. |
| Revisor de accesibilidad | Separa problemas generales, barreras técnicas y problemas de tecnología de asistencia. |

## Materiales

1. Prototipo interactivo UX-01 v0.2 con `Rendimiento sereno` y `Atlántico sereno`.
2. Selector de escenario disponible solo para la persona facilitadora.
3. Credenciales y contenido sintéticos.
4. Guion de introducción y cierre.
5. Hoja de observación por participante y matriz de síntesis.
6. Información y consentimiento de investigación aprobados antes del reclutamiento.
7. Dispositivo del participante cuando use una configuración de accesibilidad personal.

El selector de escenarios pertenece al entorno de prueba y no forma parte del producto.

## Escenarios sintéticos requeridos

| ID | Estado inicial | Resultado visible esperado | Trazabilidad |
| --- | --- | --- | --- |
| `ES-UX01-01` | Semana actual con publicación | Hoy, tarjetas, detalle y seguimiento disponibles según fecha. | `CA-UX01-02..05` |
| `ES-UX01-02` | Credenciales o estado inválidos | Error genérico sin revelar existencia, rol o estado de la cuenta. | `CA-UX01-01` |
| `ES-UX01-03` | Sesión caducada | Retorno al acceso con explicación y sin contenido protegido operativo. | `CA-UX01-01`, `CA-UX01-12` |
| `ES-UX01-04` | Semana actual sin plan | Mensaje `No tienes un plan publicado para esta semana` y acceso explícito a la siguiente semana publicada cuando exista. | `CA-UX01-02` |
| `ES-UX01-05` | Semana futura publicada | Contenido publicado sin estado ni acción de seguimiento antes de cada fecha. | `CA-UX01-02`, `CA-UX01-03` |
| `ES-UX01-06` | Conflicto por republicación al guardar | Rechazo completo, valores locales todavía visibles y acción explícita de recarga. | `CA-UX01-06` |
| `ES-UX01-07` | Ventana de seguimiento cerrada | Detalle en lectura, `Sin seguimiento` o registro existente y ausencia de acción de edición. | `CA-UX01-05`, `CA-UX01-06` |
| `ES-UX01-08` | Recurso ajeno o no disponible | Mensaje genérico sin distinguir inexistencia, propiedad, inactividad o retención. | `CA-UX01-10` |
| `ES-UX01-09` | Historial vacío | Estado informativo sin incorporar futuros, borradores ni datos ajenos. | `CA-UX01-09`, `CA-UX01-10` |

Los escenarios son fixtures de investigación, no datos de negocio ni respuestas exhaustivas del backend.

## Estructura de la sesión

| Fase | Duración orientativa | Contenido |
| --- | ---: | --- |
| Recepción | 5 min | Consentimiento, objetivo, recordatorio de que se evalúa la interfaz y no a la persona. |
| Contexto | 5 min | Experiencia corriendo, uso digital y dispositivo, sin solicitar datos de salud. |
| Tareas principales | 25 min | Acceso, plan, seguimiento, historial, privacidad y salida. |
| Estados rotatorios | 10 min | Tres estados de error o límite asignados al participante. |
| Cierre | 5 min | Expectativas, dudas, tres adjetivos y comentario final. |

Si la sesión alcanza `55` minutos, se detiene. Las tareas omitidas se reasignan a otra persona; no se presiona al participante para completar el guion.

## Guion de moderación

### Introducción

La persona moderadora explicará:

- estamos evaluando la interfaz, no al participante;
- no existen respuestas correctas esperadas por la persona moderadora;
- puede abandonar o descansar en cualquier momento;
- los datos visibles son sintéticos;
- debe evitar introducir información personal, lesiones, diagnósticos o datos de salud;
- puede pensar en voz alta, pero no está obligada a justificar cada acción.

No se explicará la arquitectura de información ni se nombrarán los botones que resuelven las tareas.

### Intervenciones permitidas

La persona moderadora puede preguntar:

- «¿Qué esperas que ocurra?»;
- «¿Qué estás buscando?»;
- «¿Qué significa esto para ti?»;
- «¿Qué harías ahora?».

Solo se presta ayuda directa cuando el participante declara que no puede continuar o permanece bloqueado durante aproximadamente dos minutos. La ayuda se registra y la tarea deja de contar como completada sin ayuda.

## Tareas principales

| ID | Consigna neutral | Preparación | Éxito observable | Criterios |
| --- | --- | --- | --- | --- |
| `T-UX01-01` | «Entra en la aplicación para consultar tu entrenamiento.» | Acceso con credenciales sintéticas válidas. | Abre `Mi plan` y reconoce que ve la semana actual. | `CA-UX01-01`, `CA-UX01-02` |
| `T-UX01-02` | «Averigua qué entrenamiento tienes hoy y dónde se realiza.» | `ES-UX01-01`. | Abre el día correcto e identifica tipo, modalidad y lugar sin interpretar huecos como sesiones. | `CA-UX01-03`, `CA-UX01-04` |
| `T-UX01-03` | «Registra que lo has realizado, con esfuerzo moderado y sensación normal.» | Detalle dentro de ventana. | Elige `Realizado`, `3 Moderado`, `Normal` y guarda explícitamente. | `CA-UX01-05`, `CA-UX01-06` |
| `T-UX01-04` | «Quieres añadir una nota, pero finalmente prefieres continuar sin autorizar comentarios. Guarda el resto.» | Sin consentimiento vigente. | Intenta comentar, rechaza y guarda el seguimiento estructurado. | `CA-UX01-07` |
| `T-UX01-05` | «Busca el entrenamiento del viernes anterior y consulta la información guardada.» | Historial con más de una semana. | Encuentra `Historial`, abre el elemento correcto y comprende el estado. | `CA-UX01-09` |
| `T-UX01-06` | «Averigua cómo impedir futuros comentarios y explícame qué ocurrirá.» | Consentimiento `granted`. | Encuentra privacidad, explica las tres consecuencias y confirma la retirada. | `CA-UX01-08` |
| `T-UX01-07` | «Has terminado. Sal de la aplicación.» | Sesión activa. | Revoca la sesión y vuelve al acceso. | `CA-UX01-12` |

La consigna puede aportar valores sintéticos necesarios para completar la tarea, pero nunca el nombre del destino, control o botón que debe utilizarse.

## Asignación de estados rotatorios

Cada participante recibe tres estados después de las tareas principales. La asignación garantiza al menos dos exposiciones por estado salvo ausencia de última hora:

| Participante | Estados |
| --- | --- |
| `P01` | `ES-UX01-02`, `ES-UX01-04`, `ES-UX01-06` |
| `P02` | `ES-UX01-03`, `ES-UX01-05`, `ES-UX01-07` |
| `P03` | `ES-UX01-08`, `ES-UX01-09`, `ES-UX01-02` |
| `P04` | `ES-UX01-04`, `ES-UX01-06`, `ES-UX01-03` |
| `P05` | `ES-UX01-05`, `ES-UX01-07`, `ES-UX01-08` |
| `P06` | `ES-UX01-09`, `ES-UX01-06`, `ES-UX01-07` |

Si falta un participante, se reasignan primero los estados que hayan quedado con una sola exposición. Las exposiciones no convierten los ejemplos en criterios exhaustivos.

## Registro de evidencia

Para cada tarea se registra:

- `sin-ayuda`: completada sin indicación directa;
- `con-ayuda`: completada después de una pista o indicación;
- `no-completada`;
- ruta seguida y retrocesos;
- errores observados;
- expectativas verbalizadas;
- palabras o etiquetas que causan confusión;
- tiempo aproximado como diagnóstico, nunca como objetivo de rendimiento;
- criterio `CA-UX01` afectado;
- cita breve anonimizada solo cuando sea necesaria para entender el hallazgo.

No se registra nombre, correo, ubicación, plan real, comentario real, dato de salud ni diagnóstico. Las notas usan exclusivamente `P01..P06` y se conservan según el tratamiento aprobado antes del reclutamiento.

## Severidad de hallazgos

| Nivel | Definición | Tratamiento |
| --- | --- | --- |
| `S1 — Bloqueante` | Impide completar un recorrido crítico, expone datos o provoca una interpretación peligrosa de privacidad. | Detener cualquier declaración de preparación; corregir y repetir. |
| `S2 — Serio` | Exige ayuda, provoca una acción equivocada persistente o aparece en dos o más participantes. | Corregir antes de implementar la interacción afectada y repetir la tarea. |
| `S3 — Moderado` | Causa duda o retroceso recuperable sin ayuda. | Priorizar según frecuencia, impacto y coste; documentar decisión. |
| `S4 — Menor` | Preferencia o fricción cosmética sin afectar comprensión o finalización. | Considerar sin desplazar problemas funcionales. |

Una petición de nueva funcionalidad no se convierte automáticamente en hallazgo de usabilidad ni en requisito. Se registra aparte como propuesta de alcance y requiere decisión de producto.

## Criterio de salida de la ronda

La ronda está lista para revisión humana cuando:

1. no existe ningún `S1` abierto;
2. toda persona que ejecutó la retirada explica correctamente las tres consecuencias antes de confirmarla;
3. ninguna tarea principal requiere ayuda en dos o más participantes sin una corrección y nueva comprobación;
4. rechazar el comentario no se interpreta como impedimento para guardar seguimiento estructurado;
5. los estados textuales se comprenden sin depender de color o iconos;
6. cada estado rotatorio tiene al menos dos exposiciones o se declara explícitamente la cobertura incompleta;
7. cada hallazgo conserva evidencia, severidad, criterio afectado, responsable y tratamiento;
8. la conclusión declara muestra, dispositivos, cobertura de accesibilidad y limitaciones.

Superar estos criterios no demuestra conformidad WCAG ni preparación para producción. La implementación mantiene pruebas automáticas, revisión manual, `320 CSS px`, zoom `400 %`, texto `200 %`, teclado y tecnologías de asistencia según la [especificación UX-01](ux-01-runner-portal-specification.md#adaptabilidad-y-accesibilidad).

## Análisis y decisión

Después de cada jornada:

1. moderador y persona de notas comparan observaciones sin fusionar problemas distintos;
2. cada observación se vincula a una tarea, escenario y criterio;
3. se separan evidencia, interpretación y propuesta;
4. el Revisor de producto clasifica cualquier posible cambio de alcance;
5. el Revisor de accesibilidad clasifica barreras técnicas o de tecnología de asistencia;
6. se priorizan primero `S1`, después `S2`, comprensión y finalmente preferencia visual;
7. se prepara una nueva versión del prototipo para la siguiente ronda.

El informe no presenta porcentajes de éxito como representativos de todos los corredores, no calcula un ranking visual y no declara una dirección ganadora nueva. `Rendimiento sereno` y `Atlántico sereno` permanecen confirmados salvo decisión humana posterior.

## Entregables de la ronda

- matriz anonimizada de tareas por participante;
- inventario de hallazgos con evidencia y severidad;
- cobertura de `CA-UX01-01..12` y estados `ES-UX01-01..09`;
- cambios propuestos separados entre corrección, decisión de producto y trabajo de implementación;
- limitaciones de muestra, dispositivos y accesibilidad;
- recomendación `repetir`, `listo para implementación de la slice afectada` o `requiere decisión`, sin autorizar merge o producción.

## Precondiciones para ejecutar

- [x] Revisor de producto aprueba preguntas, tareas y criterios de salida.
- [x] El prototipo v0.2 contiene y permite reiniciar los nueve escenarios sintéticos.
- [x] La revisión experta completa las tareas y estados sin defectos técnicos bloqueantes del prototipo.
- [x] La revisión experta registra tamaño de referencia, teclado y límites de accesibilidad sin atribuirlos a personas.
- [ ] Antes de la beta se confirma el canal de reclutamiento.
- [ ] Antes de la beta se aprueban información, consentimiento y conservación de datos de investigación.
- [ ] Antes de la beta se decide expresamente cualquier grabación; por defecto queda deshabilitada.
- [ ] Antes de la beta el moderador y la persona de notas conocen el protocolo de intervención.

## Revisión de criterios de aceptación

- Estado: listo para revisión humana
- Evidencia: tareas `T-UX01-01..07`, escenarios `ES-UX01-01..09`, trazabilidad con `CA-UX01-01..12` y recorridos cognitivos sintéticos ejecutados.
- Hallazgos: la evidencia experta cubre éxito, error y límite sin presentarse como prueba con usuarios; reclutamiento, tratamiento de datos y comprensión real permanecen aplazados hasta la beta cerrada.
- Acción requerida: ejecutar las precondiciones pendientes antes de contactar participantes y no declarar validación con corredores hasta analizar esa beta.
- Revisor humano: Revisores de producto y arquitectura

## Revisión de preguntas bloqueantes

- Estado: listo para revisión humana
- Evidencia: decisión del Revisor de producto del `2026-08-30` y sección `Decisión de ejecución para el MVP`.
- Hallazgos: la falta de participantes se aplaza deliberadamente a la beta cerrada con responsable y trigger explícitos; no bloquea la documentación ni convierte la evidencia sintética en validación humana.
- Acción requerida: mantener visible el riesgo residual y reabrir este plan al incorporar al primer corredor de la beta.
- Revisor humano: Revisor de producto

## Historial de versiones

| Versión | Fecha | Cambio |
| --- | --- | --- |
| 0.2 | 2026-08-30 | Registra la aprobación humana, ejecuta seis recorridos cognitivos sintéticos y aplaza la validación con corredores a la beta cerrada del MVP. |
| 0.1 | 2026-08-29 | Primera versión para revisión humana; define método, muestra, tareas, escenarios, evidencia y criterios de salida. |
