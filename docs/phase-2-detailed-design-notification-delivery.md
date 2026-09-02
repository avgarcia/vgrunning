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

Si este documento contradice una fuente aceptada, prevalece el ADR y deberá corregirse el diseño antes de implementar.

## Alcance

Incluye:

- API Java transaccional para crear y reemplazar solicitudes;
- outbox, prioridades, orden, leases y recuperación de trabajo;
- plantillas versionadas, renderizado HTML y texto plano y adaptador REST de Brevo;
- reconciliación de resultados inciertos dentro de la ventana idempotente;
- recepción persistente y procesamiento asíncrono de eventos de Brevo;
- supresión global de destinos y pausa por fallos globales;
- comandos excepcionales auditados para reanudar el worker o reactivar un destino;
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
4. La aceptación REST y el webhook son fuentes distintas y pueden llegar desordenadas; una inbox persistente separa la recepción fiable de la proyección de estado.
5. Correlacionar por identificadores opacos evita depender del correo incluido por Brevo y reduce datos personales en eventos, logs y métricas.
6. La operación excepcional necesita el mismo control y auditoría que la aplicación. SQL manual o un endpoint oculto serían caminos alternativos sin invariantes.
7. Un saludo genérico elimina datos personales sin reducir la utilidad de ninguno de los correos aprobados.

## Decisiones confirmadas

- La cola prioriza acceso, mantiene FIFO dentro de cada prioridad y conserva el orden de versiones de un mismo plan y destinatario.
- Las operaciones excepcionales se ejecutan como comandos de una sola ejecución con la misma imagen de la aplicación; no existe endpoint ni interfaz.
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
| Supresión | Una huella seudónima del destino puede necesitar más vida que el detalle ordinario de la solicitud para impedir nuevos envíos dañinos. | Media | Conservar solo huella y causa mínima; la política y base aplicable deben aprobarse antes de datos reales. |
| Entrega física | Brevo y el servidor receptor pueden aceptar un mensaje que la persona no llegue a leer. | Alta | `entregado` significa recepción por servidor, nunca lectura. |

## Lenguaje ubicuo

| Término | Significado |
| --- | --- |
| Solicitud de notificación | Registro lógico inmutable creado por un módulo de negocio y procesado de forma asíncrona. |
| Intento | Una evaluación de elegibilidad y, cuando procede, una llamada concreta al proveedor. |
| Aceptación del proveedor | Evidencia de que Brevo recibió la petición y asume la entrega posterior. |
| Resultado final | Última proyección técnica conocida: entregado, fallo definitivo u omitido por inactividad. |
| Inbox de entrega | Sobre mínimo persistido antes de confirmar un evento entrante. |
| Supresión | Bloqueo global de futuros envíos a un destino por dirección inválida, rebote duro o queja. |
| Pausa global | Estado operativo que impide reclamar nuevos envíos por una incidencia de configuración o cuenta del proveedor. |
| Reconciliación | Consulta y correlación que resuelve si Brevo aceptó un intento cuya respuesta se perdió. |
| Destino fijado | Dirección concreta conservada por la solicitud. En identidad se fija al crearla; en publicación, al resolver por primera vez `active(currentVerifiedEmail)`. No se vuelve a resolver en un reintento. |
| Destino elegible | Destino que supera supresión, vigencia y, para publicaciones, actividad actual antes del intento. |

En código, OpenAPI y persistencia se usarán `notification request`, `delivery attempt`, `delivery event inbox`, `suppressed destination` y `delivery control`. No se usará `email job` como concepto de dominio ni se expondrán nombres de Brevo fuera del adaptador.

## Límite modular

`notification-delivery` gobierna:

- solicitudes, intentos, leases, prioridad y orden;
- cifrado de destino y payload persistido;
- selección y renderizado de plantillas;
- comunicación con Brevo, reconciliación y normalización de resultados;
- inbox de eventos, proyección de estado, supresiones y pausa global;
- retención técnica, telemetría y comandos operativos.

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
- destino cifrado y huella HMAC de su forma canónica, obligatorios desde la creación para identidad y todavía ausentes al crear una solicitud de publicación;
- identificador y versión de plantilla, idioma `es` y payload cifrado;
- UUID idempotente estable y etiqueta opaca estable para Brevo;
- estado, motivo normalizado, contador de intentos y próximo instante;
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

Todas las tablas pertenecen al esquema `notification_delivery`.

| Tabla | Datos e invariantes principales |
| --- | --- |
| `notification_request` | Solicitud, origen, prioridad, orden, plantilla, sobres cifrados, huella, idempotencia, correlación, estado, programación, lease y resultado. Unicidad de clave lógica, UUID idempotente y etiqueta opaca. |
| `notification_attempt` | Solicitud, número, instante, etapa, resultado normalizado, código seguro, latencia y correlación. No conserva cuerpos ni cabeceras sensibles. |
| `delivery_event_inbox` | Proveedor lógico, clave deduplicada, identificador de mensaje, etiqueta opaca, tipo, instante del proveedor, huella canónica y estado de procesamiento. No conserva el payload original. |
| `delivery_transition` | Solicitud, estado anterior y nuevo, motivo, origen de la transición, instante y correlación. |
| `suppressed_destination` | Huella HMAC versionada, causa, origen, creación, reactivación y auditoría; no conserva la dirección en claro. Solo una supresión activa por huella. |
| `delivery_control` | Única fila de pausa global, causa, instante, correlación y revisión. |
| `delivery_operation_audit` | Operador, acción, motivo, objetivo opaco, resultado, instante y correlación. |

Los sobres cifrados usan AEAD con versión de clave y nonce. Las claves de cifrado y HMAC proceden de Key Vault, se cargan al arrancar y conservan versiones anteriores mientras existan filas que las necesiten. El arranque falla de forma segura si falta una versión referenciada.

## Creación transaccional e inmutabilidad

La API Java de creación participa en la transacción PostgreSQL del productor. Para identidad:

1. valida tipo, plantilla, clave lógica y payload cerrado;
2. normaliza el destino en memoria y calcula su huella;
3. cifra destino y payload localmente, sin llamar a Key Vault ni a Brevo;
4. crea una solicitud con idempotencia y correlación estables;
5. devuelve su UUID sin iniciar el envío.

Para publicación valida clave lógica, miembro, orden, plantilla y payload, cifra el payload y crea la solicitud sin destino ni huella. El primer resultado `eligible(currentVerifiedEmail)` fija ambos atómicamente antes de renderizar o transmitir. Si dos workers compiten, el lease y la precondición de destino ausente permiten una sola fijación.

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
2. Para una publicación sin destino se invoca `DeliveryEligibilityPolicy`; `eligible(currentVerifiedEmail)` fija cifrado y huella en una transacción corta protegida por el lease.
3. Fuera de transacción se descifran destino y payload.
4. Se comprueba que el destino no está suprimido y que cualquier secreto sigue vigente.
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

Los desplazamientos se calculan desde la creación de la solicitud:

| Tipo | Intentos máximos |
| --- | --- |
| Recuperación | Inmediato, `+1`, `+5`, `+15` y `+30` minutos. |
| Invitación, activación o reactivación | Inmediato, `+1`, `+5`, `+30` minutos, `+2`, `+8` y `+24` horas. |
| Verificación o aviso de cambio de correo | Usa la planificación de recuperación mientras el desafío siga vigente; el aviso sin secreto conserva la misma ventana. |
| Publicación o republicación | Inmediato, `+1`, `+5`, `+15`, `+30`, `+60` y `+120` minutos. |

`Retry-After` se respeta cuando no excede el siguiente límite útil. Nunca se envía un enlace después de caducar su desafío.

Timeout, `408`, `5xx` o pérdida de conexión después de transmitir producen `resultado-incierto`. No se ejecuta otro envío ordinario. Se reconcilia a `+1`, `+5`, `+15` y `+25` minutos usando la misma clave y buscando por referencia o etiqueta opaca:

- una clave duplicada, respuesta correlacionada o evento asociado confirma `aceptado-proveedor`;
- evidencia explícita de no aceptación permite continuar la planificación ordinaria;
- ausencia de evidencia al finalizar los `30` minutos termina como `fallo-definitivo/resultado-desconocido` y alerta;
- después de la ventana idempotente no se reenvía, porque hacerlo aceptaría un duplicado físico no controlado.

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

Tras autenticar, una transacción extrae y persiste exclusivamente referencia de mensaje, etiqueta opaca, tipo normalizado, instante del proveedor y una huella SHA-256 del sobre canónico necesario. Descarta correo, asunto, enlaces de previsualización, contenido y payload original. La clave de deduplicación usa identificador de proveedor cuando sea fiable y, como respaldo, la combinación canónica de mensaje, etiqueta, tipo e instante.

La respuesta `204` se emite después del commit. Un procesador asíncrono correlaciona primero por referencia de mensaje y después por etiqueta opaca; nunca usa el correo como clave. Un evento que llega antes de persistir la respuesta REST espera en inbox y se reintenta localmente.

Eventos soportados: aceptación, entrega, diferido, rebote blando, rebote duro, dirección inválida, bloqueo, error y queja. Aperturas y clics permanecen deshabilitados.

- `deferred` no provoca otro envío desde la aplicación;
- `soft bounce` cierra solo la solicitud cuando Brevo agota su ventana y no suprime el destino;
- rebote duro, dirección inválida o queja cierran y suprimen globalmente;
- duplicados, desorden y eventos atrasados no hacen retroceder la proyección;
- un evento sin solicitud correlacionable se conserva en la inbox mínima durante `90` días desde su recepción y genera métrica, no un envío.

## Supresiones y pausa global

Antes de cada llamada se consulta la huella del destino. Una supresión activa termina la solicitud como `fallo-definitivo/destino-suprimido` sin contactar con Brevo. La supresión aplica a todos los tipos y no se elimina al reactivar una cuenta o confirmar otra publicación.

Una dirección diferente y verificada produce otra huella y puede recibir mensajes. Reactivar el mismo destino requiere el comando operativo autorizado; no reabre solicitudes terminales ni genera correo.

La pausa global se persiste en PostgreSQL para que todas las instancias dejen de reclamar trabajo. La solicitud en curso registra el fallo global, vuelve a estado recuperable sin consumir intento y libera su lease. La pausa genera alerta inmediata y solo se levanta después de corregir la causa mediante operación auditada.

## Operaciones excepcionales

Las únicas mutaciones operativas son:

- reanudar el worker tras una pausa global corregida;
- reactivar un destino suprimido después de corregir y comprobar la causa.

Se ejecutan como proceso efímero con la misma imagen y los casos de uso de aplicación. Exigen identidad individual de operador, motivo no vacío y UUID de correlación. La dirección que deba reactivarse se lee por entrada protegida o referencia de secreto, nunca como argumento visible del proceso.

La auditoría se confirma antes de modificar el control o la supresión; si no puede persistirse, la operación falla cerrada. No se admite SQL directo, endpoint HTTP, interfaz, reintento de solicitudes terminales ni creación de una solicitud nueva. El comando devuelve solo resultado normalizado e identificadores opacos.

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
- La inbox persiste y deduplica en una transacción distinta de su procesamiento.
- Procesar un evento bloquea la solicitud y aplica una transición monotónica; inbox y transición confirman juntas.
- Pausa, supresión y operación auditada usan revisión o bloqueo para evitar pérdida de actualizaciones.
- No hay transacciones distribuidas ni compensación contra Brevo.

## Consultas e índices

Índices candidatos:

- parcial por `priority`, `next_attempt_at`, `created_at` e ID para solicitudes `pendiente`;
- por `lease_until` para recuperación;
- único por clave lógica, idempotencia y etiqueta opaca;
- por clave de orden y número de versión;
- por referencia de mensaje del proveedor;
- único por clave deduplicada de inbox y parcial por estado de procesamiento;
- único parcial por huella de destino con supresión activa;
- por `terminal_at` para retención;
- por origen y generación para reemplazo de secretos.

Se confirmarán con datos superiores a `500` destinatarios y `EXPLAIN (ANALYZE, BUFFERS)`. No se indexan cuerpos, dirección cifrada ni payload.

## Seguridad, privacidad y retención

- Destino y payload se cifran en reposo; secretos y enlaces solo existen descifrados durante renderizado y envío.
- Solo la identidad de ejecución del worker puede solicitar las claves necesarias. Los roles del producto y los comandos operativos no pueden consultar destino, payload o contenido descifrado.
- La huella HMAC no se usa en logs, métricas o respuestas y su clave se rota de forma controlada.
- Una versión de clave HMAC no puede retirarse mientras existan supresiones calculadas con ella, porque no se conserva la dirección necesaria para recalcularlas. Una exposición de esa clave exige decidir entre conservar la protección de supresión o invalidar y reconstruir sus huellas con evidencia externa; el runbook y la revisión de privacidad deben aceptar ese límite antes de producción.
- Logs, trazas, métricas, eventos y Problem Details excluyen dirección completa, nombre, contenido, secreto, enlace, cabeceras de autenticación y respuesta completa de Brevo.
- Las pruebas y entornos no productivos usan exclusivamente datos sintéticos o anonimizados.
- Solicitudes, intentos, inbox y transiciones se eliminan a los `90` días desde la última transición técnica final. Una transición posterior válida reinicia ese plazo.
- El payload cifrado se elimina antes, cuando ya no puede necesitarse para un envío seguro.
- La supresión conserva solo huella, versión de clave, causa mínima y auditoría mientras siga siendo necesaria para impedir nuevos envíos. Su plazo, base y tratamiento ante derechos requieren aprobación de privacidad antes de datos reales; no autoriza conservar la dirección completa indefinidamente.
- Restaurar una copia reaplica purgas, supresiones y pausa global antes de reabrir el servicio.

La revisión de Brevo debe confirmar DPA, subencargados, regiones, transferencias, retención y eliminación. Tener recursos europeos no demuestra por sí solo cumplimiento.

## Observabilidad

Métricas agregadas por tipo y prioridad:

- profundidad y antigüedad de cola;
- reclamaciones, leases recuperados e intentos;
- elegibilidad, omisiones y retrasos locales;
- aceptación, entrega, fallo definitivo y resultado desconocido;
- respuestas temporales, permanentes y globales;
- inbox recibida, duplicada, desordenada, huérfana y retrasada;
- supresiones y reactivaciones sin identificar destinos;
- latencias de cola, proveedor, reconciliación y procesamiento de eventos.

Alertas inmediatas:

- pausa global o fallo de credenciales, dominio o cuenta;
- webhook sin autenticación repetido o aumento de rechazos de origen;
- crecimiento sostenido o antigüedad excesiva por prioridad;
- resultados desconocidos fuera de ventana;
- inbox persistida sin procesar;
- fallos de cifrado, descifrado, rotación o eliminación;
- retención vencida o restauración sin reaplicar supresiones.

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
    suppression/
  infrastructure/input/web/
  infrastructure/input/scheduling/
  infrastructure/input/command/
  infrastructure/output/persistence/jooq/
  infrastructure/output/brevo/
  infrastructure/output/crypto/
```

El dominio no depende de Spring, OpenAPI, jOOQ, JDBC, Brevo o Key Vault. Spring Modulith y ArchUnit impedirán SQL cruzado, dependencias inversas y exposición de paquetes internos.

## Validación prevista

### Creación, contenido y prioridad

- Probar atomicidad y rollback entre cada origen y su solicitud.
- Repetir la misma clave lógica con contenido igual y distinto.
- Probar el catálogo cerrado, prioridad de acceso, FIFO y orden plan-destinatario-versión.
- Verificar saludo genérico, castellano, HTML y texto plano de todas las plantillas.
- Comprobar remitente y `Reply-To` configurados y ausencia de nombres, adjuntos y seguimiento.
- Probar cifrado, rotación, arranque sin clave, descifrado en memoria y eliminación temprana.

### Worker, fallos y proveedor

- Ejecutar varios workers y comprobar distribución con `SKIP LOCKED`, lease y protección frente a worker obsoleto.
- Detener el proceso antes, durante y después de Brevo y recuperar sin crear otra solicitud lógica.
- Probar vigencia y reemplazo de cada generación de identidad.
- Probar la única resolución previa al primer intento, fijación atómica del correo vigente, `omitido-inactivo`, `retry-later` y carreras con baja o cambio de correo.
- Probar backoff entre `5` segundos y `5` minutos, cierre a creación `+120` minutos con `elegibilidad-no-resuelta`, alerta, liberación del orden y prohibición de reapertura o envío manual.
- Probar que una reconciliación ya iniciada puede terminar después del límite sin nuevo envío y que una pausa global no reinicia el plazo previo.
- Ejecutar cada calendario de reintentos, respetar `Retry-After` y no enviar secretos caducados.
- Probar resultado incierto, reconciliación, clave duplicada y cierre sin reenvío después de `30` minutos.
- Probar errores individuales, supresión, errores globales, pausa y ausencia de consumo masivo de intentos.
- Cargar más de `500` solicitudes de publicación e insertar recuperaciones para verificar prioridad y antigüedad.

### Webhook y proyección

- Validar Bearer, comparación constante, allowlist, rotación, tamaño y rechazo sin PII.
- Simular PostgreSQL caído y comprobar `429`, `Retry-After` y persistencia posterior del reintento.
- Repetir y desordenar todos los eventos; demostrar deduplicación y precedencia monotónica.
- Enviar un evento antes de persistir la respuesta REST y correlacionarlo después por etiqueta.
- Probar `deferred`, rebotes, dirección inválida, queja, supresión y evento huérfano.
- Confirmar que apertura y clic no están configurados ni aceptados como métricas.

### Operación, privacidad y arquitectura

- Ejecutar comandos con operador, motivo y correlación; fallar si no puede auditarse.
- Comprobar que reanudar conserva solicitudes e idempotencia y que reactivar un destino no reenvía históricos.
- Verificar ausencia de endpoint, interfaz y SQL directo para operaciones.
- Probar purga a `90` días, eliminación temprana de contenido y reaplicación tras restaurar copia.
- Revisar telemetría y errores para impedir PII, secretos o cardinalidad personal.
- Ejecutar `ApplicationModules.verify()` y ArchUnit para verificar propiedad y dependencias.
- Crear y revisar OpenAPI, ejecutar Spectral, generación de contrato, MockMvc, Schemathesis y `oasdiff`.

## Alternativas descartadas

- **Enviar dentro de la transacción de negocio:** se descarta porque Brevo no participa en el commit y una caída produciría estado parcial o una transacción bloqueada por red.
- **Broker externo:** se descarta porque PostgreSQL y leases cubren la escala sin otra infraestructura, credenciales y recuperación.
- **FIFO único sin prioridad:** se descarta porque una publicación masiva podría retrasar recuperación y verificación de acceso.
- **Envío batch a varios destinatarios:** se descarta porque mezcla aceptación, error, idempotencia y reintentos parciales sin una necesidad de volumen medida.
- **Actualizar la solicitud dentro del webhook antes de responder:** se descarta porque un fallo de negocio o un evento desordenado alargaría la petición y podría perderse por la política de reintentos del proveedor.
- **Autenticar solo por IP:** se descarta porque los rangos pueden cambiar o compartirse; la posesión del Bearer es obligatoria.
- **Ruta con nombre de Brevo:** se descarta para no fijar el proveedor en la URL, aunque se reconoce que el payload y la seguridad externos siguen acoplados.
- **Endpoint o interfaz operativa:** se descartan porque amplían la superficie de ataque y convierten una recuperación excepcional en capacidad ordinaria.
- **Saludo personalizado:** se descarta porque no cambia la acción que debe realizar la persona y obliga a conservar más datos personales.
- **Plantillas multilingües desde el inicio:** se descartan porque no existe preferencia de idioma ni necesidad validada; la versión deja preparada una ampliación posterior.
- **Remitente `no-reply`:** se descarta porque el club ha decidido permitir respuestas mediante un único buzón, aceptando expresamente la obligación de monitorizarlo.

## Cambios de alcance y riesgos aceptados

Este diseño no añade eventos de producto. Concreta la infraestructura y operación ya exigidas por `ADR-0011`, añade prioridad entre los tipos aprobados y materializa la elegibilidad de `ADR-0021`.

Riesgos aceptados:

- la entrega física puede duplicarse en casos límite y nunca se promete exactamente una vez;
- un correo en vuelo puede llegar después de una baja, reemplazo o cambio operativo;
- un cambio de correo después de fijar el destino no altera la solicitud actual; solo las solicitudes futuras usan el nuevo;
- priorizar acceso puede retrasar publicaciones bajo una carga anómala; se observa antes de añadir cuotas;
- una inbox persistente añade almacenamiento y procesamiento, pero evita perder eventos por fallos internos;
- aceptar respuestas obliga al club a atender un buzón único;
- Brevo, el dominio y la supresión introducen obligaciones operativas y de privacidad que impiden usar datos reales todavía;
- la ruta neutral del webhook no evita que una sustitución de proveedor cambie su esquema de entrada, seguridad y configuración;
- un resultado desconocido se cierra sin reenviar después del TTL, aunque eso pueda omitir un correo, para no aceptar un duplicado tardío.

## Conclusiones

- La outbox conserva atomicidad y recuperabilidad sin broker ni transacción distribuida.
- La prioridad protege acceso y el orden conserva coherencia entre versiones de publicación.
- Tras fijar su destino, la solicitud permanece autocontenida y cifrada; ningún reintento reconstruye destino o contenido desde datos mutables.
- La inbox y una proyección monotónica separan recepción fiable de eventos y estado técnico.
- Supresiones, pausa y comandos auditados cubren la operación sin añadir capacidades ocultas al producto.
- El contrato HTTP se limita al evento entrante neutral; los estados y acciones operativas permanecen internos.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro del diseño detallado de `notification-delivery`.

Antes de implementar deben producirse OpenAPI, migraciones Flyway, tipos jOOQ, plantillas versionadas, configuración de Brevo sintética y pruebas de integración con PostgreSQL.

Bloqueantes para datos reales y producción:

| Bloqueante | Responsable | Tratamiento exigido |
| --- | --- | --- |
| Dominio, remitente y `Reply-To` monitorizado | Propietario del servicio | Adquirir y autenticar el dominio, crear el buzón, verificar su atención y probar entregabilidad. |
| Brevo como encargado | Responsable del tratamiento con Revisor de privacidad o DPO | Aprobar DPA, subencargados, regiones, transferencias, retención, eliminación y base aplicable antes de enviar datos reales. |
| Huella de supresión | Responsable del tratamiento con Revisor de privacidad o DPO | Aprobar finalidad, base, plazo, derechos, acceso restringido y tratamiento de claves antiguas; si no es defendible, rediseñar la supresión antes de producción. |
| Seguridad del webhook | Persona operadora y Revisor de arquitectura | Configurar y probar Bearer, allowlist, rotación, límites y secretos en un entorno controlado. |
| Operación y alertas | Persona operadora | Fijar umbrales, destinos y probar runbooks de pausa, reanudación, supresión, rotación e incidente. |
| Evidencia técnica final | Revisor de arquitectura | Ejecutar pruebas de entregabilidad, volumen, restauración, retención y ausencia de datos personales en telemetría. |

Estos bloqueantes no autorizan a sustituir Brevo, omitir seguridad o resolver operación mediante SQL manual durante la implementación.
