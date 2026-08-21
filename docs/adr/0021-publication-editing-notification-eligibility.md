# ADR-0021: Edición de publicaciones y elegibilidad de notificaciones

**Estado:** Aceptado
**Fecha:** 2026-08-20
**Fecha de aceptación:** 2026-08-21
**Responsable de revisión:** Revisor de arquitectura
**Validación documental:** Decisiones de publicación aceptadas explícitamente por el responsable el 2026-08-21

## Contexto

`ADR-0007` separa un borrador mutable de las versiones publicadas, congela destinatarios en la primera publicación y exige una versión completa por republicación. `ADR-0008` crea una solicitud de notificación por versión y destinatario, y `ADR-0011` gobierna su entrega asíncrona. `ADR-0020` conserva un borrador de trabajo después de publicar, permite acumular cambios pendientes y ofrece restaurarlo desde la versión activa.

El diseño detallado de `publication` ha confirmado un flujo diferente y más restringido. Después de publicar no existe un caso operativo para preparar cambios durante varios días, restaurar un borrador o modificar la identidad del plan. El entrenador entra explícitamente en modo edición, cambia uno o varios días permitidos en una única sesión local y confirma una republicación completa. Mantener además un borrador persistente produciría dos fuentes editables, un estado pendiente sin utilidad y una operación de restauración que el responsable de producto ha descartado.

También se han concretado dos reglas que las decisiones anteriores no cubren:

- el día local determina qué entrenamientos todavía pueden publicarse o modificarse;
- un corredor dado de baja conserva su pertenencia histórica al plan de esa semana, pero no debe recibir intentos futuros de correo mientras permanezca inactivo.

Estas reglas cambian ciclo de vida, transacciones, API, integración modular, persistencia y pruebas. No pueden introducirse como una corrección editorial de ADRs aceptados. Este ADR refina únicamente las partes incompatibles de `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0014` y `ADR-0020`; sus demás decisiones continúan vigentes. Su materialización completa se describe en [Diseño detallado de publicación](../phase-2-detailed-design-publication.md).

## Decisión

### Regla temporal

La fecha operativa se calculará siempre en la zona IANA del club, inicialmente `Europe/Madrid`, mediante un reloj inyectable del servidor. El cliente no enviará ni elegirá la zona horaria ni la fecha de negocio.

Un plan de una semana futura podrá prepararse y publicarse durante cualquiera de sus días. Para la semana local actual solo serán mutables los entrenamientos con fecha estrictamente posterior a la fecha local de la operación. El entrenamiento de hoy y los anteriores quedarán bloqueados frente a creación, sustitución, eliminación y traslado.

Un plan nunca publicado podrá eliminar entrenamientos que hayan quedado en hoy o en el pasado mientras se preparaba. Esta excepción solo permite limpiar el borrador: no permite crearlos, sustituirlos ni mover otro entrenamiento hacia esas fechas. La primera publicación rechazará cualquier plan que todavía contenga un entrenamiento fechado hoy o antes. Por tanto, no habrá primera publicación retroactiva y una semana completamente transcurrida no podrá publicarse por primera vez.

Las reglas se comprobarán de nuevo dentro de la transacción confirmada. Si una sesión atraviesa la medianoche y alguno de sus días deja de ser futuro, se rechazará toda la operación sin cambios parciales.

### Ciclo de vida después de publicar

El plan conservará únicamente los estados `borrador` y `publicado`. La primera publicación válida es irreversible dentro del PMV: no existirán retirada, despublicación, vuelta a borrador, ocultación manual ni eliminación ordinaria de un plan que alguna vez se publicó.

Después de la primera publicación, nombre, grupo y semana serán inmutables. Solo podrá cambiar el conjunto de entrenamientos futuros. Se permitirá añadir, sustituir o eliminar uno o varios días futuros dentro de una única operación, siempre que el plan resultante conserve al menos un entrenamiento completo.

Un plan publicado no conservará un borrador persistente distinto de su versión activa. Su contenido mutable en `planning` coincidirá siempre con la versión activa de `publication`. Una republicación sustituirá ese contenido y creará la nueva versión dentro de la misma transacción; si falla cualquier paso, ambos permanecerán en la versión anterior. Desaparecen `hasPendingChanges`, el descarte de cambios pendientes y la restauración desde una versión activa.

### Sesión de edición y confirmaciones

El botón de edición abrirá en el cliente una sesión local basada en la versión activa y su `ETag`. No creará un estado de dominio, un bloqueo pesimista ni un recurso persistente. El entrenador podrá modificar varios días permitidos antes de guardar.

Cancelar o abandonar la pantalla descartará la sesión. La interfaz advertirá ante cambios locales no guardados cuando el navegador lo permita. Un cierre forzado, una caída o la pérdida del dispositivo pueden impedir el aviso y perderán el borrador local; el PMV no promete recuperación automática.

Antes de la primera publicación, la interfaz mostrará grupo, semana, todos los entrenamientos, número exacto y lista exacta de destinatarios, y advertirá que el conjunto quedará congelado. La candidatura se ligará mediante una revisión opaca a contenido, plan, grupo, miembros y fecha operativa. Cualquier cambio antes del commit invalidará la confirmación y exigirá obtenerla de nuevo. La candidatura será una representación derivada, no un borrador persistido.

Antes de actualizar un plan publicado, `Guardar` mostrará la representación anterior y la propuesta resumidas por día, identificará todos los días añadidos, modificados o eliminados y advertirá que se generarán notificaciones. Solo `Confirmar y publicar` enviará la operación al servidor.

El servidor aplicará concurrencia optimista sobre la versión completa. Si dos sesiones parten de la misma versión, la primera confirmada gana y la segunda se rechaza como obsoleta. No habrá mezcla automática ni última escritura gana. El cliente conservará temporalmente el formulario rechazado para facilitar su comparación y reaplicación, sin convertirlo en estado persistido.

### Versiones, visibilidad y autoría

Cada guardado confirmado con diferencias visibles creará exactamente una instantánea completa e inmutable, aunque cambien varios días. Guardados separados crearán versiones y tandas de notificación separadas; no habrá agrupación temporal, sustitución de solicitudes pendientes ni periodo de espera. Una propuesta canónicamente igual a la versión activa se rechazará sin consumir número ni crear notificaciones.

Las versiones anteriores se conservarán internamente para integridad, seguimiento, notificaciones y retención, pero ningún rol dispondrá de una lista de versiones, comparación histórica, consulta arbitraria o restauración. El corredor verá siempre la versión activa que le corresponda. Administrador y entrenador verán además quién creó el plan y cuándo, y quién realizó la última modificación confirmada de contenido o identidad y cuándo. Publicar sin cambiar contenido no sustituirá al último modificador, aunque la versión conservará internamente el actor que la confirmó. El corredor no verá esos metadatos de autoría.

### Destinatarios y notificaciones

La primera publicación resolverá exclusivamente corredores `active` y congelará sus identificadores. Una baja posterior no reescribirá la publicación ni retirará al corredor de ese conjunto histórico. Los planes de semanas posteriores cuya primera publicación ocurra después de la baja no lo incluirán. Una reactivación tampoco recalculará destinatarios de planes ya publicados.

A los efectos de `RF-15`, `RF-20` y `D-06`, **destinatario efectivo** designa la pertenencia histórica congelada, mientras **destinatario elegible para envío** designa un destinatario efectivo que continúa `active` inmediatamente antes del intento. Crear una solicitud para el primero no garantiza contactar con el proveedor: esa acción exige además la segunda condición.

Cada versión seguirá creando, en su misma transacción, una solicitud lógica para cada destinatario efectivo congelado y conservará el destino de correo usado al crearla. Inmediatamente antes de cada intento real contra el proveedor, el worker comprobará mediante `runner-management` que el destinatario continúa `active`:

- si está activo, podrá comenzar el intento normal definido por `ADR-0011`;
- si no está activo, la solicitud pasará al estado terminal `omitido-inactivo`, liberará el orden para versiones posteriores y no admitirá reintentos ni reactivación retroactiva;
- si no puede determinarse el estado, no se contactará con el proveedor y la solicitud seguirá recuperable como fallo técnico anterior al envío.

`notification-delivery` no importará `runner-management` ni `publication`. Definirá un puerto de elegibilidad previo al intento; `publication`, que ya puede consumir ambas APIs, implementará la política para las notificaciones de planes y consultará el estado actual del corredor. La solicitud continuará autocontenida para destino, contenido e idempotencia. Esta inversión mantiene las dependencias de `ADR-0014` y evita el ciclo `notification-delivery -> runner-management -> identity-access -> notification-delivery`.

Si el corredor se reactiva, las solicitudes que ya terminaron como `omitido-inactivo` permanecerán cerradas. Una versión del mismo plan confirmada después de la reactivación sí creará otra solicitud y podrá enviarse mientras el corredor continúe `active`, porque la reactivación recupera elegibilidad actual sin alterar el conjunto histórico.

Existe una carrera inevitable entre la comprobación y la llamada externa. Si el corredor pasa a inactivo después de comprobarlo y la petición ya está en curso o ha sido aceptada por el proveedor, el mensaje todavía puede llegar. El PMV acepta expresamente ese riesgo extremo y no promete cancelar un correo en vuelo.

El correo de primera publicación conservará el resumen semanal de `ADR-0008`. El de actualización incluirá también el resumen semanal completo y destacará todos los días añadidos, modificados o eliminados, sin comparación de detalle ni copia completa de los entrenamientos. El enlace abrirá siempre la versión activa autorizada.

### Refinamientos de decisiones anteriores

Con la aceptación de este ADR quedan reemplazadas únicamente estas decisiones incompatibles:

- de `ADR-0007`, el borrador de trabajo persistente después de publicar y el indicador de cambios pendientes;
- de `ADR-0008`, la interpretación de que una solicitud para un destinatario efectivo implica necesariamente iniciar su envío aunque esté inactivo;
- de `ADR-0011`, la máquina de estados cerrada sin `omitido-inactivo` y la ausencia de una comprobación de elegibilidad específica de publicación antes de cada intento;
- de `ADR-0014`, solo la afirmación de que toda solicitud de entrega es completamente autocontenida para decidir su elegibilidad, sin añadir una dependencia modular inversa;
- de `ADR-0020`, la mutabilidad del nombre después de publicar, el borrador persistente publicado, `hasPendingChanges`, la restauración desde la versión activa y la consulta de historial de cambios del plan como capacidad de producto.

Continuarán vigentes las instantáneas completas, destinatarios efectivos congelados, orden de versiones, atomicidad, outbox, reintentos, supresiones técnicas, permisos, retención y demás decisiones no contradichas expresamente.

## Alternativas consideradas

### Alternativa A: Publicar inmediatamente cada día modificado

Se descarta. Obliga a generar una versión y un correo por cada día aunque el entrenador esté corrigiendo una única semana. La sesión local permite revisar varios días y confirmarlos como una sola versión coherente.

### Alternativa B: Conservar un borrador persistente después de publicar

Se descarta. Añade cambios pendientes, recuperación, restauración, retención y conflictos entre sesiones sin necesidad operativa. El plan tiene como máximo siete entrenamientos y la edición se completa en una única interacción.

### Alternativa C: Permitir retirada o modificación completa del plan publicado

Se descarta. La retirada rompería la referencia estable de destinatarios y seguimiento; cambiar nombre, grupo o semana alteraría identidad y contexto temporal después de haber notificado. El coste aceptado es que una publicación errónea no puede ocultarse desde el producto.

### Alternativa D: Recalcular destinatarios al republicar

Se descarta porque una baja, reactivación o reconfiguración de grupos modificaría retrospectivamente el plan semanal. La pertenencia histórica y la elegibilidad actual para correo son conceptos distintos.

### Alternativa E: Cancelar solicitudes al dar de baja o enviar siempre a congelados

Ambas se descartan. Reescribir todas las colas durante la baja acoplaría el ciclo de vida del corredor a detalles de entrega; enviar siempre ignoraría el estado vigente. La comprobación justo antes de cada intento conserva el registro lógico y evita comenzar envíos a inactivos.

### Alternativa F: Exponer historial y restauración de versiones

Se descarta porque el responsable de producto no identifica una decisión operativa que requiera esas capacidades. Las versiones se conservan como estructura interna, no como funcionalidad visible.

### Alternativa G: Bloqueo de edición o mezcla automática

Se descartan. Un bloqueo puede quedar abandonado y exige caducidad; mezclar días produce una versión que ningún entrenador confirmó completa. `ETag` global y rechazo de la sesión obsoleta son más simples y verificables.

## Consecuencias

- El plan visible y el contenido de `planning` no divergen después de publicar; toda modificación confirmada es también una republicación.
- El flujo pierde recuperación de borradores locales ante cierres forzados. Se acepta para evitar estado persistente sin valor probado.
- La ausencia de retirada hace simple la máquina de estados, pero una publicación al grupo equivocado no podrá ocultarse ni deshacerse desde el producto.
- El bloqueo temporal impide corregir retrospectivamente un entrenamiento ya alcanzado; los errores de hoy o del pasado permanecerán en la publicación histórica.
- Una única confirmación puede cambiar varios días y genera un solo correo por destinatario. Guardados separados pueden producir varios correos y no se agruparán.
- Las versiones completas siguen ocupando espacio aunque no exista interfaz de historial; su retención y supresión continúan siendo obligatorias.
- El conjunto de destinatarios conserva trazabilidad aunque un inactivo deje de recibir nuevos intentos de correo.
- El puerto de elegibilidad añade una colaboración síncrona inmediatamente antes del proveedor. Una indisponibilidad de `runner-management` retrasa el envío en lugar de asumir que el usuario está activo.
- La carrera entre comprobación y proveedor no puede eliminarse; se documenta como riesgo aceptado, no como garantía absoluta.

## Requisitos relacionados

- `RF-07`
- `RF-09`
- `RF-10`
- `RF-14`
- `RF-15`
- `RF-16`
- `RF-20`

## Decisiones de Fase 1 relacionadas

- `D-01`: la primera publicación conserva versión y destinatarios efectivos.
- `D-06`: los cambios visibles se confirman mediante republicación atómica, solicitud para cada destinatario congelado y entrega condicionada a elegibilidad `active` en cada intento.
- `D-08`: administrador y entrenador operan globalmente; el corredor solo consulta su publicación.

## Validación prevista

- Probar la fecha local alrededor de medianoche, cambios de horario y semanas actual, futura y transcurrida con reloj inyectable.
- Probar que un borrador puede eliminar entrenamientos vencidos, pero no crearlos, sustituirlos o moverlos hacia hoy o el pasado.
- Probar que la primera publicación rechaza cualquier entrenamiento de hoy o anterior y que una semana transcurrida no puede publicarse.
- Probar que no existen retirada, despublicación, edición de nombre, grupo o semana ni eliminación ordinaria después de publicar.
- Probar sesiones locales con varios días, cancelación, pérdida del borrador y confirmación única.
- Probar que un guardado modifica `planning`, crea una sola versión completa, cambia la versión activa y crea todas las solicitudes, o revierte todo.
- Probar altas, sustituciones y bajas de varios días, rechazo del último entrenamiento y rechazo atómico si uno de los días queda bloqueado.
- Probar `ETag`, dos editores, conservación temporal del formulario rechazado, ausencia de mezcla y rechazo de no-op.
- Probar la candidatura de primera publicación con lista y conteo exactos, y su invalidación ante cambios de plan, grupo, miembros o fecha.
- Probar ausencia de API e interfaz para historial, comparación, restauración y cambios pendientes; comprobar autoría solo para administrador y entrenador.
- Probar que una baja no modifica destinatarios efectivos ni publicaciones y que primeras publicaciones posteriores excluyen al inactivo.
- Probar la comprobación `active` antes de cada intento y reintento, el estado `omitido-inactivo`, su terminalidad y la liberación del orden.
- Probar que una reactivación no reabre solicitudes omitidas y que una versión posterior sí crea una solicitud nueva y enviable mientras el corredor continúe `active`.
- Simular indisponibilidad al consultar elegibilidad y comprobar que no se llama al proveedor ni se pierde la solicitud.
- Simular una baja después de la comprobación y documentar que un mensaje en vuelo puede ser aceptado o entregado.
- Probar un único correo por destinatario y versión, resumen semanal completo, resaltado de todos los días cambiados y enlace a la versión activa.
- Verificar con Spring Modulith y ArchUnit que el puerto de elegibilidad no introduce dependencias o accesos SQL inversos.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro de este ADR.

- Antes de implementar deberán producirse OpenAPI, migraciones Flyway, tipos jOOQ, catálogo común de Problem Details y pruebas transaccionales con PostgreSQL.
- Los datos personales reales, la contratación del proveedor de correo y la producción continúan bloqueados por las evidencias de privacidad y operación de `ADR-0010`, `ADR-0011`, `ADR-0016` y `ADR-0018`.
