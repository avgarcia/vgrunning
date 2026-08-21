# Diseño detallado de publicación — Fase 2

**Estado:** Validado para diseño y desarrollo con datos sintéticos
**Fecha:** 2026-08-20
**Última actualización:** 2026-08-21
**Responsable de revisión:** Revisor de arquitectura
**Restricción:** Prohibido tratar datos personales reales o habilitar el proveedor de correo hasta completar las evidencias de privacidad y operación exigidas por `ADR-0010`, `ADR-0011`, `ADR-0016` y `ADR-0018`
**Validación documental:** Diseño de publicación y decisiones de `ADR-0021` aceptados explícitamente por el responsable el 2026-08-21
**Ámbito:** `publication` y su coordinación con `planning`, `runner-management`, `notification-delivery` y `runner-portal`

## Propósito

Materializar `RF-09`, `RF-10`, `RF-14`, `RF-15` y `RF-20`, y la contribución de publicación a `RF-07`, `RF-08` y `RF-16`, para que una semana se publique y actualice como una unidad coherente, conserve destinatarios efectivos y genere notificaciones sin exponer cambios parciales ni depender del proveedor de correo.

Este diseño aplica `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0012`, `ADR-0014`, `ADR-0015`, `ADR-0017`, `ADR-0018`, `ADR-0020` y el refinamiento aceptado en `ADR-0021`.

## Resultado funcional

- La primera publicación confirma grupo, semana, contenido y lista exacta de corredores activos dentro de una única transacción.
- Los destinatarios quedan congelados para todas las versiones del plan.
- Un plan publicado no mantiene cambios pendientes en el servidor: su contenido de planificación coincide con la versión activa.
- Administrador o entrenador entra en modo edición, modifica localmente uno o varios días futuros y los confirma como una sola versión completa.
- Hoy y los días anteriores quedan bloqueados; no existe primera publicación retroactiva.
- No existen retirada, despublicación, restauración, historial visible de versiones ni modificación de nombre, grupo o semana después de publicar.
- Cada versión crea una solicitud por destinatario efectivo. El envío comprueba justo antes del proveedor que el corredor continúa activo.

## Alcance

Incluye:

- candidatura y confirmación de primera publicación;
- versión activa e instantáneas inmutables;
- edición local y sustitución atómica de varios días;
- congelación y exclusividad de destinatarios;
- autoría mínima visible;
- contenido e idempotencia lógica de notificaciones;
- elegibilidad por estado del corredor antes de cada intento;
- API HTTP administrativa orientada a recursos;
- APIs Java internas, persistencia, concurrencia, retención, observabilidad y pruebas.

Quedan fuera del PMV:

- programar una publicación para otra fecha u hora;
- retirar, ocultar, archivar o despublicar un plan;
- recalcular destinatarios después de la primera publicación;
- mantener o recuperar borradores de edición publicados;
- editar nombre, grupo o semana después de publicar;
- navegar, comparar o restaurar versiones anteriores;
- fusionar automáticamente sesiones concurrentes;
- agrupar guardados separados en un único correo;
- mostrar estado de entrega o reintentar manualmente desde el producto;
- incluir el detalle completo del entrenamiento en el correo.

## Lenguaje ubicuo

| Término | Definición |
| --- | --- |
| Candidatura de publicación | Representación derivada y no persistida del plan, destinatarios y revisiones que se deben confirmar antes de la primera publicación. |
| Publicación actual | Recurso estable que representa la única versión activa de un plan publicado. |
| Versión publicada | Instantánea completa e inmutable creada por la primera publicación o por un guardado posterior confirmado. |
| Destinatario efectivo | Corredor `active` incluido en la primera publicación y cuyo identificador queda congelado históricamente en todas las versiones del plan. |
| Destinatario elegible para envío | Destinatario efectivo que continúa `active` inmediatamente antes de un intento concreto contra el proveedor. |
| Sesión de edición | Copia local y temporal de la publicación actual; no es un estado de dominio ni un recurso del servidor. |
| Republicación | Sustitución confirmada de la publicación actual que crea una nueva versión completa; se presenta al corredor como actualización del plan. |
| Día modificable | Día de una semana futura o, en la semana actual, día estrictamente posterior a la fecha local del servidor. |
| Solicitud de notificación | Registro lógico individual por versión y destinatario, persistido atómicamente antes de cualquier intento externo. |
| Omisión por inactividad | Terminación de una solicitud sin contactar con el proveedor porque el destinatario no está `active`. |

Se usarán `publication`, `published version`, `recipient` y `notification request` en código, OpenAPI y persistencia. No se usarán `withdrawal`, `unpublish`, `restore`, `pending changes` ni `server draft` como capacidades existentes.

## Decisiones y supuestos

### Decisiones confirmadas

- La zona local inicial es `Europe/Madrid` y solo la configura el servidor.
- Las semanas futuras admiten edición; en la actual solo se modifican días posteriores a hoy.
- Un borrador nunca publicado puede eliminar un entrenamiento vencido, pero la primera publicación exige que todos sus entrenamientos sean futuros.
- La primera publicación no puede retirarse.
- El modo edición admite varios días y un único guardado confirmado.
- Cada guardado crea una versión y una tanda de notificaciones; guardados separados no se agrupan.
- El formulario local se descarta al abandonar la edición y no se recupera tras un cierre forzado.
- La concurrencia es optimista y global por plan; no existe mezcla automática.
- Las versiones históricas son internas. Solo se exponen creador y última modificación a administrador y entrenador.
- La baja no reescribe destinatarios ni planes de la semana; bloquea intentos de correo posteriores mientras el corredor permanezca inactivo.
- La reactivación no reabre solicitudes omitidas, pero permite enviar solicitudes nuevas de versiones futuras mientras el corredor continúe activo.
- Un correo ya en curso puede llegar después de una baja y ese riesgo se acepta.

### Supuestos de implementación

- El plan contiene como máximo siete entrenamientos completos, por lo que sustituir su representación completa es razonable.
- `planning` mantiene una revisión monotónica por plan y puede sustituir atómicamente su contenido mediante una API publicada.
- `runner-management` puede consultar por identificador opaco si un corredor está `active` sin revelar su perfil al worker.
- PostgreSQL, la outbox y el worker siguen las garantías aceptadas en `ADR-0011` y `ADR-0012`.
- La interfaz puede comparar localmente la publicación original y la propuesta sin persistir esa comparación.

## Límites modulares

`publication` gobierna:

- el recurso de publicación por plan;
- las versiones completas e inmutables;
- los destinatarios efectivos congelados y su copia por versión;
- la versión activa y su revisión;
- los días cambiados en cada actualización;
- la candidatura de primera publicación;
- las reglas temporales de publicación y republicación;
- la coordinación atómica con planificación y notificaciones;
- la consulta autorizada de publicaciones para `runner-portal` y `tracking-review`.

`planning` conserva identidad, nombre, grupo, semana, estructura canónica del plan y contenido vigente. Antes de publicar puede modificarlos según su propio diseño. Después de publicar solo aceptará una sustitución completa iniciada por `publication`; nunca persistirá un contenido distinto de la versión activa.

`runner-management` aporta:

- elegibilidad `active` y presentación mínima de corredores para la candidatura;
- comprobación actual de actividad antes de un intento de correo;
- exclusión de cualquier estado no activo en nuevas primeras publicaciones.

`notification-delivery` gobierna solicitudes, leases, reintentos, proveedor, webhooks, supresiones y estados técnicos. Define un puerto previo al intento. `publication` implementa para su tipo de solicitud la consulta de actividad mediante `runner-management`. Así `notification-delivery` no importa módulos de negocio y no se crea un ciclo.

`runner-portal` compondrá la consulta del corredor mediante la API publicada de `publication`; no leerá tablas ni identificadores de versiones históricas.

No habrá HTTP interno, SQL entre esquemas, joins cruzados, imports de paquetes internos ni eventos asíncronos para decidir una publicación.

## Reglas temporales

La fecha de negocio será `LocalDate` obtenida del reloj del servidor en la zona IANA configurada. La semana del plan continúa identificada por su lunes.

| Situación al confirmar | Operación permitida |
| --- | --- |
| Semana futura | Crear, sustituir o eliminar cualquiera de sus entrenamientos. |
| Semana actual, día posterior a hoy | Crear, sustituir o eliminar el entrenamiento. |
| Semana actual, hoy o día anterior, plan publicado | Ninguna mutación del entrenamiento. |
| Semana actual, hoy o día anterior, nunca publicado | Solo eliminar el entrenamiento para limpiar el borrador. |
| Semana finalizada | No se crea ni modifica contenido; solo aplican consulta y retención. |

La primera publicación exige al menos un entrenamiento y que todos tengan fecha `> businessDate`. Una actualización exige al menos una diferencia visible, que todos los días modificados sean futuros y que el resultado conserve al menos un entrenamiento.

La validación se realiza al confirmar, no solo al abrir la pantalla. Si la fecha cambia durante la sesión, toda la sustitución se rechaza mediante un Problem Details estable y se mantiene la versión activa.

## Modelo de dominio

### Publicación del plan

Existe como máximo una publicación estable por `weeklyPlanId`. Contiene:

- identificador estable y referencia opaca al plan;
- grupo, nombre visible del grupo y semana congelados;
- conjunto congelado de destinatarios;
- número e identificador de versión activa;
- revisión monotónica para `ETag`;
- creador e instante de creación del plan, copiados desde `planning`;
- último modificador e instante de la última modificación confirmada de contenido o identidad, copiados en la primera publicación y actualizados en cada sustitución posterior.

Confirmar una primera publicación sin modificar contenido no convierte al publicador en último modificador. Cada versión sí conserva internamente el actor que la confirmó para trazabilidad técnica.

El estado se deriva:

| Estado del plan | Publicación | Comportamiento |
| --- | --- | --- |
| `borrador` | No existe publicación actual. | Se prepara en `planning`; puede eliminarse y cambiar nombre, grupo o semana. |
| `publicado` | Existe exactamente una publicación actual. | Nombre, grupo y semana inmutables; solo se sustituye contenido futuro mediante `publication`. |

No existe tercer estado, publicación retirada ni indicador de cambios pendientes.

### Versión publicada

Cada versión incluye:

- número secuencial sin huecos por plan confirmado;
- actor e instante de confirmación;
- instantánea completa de nombre, grupo congelado, semana y entrenamientos;
- identificadores estables de entrenamiento y toda su estructura visible;
- huella canónica de contenido;
- conjunto completo de destinatarios copiado;
- para una actualización, colección de días `added`, `modified` o `deleted`;
- marca única de versión activa.

Una versión abortada no existe y no consume número. Las versiones anteriores no pueden volver a activarse.

### Candidatura de primera publicación

Es una representación derivada con:

- plan, grupo, semana y entrenamientos completos;
- conteo exacto de destinatarios;
- lista paginada de identificador opaco, nombre y apellidos de cada destinatario activo;
- `candidateRevision` opaca ligada a revisión del plan, grupo, composición, resultados efectivos y fecha de negocio;
- advertencia de congelación e irreversibilidad.

No contiene direcciones de correo ni estados ajenos a la confirmación. No se persiste, no bloquea recursos y caduca lógicamente cuando cambia cualquiera de sus entradas o la fecha local.

### Sesión de edición

La sesión existe solo en la SPA. Conserva la representación obtenida, su `ETag` y la propuesta local. El botón `Editar` no llama a una transición del backend ni coloca un bloqueo. El cliente impide seleccionar días bloqueados, pero el servidor repite todas las reglas.

`Guardar` calcula los días distintos y abre la confirmación. `Confirmar y publicar` sustituye la publicación actual completa. Cancelar o abandonar descarta la propuesta después del aviso posible del navegador.

## Modelo persistente

El esquema `publication` contendrá inicialmente:

| Tabla | Contenido e invariantes principales |
| --- | --- |
| `published_plan` | UUID, `weekly_plan_id` único, grupo y semana congelados, versión activa, revisión, creador y última modificación. |
| `published_plan_recipient` | Pareja única plan publicado-corredor congelada en versión `1`; nunca se recalcula. |
| `published_plan_version` | UUID, plan, número secuencial único, huella, actor, instante y marca activa única. |
| `published_version_recipient` | Copia inmutable y única versión-corredor; refuerza la trazabilidad de cada publicación. |
| `published_workout` | Versión, UUID estable de entrenamiento, día y campos visibles de cabecera. |
| `published_phase_duration` | Duraciones congeladas de calentamiento y enfriamiento por entrenamiento publicado. |
| `published_workout_block` | Orden, repeticiones, carga y objetivo congelados de la parte principal. |
| `published_workout_recovery` | Recuperación congelada opcional por bloque. |
| `published_version_changed_day` | Fecha y tipo `added`, `modified` o `deleted` para el resumen de actualización. |

Las tablas de instantánea son fuente de verdad de la publicación y nunca se reconstruyen desde `planning`. Conservarán valores visibles de catálogo necesarios para que un nombre posterior no altere una versión. Las tablas de notificación permanecen en `notification_delivery`.

Restricciones mínimas:

- un único `published_plan` por plan semanal;
- número de versión único y creciente por publicación;
- una única versión activa por plan;
- destinatario único por plan y por versión;
- un entrenamiento por versión y día;
- estructura, órdenes y variantes exclusivas coherentes con `ADR-0006` y `ADR-0020`;
- destinatario activo único por corredor y semana mediante la restricción de exclusividad aceptada;
- día cambiado único por versión y fecha;
- huella diferente de la versión activa anterior para crear otra versión.

## Casos de uso

### Obtener candidatura

Administrador o entrenador solicita la candidatura de un plan nunca publicado. `publication`:

1. autoriza al actor;
2. obtiene fecha local y revisión estable del plan;
3. valida estructura, grupo activo, semana y entrenamientos futuros;
4. obtiene revisión y miembros efectivos activos del grupo;
5. rechaza grupo vacío o conflicto con otro plan activo de la semana;
6. genera `candidateRevision`, resumen, conteo y lista autorizada.

La lectura no adquiere el bloqueo global durante toda la interacción y no reserva destinatarios. La revisión opaca evita tratarla como una promesa; la confirmación vuelve a ejecutar y comparar todo.

### Confirmar primera publicación

La interfaz presenta candidatura y advertencia. Tras confirmar, envía la representación completa y `candidateRevision`. La transacción:

1. autoriza a administrador o entrenador;
2. obtiene la fecha local;
3. adquiere `planning_coordination`;
4. bloquea el plan y filas dependientes en el orden aceptado;
5. comprueba que nunca se publicó y recalcula la candidatura;
6. exige igualdad de revisión y contenido confirmado;
7. resuelve miembros `active`, rechaza vacío y valida exclusividad corredor-semana;
8. crea `published_plan`, destinatarios y versión `1` completa;
9. crea una solicitud de notificación por destinatario;
10. registra en `planning` el estado publicado y conserva la autoría vigente del contenido;
11. confirma todo o revierte todo.

La respuesta devuelve `201 Created`, la publicación actual, `Location` y `ETag`. La visibilidad comienza tras el commit; no espera al proveedor.

### Editar y actualizar una publicación

Administrador o entrenador obtiene la publicación actual y pulsa `Editar`. La SPA crea la sesión local y bloquea visualmente días no modificables. Puede añadir, sustituir o eliminar varios días futuros.

Al guardar, la interfaz muestra todos los días cambiados y la advertencia de notificación. Tras confirmar, envía la representación completa con `If-Match`. La transacción:

1. autoriza al actor y obtiene fecha local;
2. bloquea el plan y la publicación en orden estable;
3. compara el `ETag` y rechaza una base obsoleta;
4. exige igualdad de nombre, grupo y semana;
5. calcula diferencias canónicas por día y rechaza un no-op;
6. exige que cada día cambiado sea futuro y que quede al menos un entrenamiento;
7. valida la estructura completa mediante la API de `planning`;
8. sustituye el contenido de `planning` e incrementa su revisión;
9. crea una única versión completa, sus destinatarios copiados y días cambiados;
10. crea una solicitud por destinatario efectivo y actualiza versión activa y última modificación;
11. confirma todo o revierte todo.

La respuesta devuelve `200 OK` y el nuevo `ETag`. Un guardado posterior parte de esa versión y generará otra publicación independiente.

### Abandonar o perder una edición

Cancelar no llama al backend. Navegar fuera con cambios provoca una advertencia de la SPA cuando sea técnicamente posible. Confirmar la salida o cerrar de forma forzada descarta la propuesta. No existe endpoint para recuperar, listar o eliminar sesiones.

### Procesar una notificación de publicación

Después de reclamar una solicitud y antes de iniciar cada llamada a Brevo, `notification-delivery` invoca el puerto de elegibilidad:

1. la implementación de `publication` recibe identificadores opacos de solicitud y corredor;
2. consulta a `runner-management` si el corredor continúa `active`;
3. si está activo, autoriza ese intento y continúa `ADR-0011`;
4. si no está activo, marca `omitido-inactivo`, termina la solicitud y libera el orden;
5. si la consulta falla, no contacta con Brevo, conserva la solicitud recuperable y registra fallo técnico.

Cada reintento vuelve a consultar. Reactivar al corredor no reabre solicitudes ya omitidas. Una baja posterior al comienzo de la llamada no cancela el mensaje en curso.

Si después de reactivarse se confirma otra versión del mismo plan, el corredor continúa dentro del conjunto congelado, recibe una solicitud nueva y puede volver a ser elegible para su envío. No se recuperan ni regeneran correos correspondientes a versiones omitidas durante la inactividad.

## Notificaciones

Cada solicitud conserva versión, plan, destinatario, destino usado, tipo, contenido mínimo, clave idempotente y correlación. El destino no se vuelve a resolver durante reintentos.

Primera publicación:

- identifica que el plan está disponible;
- incluye semana y, por entrenamiento, día, fecha y tipo principal;
- incluye enlace autenticado a la publicación activa.

Actualización:

- identifica que el plan se actualizó;
- incluye el mismo resumen semanal completo;
- destaca cada día añadido, modificado o eliminado;
- no incluye comparación anterior/posterior, fases, bloques, cargas, objetivos, recuperaciones, aclaraciones, ubicación ni seguimiento.

Una versión crea una sola solicitud por destinatario. Varias modificaciones dentro de la misma sesión no crean correos separados; guardados confirmados distintos sí.

Estados técnicos aplicables:

| Estado | Semántica |
| --- | --- |
| `pendiente` | Espera intento o reintento. |
| `procesando` | Reclamada con lease vigente. |
| `aceptado-proveedor` | Brevo aceptó la petición; terminal para el envío de la aplicación. |
| `entregado` | El servidor receptor confirmó entrega mediante evento. |
| `fallo-definitivo` | No habrá más intentos por fallo o política técnica. |
| `omitido-inactivo` | No se contactó con el proveedor porque el corredor no estaba activo; terminal y no reactivable. |

`omitido-inactivo` libera el orden de versiones igual que los demás terminales. No se muestra a ningún rol del producto.

## Permisos y representaciones

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Obtener candidatura y destinatarios exactos | Sí | Sí, solo corredores activos | No |
| Confirmar primera publicación | Sí | Sí | No |
| Consultar publicación administrativa actual | Sí | Sí | No |
| Editar y confirmar días futuros | Sí | Sí | No |
| Ver creador y última modificación | Sí | Sí | No |
| Consultar publicación propia activa | Mediante capacidad operativa | Mediante capacidad operativa | Sí, solo si es destinatario y conserva acceso activo |
| Consultar o restaurar versiones anteriores | No | No | No |
| Retirar o despublicar | No | No | No |
| Ver entrega o reintentar manualmente | No | No | No |

La lista de candidatura expone nombre y apellidos necesarios para verificar el grupo, no correo ni estado técnico. La representación del corredor omite autoría, destinatarios, números internos de versión y metadatos de entrega.

La baja deshabilita acceso mediante `runner-management` e identidad sin borrar la pertenencia histórica. Una reactivación puede volver a habilitar la consulta mientras la publicación siga retenida y el corredor pertenezca al conjunto congelado; nunca recalcula ese conjunto.

## APIs internas publicadas

`publication` publicará tipos propios para:

- consultar la publicación activa autorizada por plan;
- consultar por corredor y semana una publicación activa propia para `runner-portal`;
- validar que un entrenamiento pertenece a una versión visible para `tracking-review`;
- aplicar retención, anonimización o supresión sin reactivar versiones;
- implementar la elegibilidad previa al intento exigida por `notification-delivery`.

Consumirá de `planning`:

- lectura y bloqueo de un plan publicable;
- validación canónica de una representación completa;
- resolución coordinada de grupo y miembros para primera publicación;
- registro de primera publicación;
- sustitución atómica del contenido de un plan publicado;
- actualización de revisión y autoría sin acceso a tablas.

Consumirá de `runner-management`:

- presentación mínima y estado de corredores activos para candidatura;
- comprobación opaca de estado `active` previa a cada intento;
- anonimización o supresión coordinada conforme a retención.

Consumirá de `notification-delivery`:

- creación transaccional de solicitudes autocontenidas;
- puerto de elegibilidad que `publication` implementa para notificaciones de planes;
- estados terminales necesarios para orden y retención, sin exponer proveedor al dominio.

Los contratos transportan `ActorContext`, correlación, revisiones, fecha calculada por servidor y resultados cerrados. No exponen jOOQ, tablas, modelos OpenAPI ni entidades internas.

## API HTTP prevista

OpenAPI `3.1` será la fuente de verdad antes de implementar. Las operaciones administrativas son:

| Actor | Método y recurso | Semántica |
| --- | --- | --- |
| Administrador o entrenador | `GET /api/weekly-plans/{weeklyPlanId}/publication-candidates/current` | Obtiene candidatura derivada, resumen, conteo, `candidateRevision` y revisión de sus dependencias. Solo existe antes de publicar. |
| Administrador o entrenador | `GET /api/weekly-plans/{weeklyPlanId}/publication-candidates/current/recipients` | Recorre con cursor la lista exacta ligada a `candidateRevision`; un cambio invalida el cursor. |
| Administrador o entrenador | `GET /api/weekly-plans/{weeklyPlanId}/publications/current` | Obtiene la publicación activa completa, días modificables, autoría y `ETag`. |
| Administrador o entrenador | `PUT /api/weekly-plans/{weeklyPlanId}/publications/current` | Crea la primera publicación con `If-None-Match: *` y `candidateRevision`, o sustituye la actual con `If-Match`; recibe representación completa. |

`publication-candidate` es un recurso derivado real: representa el estado publicable actual, tiene identidad contextual, representación y revisión, pero no persistencia ni mutaciones. `publications/current` es la publicación activa estable; cada sustitución crea una versión interna sin exponer una colección histórica.

No existirán `POST /publish`, `POST /republish`, `/restore`, `/withdraw`, `/unpublish`, `/history` ni prefijos por rol. Tampoco se expondrán rutas por identificador de versión anterior.

Semántica mínima de `PUT`:

- primera publicación: cuerpo completo y `candidateRevision`, `If-None-Match: *`, respuesta `201 Created`, `Location` y `ETag`;
- actualización: cuerpo completo e `If-Match`, respuesta `200 OK` y nuevo `ETag`;
- repetición con precondición antigua: `412 Precondition Failed`, sin otra versión ni correo;
- estado incompatible, día bloqueado, último entrenamiento eliminado o no-op: `409 Conflict` con tipo estable;
- campo o estructura inválidos: `422 Unprocessable Content` si el OpenAPI común adopta esa convención;
- actor no autorizado: tratamiento de `ADR-0015` sin revelar recursos ajenos.

El `PUT` es idempotente desde la perspectiva HTTP: la misma precondición solo puede confirmar una sustitución. Si la respuesta se pierde, repetirla no crea otra versión; el cliente obtiene la publicación actual y compara su huella o representación.

La lista de destinatarios usa cursor opaco ligado a plan, candidatura, orden y revisiones. No incluye datos personales en el cursor ni permite reutilizarlo tras cambios.

## Concurrencia y transacciones

Orden de bloqueo:

1. `planning_coordination` solo para primera publicación;
2. plan semanal por UUID;
3. publicación estable por UUID;
4. versión activa y filas dependientes por UUID estable;
5. destinatarios y solicitudes en orden de corredor.

La primera publicación comparte el bloqueo de coordinación con clasificación y grupos. Una actualización no lo adquiere porque conserva exactamente los destinatarios efectivos congelados, incluso si el grupo está inactivo.

Toda sustitución compara revisión global. No existen revisiones por día porque una versión representa la semana completa. Dos sesiones que cambien días distintos no se mezclan: una se confirma y la otra recibe `412`.

Versión, instantánea, destinatarios, días cambiados, versión activa, contenido de `planning`, autoría y solicitudes de outbox participan en la misma transacción PostgreSQL. Ninguna llamada a Brevo ocurre dentro de ella.

Las restricciones físicas impiden dos versiones activas, números duplicados y un segundo plan activo para el mismo corredor y semana. Un error revierte todo; no se usan compensaciones.

## Consultas e índices

Índices candidatos:

- publicación por plan semanal;
- versión activa por publicación y número de versión;
- destinatarios por publicación, versión y corredor;
- exclusividad activa por corredor y semana;
- publicaciones activas por corredor y semana para `runner-portal`;
- entrenamientos por versión y día;
- solicitudes por plan, destinatario, versión, estado y siguiente intento;
- versiones y contenido por fecha de entrenamiento para retención.

Los índices se confirmarán con cardinalidades y `EXPLAIN (ANALYZE, BUFFERS)`. No se indexará contenido de entrenamientos sin un patrón medido.

## Retención y privacidad

Planes publicados y contenido aplican `24` meses desde la fecha de cada entrenamiento conforme a `ADR-0010`. Las acciones de retención pueden anonimizar o suprimir datos vencidos sin considerarse una edición ni reactivar una versión. El plan raíz se elimina o anonimiza cuando ya no conserva ningún entrenamiento ni finalidad vigente.

Las solicitudes y datos de entrega aplican `90` días desde su estado técnico final, incluido `omitido-inactivo`. Autoría e historial técnico aplican la categoría y plazo que les corresponda; no se prolongan por consultas, reactivaciones o nuevas versiones.

La candidatura no se persiste y sus presentaciones se mantienen solo durante la respuesta. Logs, métricas, trazas y Problem Details no incluyen nombres, correos, lista de destinatarios, contenido del plan ni motivos personales de baja.

Una solicitud de derechos no altera silenciosamente publicaciones como si nunca hubieran existido. Se ejecuta por categorías y puede anonimizar referencias conforme a la política aprobada.

No se usarán datos personales reales ni se habilitará el proveedor mientras sigan pendientes las evidencias de privacidad, dominio, DPA, subencargados, regiones, retención, EIPD y operación identificadas en los ADRs aplicables.

## Observabilidad

Métricas agregadas:

- candidaturas obtenidas, invalidadas y rechazadas;
- primeras publicaciones y actualizaciones confirmadas o abortadas;
- versiones por plan y días cambiados por actualización;
- conflictos de `ETag`, no-op y bloqueos temporales;
- tamaño de destinatarios por publicación sin identidades;
- solicitudes creadas, omitidas por inactividad y retrasadas por fallo de elegibilidad;
- latencia de transacción, cola y aceptación del proveedor;
- fallos de retención y antigüedad de datos vencidos.

Logs estructurados admiten identificadores opacos de correlación, plan, publicación y versión, pero no nombres, correo, contenido ni listas. Las métricas no usan `runnerId` como etiqueta.

Alertas:

- publicación abortada después de un fallo de persistencia inesperado;
- violación de restricciones de versión o exclusividad;
- crecimiento sostenido de solicitudes que no pueden comprobar elegibilidad;
- divergencia detectada entre contenido de `planning` y versión activa;
- trabajo de retención vencido o fallido;
- las alertas de proveedor y outbox continúan bajo `ADR-0011` y `ADR-0016`.

## Validación prevista

### Primera publicación

- Probar candidatura con grupo, semana, contenido, conteo y lista exactos.
- Invalidar la candidatura por cambios de plan, grupo, clasificación, actividad o fecha local.
- Rechazar grupo inactivo, vacío, conflicto corredor-semana y cualquier entrenamiento de hoy o anterior.
- Inyectar fallos entre cada escritura y comprobar rollback de planificación, versión, destinatarios y outbox.
- Ejecutar carrera con reconfiguración y demostrar que el bloqueo común captura un estado completo.

### Edición y republicación

- Probar sesión local, varios días, advertencia de salida y pérdida tras cierre forzado.
- Probar añadir, sustituir y eliminar varios días en una única versión.
- Rechazar hoy, pasado, semana finalizada, eliminación del último entrenamiento y no-op.
- Atravesar medianoche durante la edición y rechazar toda la propuesta si un día deja de ser futuro.
- Ejecutar dos editores sobre el mismo `ETag`; confirmar uno y conservar localmente la propuesta rechazada del otro.
- Comprobar que nombre, grupo y semana no cambian y que no existen rutas de retirada, restauración o historial.
- Probar que `planning` y publicación activa siempre confirman o revierten juntos.

### Destinatarios y correo

- Probar congelación en versión `1` y copia exacta en cada versión posterior.
- Dar de baja después de publicar y comprobar que el destinatario histórico permanece, pero un intento posterior termina `omitido-inactivo`.
- Publicar por primera vez un plan posterior a la baja y comprobar que el corredor queda excluido.
- Reactivar y demostrar que no se recalculan publicaciones ni se reabren solicitudes omitidas.
- Confirmar otra versión después de reactivar y comprobar que crea una solicitud nueva que puede enviarse mientras el corredor continúe `active`.
- Cambiar estado entre intentos y repetir la comprobación antes de cada llamada.
- Simular fallo de elegibilidad y comprobar que no se llama a Brevo ni se pierde la solicitud.
- Simular baja después de la comprobación y aceptar que el mensaje en vuelo puede llegar.
- Verificar resumen semanal, todos los días cambiados, ausencia de detalle y enlace a la publicación activa.
- Probar orden por plan-destinatario-versión con `omitido-inactivo` como terminal liberador.

### Seguridad, módulos y API

- Probar permisos de administrador, entrenador y corredor, incluida la ausencia de autoría en la vista del corredor.
- Verificar que ningún identificador concede acceso y que listados y conteos aplican el mismo predicado.
- Ejecutar `ApplicationModules.verify()` y ArchUnit para impedir ciclos, SQL cruzado y acceso a internos.
- Revisar recursos, métodos, seguridad, precondiciones, idempotencia y Problem Details contra `ADR-0017`.
- Generar servidor y cliente desde OpenAPI, ejecutar Spectral y bloquear incompatibilidades con `oasdiff`.
- Verificar retención de publicaciones y solicitudes, reaplicación tras restaurar copias y ausencia de datos personales en telemetría.

## Cambios de alcance y riesgos aceptados

Este diseño reemplaza respecto a `ADR-0020` la preparación persistente de cambios después de publicar. Desaparecen edición silenciosa del borrador publicado, `hasPendingChanges`, restauración y mutabilidad del nombre. La edición publicada pasa a ser una sustitución atómica de la publicación actual iniciada desde una sesión local.

No se amplía el PMV con nuevas notificaciones ni historial visible. Se añade la comprobación de actividad previa a cada intento de publicación y el estado técnico `omitido-inactivo`.

Riesgos aceptados:

- una publicación equivocada no puede retirarse;
- un error en hoy o el pasado no puede corregirse mediante republicación;
- un cierre forzado pierde la edición local;
- sesiones concurrentes pueden exigir reaplicar cambios manualmente;
- guardados separados producen correos separados;
- un correo ya en vuelo puede llegar tras la baja;
- las versiones históricas ocupan almacenamiento aunque no aporten una interfaz visible.

## Conclusiones

- La publicación actual es la única fuente visible y siempre coincide con el contenido publicado de `planning`.
- La sesión local permite corregir varios días sin introducir un borrador persistente ni correos por cada cambio de formulario.
- Las precondiciones, el reloj local y la transacción completa impiden cambios retrospectivos, pérdida silenciosa y estados parciales.
- Destinatario histórico y elegibilidad actual para enviar correo quedan separados sin recalcular el plan.
- La API representa candidatura y publicación actual como recursos; no expone comandos nominalizados ni historial innecesario.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro del diseño detallado de `publication`.

- Antes de implementar deberán producirse OpenAPI, migraciones Flyway, tipos jOOQ, catálogo común de Problem Details, límites de página medidos y pruebas transaccionales con PostgreSQL.
- El diseño detallado de `runner-portal` deberá materializar la representación móvil del corredor sin exponer metadatos administrativos ni versiones históricas.
- Los datos personales reales, el proveedor de correo y la producción continúan bloqueados hasta completar las evidencias de privacidad y operación aplicables.
