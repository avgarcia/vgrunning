# ADR-0011: Entrega de correo transaccional

**Estado:** Aceptado
**Fecha:** 2026-08-13
**Fecha de aceptación:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV necesita correo transaccional para invitación, recuperación de acceso y publicación o republicación de planes. No habrá campañas, boletines, publicidad ni otros eventos de notificación.

`ADR-0003` define los secretos de acceso y sus caducidades: `72` horas para activación y una hora para recuperación. `ADR-0007` y `ADR-0008` hacen visible la publicación sin esperar al proveedor, crean una solicitud individual por versión y destinatario dentro de la transacción y exigen procesar las versiones en orden. Este ADR no modifica esas semánticas; decide cómo entregar todas las solicitudes de correo después de confirmarlas.

El diseño todavía no ha elegido runtime ni plataforma de despliegue. `ADR-0012` define PostgreSQL como persistencia y concreta la reclamación recuperable de la outbox. Tampoco existe un dominio propio de envío. `ADR-0010` exige revisar encargados, subencargados, regiones, contrato, retención y transferencias antes de habilitar un proveedor con datos reales.

La escala prevista, superior a `500` corredores pero limitada a un único club, no justifica introducir un broker distribuido solo para correo. Sí exige recuperar trabajo después de caídas, evitar duplicados lógicos, distinguir aceptación del proveedor de entrega y operar rebotes o fallos globales sin depender de estados visibles en el producto.

## Decisión

El PMV usará **Brevo mediante su API REST de correo transaccional**. No usará el relay SMTP, plantillas editables exclusivamente en el proveedor ni funciones de campañas o gestión de contactos. La contratación y habilitación de Brevo quedan condicionadas a la revisión de privacidad y encargos exigida por `ADR-0010`.

El módulo de publicación y notificación será propietario de una interfaz interna de entrega y de un adaptador de Brevo. El dominio no expondrá tipos, estados ni identificadores específicos del proveedor fuera del adaptador. Sustituir Brevo exigirá una nueva decisión si cambia retención, contrato, regiones, semántica de entrega o comportamiento operativo.

### Outbox y ejecución asíncrona

Todas las solicitudes se persistirán en una outbox de la misma base de datos de la aplicación:

- la transacción que emite o reemplaza un secreto de invitación o recuperación creará su solicitud de correo;
- la transacción de publicación ya crea sus solicitudes según `ADR-0008`;
- si no puede persistirse la solicitud, tampoco se confirmará el secreto o publicación que la origina;
- confirmar la transacción no esperará a Brevo.

Un worker ejecutado dentro de la misma aplicación reclamará solicitudes mediante el bloqueo, lease con token y recuperación definidos por `ADR-0012`. Deberá impedir procesamiento concurrente ordinario y devolver a `pendiente` cualquier trabajo abandonado tras una caída. El PMV no incorporará un broker externo para esta carga.

La entrega tendrá semántica **al menos una vez**. Cada solicitud lógica será inmutable y conservará una clave idempotente UUID estable y una etiqueta opaca de correlación. Todos sus intentos usarán la misma clave, etiqueta, destino y contenido. El adaptador enviará ambos identificadores a Brevo y persistirá el identificador de mensaje devuelto. La idempotencia reduce duplicados ante respuestas perdidas, pero no permite prometer exactamente un correo físico y Brevo solo conserva su clave durante `30` minutos.

Cada solicitud de invitación o recuperación referenciará la generación del secreto que comunica. Emitir un secreto nuevo invalidará el anterior y, dentro de la misma transacción, marcará como `fallo-definitivo` con motivo `reemplazado` cualquier solicitud anterior que siga `pendiente`. El correo nuevo podrá procesarse inmediatamente y no quedará ordenado detrás del antiguo.

Antes de iniciar una llamada a Brevo, el worker comprobará que la generación del secreto sigue vigente y descartará como `reemplazada` una solicitud obsoleta. Si la llamada externa ya está en curso o Brevo ya aceptó el mensaje, no se intentará una cancelación que el sistema no puede garantizar. El secreto antiguo seguirá inválido y las plantillas de invitación y recuperación indicarán que solo funciona el enlace más reciente.

### Estados y eventos

El estado técnico normalizado será:

- `pendiente`: disponible ahora o en el siguiente instante programado;
- `procesando`: reclamada por un worker con arrendamiento vigente;
- `aceptado-proveedor`: Brevo aceptó la solicitud y devolvió identificador;
- `entregado`: el servidor receptor confirmó la entrega mediante evento;
- `fallo-definitivo`: no habrá más intentos automáticos.

Cada transición conservará instante, contador de intentos, siguiente intento, código normalizado y referencia del proveedor cuando exista. No se conservará el cuerpo en los eventos técnicos ni se copiarán secretos a logs. Estos estados no serán visibles para administrador, entrenador ni corredor y no habilitarán reintentos desde el producto.

`aceptado-proveedor` es terminal para el envío desde la aplicación y libera el orden de procesamiento, aunque un evento posterior pueda precisar el resultado como `entregado` o `fallo-definitivo`. Para una pareja plan-destinatario, la versión siguiente podrá comenzar cuando la anterior alcance `aceptado-proveedor` o `fallo-definitivo`; no esperará a la entrega física.

Brevo notificará eventos mediante un endpoint HTTPS autenticado con un secreto Bearer o cabecera equivalente, complementado con restricción de origen cuando sea viable. El receptor validará autenticación antes de procesar, deduplicará por identificador de evento o mensaje y tolerará eventos repetidos o fuera de orden sin hacer retroceder un estado terminal.

Solo se habilitarán eventos necesarios para aceptación, entrega, retraso, rebote, dirección inválida, bloqueo, error y queja. No se habilitará seguimiento de aperturas ni clics. Un evento `entregado` significa entrega al servidor receptor, no lectura por la persona.

Después de `aceptado-proveedor`, un evento `deferred` no generará otro envío desde la aplicación: Brevo seguirá intentando la entrega durante su ventana técnica. Si termina en `soft bounce`, la solicitud pasará a `fallo-definitivo`, pero la dirección no quedará suprimida para futuros correos. Dirección inválida, rebote duro y queja sí aplicarán la supresión global decidida en este ADR.

### Reintentos y fallo definitivo

Los instantes siguientes son desplazamientos desde la creación de la solicitud, no pausas acumulativas entre intentos. Esta planificación solo continúa cuando existe certeza de que Brevo no aceptó el intento anterior:

| Tipo | Intentos máximos y desplazamientos |
| --- | --- |
| Recuperación | Inmediato, `+1`, `+5`, `+15` y `+30` minutos. |
| Invitación | Inmediato, `+1`, `+5`, `+30` minutos, `+2`, `+8` y `+24` horas. |
| Publicación o republicación | Inmediato, `+1`, `+5`, `+15`, `+30`, `+60` y `+120` minutos. |

Una respuesta explícita `429`, un rechazo temporal inequívoco o un fallo anterior a transmitir la petición permiten el siguiente intento previsto. Se respetará `Retry-After` cuando no exceda el siguiente límite útil; no se enviará un enlace de recuperación después de caducar su secreto.

Timeout, `408`, respuesta `5xx` o pérdida de conexión después de transmitir la petición se considerarán resultado incierto, porque Brevo podría haber aceptado el mensaje sin que la aplicación recibiese el identificador. Desde el primer resultado incierto se reconciliará a `+1`, `+5`, `+15` y `+25` minutos con la misma clave y se consultarán los eventos mediante la etiqueta opaca. Una respuesta de clave duplicada o un evento correlacionado confirmará `aceptado-proveedor`.

Si al agotar esa ventana no existe evidencia concluyente, la solicitud pasará a `fallo-definitivo` con motivo `resultado-desconocido` y generará una alerta. No se volverá a enviar después de caducar los `30` minutos de idempotencia, porque hacerlo aceptaría un duplicado físico no controlado. Los intentos posteriores de la tabla solo se usarán cuando el rechazo anterior demuestre que Brevo no procesó la petición.

Dirección inválida, rebote duro, queja o rechazo permanente producirán `fallo-definitivo`. La dirección quedará suprimida para todos los tipos de correo hasta que una operación autorizada corrija la causa y reactive explícitamente el destino. La reactivación será auditada y no reenviará por sí sola solicitudes antiguas.

Credenciales inválidas, dominio no autenticado, cuenta suspendida, cuota contractual agotada o configuración global inválida pausarán el worker y generarán una alerta operativa. No consumirán los intentos individuales mientras persista la incidencia.

Tras corregir una incidencia global, una persona operadora podrá reanudar de forma auditada las solicitudes no terminales. La reanudación reutilizará la solicitud, contenido y clave idempotente originales; no creará una notificación lógica nueva ni ofrecerá la operación en el producto. No reanudará un `resultado-desconocido` fuera de su ventana idempotente. En publicaciones seguirá aplicándose el orden por plan, destinatario y versión definido por `ADR-0008`.

### Contenido, dominio y secretos

Las plantillas estarán versionadas con la aplicación y se renderizarán de forma determinista en HTML y texto plano. Cada solicitud conservará los datos mínimos necesarios para reproducir su contenido sin depender de una plantilla mutable en el panel de Brevo. No habrá adjuntos, píxeles de apertura ni reescritura de enlaces para seguimiento.

Logs, métricas y alertas no incluirán cuerpo, secretos de activación o recuperación ni direcciones completas. La clave API y el secreto del webhook se inyectarán desde el gestor de secretos del entorno, se rotarán y nunca se almacenarán en el repositorio o la base de datos de negocio.

Antes de cualquier envío real se adquirirá un dominio controlado por el responsable del servicio y se definirá un remitente transaccional. El dominio se autenticará según el método vigente de Brevo, con `DKIM` y `DMARC` válidos y verificación del resultado y alineación de `SPF` del mecanismo usado. No se añadirá un segundo registro SPF ni se asumirá que una IP compartida requiere configuración propia de SPF. La ausencia actual de dominio bloquea pruebas externas y producción, pero no la aceptación de esta arquitectura.

### Observabilidad y conservación

La observabilidad interna medirá por tipo de correo: profundidad y antigüedad de cola, intentos, aceptación, entrega, fallo definitivo, supresión, latencia y errores de webhook. Alertará inmediatamente ante pausa global, fallos de autenticación de webhooks o crecimiento sostenido de la cola. Los umbrales concretos y el canal de guardia se fijarán con la plataforma de despliegue antes de producción.

Los registros de notificación aplicarán la retención de `90` días desde su estado técnico final definida por `ADR-0010`. Si no llega ningún evento posterior, `aceptado-proveedor` será el estado final y su instante iniciará el plazo; si llega después un resultado de entrega o fallo, el plazo se contará desde esa última transición. Las métricas posteriores no conservarán dirección, contenido, identificador del plan ni otros datos que permitan identificar al corredor.

## Alternativas consideradas

### Alternativa A: Amazon SES

Se descarta para el PMV mientras no exista una decisión de despliegue en AWS. Su coste unitario es bajo y dispone de regiones europeas, pero exige integrar más piezas para eventos, credenciales, supresiones y operación. Elegirlo ahora acoplaría el correo a una plataforma todavía no decidida.

### Alternativa B: Postmark, Mailgun o Resend

Se descartan como primera opción. Son servicios válidos y algunos ofrecen mejores garantías concretas de idempotencia o experiencia de integración, pero Brevo reduce la complejidad de residencia para este servicio europeo y cubre la API y los eventos necesarios. La revisión contractual sigue siendo obligatoria; alojar bases en la UE no demuestra por sí solo cumplimiento.

### Alternativa C: Relay SMTP del proveedor

Se descarta porque la API ofrece respuestas estructuradas, clave idempotente, identificadores correlacionables y errores clasificables. SMTP dificultaría distinguir aceptación, reintento seguro y fallo de configuración.

### Alternativa D: Enviar de forma síncrona

Se descarta porque haría depender activación, recuperación y publicación de la latencia y disponibilidad de Brevo, y no recuperaría de forma fiable una caída después de confirmar el estado de negocio.

### Alternativa E: Broker de mensajes externo

Se descarta para el PMV. La outbox persistente y un worker recuperable cubren la escala y los fallos previstos sin introducir otro servicio, credenciales, despliegue y observabilidad.

### Alternativa F: Servidor SMTP propio

Se descarta porque trasladaría reputación, entregabilidad, seguridad, rebotes, listas de supresión y operación al equipo sin aportar valor de producto.

## Consecuencias

- El correo queda desacoplado de las transacciones externas, pero la aplicación debe operar un worker fiable.
- La outbox usa la misma persistencia y evita un broker, a costa de implementar reclamación, arrendamientos y barrido de trabajo abandonado.
- La entrega puede duplicarse físicamente en casos límite; no se promete exactamente una vez.
- La idempotencia de Brevo caduca a los `30` minutos; ante un resultado que siga siendo desconocido se prefiere detener el envío y alertar antes que arriesgar un duplicado tardío.
- Recuperación prioriza rapidez y deja de intentarse antes de caducar; invitación y publicación toleran ventanas mayores.
- Una versión posterior no espera confirmación de entrega de la anterior: conserva el orden de aceptación por el proveedor, no un orden físico que no puede garantizarse.
- Los fallos globales se separan de los fallos de un destinatario y no agotan masivamente intentos.
- La supresión global protege reputación y evita insistir sobre direcciones inválidas o quejas, pero requiere una operación autorizada para corregir falsos positivos.
- Reemplazar un secreto evita enviar solicitudes antiguas todavía pendientes, pero un mensaje ya en curso o aceptado puede llegar después; su enlace será inválido y el contenido advertirá que solo vale el más reciente.
- No exponer estados simplifica el PMV, pero obliga a que métricas, alertas y operación interna sean suficientes.
- Versionar plantillas en la aplicación evita cambios silenciosos en Brevo, pero exige desplegar para cambiar contenido.
- No medir aperturas o clics reduce datos personales y complejidad, a costa de renunciar a métricas de interacción que no son necesarias.
- Elegir Brevo introduce dependencia contractual y técnica; la interfaz interna limita su propagación, pero no hace gratuita una migración.
- Sin dominio propio autenticado no puede probarse entregabilidad ni autorizarse producción.

## Requisitos relacionados

- `RF-01`
- `RF-15`
- `RF-20`

## Decisiones de Fase 1 relacionadas

- `D-06`: una publicación o republicación confirmada solicita correo para sus destinatarios efectivos afectados.

## Validación prevista

- Probar atomicidad entre invitación o recuperación y su solicitud de correo, y reutilizar las pruebas transaccionales de `ADR-0008` para publicación.
- Probar que emitir un secreto nuevo invalida el anterior, cierra como `reemplazada` su solicitud pendiente y permite procesar inmediatamente el correo nuevo.
- Simular que el correo antiguo ya está en curso o aceptado, comprobar que no se promete cancelarlo y que su enlace no puede utilizarse después del reemplazo.
- Detener el worker después de reclamar una solicitud y comprobar que el arrendamiento permite recuperarla sin crear otra solicitud lógica.
- Simular respuesta perdida después de aceptar el proveedor y comprobar que todos los intentos usan la misma clave idempotente y contenido.
- Probar reconciliación de resultados inciertos a `+1`, `+5`, `+15` y `+25` minutos, confirmación por clave duplicada o evento correlacionado y cierre `resultado-desconocido` sin envío posterior al TTL.
- Probar cada secuencia temporal y que recuperación nunca se intenta después de caducar el secreto.
- Probar clasificación de timeout, `408`, `429`, `5xx`, diferido, rebote blando, rebote duro, dirección inválida, queja y errores globales.
- Probar que `deferred` no reenvía desde la aplicación, que `soft bounce` cierra solo la solicitud y que Brevo puede completar la entrega durante su ventana propia.
- Probar que una incidencia global pausa el worker, alerta y no consume intentos individuales.
- Probar supresión para todos los tipos, reactivación auditada y ausencia de reenvío retroactivo automático.
- Probar reanudación operativa con la solicitud original y orden por versión de publicación.
- Probar que la versión siguiente se libera con `aceptado-proveedor` o `fallo-definitivo` y no espera a `entregado`.
- Validar autenticación, deduplicación, repetición y desorden de webhooks, incluido rechazo de peticiones no autenticadas.
- Comprobar que los estados terminales no retroceden y que `entregado` no se interpreta como apertura.
- Probar HTML y texto plano de cada plantilla versionada y la exclusión de adjuntos, seguimiento, contenido excesivo y datos de otros corredores.
- Verificar que logs, métricas y alertas no contienen cuerpo, secretos ni direcciones completas.
- Rotar API key y secreto de webhook sin modificar código ni perder solicitudes persistidas.
- Autenticar el futuro dominio y verificar `DKIM`, `DMARC` y el resultado y alineación SPF en mensajes recibidos.
- Revisar DPA, subencargados, regiones y retención de Brevo conforme a `ADR-0010` antes de habilitar datos reales.
- Ejecutar una prueba de volumen superior a `500` destinatarios manteniendo solicitudes individuales, límites de API y antigüedad de cola dentro de los umbrales operativos.
- Verificar la supresión de registros a los `90` días desde el estado final.

## Decisiones pendientes

- **Bloqueante para producción:** adquirir y controlar un dominio, definir remitente y autenticarlo con Brevo. Responsable: propietario del servicio. Tratamiento: completar DNS y pruebas de entrega antes del primer envío real.
- **Bloqueante para producción:** revisar y aprobar Brevo como encargado bajo `ADR-0010`, incluidos DPA, subencargados, ubicaciones, retención y cualquier transferencia. Responsable: responsable del tratamiento con asesoramiento de privacidad. Tratamiento: no contratar ni enviar datos reales hasta aportar la evidencia.
- **Bloqueante para producción:** fijar canal, destinatario y umbrales de alertas con la plataforma de despliegue. Responsable: revisor de arquitectura y persona operadora. Tratamiento: documentar y probar antes de habilitar el worker en producción.
- **Pendiente, sin bloquear la aceptación:** concretar proceso, frecuencia, concurrencia, timeouts y tamaño de lote del worker con el runtime y despliegue elegidos. Responsable: revisor de arquitectura. Tratamiento: conservar la semántica verificable de este ADR y el mecanismo de reclamación de `ADR-0012`.
- **Resuelto por `ADR-0012`:** PostgreSQL reclamará lotes con `FOR UPDATE SKIP LOCKED`, lease persistente y token frente a workers obsoletos.

## Referencias

- [Brevo: API de correo transaccional](https://developers.brevo.com/reference/send-transac-email).
- [Brevo: eventos de webhooks transaccionales](https://developers.brevo.com/docs/transactional-webhooks) y [protección de webhooks](https://developers.brevo.com/docs/secured-webhooks).
- [Brevo: ubicación de almacenamiento](https://help.brevo.com/hc/en-us/articles/360001005510-Data-storage-location) y [DPA](https://help.brevo.com/hc/en-us/articles/15403782599570-Where-can-I-find-the-Data-Processing-Agreement-DPA).
- [Brevo: autenticación del dominio](https://help.brevo.com/hc/en-us/articles/12163873383186-Authenticate-your-domain-with-Brevo-Brevo-code-DKIM-DMARC).
