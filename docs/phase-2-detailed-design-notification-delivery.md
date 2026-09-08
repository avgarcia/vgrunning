# Diseño detallado de entrega de notificaciones — Fase 2

**Estado:** Validado como diseño — únicamente autorizada la preparación técnica con datos sintéticos
**Fecha:** 2026-08-23
**Fecha de validación:** 2026-08-23
**Responsable de revisión:** Revisor de arquitectura
**Restricción:** Prohibido habilitar Brevo o tratar datos personales reales hasta completar las evidencias de privacidad, dominio y operación exigidas por `ADR-0010`, `ADR-0011` y `ADR-0016`
**Ámbito:** `notification-delivery` y su coordinación con `identity-access` y `publication`

## Propósito

Materializar la aportación de entrega de `RF-01`, `RF-15` y `RF-20` antes de crear el contrato OpenAPI, las migraciones y el worker. El diseño concreta la creación transaccional de solicitudes, prioridad, orden, cifrado, renderizado, envío a Brevo, reconciliación, webhooks, supresiones, operación y retención sin convertir el correo en fuente de verdad del producto.

## Resultado funcional

- Una invitación, reactivación, recuperación, verificación de correo, aviso de cambio de correo, publicación o republicación confirmada deja una solicitud recuperable en la misma transacción que su origen.
- Los enlaces de acceso se entregan con rapidez sin permitir que una publicación masiva los bloquee.
- Cada solicitud usa contenido, plantilla, clave idempotente y correlación estables. Identidad aporta el destino al crearla; publicación lo fija al comenzar el primer procesamiento elegible y lo conserva para todos los intentos.
- Una publicación solo contacta con Brevo cuando el corredor continúa `active` inmediatamente antes de ese intento.
- Brevo puede aceptar, entregar o rechazar el mensaje sin que una caída de la aplicación pierda la solicitud o fuerce un reenvío inseguro.
- Ningún rol del producto consulta estados de entrega ni solicita reintentos manuales.

## Fuentes normativas

- [Requisitos de Fase 1](phase-1-requirements.md), [criterios de aceptación](phase-1-acceptance-criteria.md) y decisión `D-06` de la [matriz de decisiones](phase-1-decision-matrix.md).
- [Diseño de alto nivel](phase-2-high-level-design.md).
- [Diseño detallado de identidad y acceso](phase-2-detailed-design-identity-access.md), que define generaciones de secretos y payloads AEAD.
- [Diseño detallado de publicación](phase-2-detailed-design-publication.md), que define destinatarios congelados, contenido y elegibilidad.
- `ADR-0003`: identidad, invitación, recuperación y secretos de acceso.
- `ADR-0008`: solicitud individual y transaccional por versión y destinatario.
- `ADR-0010`: minimización, derechos y retención de notificaciones durante `90` días desde el resultado técnico final.
- `ADR-0011`: Brevo REST, outbox, estados, idempotencia, reintentos, webhooks, supresiones y observabilidad.
- `ADR-0012`: PostgreSQL, `FOR UPDATE SKIP LOCKED`, lease y transacciones cortas.
- `ADR-0013`: Spring Scheduling, ejecutor dedicado, cliente HTTP, timeouts y controles del build.
- `ADR-0014`: propiedad del módulo, APIs Java y dependencias permitidas.
- `ADR-0016`: Key Vault, despliegue, alertas y operación en Azure.
- `ADR-0017` y la [guía de API HTTP](api-design-guidelines.md): contrato OpenAPI y seguridad de operaciones HTTP propias.
- `ADR-0021`: estado `omitido-inactivo` y puerto de elegibilidad previo a cada intento de publicación.
- `ADR-0031`: modelo persistente mínimo y delegación de supresión, reconciliación de resultados inciertos y pausa global en el proveedor de correo.

Si este documento contradice una fuente aceptada, prevalece el ADR y deberá corregirse el diseño antes de implementar.

## Alcance

Incluye:

- API Java transaccional para crear y reemplazar solicitudes;
- outbox, prioridades, orden, leases y recuperación de trabajo;
- plantillas versionadas, renderizado HTML y texto plano y adaptador REST de Brevo;
- ventana de reconciliación acotada apoyada en la idempotencia nativa del proveedor (`ADR-0031`);
- recepción y proyección directa de eventos de Brevo sobre la propia solicitud;
- pausa global mediante circuit breaker en memoria (`ADR-0031`); la supresión de destinos se delega en el proveedor;
- métricas, alertas, retención y eliminación de contenido sensible.

Quedan fuera:

- campañas, boletines, publicidad, contactos de marketing y plantillas editables en Brevo;
- SMS, WhatsApp, notificaciones push o mensajería interna;
- adjuntos, píxeles de apertura, seguimiento de clics o reescritura de enlaces;
- personalización con nombre o apellidos;
- elección de idioma por cuenta;
- consulta de entrega, reintento, supresión o diagnóstico desde la interfaz del producto;
- reenvío retroactivo de solicitudes terminales;
- cancelación garantizada de mensajes ya enviados o aceptados por Brevo.

## Razonamiento de diseño

1. La misma base de datos permite confirmar origen y solicitud como una unidad; enviar dentro de esa transacción introduciría una red externa que no puede participar en su commit.
2. Una solicitud inmutable con clave estable permite recuperar caídas y reconciliar respuestas perdidas sin inventar una promesa de entrega exactamente una vez.
3. Los correos de acceso tienen una urgencia real superior a la publicación; la prioridad evita que una tanda de más de `500` destinatarios retrase una recuperación de contraseña.
4. La aceptación REST y el webhook son fuentes distintas y pueden llegar desordenadas; una proyección monotónica por `status_rank` sobre la propia solicitud resuelve el desorden sin necesitar una tabla de inbox intermedia (`ADR-0031`): persistir el sobre y proyectar el estado son la misma escritura.
5. Correlacionar por identificadores opacos evita depender del correo incluido por Brevo y reduce datos personales en eventos, logs y métricas.
6. Brevo ya gestiona de forma nativa la supresión de destinos inválidos y ofrece una clave de idempotencia con efecto de deduplicación real en el envío; reimplementar ambas capacidades duplicaría "capacidades maduras del proveedor sin una necesidad funcional o de escala demostrada" (mismo criterio que `ADR-0025` aplicó al framework) (`ADR-0031`).
7. Un saludo genérico elimina datos personales sin reducir la utilidad de ninguno de los correos aprobados.

## Decisiones confirmadas

- La cola prioriza acceso, mantiene FIFO dentro de cada prioridad y conserva el orden de versiones de un mismo plan y destinatario.
- No existen comandos operativos excepcionales propios: reanudar tras una pausa global es automático al corregirse la causa (circuit breaker en memoria, `ADR-0031`) y reactivar un destino suprimido se hace en la consola de Brevo, fuera de la aplicación.
- El webhook exige Bearer y aplica allowlist de IP como segunda barrera, nunca como autenticación única.
- El webhook confirma con `204 No Content` solo después de persistir una inbox mínima; una indisponibilidad transitoria de PostgreSQL responde `429 Too Many Requests` para solicitar reintento.
- Las plantillas del PMV están únicamente en castellano y su versión incluye el idioma para permitir una evolución futura sin reinterpretar solicitudes antiguas.
- El saludo es genérico; la solicitud no contiene nombre ni apellidos.
- Todos los mensajes usan un único `Reply-To` monitorizado por el club. La dirección exacta es configuración de despliegue.
- El recurso de entrada es `POST /api/notification-delivery-events`, neutral respecto al proveedor.

## Supuestos e incertidumbres

| Elemento | Supuesto o incertidumbre | Confianza | Tratamiento |
| --- | --- | --- | --- |
| Proveedor activo | Solo existe un proveedor de correo activo simultáneamente. | Alta | El adaptador y la configuración impiden doble envío; una migración con coexistencia exige otra decisión. |
| Contrato de webhook | Una ruta neutral no neutraliza el payload, autenticación o semántica del proveedor. | Alta | Mantener el nombre estable, aislar la traducción y revisar OpenAPI al sustituir Brevo. |
| Volumen | El volumen de acceso es pequeño frente a las tandas de publicación y queda acotado por los límites de abuso de identidad. | Alta | Prioridad estricta y métricas de antigüedad por nivel; reevaluar cuotas solo con evidencia de inanición. |
| Eventos | Brevo puede repetir o desordenar eventos y no aporta una identidad de evento suficiente en todos los casos. | Alta | Clave de deduplicación canónica, inbox y proyección monotónica. |
| Allowlist | Los rangos de origen de Brevo pueden cambiar. | Alta | Configuración operativa actualizable y Bearer obligatorio aunque la allowlist esté correcta. |
| Respuestas al correo | El club mantendrá atendido el buzón configurado como `Reply-To`. | Media | Es requisito operativo previo a producción; un buzón no monitorizado invalida esta decisión y deberá sustituirse por `no-reply`. |
| Entrega física | Brevo y el servidor receptor pueden aceptar un mensaje que la persona no llegue a leer. | Alta | `entregado` significa recepción por servidor, nunca lectura. |
| `idempotencyKey` en envío individual | La documentación de Brevo describe esa clave bajo el envío por lotes; su efecto de deduplicación real (no solo correlación) en `POST /v3/smtp/email` no está confirmado por prueba propia. | Media | Validar con prueba sintética antes de habilitar producción (`ADR-0031`). Si solo correlacionase, la reconciliación pasa a política de fallo cerrado: resultado incierto cierra como `fallo-definitivo/resultado-desconocido` sin reintento. |

## Lenguaje ubicuo

| Término | Significado |
| --- | --- |
| Solicitud de notificación | Registro lógico inmutable creado por un módulo de negocio y procesado de forma asíncrona. |
| Intento | Una evaluación de elegibilidad y, cuando procede, una llamada concreta al proveedor; se registra como fila de bitácora (`notification_attempt`). |
| Aceptación del proveedor | Evidencia de que Brevo recibió la petición y asume la entrega posterior. |
| Resultado final | Última proyección técnica conocida: entregado, fallo definitivo u omitido por inactividad. |
| Supresión | Bloqueo de futuros envíos a un destino por dirección inválida, rebote duro o queja, gestionado por Brevo (`ADR-0031`); la aplicación no mantiene su propia lista. |
| Pausa global | Circuit breaker en memoria del proceso que impide reclamar nuevos envíos por una incidencia de configuración o cuenta del proveedor; no sobrevive a un reinicio (`ADR-0031`). |
| Reconciliación | Ventana acotada (30 minutos) que espera confirmación de Brevo tras un resultado incierto, apoyada en su idempotencia nativa; fuera de esa ventana se cierra sin reenviar. |
| Destino fijado | Dirección concreta conservada por la solicitud. En identidad se fija al crearla; en publicación, al resolver por primera vez `active(currentVerifiedEmail)`. No se vuelve a resolver en un reintento. |
| Destino elegible | Destino que supera vigencia y, para publicaciones, actividad actual antes del intento; la supresión la resuelve Brevo al aceptar o rechazar el envío. |

En código, OpenAPI y persistencia se usarán `notification request` y `delivery attempt`. No se usará `email job` como concepto de dominio ni se expondrán nombres de Brevo fuera del adaptador.

## Límite modular

`notification-delivery` gobierna:

- solicitudes, intentos, leases, prioridad y orden;
- cifrado del payload persistido (el destino se conserva en claro, ver *Seguridad, privacidad y retención*);
- selección y renderizado de plantillas;
- comunicación con Brevo, reconciliación acotada y normalización de resultados;
- recepción y proyección directa de eventos, pausa global en memoria;
- retención técnica y telemetría.

`identity-access` decide cuándo existen invitación, reactivación, recuperación, cambio de correo o aviso de seguridad. Entrega el tipo, la generación vigente, el destino y el payload mínimo, pero no accede al esquema de notificaciones.

`publication` decide cuándo existe una publicación o republicación, crea una solicitud por miembro efectivo sin copiar su correo y aporta el contenido aprobado. También implementa el puerto de elegibilidad definido por `notification-delivery`, consulta conjuntamente estado y correo vigente mediante `runner-management` y devuelve un resultado cerrado; entrega no importa módulos de negocio.

La dependencia queda:

```text
identity-access ────────> notification-delivery
publication ───────────> notification-delivery
publication ───────────> runner-management
notification-delivery ─> DeliveryEligibilityPolicy <─ publication
```

No existen lecturas SQL entre esquemas ni llamadas de Brevo desde los módulos productores.

## Catálogo cerrado y prioridad

| Prioridad | Tipo | Origen | Contenido mínimo |
| --- | --- | --- | --- |
| `1` | Recuperación de contraseña | `identity-access` | Vigencia, enlace de un solo uso y aviso de usar solo el más reciente. |
| `1` | Verificación de cambio de correo | `identity-access` | Vigencia, enlace de un solo uso y consecuencia de confirmar. |
| `1` | Aviso de cambio al correo anterior | `identity-access` | Aviso de seguridad y canal de respuesta; no contiene secreto. |
| `2` | Invitación y activación | `identity-access` | Vigencia, enlace de activación y aviso de usar solo el más reciente. |
| `2` | Reactivación | `identity-access` | Vigencia, enlace para establecer nueva contraseña y conservar la cuenta. |
| `3` | Primera publicación | `publication` | Semana, resumen por día y enlace a la publicación activa. |
| `3` | Republicación | `publication` | Semana, resumen completo, días añadidos, modificados o eliminados y enlace activo. |

No se crea una notificación por otros cambios de cuentas, corredores, clasificación, planificación o seguimiento. Añadir otro tipo exige revisar alcance, contenido, prioridad, retención y criterios de aceptación.

## Solicitud y máquina de estados

Cada solicitud conserva:

- UUID propio, clave lógica única del productor, tipo y prioridad;
- referencia opaca de origen y, si aplica, generación del secreto;
- clave de orden y número de versión para publicación;
- destino en claro (`destination_email`), obligatorio desde la creación para identidad y todavía ausente al crear una solicitud de publicación — ver *Seguridad, privacidad y retención* sobre por qué no se cifra;
- identificador y versión de plantilla, idioma `es` y payload cifrado (`ADR-0031` mantiene el cifrado del payload, no del destino);
- UUID idempotente estable y etiqueta opaca estable para Brevo;
- estado, `status_rank` (entero para la proyección monotónica), motivo normalizado, contador de intentos y próximo instante;
- lease, referencia del proveedor e instantes de creación, transición y terminalidad.

Estados:

| Estado | Semántica | Puede volver a enviarse |
| --- | --- | --- |
| `pendiente` | Disponible ahora o en el instante programado. | Sí. |
| `procesando` | Reclamada por un worker con lease vigente. | Solo tras resultado explícito recuperable o expiración del lease. |
| `aceptado-proveedor` | Brevo aceptó la petición; libera el orden y termina el envío de la aplicación. | No. |
| `entregado` | El servidor receptor confirmó entrega. | No. |
| `fallo-definitivo` | No habrá más intentos por fallo, caducidad, reemplazo, supresión o resultado desconocido. | No. |
| `omitido-inactivo` | Publicación no enviada porque el corredor no estaba `active`. | No. |

`aceptado-proveedor` es terminal para decidir un nuevo envío, pero admite una precisión posterior a `entregado` o `fallo-definitivo`. Un evento de queja o rebote permanente tiene precedencia sobre `entregado` y activa supresión; un evento atrasado de entrega nunca revierte una queja o fallo permanente. Los eventos sin transición válida se conservan como evidencia mínima y no cambian la proyección.

Motivos terminales normalizados incluyen `reemplazado`, `contenido-caducado`, `destino-suprimido`, `elegibilidad-no-resuelta`, `rechazo-permanente`, `rebote-blando`, `rebote-duro`, `direccion-invalida`, `queja`, `resultado-desconocido` y `intentos-agotados`. Ningún motivo contiene dirección, contenido o respuesta completa del proveedor.

## Modelo persistente

`ADR-0031` reduce el esquema `notification_delivery` a 2 tablas, delegando en Brevo la supresión de destinos y apoyándose en su idempotencia nativa para la reconciliación:

| Tabla | Datos e invariantes principales |
| --- | --- |
| `notification_request` | Solicitud, origen, prioridad, orden, plantilla, destino en claro, payload cifrado, idempotencia, correlación, estado, `status_rank`, programación, lease y resultado. Unicidad de clave lógica, UUID idempotente y etiqueta opaca. |
| `notification_attempt` | Solicitud, origen del evento (`eligibility`, `provider`, `webhook`, `sweeper`), instante, resultado normalizado, código seguro del proveedor, latencia y correlación. `UNIQUE (provider_event_id) WHERE provider_event_id IS NOT NULL` deduplica webhooks sin tabla de inbox separada. No conserva cuerpos ni cabeceras sensibles. |

`delivery_event_inbox`, `delivery_transition`, `suppressed_destination`, `delivery_control` y `delivery_operation_audit` **desaparecen**:

- La deduplicación e ingesta de webhooks que hacía `delivery_event_inbox` la resuelve la propia inserción en `notification_attempt` con `ON CONFLICT DO NOTHING` sobre `provider_event_id`.
- El historial que ofrecía `delivery_transition` se reconstruye leyendo `notification_attempt` ordenado por `occurred_at`; la proyección de estado usa `status_rank` (`pendiente` 0, `procesando` 10, `aceptado-proveedor` 40, `entregado` 50, terminales de fallo 60) para que un evento atrasado no pueda hacer retroceder la solicitud (`UPDATE ... WHERE status_rank < :newRank`).
- `suppressed_destination` se sustituye por la consulta directa a la API de Brevo (`GET /v3/smtp/blockedContacts`); reactivar un destino se hace en su consola, no mediante comando propio.
- `delivery_control` se sustituye por un circuit breaker en memoria del proceso: válido solo mientras el PMV corra en un único nodo, con la misma cláusula de alcance que `ADR-0025` aplicó a Bucket4j. Un fallo global se registra como `notification_attempt` con `outcome='global-error'` **sin incrementar `attempt_count`**, preservando la garantía de `ADR-0011` de que un fallo global no consume intentos individuales.
- `delivery_operation_audit` desaparece porque las dos operaciones que auditaba (reanudar el worker, reactivar un destino) dejan de ser mutaciones propias: la primera es automática al corregirse la causa, la segunda ocurre en Brevo.

El payload sigue cifrado con AEAD (versión de clave y nonce desde Key Vault); el destino se conserva en claro. Ver *Seguridad, privacidad y retención* para la justificación de esta asimetría.

## Creación transaccional e inmutabilidad

La API Java de creación participa en la transacción PostgreSQL del productor. Para identidad:

1. valida tipo, plantilla, clave lógica y payload cerrado;
2. normaliza el destino en memoria;
3. cifra el payload localmente, sin llamar a Key Vault ni a Brevo;
4. crea una solicitud con idempotencia y correlación estables;
5. devuelve su UUID sin iniciar el envío.

Para publicación valida clave lógica, miembro, orden, plantilla y payload, cifra el payload y crea la solicitud sin destino. El primer resultado `eligible(currentVerifiedEmail)` fija el destino atómicamente antes de renderizar o transmitir. Si dos workers compiten, el lease y la precondición de destino ausente permiten una sola fijación.

Una repetición con la misma clave lógica y el mismo contenido devuelve la solicitud existente. La misma clave con contenido distinto es un conflicto de programación y revierte la transacción.

Reemitir una generación de identidad invalida el desafío anterior y, dentro de la misma transacción, cierra su solicitud no terminal como `fallo-definitivo/reemplazado`. Una solicitud ya transmitida o aceptada no puede cancelarse; el enlace anterior permanece inválido.

El payload no se modifica durante los reintentos. Cambiar texto o plantilla exige otra versión desplegada y solo afecta solicitudes nuevas.

## Plantillas, idioma y remitente

Las plantillas se versionan con la aplicación mediante un identificador que incluye propósito, idioma y versión, por ejemplo `password-recovery.es.v1`. Cada versión tiene HTML y texto plano generados de forma determinista y probados como pareja.

- El idioma del PMV es `es`; no se persiste preferencia de idioma en cuentas o perfiles.
- El saludo es genérico y no incorpora nombre ni apellidos.
- El contenido se limita al catálogo anterior y nunca copia información de otros corredores.
- Los enlaces son absolutos, apuntan al dominio controlado y no incluyen parámetros de seguimiento.
- No existen adjuntos, recursos remotos de seguimiento, píxeles de apertura ni reescritura de enlaces.
- El remitente transaccional y el único `Reply-To` monitorizado se inyectan por configuración y deben pertenecer al dominio autenticado.
- La dirección exacta, el nombre visible del remitente y los textos legales se prueban y aprueban antes de producción; no se codifican en Brevo.

El worker descifra destino y payload solo en memoria inmediatamente antes de validar y renderizar. El payload se elimina al alcanzar `aceptado-proveedor`, `fallo-definitivo` u `omitido-inactivo`, al ser reemplazado o al caducar su secreto. El destino permanece cifrado solo durante la retención técnica necesaria.

## Selección, prioridad y orden

El sondeo inicial es cada `5` segundos, con lote máximo `20`, concurrencia máxima `4` y lease de `90` segundos conforme a `ADR-0013`.

Una solicitud es reclamable si:

- está `pendiente` y `next_attempt_at <= now()`;
- no existe pausa global;
- no está bloqueada por una versión anterior de la misma pareja plan-destinatario;
- su lease anterior no existe o ha caducado;
- su contenido no ha caducado ni fue reemplazado.

La consulta usa `FOR UPDATE SKIP LOCKED` y ordena por prioridad ascendente, `next_attempt_at`, creación y UUID. FIFO se interpreta dentro de una prioridad y disponibilidad equivalente; un reintento programado no adelanta su instante por la llegada de trabajo nuevo.

Para una pareja plan-miembro, la versión `n+1` espera hasta que `n` alcance `aceptado-proveedor`, `entregado`, `fallo-definitivo` u `omitido-inactivo`. La prioridad nunca salta esta barrera, una supresión, la vigencia de un secreto o la elegibilidad de publicación.

La antigüedad de cola se mide por prioridad. Si una prioridad inferior creciera de forma sostenida se ajustaría capacidad o reparto con evidencia; no se introduce ahora un algoritmo de cuotas que la escala prevista no necesita.

## Flujo de procesamiento

1. Una transacción corta reclama la fila, asigna token y vencimiento de lease y confirma.
2. Para una publicación sin destino se invoca `DeliveryEligibilityPolicy`; `eligible(currentVerifiedEmail)` fija el destino en claro en una transacción corta protegida por el lease.
3. Fuera de transacción se descifra el payload.
4. Se comprueba que cualquier secreto sigue vigente. La supresión del destino no se comprueba localmente: si Brevo lo tiene bloqueado, rechaza el envío y el resultado se clasifica como `fallo-definitivo/destino-suprimido`.
5. Se renderiza la versión fija de plantilla y se llama a Brevo con la misma idempotencia y correlación.
6. Otra transacción corta actualiza solo si el token de lease continúa vigente.
7. El contenido sensible se elimina cuando deja de ser necesario y el ejecutor libera memoria.

Una política de publicación responde `eligible(currentVerifiedEmail)`, `ineligible` o `retry-later`:

- `eligible(currentVerifiedEmail)` fija el destino si todavía está ausente y permite continuar;
- `ineligible` termina como `omitido-inactivo`, sin contactar con Brevo;
- `retry-later` vuelve a `pendiente`, no consume intento del proveedor y usa backoff local desde `5` segundos hasta un máximo de `5` minutos.

La elegibilidad tiene un máximo absoluto en creación `+120` minutos. Si continúa sin resolverse, termina como `fallo-definitivo/elegibilidad-no-resuelta`, genera alerta y libera la versión siguiente; no se reabre, recrea ni envía manualmente. Una vez fijado el destino no se vuelve a consultar actividad o correo. Una baja o cambio de correo posterior puede dejar llegar el correo al destino fijado y ese riesgo permanece aceptado. La reconciliación de una llamada al proveedor ya iniciada puede concluir después del límite sin realizar un nuevo envío. La pausa global del proveedor es un incidente separado y no reinicia el plazo previo de elegibilidad.

## Adaptador de Brevo

Cada solicitud se envía mediante una llamada individual a la API REST transaccional, no mediante lotes de varios destinatarios. Esta elección mantiene correspondencia uno a uno entre idempotencia, respuesta, reintento, supresión y estado; la escala inicial no justifica introducir fallos parciales de batch.

La llamada incluye:

- remitente y `Reply-To` configurados;
- un único destino;
- asunto, HTML y texto plano renderizados;
- UUID estable como `idempotencyKey`;
- etiqueta opaca estable de correlación;
- ningún dato de contacto de marketing ni seguimiento.

El timeout de conexión inicial es `3` segundos y el total de respuesta `10` segundos. La referencia de mensaje devuelta se persiste antes de liberar el orden. No se conserva la respuesta completa.

Resultado explícito:

- éxito o clave duplicada correlacionable: `aceptado-proveedor`;
- `429`, rechazo temporal inequívoco o fallo previo a transmitir: siguiente intento previsto;
- rechazo permanente de la dirección: `fallo-definitivo` y, cuando corresponde, supresión;
- error global de credencial, dominio, cuenta, cuota contractual o configuración: pausa global sin consumir intentos individuales.

## Reintentos y resultados inciertos

`ADR-0031` sustituye los cuatro calendarios diferenciados por dos escalares (`expires_at`, `max_attempts`) y un backoff exponencial único, calculado desde la creación de la solicitud: `next_attempt_at = now() + min(30 min, 1 min · 2^(n-1)) · jitter`. Las series resultantes (0, 1, 2, 4, 8, 16, 30, 60 min) aproximan las nominales del diseño anterior con desviación de minutos; ningún `RF` ni `CA` depende del desplazamiento exacto.

| Tipo | `expires_at` |
| --- | --- |
| Recuperación, verificación o aviso de cambio de correo | Caducidad del desafío o secreto asociado. Nunca se envía un enlace después de caducar. |
| Invitación, activación o reactivación | Caducidad del secreto de invitación/reactivación. |
| Publicación o republicación | `created_at + 120 minutos` — el mismo valor unifica el horizonte de reintentos y el máximo absoluto de elegibilidad de `ADR-0021`, que en el diseño anterior eran dos mecanismos con la misma cifra. |

`Retry-After` se respeta cuando no excede `expires_at`.

Timeout, `408`, `5xx` o pérdida de conexión después de transmitir producen `resultado-incierto`. La reconciliación se apoya en el `idempotencyKey` de Brevo en lugar de en una ventana de búsqueda activa por referencia:

- se reintenta con la **misma clave de idempotencia** en el bucle ordinario de backoff mientras `now() < first_transmit_at + 30 minutos` — si Brevo suprime el duplicado físico (ver la incertidumbre declarada en *Supuestos e incertidumbres*), la repetición no crea un segundo envío;
- una respuesta de clave duplicada confirma `aceptado-proveedor`, igual que en el diseño anterior;
- cerrada la ventana de 30 minutos sin respuesta concluyente, termina como `fallo-definitivo/resultado-desconocido` y alerta; no se reenvía después, porque la clave de idempotencia ya habría caducado y el duplicado dejaría de estar controlado.

Si la prueba sintética pendiente (ver *Supuestos e incertidumbres*) confirma que `idempotencyKey` en `POST /v3/smtp/email` solo correlaciona sin suprimir el envío físico, esta política pasa a ser de **fallo cerrado**: un resultado incierto se cierra directamente como `fallo-definitivo/resultado-desconocido` con alerta, sin reintentar, para no arriesgar un duplicado no controlado.

## Recepción de eventos

Brevo se configura para enviar eventos individuales a `POST /api/notification-delivery-events`. El endpoint no es una capacidad del producto y usa un esquema de seguridad máquina a máquina distinto de sesión y CSRF.

Controles de entrada:

- HTTPS obligatorio en despliegue;
- Bearer de alta entropía obtenido de Key Vault y comparación en tiempo constante;
- allowlist configurable de rangos oficiales de Brevo como defensa adicional;
- durante una rotación controlada se aceptan token actual y anterior durante un máximo de `15` minutos;
- cuerpo máximo `64 KiB`, JSON y esquema mínimo cerrado para los campos procesados;
- autenticación y tamaño se validan antes de analizar o registrar campos personales.

Respuestas:

| Condición | Respuesta | Efecto |
| --- | --- | --- |
| Evento válido nuevo o duplicado ya persistido | `204 No Content` | Brevo no necesita repetirlo. |
| Evento autenticado pero no soportado | `204 No Content` | Se persiste la inbox mínima, se marca ignorado y no modifica solicitudes. |
| Sobre inválido permanente | `400 Bad Request` | No se persiste ni se solicita reintento. |
| Bearer ausente o inválido | `401 Unauthorized` | No se procesa ni se registra PII. |
| Origen fuera de allowlist | `403 Forbidden` | No se procesa. |
| Cuerpo excesivo | `413 Content Too Large` | No se procesa. |
| PostgreSQL transitoriamente no disponible | `429 Too Many Requests` con `Retry-After` | Brevo debe reintentar. |

Tras autenticar, una única transacción corta hace la proyección directa, sin tabla de inbox intermedia (`ADR-0031`):

1. correlaciona por referencia de mensaje del proveedor y, en su defecto, por etiqueta opaca; nunca usa el correo como clave;
2. inserta la fila de bitácora: `INSERT INTO notification_attempt (…, source='webhook', provider_event_id, …) ON CONFLICT (provider_event_id) DO NOTHING` — 0 filas afectadas significa evento duplicado, se responde `204` sin más efecto;
3. proyecta el estado con precedencia monotónica: `UPDATE notification_request SET status = :new, status_rank = :newRank, … WHERE id = :id AND status_rank < :newRank` — un evento atrasado (`status_rank` menor o igual) no modifica la fila;
4. confirma; la respuesta `204` se emite después del commit.

El argumento original para una tabla de inbox separada — *"un fallo de negocio o un evento desordenado alargaría la petición"* — no se sostiene: la proyección es un `UPDATE` por clave primaria, no más costoso que persistir el sobre que la inbox guardaba.

**La deduplicación por `provider_event_id` solo aplica a eventos correlacionables.** `notification_attempt.request_id` es `NOT NULL` (referencia a la solicitud), así que un evento huérfano —sin solicitud correlacionable por referencia de mensaje ni etiqueta opaca— no tiene fila que insertar y no se persiste: se registra como métrica y log, sin la evidencia de 90 días que ofrecía la inbox. Esto es idempotente por una razón distinta a la de los eventos correlacionados: un huérfano repetido no tiene ningún efecto que duplicar, porque nunca tuvo ninguno la primera vez. La pérdida real es la trazabilidad del huérfano en sí, no un riesgo de duplicación — pérdida aceptada, ver *Cambios de alcance y riesgos aceptados*.

Eventos soportados: aceptación, entrega, diferido, rebote blando, rebote duro, dirección inválida, bloqueo, error y queja. Aperturas y clics permanecen deshabilitados.

- `deferred` no provoca otro envío desde la aplicación;
- `soft bounce` cierra solo la solicitud cuando Brevo agota su ventana; no afecta a la lista de supresión de Brevo;
- rebote duro, dirección inválida o queja cierran la solicitud como `fallo-definitivo/destino-suprimido`; la supresión del destino ya la aplicó Brevo, la aplicación solo proyecta el resultado (`ADR-0031`);
- duplicados, desorden y eventos atrasados no hacen retroceder la proyección (`status_rank` monotónico);
- un evento sin solicitud correlacionable (huérfano) no se persiste; genera métrica y log, no un envío.

## Supresiones y pausa global

`ADR-0031` delega la supresión de destinos en Brevo en lugar de mantener una lista local:

- **Antes de enviar:** no hay comprobación local previa. Brevo bloquea automáticamente el contacto transaccional tras un rebote duro, una queja o una desuscripción; si el envío llega a un destino ya suprimido, Brevo lo rechaza y el resultado se clasifica como `fallo-definitivo/destino-suprimido` (doble red con el evento `blocked` del webhook, ver arriba).
- **Consulta operativa:** `GET /v3/smtp/blockedContacts` (paginado, con la causa) para auditoría o soporte, ejecutado directamente contra la API de Brevo, no como comando propio.
- **Reactivación:** `DELETE /v3/smtp/blockedContacts/{email}`, ejecutado por la persona operadora en la consola o API de Brevo. No existe comando propio ni endpoint interno para esta operación.

Esto elimina la huella HMAC versionada, su rotación y el bloqueante de producción asociado (*"Huella de supresión"*, ver *Decisiones pendientes*): una migración de proveedor exportaría la lista de supresión con `GET /v3/smtp/blockedContacts` en vez de reconstruir huellas.

La pausa global es un **circuit breaker en memoria del proceso**, no una fila persistida — válida solo mientras el PMV corra en un único nodo, con la misma cláusula de alcance que `ADR-0025` fijó para Bucket4j. La solicitud en curso registra el fallo global como `notification_attempt` con `outcome='global-error'` **sin incrementar `attempt_count`**, vuelve a `pendiente` con `next_attempt_at` desplazado y libera su lease — preservando la garantía de `ADR-0011` de que un fallo global no consume intentos individuales. La pausa genera alerta inmediata y se levanta automáticamente al reiniciar el proceso tras corregir la causa; no requiere una operación auditada porque no hay estado que reabrir.

## API Java publicada

```text
NotificationRequestApi
  create(command, correlation) -> NotificationRequestId
  createReplacing(command, previousOriginGeneration, correlation) -> NotificationRequestId
  closePendingByOrigin(originGeneration, reason, correlation)

DeliveryEligibilityPolicy
  supports(notificationType)
  evaluate(requestContext) -> eligible(currentVerifiedEmail) | ineligible | retry-later
```

`NotificationCommand` es una jerarquía cerrada por tipo. Los comandos de identidad contienen clave lógica, origen opaco, destino, plantilla, payload mínimo y caducidad; los de publicación contienen miembro efectivo y orden, pero no destino. El consumidor no elige prioridad, estado, reintentos, idempotencia ni nombres de plantilla arbitrarios.

Los contratos no exponen entidades, tablas, jOOQ, tipos de Brevo, cuerpos renderizados, secretos descifrados, referencia de mensaje ni estado técnico a módulos de producto.

## API HTTP prevista

OpenAPI `3.1` definirá una única operación entrante:

| Cliente | Método y recurso | Éxito | Seguridad | Idempotencia |
| --- | --- | --- | --- | --- |
| Brevo | `POST /api/notification-delivery-events` | `204 No Content` tras persistir | Bearer específico y allowlist; sin sesión ni CSRF | Deduplicación del sobre; repetir no duplica transiciones. |

`notification-delivery-event` es un evento técnico con identidad derivada y ciclo de procesamiento real. La ruta no incorpora proveedor y permite sustituir el adaptador sin cambiar el nombre del recurso. No existen operaciones `GET`, endpoints de estado, rutas de reintento, prefijos por rol ni secretos en URL.

Aunque `ADR-0017` excluye los webhooks condicionados por proveedores de sus convenciones generales, este contrato mantiene deliberadamente `/api`, colección plural y nombre neutral. OpenAPI documentará el subconjunto del payload de Brevo que la aplicación acepta; el adaptador web lo traduce a un sobre interno normalizado sin propagarlo al dominio.

## Concurrencia y transacciones

- Crear, reemplazar o cerrar una solicitud comparte la transacción del módulo productor.
- Reclamar usa una transacción corta; ninguna llamada de red ni descifrado mantiene bloqueos SQL.
- Persistir una respuesta exige token de lease vigente y actualización condicionada; un worker obsoleto no sobrescribe al recuperador.
- El barrido devuelve a `pendiente` leases caducados, salvo que exista evidencia persistida de aceptación que obligue a reconciliar.
- Procesar un evento del webhook deduplica e inserta en `notification_attempt` y proyecta el estado en `notification_request` dentro de una única transacción corta (`ADR-0031`): no hay una tabla de inbox separada que confirmar junto a otra de transición.
- La pausa global vive en memoria del proceso, no en una transacción PostgreSQL; el fallo se registra como intento sin incrementar `attempt_count` en la misma transacción del intento fallido.
- No hay transacciones distribuidas ni compensación contra Brevo.

## Consultas e índices

Índices candidatos:

- parcial por `priority`, `next_attempt_at`, `created_at` e ID para solicitudes `pendiente`;
- por `lease_until` para recuperación;
- único por clave lógica, idempotencia y etiqueta opaca;
- por clave de orden y número de versión;
- único por referencia de mensaje del proveedor;
- único parcial por `provider_event_id` en `notification_attempt` para deduplicar webhooks;
- por `terminal_at` para retención;
- por origen y generación para reemplazo de secretos.

La eliminación de `suppressed_destination` y `delivery_event_inbox` (`ADR-0031`) retira sus índices sin sustituto: la consulta de supresión se resuelve contra la API de Brevo, no contra PostgreSQL. Se confirmarán con datos superiores a `500` destinatarios y `EXPLAIN (ANALYZE, BUFFERS)`. No se indexan cuerpos ni payload.

## Seguridad, privacidad y retención

- **Cifrado asimétrico, decidido en `ADR-0031`:** el payload se cifra en reposo con AEAD; el destino se conserva en claro. La razón es que `identity_access.account_email` ya guarda el correo sin cifrar — cifrar una copia del mismo dato, en la misma base y bajo el mismo cifrado en reposo, no protege frente a ningún adversario realista y solo añade dependencia de ciclo de vida de claves sobre filas que viven 90 días. El payload sí se cifra porque `access_challenge` persiste solo el verificador del secreto: la outbox es el único lugar del sistema donde vive un secreto portador vigente en forma explotable, y leerlo en claro concedería toma de control de cuenta durante hasta 72 horas.
- Solo la identidad de ejecución del worker puede solicitar la clave de payload necesaria. Los roles del producto no pueden consultar el payload descifrado.
- Logs, trazas, métricas, eventos y Problem Details excluyen dirección completa, nombre, contenido, secreto, enlace, cabeceras de autenticación y respuesta completa de Brevo. El destino en claro en la fila no amplía la superficie expuesta: `account_email` y `access_challenge.purpose` ya permiten deducir el mismo hecho ("a esta dirección se le envió una invitación").
- Las pruebas y entornos no productivos usan exclusivamente datos sintéticos o anonimizados.
- Solicitudes e intentos se eliminan a los `90` días desde la última transición técnica final (`ON DELETE CASCADE` desde `notification_request` purga sus `notification_attempt` en la misma operación). Una transición posterior válida reinicia ese plazo.
- El payload cifrado se elimina antes, cuando ya no puede necesitarse para un envío seguro. El destino en claro sigue la misma retención de 90 días que el resto de la solicitud; no requiere un tratamiento propio de huella ni de rotación de clave, porque no está cifrado.
- La supresión de destinos vive en Brevo, no en esta aplicación; su plazo, base jurídica y tratamiento ante derechos son responsabilidad del proveedor como encargado del tratamiento, sujetos a la misma revisión de DPA que el resto de su servicio.
- Restaurar una copia reaplica purgas y recupera el estado desde la última proyección persistida; la pausa global no sobrevive a un reinicio por diseño, así que no hay nada que reaplicar en ese punto.

La revisión de Brevo debe confirmar DPA, subencargados, regiones, transferencias, retención y eliminación. Tener recursos europeos no demuestra por sí solo cumplimiento.

## Observabilidad

Métricas agregadas por tipo y prioridad:

- profundidad y antigüedad de cola;
- reclamaciones, leases recuperados e intentos;
- elegibilidad, omisiones y retrasos locales;
- aceptación, entrega, fallo definitivo (incluido `destino-suprimido`) y resultado desconocido;
- respuestas temporales, permanentes y globales;
- eventos de webhook recibidos, duplicados, desordenados y huérfanos (sin persistir estos últimos, ver *Recepción de eventos*);
- latencias de cola, proveedor, reconciliación y procesamiento de eventos.

Alertas inmediatas:

- pausa global (circuit breaker en memoria) o fallo de credenciales, dominio o cuenta;
- webhook sin autenticación repetido o aumento de rechazos de origen;
- crecimiento sostenido o antigüedad excesiva por prioridad;
- resultados desconocidos fuera de ventana;
- tasa de `destino-suprimido` anómala, como indicio de un problema de reputación en Brevo;
- fallos de cifrado, descifrado o eliminación del payload;
- retención vencida.

Los umbrales, canales, responsable y runbooks continúan bloqueando producción conforme a `ADR-0016`.

## Paquetes previstos

```text
com.vgrunning.notificationdelivery/
  api/
    command/
    eligibility/
  application/
    service/
    port/out/
  domain/
    request/
    delivery/
  infrastructure/input/web/
  infrastructure/input/scheduling/
  infrastructure/output/persistence/jooq/
  infrastructure/output/brevo/
  infrastructure/output/crypto/
```

`domain/suppression/` desaparece (la supresión la resuelve Brevo). `infrastructure/input/command/` desaparece con las operaciones excepcionales: no queda ningún comando propio que ejecutar fuera de HTTP.

El dominio no depende de Spring, OpenAPI, jOOQ, JDBC, Brevo o Key Vault. Spring Modulith y ArchUnit impedirán SQL cruzado, dependencias inversas y exposición de paquetes internos.

## Validación prevista

### Creación, contenido y prioridad

- Probar atomicidad y rollback entre cada origen y su solicitud.
- Repetir la misma clave lógica con contenido igual y distinto.
- Probar el catálogo cerrado, prioridad de acceso, FIFO y orden plan-destinatario-versión.
- Verificar saludo genérico, castellano, HTML y texto plano de todas las plantillas.
- Comprobar remitente y `Reply-To` configurados y ausencia de nombres, adjuntos y seguimiento.
- Probar cifrado del payload, arranque sin clave, descifrado en memoria y eliminación temprana; comprobar que el destino se persiste y se lee en claro.

### Worker, fallos y proveedor

- Ejecutar varios workers y comprobar distribución con `SKIP LOCKED`, lease y protección frente a worker obsoleto.
- Detener el proceso antes, durante y después de Brevo y recuperar sin crear otra solicitud lógica.
- Probar vigencia y reemplazo de cada generación de identidad.
- Probar la única resolución previa al primer intento, fijación atómica del correo vigente, `omitido-inactivo`, `retry-later` y carreras con baja o cambio de correo.
- Probar backoff entre `5` segundos y `5` minutos, cierre a creación `+120` minutos con `elegibilidad-no-resuelta`, alerta, liberación del orden y prohibición de reapertura o envío manual.
- Probar que un resultado incierto reintenta con la misma clave de idempotencia dentro de los 30 minutos y que una pausa global no reinicia el plazo de elegibilidad previo.
- **Validar contra Brevo real (entorno sintético) que `idempotencyKey` suprime el envío físico duplicado en `POST /v3/smtp/email`**, no solo que correlaciona la respuesta; si no lo confirma, activar la política de fallo cerrado descrita en *Reintentos y resultados inciertos*.
- Ejecutar el backoff exponencial único, respetar `Retry-After` dentro de `expires_at` y no enviar secretos caducados.
- Probar resultado incierto y cierre sin reenvío después de `30` minutos.
- Probar que un envío a un destino suprimido en Brevo se clasifica `fallo-definitivo/destino-suprimido` sin comprobación local previa, errores globales, pausa en memoria y ausencia de consumo de `attempt_count` en el fallo global.
- Cargar más de `500` solicitudes de publicación e insertar recuperaciones para verificar prioridad y antigüedad.

### Webhook y proyección

- Validar Bearer, comparación constante, allowlist, rotación, tamaño y rechazo sin PII.
- Simular PostgreSQL caído y comprobar `429`, `Retry-After` y persistencia posterior del reintento.
- Repetir y desordenar todos los eventos; demostrar deduplicación por `provider_event_id` y precedencia monotónica por `status_rank` en una única transacción.
- Probar `deferred`, rebotes, dirección inválida, queja y evento huérfano (verificar que el huérfano no se persiste y sí genera métrica).
- Confirmar que apertura y clic no están configurados ni aceptados como métricas.

### Operación, privacidad y arquitectura

- Probar purga a `90` días mediante `ON DELETE CASCADE` desde `notification_request` y eliminación temprana del payload.
- Revisar telemetría y errores para impedir PII, secretos o cardinalidad personal; comprobar que el destino en claro no aparece en logs, métricas ni Problem Details.
- Ejecutar `ApplicationModules.verify()` y ArchUnit para verificar propiedad y dependencias, incluida la ausencia de `domain/suppression/`.
- Crear y revisar OpenAPI, ejecutar Spectral, generación de contrato, MockMvc y `oasdiff`.

## Alternativas descartadas

- **Enviar dentro de la transacción de negocio:** se descarta porque Brevo no participa en el commit y una caída produciría estado parcial o una transacción bloqueada por red.
- **Broker externo:** se descarta porque PostgreSQL y leases cubren la escala sin otra infraestructura, credenciales y recuperación.
- **FIFO único sin prioridad:** se descarta porque una publicación masiva podría retrasar recuperación y verificación de acceso.
- **Envío batch a varios destinatarios:** se descarta porque mezcla aceptación, error, idempotencia y reintentos parciales sin una necesidad de volumen medida.
- **Inbox persistida y proyección en un procesador asíncrono separado:** era el diseño original; se descarta desde `ADR-0031` porque la proyección directa en una única transacción corta (deduplicar e insertar `notification_attempt`, actualizar `notification_request` con precedencia monotónica) no es más costosa que persistir el sobre en una tabla intermedia, y evita mantener dos tablas y su reconciliación mutua.
- **Autenticar solo por IP:** se descarta porque los rangos pueden cambiar o compartirse; la posesión del Bearer es obligatoria.
- **Ruta con nombre de Brevo:** se descarta para no fijar el proveedor en la URL, aunque se reconoce que el payload y la seguridad externos siguen acoplados.
- **Endpoint o interfaz operativa:** se descartan porque amplían la superficie de ataque y convierten una recuperación excepcional en capacidad ordinaria.
- **Saludo personalizado:** se descarta porque no cambia la acción que debe realizar la persona y obliga a conservar más datos personales.
- **Plantillas multilingües desde el inicio:** se descartan porque no existe preferencia de idioma ni necesidad validada; la versión deja preparada una ampliación posterior.
- **Remitente `no-reply`:** se descarta porque el club ha decidido permitir respuestas mediante un único buzón, aceptando expresamente la obligación de monitorizarlo.
- **Supresión propia con huella HMAC versionada:** se descarta desde `ADR-0031`. Brevo ya bloquea automáticamente los destinos con rebote duro, queja o desuscripción; mantener una lista local duplica esa capacidad y añade una clave rotable que, según el propio diseño anterior, no podía retirarse mientras existieran supresiones calculadas con ella.
- **Cuatro calendarios de reintento diferenciados por tipo:** se descartan desde `ADR-0031` en favor de un backoff exponencial único con `expires_at`/`max_attempts` por tipo; ningún `RF` ni `CA` depende del desplazamiento exacto en minutos.
- **Ventana de reconciliación con búsqueda activa por referencia:** se descarta desde `ADR-0031` en favor de reintentar con la misma clave de idempotencia dentro de la ventana de 30 minutos, apoyándose en la deduplicación nativa de Brevo en lugar de reimplementarla.
- **Pausa global persistida en PostgreSQL:** se descarta desde `ADR-0031` para un único nodo, con el mismo criterio que `ADR-0025` aplicó a Bucket4j; un circuit breaker en memoria preserva la garantía de no consumir intentos individuales sin coordinar instancias que no existen.
- **Cifrado AEAD simétrico de destino y payload:** se descarta desde `ADR-0031`. Cifrar el destino no protege nada que `identity_access.account_email` no exponga ya en claro en la misma base; el payload sí se conserva cifrado porque es el único lugar del sistema con un secreto portador vigente.

## Cambios de alcance y riesgos aceptados

Este diseño no añade eventos de producto. Concreta la infraestructura y operación ya exigidas por `ADR-0011`, añade prioridad entre los tipos aprobados, materializa la elegibilidad de `ADR-0021` y delega en el proveedor de correo la supresión de destinos y la reconciliación de resultados inciertos (`ADR-0031`).

Riesgos aceptados:

- la entrega física puede duplicarse en casos límite y nunca se promete exactamente una vez;
- un correo en vuelo puede llegar después de una baja, reemplazo o cambio operativo;
- un cambio de correo después de fijar el destino no altera la solicitud actual; solo las solicitudes futuras usan el nuevo;
- priorizar acceso puede retrasar publicaciones bajo una carga anómala; se observa antes de añadir cuotas;
- **la evidencia de 90 días de eventos huérfanos se pierde**: al no persistirse, un evento sin solicitud correlacionable solo deja métrica y log, no una fila auditable;
- **la lista de supresión deja de ser propia**: una migración de proveedor arrastra la reputación acumulada en Brevo, mitigable exportándola con `GET /v3/smtp/blockedContacts` antes de migrar;
- **la pausa global no sobrevive a un reinicio del proceso** — sí sobrevive la garantía de no consumir intentos individuales durante el fallo; escalar a varias réplicas exige una decisión previa, igual que Bucket4j en `ADR-0025`;
- **los desplazamientos de reintento dejan de reproducir al minuto** los del diseño anterior; ningún `RF`/`CA` los exige con esa precisión;
- aceptar respuestas obliga al club a atender un buzón único;
- Brevo y el dominio introducen obligaciones operativas y de privacidad que impiden usar datos reales todavía;
- la ruta neutral del webhook no evita que una sustitución de proveedor cambie su esquema de entrada, seguridad y configuración;
- un resultado desconocido se cierra sin reenviar después del TTL, aunque eso pueda omitir un correo, para no aceptar un duplicado tardío.

## Conclusiones

- La outbox conserva atomicidad y recuperabilidad sin broker ni transacción distribuida, con 2 tablas en lugar de 7.
- La prioridad protege acceso y el orden conserva coherencia entre versiones de publicación.
- Tras fijar su destino, la solicitud permanece autocontenida; el payload sigue cifrado, el destino se conserva en claro porque ya lo está en `identity_access`.
- Una proyección monotónica por `status_rank`, en una sola transacción con la ingesta del webhook, separa recepción fiable de eventos y estado técnico sin tabla de inbox intermedia.
- Supresión, reconciliación y pausa se apoyan en capacidades del proveedor y del proceso en memoria en lugar de reimplementarlas, con las mismas garantías de negocio que exigían `ADR-0008`, `ADR-0011` y `ADR-0021`.
- El contrato HTTP se limita al evento entrante neutral; los estados permanecen internos.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro del diseño detallado de `notification-delivery`.

Antes de implementar deben producirse OpenAPI, la migración Flyway `V005__evolve_notification_request_outbox.sql` (evoluciona la tabla ya creada en `V004`, ver `ADR-0031`), tipos jOOQ, plantillas versionadas, configuración de Brevo sintética, la prueba sintética de `idempotencyKey` descrita en *Supuestos e incertidumbres* y pruebas de integración con PostgreSQL.

Bloqueantes para datos reales y producción:

| Bloqueante | Responsable | Tratamiento exigido |
| --- | --- | --- |
| Dominio, remitente y `Reply-To` monitorizado | Propietario del servicio | Adquirir y autenticar el dominio, crear el buzón, verificar su atención y probar entregabilidad. |
| Brevo como encargado | Responsable del tratamiento con Revisor de privacidad o DPO | Aprobar DPA, subencargados, regiones, transferencias, retención, eliminación y base aplicable antes de enviar datos reales — incluida la lista de supresión que Brevo mantiene como encargado. |
| Seguridad del webhook | Persona operadora y Revisor de arquitectura | Configurar y probar Bearer, allowlist, rotación, límites y secretos en un entorno controlado. |
| Operación y alertas | Persona operadora | Fijar umbrales, destinos y probar runbooks de pausa (reinicio del proceso), incidente y consulta de supresión en Brevo. |
| Evidencia técnica final | Revisor de arquitectura | Ejecutar pruebas de entregabilidad, volumen, restauración, retención, ausencia de datos personales en telemetría y la validación de `idempotencyKey` en envío individual. |

Estos bloqueantes no autorizan a sustituir Brevo, omitir seguridad o resolver operación mediante SQL manual durante la implementación. El bloqueante de «Huella de supresión» del diseño anterior desaparece: la supresión ya no es responsabilidad de esta aplicación.
