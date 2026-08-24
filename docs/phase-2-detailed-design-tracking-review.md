# Diseño detallado de seguimiento y revisión — Fase 2

**Estado:** Validado como diseño — únicamente autorizada la preparación técnica con datos sintéticos
**Fecha:** 2026-08-23
**Fecha de validación:** 2026-08-23
**Responsable de revisión:** Revisor de arquitectura
**Validación documental:** Decisiones de seguimiento y revisión aceptadas explícitamente por el responsable el 2026-08-23
**Restricción:** Prohibido tratar datos personales reales o habilitar comentarios fuera de entornos sintéticos hasta completar las evidencias de privacidad y la EIPD exigidas por `ADR-0010` y `ADR-0018`
**Ámbito:** `tracking-review` y su coordinación con `publication`, `runner-management` y `runner-portal`

## Propósito

Materializar `RF-17`, la aportación de seguimiento a `RF-18` y la revisión global de `RF-19` antes de crear OpenAPI, migraciones y código. El diseño concreta la identidad y edición del registro, la ventana de siete días, el consentimiento del comentario, el historial derivado de publicaciones y la consulta de administrador y entrenador sin introducir un flujo de aprobación.

## Resultado funcional

- Desde el inicio de la fecha local de un entrenamiento publicado, un corredor `active` puede declarar una única respuesta `realizado` o `no-realizado` y corregirla hasta terminar su ventana de siete días.
- El seguimiento estructurado funciona aunque el corredor no acepte el comentario opcional.
- El comentario solo se habilita después de un consentimiento explícito, separado, versionado y revocable; retirarlo elimina su contenido del uso ordinario sin borrar el seguimiento estructurado.
- El historial empieza en la fecha del entrenamiento, nunca muestra entrenamientos futuros y distingue `sin-seguimiento`, `realizado`, `no-realizado` y `retirado`.
- Administrador y entrenador abren la semana actual, consultan y analizan conteos por plan y entrenamiento y acceden al detalle por corredor sin modificar, responder ni marcar como revisado. El sistema no garantiza ni registra que una persona haya leído cada elemento.
- La baja de un corredor bloquea nuevas escrituras y oculta sus datos al entrenador, pero no reescribe entrenamientos publicados ni seguimiento histórico; el administrador conserva acceso auditado.

## Fuentes normativas

- [Requisitos de Fase 1](phase-1-requirements.md), [criterios de aceptación](phase-1-acceptance-criteria.md) y [matriz de decisiones](phase-1-decision-matrix.md), especialmente `RF-17`, `RF-18`, `RF-19`, `D-07` y `D-08`.
- [Diseño de alto nivel](phase-2-high-level-design.md).
- [Diseño detallado de gestión de corredores](phase-2-detailed-design-runner-management.md), que define estados, inactividad, reactivación y retención del corredor.
- [Diseño detallado de publicación](phase-2-detailed-design-publication.md), que gobierna versiones, destinatarios, visibilidad y retirada de entrenamientos mediante republicación.
- `ADR-0004`: escritura del corredor propietario, lectura global de administrador y entrenador y aislamiento.
- `ADR-0009`: registro único, campos cerrados, ventana, historial, versiones de referencia, retirados y revisión de solo lectura, con escala de esfuerzo refinada por `ADR-0022`.
- `ADR-0010`: consentimiento del comentario, retirada, minimización, derechos, retención y bloqueo de producción.
- `ADR-0012`: PostgreSQL, transacciones, restricciones, índices y cursores.
- `ADR-0014`: propiedad de `tracking-review`, APIs Java y dependencias permitidas.
- `ADR-0015`: `ActorContext`, autorización en aplicación y alcance dentro de consultas y conteos.
- `ADR-0017` y la [guía de API HTTP](api-design-guidelines.md): recursos, métodos, precondiciones y contrato OpenAPI.
- `ADR-0018`: exclusión operativa del corredor inactivo, conservación histórica y reactivación durante un máximo de `24` meses.
- `ADR-0021`: permanencia de destinatarios históricos y versiones necesarias para seguimiento.

Si este documento contradice un ADR aceptado, prevalece el ADR y deberá corregirse el diseño antes de implementar.

## Alcance

Incluye:

- creación y sustitución del único registro de seguimiento del corredor;
- validación de valores, pertenencia, fecha, ventana, versión y concurrencia;
- consentimiento vigente para comentario y retirada con supresión operativa;
- historial propio consumido por `runner-portal`;
- revisión semanal global, conteos, filtros y detalle para administrador y entrenador;
- tratamiento de entrenamientos retirados y corredores inactivos;
- persistencia, consultas, retención, seguridad, observabilidad y pruebas del módulo.

Quedan fuera:

- notificaciones por crear o modificar seguimiento;
- respuestas, notas, asignaciones, aprobación o estado `revisado` del entrenador;
- historial de ediciones del registro;
- eliminación ordinaria del registro o vuelta a `sin-seguimiento` después de responder;
- búsqueda por el texto del comentario, exportación de comentarios o exposición masiva de estos;
- métricas de rendimiento deportivo, recomendaciones, inferencias de salud o perfilado;
- filtros por grupo, segmento o etiqueta y analítica longitudinal;
- un canal de ejercicio de derechos dentro del producto;
- la experiencia móvil completa, que pertenece a `runner-portal`.

## Razonamiento de diseño

1. La publicación y sus destinatarios determinan qué entrenamiento existió para cada corredor; duplicar esa verdad en seguimiento produciría historias divergentes.
2. `sin-seguimiento` es ausencia de una declaración, no una respuesta. Derivarlo evita escrituras masivas al publicar, retirar o avanzar el tiempo.
3. Una única respuesta sustituible durante una ventana limitada permite corregir errores sin conservar un historial personal que el PMV no utiliza.
4. El comentario puede contener datos de salud. Ocultarlo en listados, impedir su búsqueda y exigir consentimiento reduce exposición, pero no elimina el bloqueo de privacidad.
5. La revisión necesita partir de asignaciones publicadas, no solo de respuestas existentes; de otro modo desaparecerían precisamente las ausencias que `RF-19` exige revisar.
6. La inactividad y la retirada son dimensiones distintas: la primera describe al corredor actual y la segunda el ciclo del entrenamiento. Ninguna debe reescribir una declaración histórica.

## Decisiones confirmadas

1. El consentimiento del comentario se solicita justo cuando el corredor intenta usarlo por primera vez.
2. El consentimiento es explícito, separado, versionado y permanece vigente para comentarios posteriores hasta que se retire.
3. Rechazar o retirar el consentimiento no impide crear ni modificar el seguimiento estructurado.
4. Después del primer guardado no existe eliminación ordinaria del registro ni vuelta a `sin-seguimiento`; dentro de la ventana se puede cambiar entre `realizado` y `no-realizado` y vaciar el comentario.
5. Un entrenamiento futuro no aparece en historial ni revisión. Entra al comenzar su fecha local y, si no tiene respuesta, se representa como `sin-seguimiento` aunque la ventana siga abierta.
6. La revisión abre por defecto en la semana actual, agrupada por plan y entrenamiento, con conteos `realizado`, `no-realizado` y `sin-seguimiento` y acceso al detalle de corredores.
7. Se puede navegar por semanas anteriores y filtrar por corredor, plan semanal, entrenamiento y estado de seguimiento. Las semanas futuras no producen elementos revisables.
8. El comentario no aparece en listados ni resúmenes. La fila solo indica si existe y el texto exige abrir el detalle; no se busca ni filtra por su contenido.
9. Un corredor inactivo permanece en los entrenamientos ya publicados y en el histórico, sin reescribir datos. Solo el administrador puede consultarlo con acceso auditado e identificado como inactivo; el entrenador deja de verlo mientras dure la inactividad. No puede crear ni modificar seguimiento y no participa en nuevas primeras publicaciones.
10. Retirar el consentimiento elimina los comentarios anteriores de las representaciones y del almacenamiento operativo, impide otros nuevos y conserva el seguimiento estructurado y la evidencia de consentimiento. Volver a consentir no recupera textos anteriores.
11. Un entrenamiento retirado permanece en el histórico y conserva su seguimiento, si existía, pero queda fuera de los conteos `realizado`, `no-realizado` y `sin-seguimiento`.
12. La revisión significa consultar y analizar información y es estrictamente de lectura: no hay titularidad de entrenador, respuesta, nota, prioridad, aprobación, SLA ni marca de revisado. Tampoco existe prueba de lectura; pueden producirse consultas duplicadas u omisiones humanas y el PMV acepta ese límite.

## Supuestos e incertidumbres

| Elemento | Supuesto o incertidumbre | Confianza | Tratamiento |
| --- | --- | --- | --- |
| Zona horaria | El club dispone de una única zona IANA configurada y todos los límites temporales se calculan en ella. | Alta | Reutilizar el reloj y la zona canónicos de planificación y publicación; probar cambios de horario. |
| Entrenamiento retirado con respuesta | La retirada impide una primera respuesta, pero no acorta la ventana ya fijada de un registro existente. | Media | Aplicar la regla general de edición de `ADR-0009` y probar retirada antes y después de la primera respuesta. |
| Inactivo sin respuesta | La inactividad no cambia el estado conservado: para el administrador la fila sigue siendo `sin-seguimiento` con indicador `inactive`; para el entrenador queda fuera de resultados y conteos. | Alta | Aplicar el alcance antes de recuperar filas y calcular conteos; una reactivación no reconstruye datos. |
| Versión del consentimiento | La versión efectiva del texto se configura; una nueva versión marcada como material exige consentir de nuevo antes de añadir comentarios. | Media | La revisión de privacidad decidirá qué cambios exigen renovación; por defecto el módulo no reutiliza una versión obsoleta. |
| Borrado del comentario | La base operativa elimina el texto al retirar consentimiento, pero copias, bloqueo excepcional y destrucción final dependen de la política aprobada. | Alta | Reaplicar supresiones tras restaurar copias y bloquear producción hasta aprobar y probar la política. |
| Volumen | Más de `500` corredores caben en conteos y páginas por cursor sin una proyección persistida inicial. | Media | Medir consultas con PostgreSQL real; añadir una proyección solo con evidencia y sin convertirla en fuente de verdad. |

## Lenguaje ubicuo

| Término | Significado |
| --- | --- |
| Entrenamiento publicado para el corredor | Entrenamiento incluido en una versión publicada cuyo conjunto congelado contiene al corredor. |
| Registro de seguimiento | Única declaración vigente del corredor para un entrenamiento lógico publicado. |
| `realizado` | Declaración explícita que exige esfuerzo y sensación. |
| `no-realizado` | Declaración explícita que no admite esfuerzo ni sensación. |
| `sin-seguimiento` | Ausencia derivada de registro desde la fecha del entrenamiento; nunca se persiste como declaración. |
| `retirado` | Entrenamiento que llegó a publicarse y una versión posterior eliminó. |
| Versión de referencia | Versión publicada contra la que se creó por primera vez el registro y que fija contenido, fecha y ventana. |
| Ventana de respuesta | Siete días naturales desde el comienzo de la fecha del entrenamiento hasta el final del sexto día posterior, en la zona del club. |
| Consentimiento de comentario | Recurso propio del corredor que habilita el contenido libre para una versión informativa concreta hasta su retirada. |
| Elemento de revisión | Proyección de un entrenamiento publicado para un corredor y su respuesta vigente o ausencia. |
| Resumen de revisión | Conteos derivados de elementos no retirados, agrupados por plan y entrenamiento. |

En código, OpenAPI y persistencia se usarán `tracking record`, `tracking comment consent`, `training history item`, `training review item`, `performed`, `effort`, `feeling`, `no tracking` y `withdrawn workout`. No se usará `completed` como sinónimo ambiguo de `realizado` ni `review` como estado del registro.

## Límite modular

`tracking-review` gobierna:

- registros de seguimiento, contenido vigente, revisión optimista y versión de referencia;
- estado y evidencia del consentimiento de comentario;
- eliminación operativa de comentarios al retirar el consentimiento;
- composición de historial y revisión a partir de contratos publicados;
- políticas de autorización, retención y consultas propias.

Consume `publication` para:

- validar que el entrenamiento lógico fue publicado para el corredor;
- obtener versión activa, fecha, estado retirado y contexto visible antes de la primera respuesta;
- recorrer asignaciones publicadas por corredor, semana, plan o entrenamiento con un cursor estable;
- fijar y reproducir la versión de referencia sin acceder al esquema `publication`.

Consume `runner-management` para:

- comprobar que el corredor autenticado continúa `active` antes de escribir;
- obtener por lote la presentación mínima y el estado actual de los corredores de una página de revisión;
- coordinar anonimización, supresión o vencimiento sin reactivar registros.

Publica para `runner-portal` consultas de historial propio y capacidades de lectura y escritura de seguimiento. El adaptador HTTP puede invocar los mismos puertos, pero `runner-portal` no adquiere tablas ni reglas de seguimiento.

```text
tracking-review ──> publication
tracking-review ──> runner-management
runner-portal ────> tracking-review
```

No habrá HTTP interno, SQL entre esquemas, imports de paquetes internos ni eventos para decidir autorización o el contexto de la primera respuesta.

## Reglas temporales y de estado

El servidor calcula `today` una sola vez por operación usando el reloj y la zona IANA del club. El entrenamiento entra en historial y revisión al comenzar `workoutDate`; antes de ese instante no existe un elemento revisable ni puede crearse seguimiento.

La fecha del entrenamiento es el día `1`. La creación y edición terminan al finalizar el sexto día posterior. La comprobación se repite dentro de la transacción para impedir que una operación iniciada antes de medianoche confirme después del cierre.

Antes de la primera respuesta, `publication` aporta la versión activa, fecha y estado del entrenamiento. Una republicación que conserve la identidad lógica puede moverlo y cambiar su ventana. La primera respuesta fija de forma inmutable `referencePublicationVersionId`, `referenceWorkoutDate` y `responseClosesAt`; una republicación posterior no los cambia.

Una retirada anterior a la primera respuesta produce `retirado` y bloquea la creación. Si ya existe registro, se conserva y puede sustituirse hasta su cierre fijado; la retirada no amplía ni acorta la ventana. En revisión, el entrenamiento retirado muestra sus destinatarios y respuestas históricas, pero no participa en los tres conteos operativos.

La baja del corredor bloquea inmediatamente creación y sustitución. Las filas históricas permanecen: el administrador puede consultarlas con acceso auditado y `runnerStatus=inactive`; el entrenador no las recibe ni las incluye en sus conteos. Si no existía respuesta, el estado conservado continúa siendo `sin-seguimiento`. Una reactivación vuelve a permitir la revisión del entrenador solo sobre datos que sigan dentro de su retención y no reabre ventanas vencidas.

## Contenido y validación del seguimiento

La representación completa contiene:

- `performed`: booleano obligatorio;
- `effort`: entero de `1` a `5`, obligatorio solo cuando `performed=true`;
- `feeling`: `bien`, `normal` o `mal`, obligatorio solo cuando `performed=true`;
- `comment`: texto plano opcional de hasta `1.000` caracteres;
- identificadores opacos, versión de referencia, fechas y revisión expuestos según el actor.

Cuando `performed=false`, `effort` y `feeling` deben estar ausentes; no se ignoran valores recibidos. Cambiar desde `realizado` a `no-realizado` elimina ambos campos en la misma sustitución. Cambiar en sentido contrario exige ambos.

El formulario pregunta exactamente «¿Cuánto esfuerzo te supuso este entrenamiento?» sin valor por defecto y muestra la escala única: `1 Muy suave`, `2 Suave`, `3 Moderado`, `4 Intenso`, `5 Muy intenso`. Solo se persiste el entero. La escala describe percepción subjetiva del entrenamiento y no representa un máximo fisiológico, una medición clínica ni una inferencia de salud.

El comentario se normaliza eliminando espacios exteriores y conservando saltos de línea y espacios interiores. Una cadena vacía después de normalizar equivale a ausencia. Exactamente `1.000` caracteres se aceptan; `1.001` rechaza la operación completa y nunca se trunca. No se admite HTML ni formato enriquecido.

No existe `DELETE` ordinario. Una sustitución válida puede cambiar la declaración, el esfuerzo, la sensación y el comentario, pero no el corredor, entrenamiento, versión de referencia, fecha ni cierre. Después de responder, omitir el recurso o vaciar todos los campos no restaura `sin-seguimiento`.

## Consentimiento del comentario

El comentario permanece deshabilitado hasta que el corredor intenta usarlo. En ese momento la interfaz presenta información separada del contrato, la versión efectiva y una advertencia visible para no introducir diagnósticos, lesiones ni otros datos de salud. Rechazarla cierra el paso de comentario, pero el formulario estructurado sigue disponible.

El recurso de consentimiento distingue `not-granted`, `granted` y `withdrawn`, junto con versión de información, actor e instantes cuando existen. `not-granted` es la representación derivada previa a la primera aceptación y no necesita un evento de rechazo. Una aceptación vigente sirve para comentarios posteriores. Si la versión efectiva requiere renovación, el módulo trata el consentimiento anterior como insuficiente para nuevas escrituras hasta registrar otra aceptación.

Retirar consentimiento es una operación independiente de la ventana de seguimiento. Dentro de una única transacción:

1. registra el evento inmutable de retirada;
2. cambia el consentimiento vigente a `withdrawn`;
3. sustituye por `null` todos los comentarios operativos del corredor;
4. conserva campos estructurados, versiones y fechas necesarias;
5. impide que una lectura o restauración ordinaria vuelva a exponer esos textos.

Volver a consentir crea otra evidencia y habilita comentarios nuevos solo en registros cuya ventana continúe abierta. Nunca recupera comentarios eliminados. Las copias y un eventual bloqueo legal siguen la política de privacidad, no una ruta de producto.

## Modelo persistente

Todas las tablas pertenecen al esquema `tracking_review`.

| Tabla | Datos e invariantes principales |
| --- | --- |
| `tracking_record` | UUID, `runner_id`, `logical_workout_id`, `reference_publication_version_id`, fecha de referencia, cierre, `performed`, esfuerzo, sensación, comentario opcional, creación, actualización y revisión. Unicidad física por corredor y entrenamiento lógico; restricciones cerradas para combinaciones de campos. |
| `tracking_comment_consent` | Un estado persistido por corredor después de la primera concesión, versión informativa, estado `granted` o `withdrawn`, concesión, retirada y revisión optimista. La ausencia se representa como `not-granted` y la tabla no contiene el texto comentado. |
| `tracking_comment_consent_event` | Evidencia inmutable de concesión o retirada con versión, actor, instante y correlación; una repetición idempotente no crea otro evento. |

`sin-seguimiento`, `retirado`, el nombre del corredor, su estado actual y los resúmenes no se persisten como fuente de verdad en este módulo. Se derivan de `publication`, `runner-management` y `tracking_record`.

Las claves a versión, corredor y entrenamiento usan identificadores estables y pueden tener claves foráneas entre esquemas según `ADR-0014`, pero no conceden lectura SQL. Las restricciones de PostgreSQL impiden duplicados y combinaciones inválidas aunque falle una validación de aplicación.

Índices iniciales:

- único por `(runner_id, logical_workout_id)`;
- historial por `(runner_id, reference_workout_date DESC, id DESC)`;
- recuperación por lote de revisión sobre `logical_workout_id` y `runner_id`;
- consentimiento vigente por `runner_id` y eventos por `(runner_id, occurred_at DESC, id DESC)`.

Los índices y límites de página se confirmarán con planes de consulta sobre un volumen representativo superior a `500` corredores antes de implementar.

## Casos de uso

### Obtener contexto y seguimiento propio

1. El adaptador resuelve `ActorContext` y el corredor vinculado, sin aceptar un identificador de corredor como prueba de identidad.
2. `publication` valida que el entrenamiento fue publicado para ese corredor y devuelve el contexto autorizado.
3. El módulo calcula si es futuro, abierto, cerrado o retirado y carga el registro existente.
4. La respuesta informa el estado derivado, la ventana, la capacidad de escritura y el `ETag` cuando existe registro.

### Crear el primer registro

1. Se exige corredor `active`, `If-None-Match: *` y entrenamiento no futuro ni retirado.
2. `publication` bloquea o revalida contexto, versión activa y fecha dentro de la transacción.
3. Se validan ventana, contenido y consentimiento si existe comentario.
4. Se inserta el registro con versión, fecha y cierre fijados.
5. Un cambio concurrente de publicación o un duplicado revierte la operación completa.

### Sustituir un registro

1. Se exige corredor propietario `active`, registro existente e `If-Match` vigente.
2. Se usan versión, fecha y cierre ya fijados; no se reinterpretan desde la publicación activa.
3. Se valida la representación completa y el consentimiento vigente si contiene comentario.
4. La sustitución incrementa la revisión y devuelve otro `ETag`.
5. Una precondición obsoleta, ventana cerrada o entrada inválida no altera el último contenido válido.

### Retirar el consentimiento

La retirada no requiere que exista seguimiento ni que una ventana esté abierta. Se serializa por corredor, registra una única transición efectiva y elimina todos los comentarios operativos del corredor. Repetir `withdrawn` es idempotente y no vuelve a modificar registros ni crear evidencias duplicadas.

### Consultar historial propio

`publication` entrega por cursor los entrenamientos publicados cuya fecha local ya comenzó, incluidos retirados. `tracking-review` obtiene los registros correspondientes por lote y compone cada elemento con estado derivado. El orden es fecha descendente e identificador estable como desempate. `runner-portal` recibe solo datos propios y la representación deportiva necesaria.

### Consultar revisión global

La primera carga usa la semana local actual. Para cada plan y entrenamiento no futuro:

- `publication` aporta asignaciones históricas y estado retirado;
- `tracking-review` agrega respuestas y deriva ausencias;
- `runner-management` aporta por lote nombre, apellidos y estado actual y excluye presentaciones inactivas para el entrenador;
- la autorización se aplica antes de conteos, cursores y resultados.

Los resúmenes contienen identificadores, fecha, plan, entrenamiento y los tres conteos dentro del alcance del actor. Un retirado se identifica como tal y no incorpora esos conteos; su detalle histórico sigue accesible según permisos. El listado de corredores expone campos estructurados y `hasComment`, nunca el texto. El detalle individual puede incluir el comentario vigente.

## API Java publicada

Los puertos de `tracking-review` usarán tipos propios y `ActorContext`:

- `GetOwnTrackingContextQuery` y `PutOwnTrackingRecordCommand`;
- `GetOwnTrainingHistoryQuery` para `runner-portal`;
- `GetTrackingCommentConsentQuery` y `PutTrackingCommentConsentCommand`;
- `GetTrainingReviewSummariesQuery`, `GetTrainingReviewItemsQuery` y `GetTrainingReviewItemQuery`;
- `ApplyTrackingRetentionCommand` para supresión, anonimización y reaplicación tras restauración.

Resultados y cursores no exponen modelos OpenAPI, entidades, tablas ni tipos jOOQ. Las consultas a `publication` y `runner-management` son por lotes y conservan el alcance del actor; no existe un repositorio compartido.

## API HTTP prevista

OpenAPI `3.1` será la fuente de verdad antes de implementar.

| Actor | Método y recurso | Semántica |
| --- | --- | --- |
| Corredor | `PUT /api/workouts/{workoutId}/tracking-records/current` | Crea con `If-None-Match: *` o sustituye por completo con `If-Match` el único registro propio. |
| Corredor | `GET /api/runners/me/tracking-comment-consents/current` | Consulta la versión requerida y el estado propio `not-granted`, `granted` o `withdrawn`. |
| Corredor | `PUT /api/runners/me/tracking-comment-consents/current` | Establece la representación completa `granted` o `withdrawn` con precondición. |
| Administrador o entrenador | `GET /api/training-review-summaries` | Consulta resúmenes por semana y filtros autorizados. |
| Administrador o entrenador | `GET /api/training-review-items` | Recorre con cursor los corredores de un resumen o filtro. No devuelve el comentario. |
| Administrador o entrenador | `GET /api/training-review-items/{reviewItemId}` | Obtiene el detalle estructurado y el comentario vigente, si existe. |

`tracking-records/current` identifica un recurso único por entrenamiento y corredor autenticado; no crea una colección de ediciones. `tracking-comment-consents/current` es un recurso real con estado, revisión y ciclo de vida. Los resúmenes y elementos de revisión son recursos derivados de lectura, no estados de aprobación.

`reviewItemId` es un identificador opaco y estable de la asignación publicada corredor-entrenamiento; no contiene identificadores personales legibles y nunca concede acceso por sí mismo. Omitir `weekStart` en los resúmenes selecciona la semana local actual.

Semántica mínima:

- primer `PUT` de seguimiento válido: `201 Created`, `Location` y `ETag`;
- sustitución válida: `200 OK` y nuevo `ETag`;
- primera concesión de consentimiento: `If-None-Match: *`; cambios o retirada posteriores: `If-Match` con nuevo `ETag` al confirmar;
- precondición ausente: `428 Precondition Required`; obsoleta: `412 Precondition Failed`;
- combinación o comentario inválidos: `422 Unprocessable Content`, sin sustitución parcial;
- entrenamiento futuro, ventana cerrada, retirada sin registro previo o estado incompatible: `409 Conflict` con tipo estable;
- candidato inexistente, ajeno o no visible: respuesta no enumerable según `ADR-0015`;
- retirada repetida del consentimiento: resultado idempotente, sin otro evento ni efectos laterales.

Los filtros de revisión incluyen `weekStart`, `runnerId`, `weeklyPlanId`, `workoutId` y `trackingStatus`. No existe `comment`, texto libre, grupo, segmento o etiqueta como filtro. Las colecciones usan cursor opaco, orden estable y límite acotado; sus valores concretos se medirán antes de cerrar OpenAPI.

No existirán `POST /track`, `POST /complete`, `POST /review`, `DELETE` de seguimiento, rutas por rol ni endpoints de respuesta o aprobación del entrenador.

## Concurrencia y transacciones

- La unicidad física resuelve dos primeras respuestas simultáneas; solo una confirma y la otra recibe conflicto o precondición fallida.
- Cada sustitución usa revisión optimista, `ETag` e `If-Match`; nunca se aplica last-write-wins silencioso.
- La primera respuesta revalida el contexto de publicación en la misma transacción para no fijar una versión retirada o reemplazada durante la operación.
- La comprobación de fecha y actividad se repite antes de confirmar.
- Retirar consentimiento bloquea el consentimiento vigente y elimina comentarios en una única transacción. Una escritura concurrente con comentario confirma antes de la retirada o se rechaza después; no puede dejar un texto visible con estado `withdrawn`.
- Retención y supresión son idempotentes, trabajan por lotes acotados y no reabren registros ni consentimientos.

## Consultas, conteos y cursores

El historial y la revisión parten de una página estable de asignaciones publicadas. Para esa página, el módulo obtiene en lotes registros y presentaciones, compone resultados y genera un cursor ligado a actor, filtros, orden y revisión de publicación. Un cursor usado con otros filtros o contexto devuelve error estable.

Los conteos de un entrenamiento no retirado se calculan sobre sus destinatarios históricos autorizados. Para el administrador incluyen activos e inactivos retenidos; para el entrenador solo corredores `active`:

- `realizado`: registro vigente con `performed=true`;
- `no-realizado`: registro vigente con `performed=false`;
- `sin-seguimiento`: destinatario autorizado sin registro, aunque conserve la ventana abierta.

Un entrenamiento retirado conserva elementos históricos, pero se excluye completo de estos conteos. La interfaz lo identifica de forma separada para no interpretar su retirada como incumplimiento.

No se cargarán todos los comentarios para construir una lista ni se usarán como criterio de orden, filtro, caché o telemetría. Una futura proyección persistida deberá documentar propietario, consistencia, retención y reconstrucción y no sustituirá a publicación o seguimiento como fuentes de verdad.

## Autorización y minimización

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Crear o sustituir seguimiento | No | No | Solo propio y estando `active` |
| Consultar historial propio | No como corredor | No como corredor | Solo propio |
| Consultar revisión global | Sí, incluidos inactivos con acceso auditado | Sí, solo corredores `active` | No |
| Leer comentario en detalle | Sí, incluidos inactivos con acceso auditado | Sí, solo de corredores `active` | Solo el propio |
| Conceder o retirar consentimiento | No | No | Solo propio |
| Responder, corregir o revisar en nombre del corredor | No | No | No |

Los listados aplican autorización antes de recuperar datos y no incluyen comentario, correo, identificadores de cuenta, clasificación ni datos ajenos al contexto. El detalle no revela textos suprimidos. Problem Details, logs, métricas, trazas y ejemplos usan identificadores opacos o datos sintéticos y nunca contenido de comentario.

## Privacidad y retención

El seguimiento estructurado se conserva durante `24` meses desde la fecha del entrenamiento conforme a `ADR-0010`; la baja y reactivación no reinician el plazo. Al vencer se suprime o anonimiza irreversiblemente según la política aprobada y se reaplica el resultado después de restaurar una copia.

La retirada del consentimiento elimina el comentario de la base operativa con independencia del plazo del seguimiento. La política definitiva debe decidir y probar copias, bloqueos excepcionales, destrucción y retención de la evidencia de consentimiento. Esa evidencia no justifica conservar el texto retirado para uso ordinario.

Los derechos se atienden mediante el canal externo de privacidad. No existe eliminación ordinaria del registro porque una petición de producto no puede decidir automáticamente supresión, bloqueo, exactitud histórica o derechos de terceros. Exportaciones de derechos excluirán datos de otras personas y secretos.

Hasta completar revisión especializada, EIPD, base, información, retención, derechos y controles, solo se usarán datos ficticios, sintéticos o anonimizados de forma irreversible. La completitud de este diseño no constituye aprobación legal ni autoriza producción.

## Observabilidad

Métricas agregadas:

- creaciones, sustituciones y rechazos por causa normalizada;
- conflictos de precondición y unicidad;
- latencia y tamaño de páginas de historial y revisión;
- conteos agregados por estado sin dimensión de corredor, plan o comentario;
- concesiones y retiradas de consentimiento como volumen agregado;
- trabajos de retención vencidos, completados y fallidos.

Auditoría mínima:

- aceptación y retirada de consentimiento con versión y actor;
- lectura excepcional o administrativa del detalle cuando la política de seguridad lo exija;
- supresión, anonimización, bloqueo y reaplicación tras restauración;
- denegaciones sensibles sin almacenar contenido ni parámetros personales innecesarios.

No se registran esfuerzo, sensación, comentario, nombre, correo ni filtros personales en logs, métricas o trazas. Las alertas se limitan a errores sistémicos, crecimiento anómalo, conflictos y fallos de retención.

## Paquetes previstos

```text
com.vgrunning.trackingreview/
  api/
    command/
    query/
  application/
    service/
    port/out/
  domain/
    tracking/
    consent/
    review/
  adapter/in/web/
  adapter/out/persistence/jooq/
```

El dominio no depende de Spring, OpenAPI, jOOQ, JDBC ni módulos consumidores. Spring Modulith y ArchUnit verificarán `allowedDependencies = {"publication::api", "runner-management::api"}`, acceso solo a APIs y ausencia de SQL cruzado.

## Validación prevista

### `RF-17` — Registro de seguimiento

- Probar primera respuesta válida `realizado` con esfuerzo `1` y `5`, sensaciones cerradas y comentario ausente o vigente.
- Probar la pregunta exacta, ausencia de selección inicial, las cinco etiquetas y persistencia exclusiva del entero en todas las vistas.
- Probar `no-realizado` sin esfuerzo ni sensación y rechazo si alguno se envía.
- Cambiar en ambos sentidos y comprobar sustitución completa, limpieza de campos y ausencia de historial de ediciones.
- Rechazar esfuerzo `0` y `6`, números no enteros, sensación desconocida, cuerpo incompleto y comentario de `1.001` caracteres sin alterar el registro anterior.
- Aceptar `1.000` caracteres, conservar saltos interiores, quitar espacios exteriores y tratar texto vacío como ausencia.
- Probar inicio exacto de la fecha, final del sexto día posterior, instante siguiente, cambio de horario y carrera al cruzar medianoche.
- Rechazar entrenamiento futuro, borrador, publicación ajena, corredor inactivo y primera respuesta a un retirado.
- Probar versión activa antes de responder, fijación al crear y estabilidad ante republicaciones posteriores.
- Probar `If-None-Match`, `If-Match`, dos envíos concurrentes y ausencia de pérdida silenciosa.
- Verificar que no existe eliminación ordinaria y que una sustitución no puede volver a `sin-seguimiento`.

### Consentimiento y comentario

- Mantener comentario deshabilitado hasta el intento de uso y mostrar información separada, versión y advertencia sanitaria.
- Rechazar consentimiento sin bloquear el guardado estructurado.
- Reutilizar una aceptación vigente y exigir otra cuando la versión efectiva material lo requiera.
- Retirar dentro y fuera de la ventana, eliminar todos los comentarios operativos y conservar campos estructurados y evidencia.
- Ejecutar carrera entre retirada y guardado con comentario y demostrar que no queda texto visible bajo estado `withdrawn`.
- Volver a consentir y comprobar que no reaparecen textos anteriores y que solo se escribe dentro de una ventana abierta.
- Revisar base, libertad, información, versión, retirada, copias y EIPD antes de usar datos reales.

### `RF-18` — Historial

- Probar que un entrenamiento entra al comenzar su fecha y que ninguno futuro aparece.
- Componer `sin-seguimiento`, `realizado`, `no-realizado` y `retirado` desde publicaciones y respuesta vigente.
- Retirar antes y después de la primera respuesta; conservar contexto y bloquear solo la primera respuesta inexistente.
- Dar de baja y reactivar sin reescribir historial, prolongar ventanas ni recuperar datos vencidos.
- Recorrer páginas con orden estable y más de `500` corredores o entrenamientos sin duplicados ni omisiones.
- Probar aislamiento en acceso directo, listas, conteos, cursores y errores.

### `RF-19` — Revisión global

- Abrir por defecto la semana actual, agrupar por plan y entrenamiento y mostrar los tres conteos exactos.
- Navegar semanas anteriores y filtrar por corredor, plan, entrenamiento y estado; rechazar combinaciones o cursores incompatibles.
- Dar de baja un corredor y comprobar que el administrador conserva la fila con indicador independiente, mientras el entrenador deja de verla y sus conteos la excluyen.
- Reactivar dentro de la retención y comprobar que el entrenador vuelve a ver únicamente la información todavía conservada, sin reabrir ventanas.
- Mostrar retirados fuera de los conteos y conservar su detalle histórico.
- Comprobar que listados solo incluyen `hasComment` y que el texto aparece únicamente al abrir un detalle autorizado.
- Verificar ausencia de búsqueda, filtro, exportación masiva o telemetría por comentario.
- Probar que administrador y entrenador consultan globalmente y no pueden crear, modificar, responder, anotar, priorizar, asignar SLA, aprobar ni marcar como revisado.
- Verificar que no se registra ni promete lectura individual y que abrir repetidamente u omitir un elemento no crea estado.

### Arquitectura, API y privacidad

- Ejecutar `ApplicationModules.verify()` y ArchUnit para impedir ciclos, dependencias no permitidas, SQL cruzado e imports internos.
- Aplicar Flyway sobre PostgreSQL vacío y probar restricciones, índices, cursores y planes de consulta.
- Revisar cada recurso, método, estado, precondición, idempotencia, seguridad y Problem Details contra `ADR-0017`.
- Generar servidor y cliente desde OpenAPI, ejecutar Spectral, pruebas de contrato y `oasdiff`.
- Probar retención, supresión, anonimización y reaplicación tras restaurar copias con datos sintéticos.
- Revisar logs, métricas, trazas, errores y ejemplos para demostrar ausencia de comentarios y datos personales innecesarios.

## Alternativas descartadas

- **Consentimiento por cada comentario:** se descarta porque añade fricción repetida sin aportar una finalidad distinta; una aceptación versionada y revocable cubre usos posteriores.
- **Comentario habilitado por contrato o por defecto:** se descarta porque contradice `ADR-0010` y no ofrece una alternativa real sin datos libres.
- **Eliminar todo el registro desde el producto:** se descarta porque convertiría una declaración existente en ausencia y rompería la trazabilidad; las correcciones sustituyen contenido y los derechos siguen otro proceso.
- **Mostrar entrenamientos futuros como `sin-seguimiento`:** se descarta porque presenta como ausencia una respuesta que todavía no puede existir.
- **Persistir `sin-seguimiento`:** se descarta porque obligaría a sincronizar registros artificiales con publicaciones, retiradas y tiempo.
- **Mostrar comentarios en listas o buscarlos:** se descarta por exposición innecesaria de contenido potencialmente sanitario sin mejorar el resumen operativo.
- **Eliminar datos de inactivos del histórico:** se descarta porque reescribiría destinatarios publicados y destruiría el contexto. `ADR-0018` conserva los datos, limita su consulta al administrador y los oculta al entrenador mientras dure la inactividad.
- **Contar retirados como incumplimiento:** se descarta porque un entrenamiento que dejó de estar vigente distorsionaría los conteos operativos.
- **Añadir respuesta o estado revisado:** se descarta porque `RF-19` exige consulta, no una bandeja de trabajo ni coordinación con el corredor.

## Cambios de alcance y riesgos aceptados

Este diseño no amplía el PMV con mensajería, revisión clínica ni analítica. Concreta comportamientos que `ADR-0009`, `ADR-0010`, `ADR-0018` y el refinamiento de escala de `ADR-0022` exigen en interfaz y contratos: esfuerzo entero `1..5`, consentimiento justo a tiempo, ausencia de borrado ordinario, entrada temporal al historial, presentación semanal, minimización del comentario, alcance de inactivos y conteos de retirados.

Riesgos aceptados:

- durante la ventana, `sin-seguimiento` significa ausencia actual y no incumplimiento definitivo;
- ocultar al inactivo cambia los conteos que ve el entrenador, aunque el administrador conserve el histórico completo dentro de la retención;
- un retirado conserva información consultable, pero queda fuera de los conteos;
- el contenido libre puede incluir datos de salud aunque se advierta lo contrario;
- no existe historial de correcciones ni forma de restaurar un comentario vaciado o retirado;
- una publicación equivocada seguirá apareciendo como retirada o histórica según sus versiones;
- componer consultas entre módulos puede requerir optimización después de medir, pero no autoriza SQL cruzado;
- la documentación técnica no resuelve la base jurídica ni autoriza datos reales.

No se crea un ADR nuevo porque estas decisiones materializan y no contradicen los ADR aceptados. Cualquier futura notificación de seguimiento, búsqueda de comentarios, aprobación del entrenador, analítica, asignación de entrenadores o cambio de retención deberá revisar alcance, privacidad y arquitectura antes de implementarse.

## Conclusiones

- El registro único y sustituible preserva la declaración vigente sin acumular historial innecesario.
- Publicación gobierna qué existió y seguimiento gobierna qué respondió el corredor; `sin-seguimiento` se obtiene al componer ambas verdades.
- La fecha local, la versión fijada y las precondiciones hacen verificables ventana, republicación y concurrencia.
- Consentimiento y comentario son opcionales; retirar uno no degrada el seguimiento estructurado ni recupera textos eliminados.
- La revisión semanal aporta conteos y detalle sin convertir al entrenador en editor o aprobador.
- Inactividad y retirada nunca reescriben la historia; la primera restringe además el alcance del entrenador según `ADR-0018`.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro del diseño detallado de `tracking-review`.

Antes de implementar deberán producirse OpenAPI, migraciones Flyway, tipos jOOQ, catálogo común de Problem Details, versión sintética de la información de consentimiento, límites de página medidos y pruebas de integración con PostgreSQL.

Bloqueantes para datos reales y producción:

| Bloqueante | Responsable | Tratamiento exigido |
| --- | --- | --- |
| Base y consentimiento del comentario | Responsable del tratamiento con Revisor de privacidad o DPO | Confirmar libertad, especificidad, información, versión, retirada y condición aplicable a posibles datos de salud; eliminar el comentario del alcance si no es defendible. |
| Retirada, copias y destrucción | Responsable del tratamiento y persona operadora | Aprobar supresión, eventual bloqueo excepcional, caducidad de copias y reaplicación tras restauración; demostrar que un texto retirado no vuelve al uso ordinario. |
| Retención de seguimiento y evidencia | Responsable del tratamiento con Revisor de privacidad o DPO | Confirmar finalidad, base, plazo, acceso, anonimización o supresión y conservación mínima de evidencias. |
| Acceso global | Revisor de privacidad y Revisor de seguridad | Evaluar necesidad, medidas, auditoría y revisión periódica de cuentas privilegiadas. |
| EIPD y pruebas finales | Responsable del tratamiento | Completar EIPD, aceptar riesgo residual y verificar derechos, telemetría, entornos y datos sintéticos antes de datos reales. |
