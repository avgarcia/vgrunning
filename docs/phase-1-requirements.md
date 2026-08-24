# Requisitos funcionales y no funcionales — Fase 1

**Estado:** Validado — Fase 1 cerrada
**Fecha:** 2026-08-10
**Última actualización:** 2026-08-24 — decisiones H-01 a H-20 de la auditoría documental incorporadas para nueva revisión

## Actores y permisos

| Actor | Responsabilidades PMV |
| --- | --- |
| Administrador | Da de alta e invita usuarios; asigna roles; mantiene las definiciones de etiquetas y sus valores permitidos; gobierna el ciclo de vida de los corredores y puede consultar inactivos mediante acceso auditado. |
| Entrenador | Gestiona globalmente los corredores `active`, etiquetas, segmentos, planes semanales, entrenamientos e información de seguimiento. El alcance no se restringe por entrenador en el PMV, pero excluye cualquier corredor no activo. |
| Corredor | Consulta sus planes y entrenamientos, registra su ejecución y envía información de seguimiento sobre ellos. |

El corredor solo accede a sus propios datos mientras permanece `active`. El administrador accede a los datos operativos según finalidad y estado; el entrenador solo recibe datos de corredores `active` y no puede localizar pendientes, inactivos o cancelados.

## Requisitos funcionales priorizados (MoSCoW)

### Imprescindible

- **RF-01.** Invitación de corredores por el administrador mediante correo electrónico; activación con correo electrónico, contraseña y restablecimiento de contraseña. El PMV admite exclusivamente personas de `18` años o más: el administrador declara esa condición al invitar y la persona invitada la confirma durante la activación inicial; ambas declaraciones son obligatorias y se conservan con actor, origen, instante y versión del texto, sin fecha de nacimiento, documento ni copia acreditativa.
- **RF-02.** Gestión de cuentas, asignación inicial de un único rol inmutable, definiciones de etiquetas y sus valores permitidos por el administrador. Un rol no puede modificarse después de crear la cuenta.
- **RF-03.** Etiquetas controladas para clasificar corredores y segmentos dinámicos construidos a partir de reglas de etiquetas. No se permite crear etiquetas ni valores libres.
- **RF-04.** Modalidad en línea o presencial representada mediante una etiqueta controlada del corredor y modalidad propia del entrenamiento. Ambos datos son informativos e independientes: la modalidad del corredor no restringe grupos, planes, publicaciones ni modalidad de los entrenamientos. El lugar de encuentro es texto libre opcional de un entrenamiento presencial, tampoco bloquea su publicación y no se limita al Retiro.
- **RF-05.** Reglas de segmentos en el PMV limitadas a condiciones sobre etiquetas con operador Y entre criterios, selección de uno o varios valores permitidos dentro de cada etiqueta y exclusión manual de corredores. No se incluyen expresiones avanzadas ni reglas libres.
- **RF-06.** Inclusión y exclusión manual de corredores en un segmento.
- **RF-07.** Creación de un plan semanal que agrupe varios entrenamientos fechados.
- **RF-08.** Gestión de grupos de planificación que combinan uno o varios segmentos y, excepcionalmente, inclusiones o exclusiones individuales persistentes. Cada plan semanal pertenece a un único grupo y no se asigna directamente a segmentos o corredores. Un grupo o plan puede contener corredores de ambas modalidades sin bloqueo, exclusión, advertencia ni confirmación adicional. Las excepciones solo cambian membresía: todos los miembros reciben el mismo contenido del plan.
- **RF-09.** Publicación atómica del plan semanal: todos sus entrenamientos se hacen visibles a la vez.
- **RF-10.** Registro de la versión publicada y de los destinatarios efectivos de cada publicación, para conservar trazabilidad si cambian las etiquetas, segmentos o asignaciones posteriores.
- **RF-11.** Catálogo de entrenamientos: rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas y carrera/test.
- **RF-12.** Objetivos por frecuencia cardiaca o ritmo relativo al corredor, según el tipo de entrenamiento, y texto libre de aclaraciones. La aplicación representa `Z1..Z5` o una distancia y desviación relativas a las referencias externas acordadas entre corredor y entrenador; no almacena, calcula ni valida zonas, marcas o ritmos personales y no bloquea la publicación si el corredor desconoce esa referencia.
- **RF-13.** Lugar de encuentro opcional e informativo en entrenamientos presenciales. Su ausencia no bloquea guardado ni publicación, queda diferenciada de un lugar informado y nunca se completa desde otro entrenamiento.
- **RF-14.** Estados del plan: borrador y publicado.
- **RF-15.** Administrador o entrenador puede editar uno o varios entrenamientos futuros de un plan publicado mediante una única republicación atómica; el entrenamiento de hoy y los anteriores permanecen inmutables. Se considera cambio relevante cualquier diferencia canónica visible para el corredor en entrenamientos futuros: alta o retirada de un día, modalidad, estructura, carga, objetivo, recuperación, aclaración o ubicación. Quedan excluidos los cambios solo técnicos o de auditoría y una propuesta canónicamente idéntica. Cada nueva versión crea una solicitud de notificación para cada destinatario efectivo congelado y la entrega aplica `RF-20`.
- **RF-16.** Consulta web adaptable y accesible de planes, entrenamientos y lugar de encuentro por el corredor, conforme a WCAG `2.2` nivel `AA` en todo el PMV web.
- **RF-17.** Registro por entrenamiento: realizado/no realizado, esfuerzo percibido entero `1..5`, sensación general con valores bien/normal/mal y comentario opcional. La pregunta será «¿Cuánto esfuerzo te supuso este entrenamiento?», sin valor por defecto, y mostrará el catálogo `1 Muy suave`, `2 Suave`, `3 Moderado`, `4 Intenso`, `5 Muy intenso`; solo se persiste el entero y la escala no representa un máximo fisiológico ni una escala clínica.
- **RF-18.** Historial básico de entrenamientos e información de seguimiento del corredor.
- **RF-19.** Vista de consulta para que administrador y entrenador analicen información de seguimiento por corredor, plan semanal y entrenamiento, con visibilidad de estado realizado/no realizado, esfuerzo, sensación y comentario. Para el entrenador incluye únicamente corredores `active` y no constituye una bandeja de trabajo: no garantiza lectura humana ni incorpora estado revisado, prioridad, notas, respuesta, asignación o SLA.
- **RF-20.** Cuando administrador o entrenador confirma la publicación o republicación de un plan semanal, el sistema crea una solicitud lógica para cada destinatario efectivo congelado. Al comenzar a procesarla obtiene conjuntamente el estado `active` y el correo vigente y verificado del corredor; si es elegible, congela ese correo para todos los intentos y reconciliaciones de la solicitud actual. Un cambio posterior de correo se descarta para ese envío y solo afecta a solicitudes futuras. Si el corredor no está `active`, termina como `omitido-inactivo`; si la elegibilidad no puede resolverse antes de `createdAt + 120 minutos`, termina como `fallo-definitivo/elegibilidad-no-resuelta`, alerta y libera el orden, sin reapertura, recreación ni envío manual. Una reactivación solo habilita solicitudes de versiones futuras. El correo incluye semana, resumen y enlace al plan publicado. No se incluyen otros eventos de notificación.
- **RF-21.** Administrador y entrenador pueden consultar, para una semana seleccionada —la actual por defecto—, la cobertura de todos los corredores `active`: resumen por estado y lista paginada con exactamente uno de `cubierto`, `sin-grupo`, `grupo-sin-plan`, `plan-en-borrador` o `fuera-de-publicacion`. La información se calcula bajo demanda desde el estado vigente de corredores, grupos, planes y publicaciones; no bloquea ninguna operación, no crea una proyección persistente y muestra `sin-modalidad` como indicador informativo independiente.

### Deseable

- Filtros y búsquedas operativas de corredores, segmentos y planes.
- Registro básico del estado de entrega del correo electrónico de publicación o republicación.

### Opcional

- Duplicación de planes semanales como plantilla operativa.
- Exportación básica de planes o información de seguimiento.

### Fuera del PMV

- Chat o mensajería interna.
- Integración con WhatsApp, Strava, Garmin u otras fuentes de actividad.
- Aplicaciones móviles nativas.
- Uso sin conexión como prioridad.
- Reservas, asistencia, aforo, pagos, cancelaciones o asignación de entrenadores para sesiones presenciales.
- Métricas de salud adicionales: dolor/lesiones, fatiga o sueño.
- Comercialización del producto a otros clubes y soporte multiclub.

## Matriz de trazabilidad de Fase 1 a Fase 2

Al cerrar Fase 1, esta matriz fue el contrato de entrada para Fase 2. Tras el cierre de Fase 2, cada fila conserva el elemento solicitado y registra su materialización; los diseños y criterios enlazados continúan siendo obligatorios antes de implementar.

| Requisito de Fase 1 | Elemento de diseño requerido en Fase 2 | Estado |
| --- | --- | --- |
| RF-01 | Flujo de invitación, activación, inicio y restablecimiento de contraseña | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-02 | Modelo de cuentas, rol inicial inmutable, perfiles y administración de taxonomías | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-03 | Modelo de etiquetas y evaluación de segmentos dinámicos | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-04 | Modelo de modalidad y ubicación de entrenamiento | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-05 | Semántica y límites de reglas de segmentación | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-06 | Interacción y persistencia de inclusiones y exclusiones manuales | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-07 | Modelo y ciclo de vida del plan semanal | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-08 | Modelo de grupos de planificación, segmentos y excepciones persistentes | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-09 | Flujo y consistencia de publicación atómica | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-10 | Versionado y registro de destinatarios efectivos | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-11 | Modelo de catálogo y tipos de entrenamiento | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-12 | Modelo de objetivos por frecuencia cardiaca, ritmo y aclaraciones | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-13 | Captura y consulta del lugar de encuentro presencial | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-14 | Máquina de estados de borrador y publicado | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-15 | Flujo de republicación y destinatarios afectados | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-16 | Experiencia móvil de consulta para corredores | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-17 | Modelo y captura de información de seguimiento | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-18 | Modelo de historial de entrenamientos y seguimiento | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-19 | Consulta y permisos de revisión para entrenadores | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-20 | Contrato de contenido, elegibilidad, correo vigente y entrega de correo electrónico | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |
| RF-21 | Vista semanal informativa de cobertura de corredores activos | [Materializado y trazado en Fase 2](phase-2-high-level-design.md#trazabilidad-de-requisitos) |

La comprobación de que cada requisito `RF-01` a `RF-21` enlaza con diseño, criterios de aceptación y decisión técnica se registra en [Cierre documental — Fase 2](phase-2-closure.md). En el flujo de un único mantenedor, el autor asume la revisión y la PR de cierre conserva la evidencia, la ausencia de revisión independiente y la aceptación expresa del riesgo.

## Requisitos no funcionales

| Área | Requisito acordado |
| --- | --- |
| Canal | Aplicación web adaptable, plenamente utilizable desde móvil. |
| Accesibilidad | Todo el PMV web cumple WCAG `2.2` nivel `AA`, incluidos reflow a `320 CSS px`, zoom del navegador al `400 %`, texto al `200 %`, navegación por teclado, foco visible, etiquetas, errores y tamaño mínimo de objetivos `24 × 24 CSS px` con las excepciones normativas. |
| Escala | Más de 500 corredores registrados; picos iniciales inferiores a 100 usuarios concurrentes. |
| Disponibilidad | Nivel normal de SaaS; no se ha definido un SLA formal. |
| Conectividad | Sin requisito de uso sin conexión como prioridad. |
| Seguridad | Autenticación por correo electrónico y contraseña; acceso por rol y aislamiento de los datos del corredor. |
| Notificaciones | Correo electrónico en publicación y republicación de planes. |
| Datos | La aplicación gestiona datos personales e información de seguimiento declarada; cumplimiento RGPD, retención y derechos de acceso son precondiciones de salida a producción. |

## Decisiones de diseño con impacto funcional

- Un segmento basado en etiquetas es dinámico. Las etiquetas y sus valores provienen de una taxonomía cerrada administrada por el administrador. La aplicación debe conservar los destinatarios efectivos de cada publicación para que cambios posteriores de etiquetas no alteren retrospectivamente un plan ya publicado.
- La modalidad del corredor se modela con etiquetas controladas para evitar un segundo sistema paralelo de clasificación, pero es informativa y no restringe grupos, planes ni entrenamientos de modalidad distinta.
- El PMV se limita a la operación interna de un único club. La comercialización a otros clubes y cualquier requisito de multiclub quedan fuera de alcance; no se diseñará aislamiento organizativo en esta fase.
- El Retiro es el contexto operativo inicial, no una restricción de producto. Cada entrenamiento presencial puede indicar cualquier lugar de encuentro mediante texto libre.
- Las reglas de segmentos se limitan deliberadamente en el PMV. Si el club necesita lógica compleja, se revisará después de validar que los criterios operativos simples reducen trabajo manual.
- Un cambio relevante en un plan publicado no es una edición silenciosa: republica el plan semanal completo y comunica el cambio por correo electrónico.
- Recoger información de seguimiento sin vista de revisión no aporta valor operativo. Por eso la vista mínima de seguimiento para entrenadores forma parte del PMV.
- Los entrenadores tienen permisos globales por simplicidad del PMV. Esta decisión no debe confundirse con un modelo de permisos definitivo.
- La cobertura semanal de corredores `active` se presenta como diagnóstico informativo calculado bajo demanda; no impide crear, publicar o republicar planes.
- El PMV está restringido a personas de `18` años o más mediante dos declaraciones sin recopilar fecha de nacimiento o documentos.
- La accesibilidad WCAG `2.2 AA` se aplica a toda la aplicación web y forma parte del contrato verificable, no de una mejora posterior.

## Supuestos cerrados para Fase 1

- La información de seguimiento se asociará al entrenamiento publicado para el corredor dentro de un plan semanal, tanto si declara `realizado` como `no-realizado`.
- No se recopilarán datos de salud especiales en el PMV; si se incorporan lesiones o similares, el análisis de privacidad deberá ampliarse.
- La primera versión no necesita reglas avanzadas de segmentación para demostrar mejora operativa frente al envío manual de archivos PDF.
- El correo electrónico actúa como notificación de disponibilidad del plan, no como canal principal de gestión del entrenamiento.

## Criterios de cierre de Fase 1

- El alcance del PMV queda delimitado mediante requisitos imprescindibles, deseables, opcionales y fuera de alcance.
- Los riesgos principales de Fase 0 quedan tratados: planificación por grupos, información de seguimiento revisable, presencial con lugar de encuentro y correo como notificación, no como sistema de gestión.
- Las decisiones que condicionan Fase 2 quedan cerradas: segmentos dinámicos con etiquetas controladas, trazabilidad de publicaciones, republicación atómica, modalidad como etiqueta, información de seguimiento mínima estructurada, operación de un único club y lugar de encuentro no restringido al Retiro.
- Las decisiones de diseño, sus motivos, alternativas descartadas, impactos y materialización prevista están documentadas en [Matriz de decisiones de Fase 1](phase-1-decision-matrix.md).
- Al cerrar Fase 1, los requisitos imprescindibles `RF-01` a `RF-20` tenían un elemento de diseño identificado y pendiente. `RF-21` se incorporó durante la auditoría de cierre y la matriz registra su tratamiento para la nueva revisión de Fase 2.
- Los criterios de aceptación de éxito y error para los requisitos imprescindibles están documentados en [Criterios de aceptación de Fase 1](phase-1-acceptance-criteria.md).
- Las obligaciones de privacidad quedan identificadas como precondición antes de producción, sin bloquear el diseño funcional de Fase 2.

Los controles aplicables de trazabilidad, requisitos verificables, terminología, decisiones de diseño, preguntas bloqueantes, criterios de aceptación y cambios de alcance han quedado listos para revisión humana. En el flujo de un único mantenedor, la autovalidación y aceptación explícita de la ausencia de revisión independiente quedan registradas en la PR de cierre de esta fase.
