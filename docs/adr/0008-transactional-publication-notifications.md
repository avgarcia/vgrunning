# ADR-0008: Solicitud transaccional de notificaciones de publicación

**Estado:** Aceptado
**Fecha:** 2026-08-12
**Responsable de revisión:** Revisor de arquitectura
**Refinado parcialmente por:** [ADR-0021](0021-publication-editing-notification-eligibility.md)

## Contexto

`RF-15`, `RF-20` y `D-06` exigen correo al publicar o republicar un plan semanal. El correo es una notificación de disponibilidad o cambio, no el canal principal para consultar el entrenamiento. Debe incluir la semana, un resumen de entrenamientos y un enlace al plan publicado.

`ADR-0007` hace visible cada versión de forma atómica, congela los destinatarios en la primera publicación y evita que la entrega de un sistema externo participe en la transacción de publicación. Queda por garantizar que toda versión confirmada produzca las solicitudes de notificación correctas sin perderlas, duplicarlas como eventos lógicos ni revertir una publicación porque el proveedor de correo esté temporalmente indisponible.

Este ADR decide la semántica de las notificaciones de publicación y su vínculo con la versión publicada. `ADR-0011` define la infraestructura común de correo transaccional para acceso y publicación: proveedor, procesamiento asíncrono, política temporal de reintentos, credenciales, observabilidad técnica y operación del canal.

> **Refinamiento aceptado:** `ADR-0021` conserva una solicitud por versión y miembro congelado, pero introduce la omisión terminal cuando el corredor no está `active` y la fijación del correo verificado vigente al comenzar el primer procesamiento elegible. La baja no reescribe la pertenencia histórica.

## Decisión

El modelo distingue dos hechos:

- **Publicación confirmada:** versión que ha quedado activa según `ADR-0007`.
- **Solicitud de notificación:** registro persistente e inmutable que ordena comunicar esa versión a un destinatario concreto.

La misma transacción que crea y activa una versión publicada deberá crear todas sus solicitudes de notificación. Si no puede persistir cualquiera de ellas, la publicación completa se abortará. Una vez confirmada la transacción, el plan será visible inmediatamente y la entrega se procesará de forma asíncrona; la indisponibilidad o el rechazo posterior del proveedor no revertirá la versión publicada.

Cada publicación y republicación creará una solicitud individual para cada destinatario congelado del plan. Todos ellos se considerarán afectados en cada versión; no habrá selección de subconjuntos, agrupación de direcciones, copia ni copia oculta.

Cada solicitud referenciará como mínimo la versión publicada, el plan, el miembro efectivo, el tipo de evento `publicacion` o `republicacion`, el instante de creación y un identificador idempotente. El destino no se copiará al crear una solicitud de publicación: se fijará atómicamente con el correo verificado vigente cuando el primer procesamiento confirme que el corredor continúa activo. La combinación lógica de versión, miembro y tipo de notificación será única, de modo que reintentar el procesamiento no cree una segunda solicitud para el mismo hecho.

El contenido se generará exclusivamente a partir de la instantánea inmutable de la versión y de los datos conservados en la solicitud. Una vez fijado, todos los intentos y la reconciliación de esa solicitud usarán el mismo destino; un cambio de correo posterior solo afectará a solicitudes futuras. Incluirá la semana del plan y, para cada entrenamiento, día, fecha y tipo de la parte principal, además de un enlace autenticado a la consulta del plan. No incluirá fases, bloques, objetivos, recuperaciones, aclaraciones, ubicación, datos de seguimiento ni información de otros corredores. El mensaje identificará de forma distinta una primera publicación y una actualización.

El enlace abrirá el plan semanal activo del destinatario, no concederá acceso por sí mismo y aplicará la autorización de `ADR-0004`. Si existe una versión posterior cuando se abre el enlace, se mostrará la versión activa actual; el correo no crea acceso permanente a una versión histórica.

La solicitud conservará un estado lógico de entrega suficiente para trazabilidad técnica, pero el PMV no expondrá estados ni detalles de entrega a administrador, entrenador o corredor. El procesamiento, las transiciones técnicas, la política de reintentos automáticos y el criterio de fallo definitivo se concretan en `ADR-0011` sin cambiar la relación inmutable entre solicitud, publicación y destinatario.

El PMV no ofrecerá reintento manual a administrador ni entrenador. Un reintento automático entregará una solicitud existente y no constituirá una nueva publicación, republicación ni notificación lógica. Editar un borrador, consultar un plan, cambiar un grupo o ejecutar cualquier otra acción no creará solicitudes de correo de publicación.

Todas las publicaciones y republicaciones conservarán su propia solicitud y ninguna sustituirá a otra pendiente. Para cada pareja plan-miembro, las solicitudes se procesarán por número de versión: una versión posterior no comenzará sus intentos hasta que la anterior quede `aceptado-proveedor`, `omitido-inactivo` o alcance `fallo-definitivo` según `ADR-0011` y `ADR-0021`. Esta regla garantiza orden de procesamiento, no una entrega física que ningún proveedor puede asegurar.

## Alternativas consideradas

### Alternativa A: Enviar el correo dentro de la petición de publicación

Se descarta porque hace depender la disponibilidad y latencia de publicación de un sistema externo y no permite una transacción atómica fiable entre persistencia y proveedor.

### Alternativa B: Publicar y después crear las solicitudes de correo

Se descarta porque una caída entre ambas operaciones dejaría una versión visible sin notificación y no existiría una forma inequívoca de distinguirla de una entrega todavía pendiente.

### Alternativa C: Crear una única solicitud para todos los destinatarios

Se descarta porque impide trazabilidad e idempotencia por corredor y aumenta el riesgo de exponer direcciones o mezclar resultados de entrega.

### Alternativa D: Considerar el correo como fuente del plan

Se descarta porque duplicaría contenido operativo, produciría mensajes obsoletos y contradiría la decisión de usar el correo solo como notificación. El plan autenticado seguirá siendo la fuente de verdad.

### Alternativa E: Revertir la publicación si falla la entrega posterior

Se descarta porque la entrega externa ocurre después de confirmar la transacción y no puede deshacer de forma segura lo que los corredores ya pueden consultar.

### Alternativa F: Sustituir notificaciones pendientes por la última versión

Se descarta porque cada publicación y republicación es un hecho exigido por `RF-20`. Sustituir solicitudes ocultaría versiones publicadas y convertiría el resultado en dependiente de la velocidad del proveedor.

### Alternativa G: Exponer estados y reintentos manuales

Se descarta para el PMV porque el registro de entrega es deseable, no imprescindible, y añadiría interfaz, permisos y operación manual. La trazabilidad técnica y los reintentos automáticos se resuelven en `ADR-0011`.

## Consecuencias

- No puede existir una publicación confirmada sin solicitudes persistidas para los destinatarios que deban ser notificados.
- La publicación no espera al proveedor y conserva visibilidad inmediata aunque la entrega esté pendiente o falle.
- Habrá un registro por versión y destinatario, con coste de almacenamiento aceptable para la escala del PMV.
- La idempotencia lógica evita crear solicitudes duplicadas, pero `ADR-0011` asume que ningún proveedor garantiza exactamente una entrega física.
- El resumen se mantiene deliberadamente breve y excluye contenido detallado del entrenamiento para no convertir el correo en una copia del plan ni ampliar exposición de datos.
- No existe operación manual ni visibilidad de entrega en el PMV; diagnosticar fallos dependerá de la observabilidad técnica de `ADR-0011`.
- El orden por plan y destinatario puede retrasar una versión posterior mientras se resuelve la anterior, pero evita mensajes fuera de secuencia y no descarta ninguna publicación.
- `ADR-0011` aceptado permite implementar el envío real; sus bloqueos de dominio, privacidad y operación siguen impidiendo producción.

## Requisitos relacionados

- `RF-15`
- `RF-20`

## Decisiones de Fase 1 relacionadas

- `D-06`: una publicación o republicación confirmada solicita correo para sus destinatarios efectivos afectados.

## Validación prevista

- Probar que publicación, versión, miembros efectivos y solicitudes se confirman o abortan dentro de una única transacción.
- Inyectar un fallo al persistir cualquier solicitud y comprobar que la versión no queda visible.
- Simular indisponibilidad del proveedor después de confirmar y comprobar que la publicación sigue activa y las solicitudes permanecen recuperables.
- Probar unicidad e idempotencia por versión, destinatario y tipo de notificación.
- Probar que una solicitud de publicación nace sin destino, fija el correo verificado vigente en su primer procesamiento elegible y lo conserva aunque el usuario lo cambie durante los reintentos.
- Probar que cada solicitud usa exclusivamente datos de la versión inmutable y no del borrador actual.
- Probar que el enlace exige autenticación y autorización y abre el plan activo propio.
- Probar que acciones distintas de publicar o republicar no crean solicitudes.
- Probar que un reintento automático reutiliza la solicitud existente y no crea una nueva notificación lógica.
- Probar que ninguna solicitud agrupa direcciones ni expone destinatarios entre sí.
- Probar el contenido exacto del resumen y la exclusión de fases, bloques, objetivos, recuperaciones, aclaraciones y ubicación.
- Probar que ningún rol puede consultar estados de entrega ni ejecutar reintentos manuales.
- Probar que cada republicación conserva su solicitud y que el procesamiento por plan y destinatario respeta el orden de versión incluso cuando una anterior sigue pendiente o falla definitivamente.

## Decisiones pendientes

Ninguna.
