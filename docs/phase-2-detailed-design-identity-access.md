# Diseño detallado de identidad y acceso - Fase 2

**Estado:** Propuesto
**Fecha:** 2026-08-14
**Responsable de revisión:** Revisor de arquitectura

## Propósito

Definir el comportamiento, los límites modulares, el modelo de datos, las transacciones, la API y las pruebas de identidad y acceso antes de crear el contrato OpenAPI y comenzar la implementación.

Este diseño cubre completamente `RF-01` y la parte de cuentas y roles de `RF-02`. También concreta la autenticación y autorización necesarias para `RF-16`, `RF-18` y `RF-19`. No diseña el perfil operativo del corredor, las taxonomías ni su asignación: esos conceptos pertenecen respectivamente a `runner-management` y `classification-segmentation` y necesitan diseños detallados propios.

## Fuentes normativas

- [Requisitos de Fase 1](phase-1-requirements.md) y [criterios de aceptación](phase-1-acceptance-criteria.md).
- [Línea base de seguridad de acceso](phase-2-access-security-baseline.md).
- `ADR-0003`: identidad local, invitación, activación, recuperación, sesiones opacas y primer administrador.
- `ADR-0004`: roles inmutables, capacidades y aislamiento del corredor.
- `ADR-0010`: privacidad, conservación, ejercicio de derechos y mayoría de edad.
- `ADR-0011`: entrega asíncrona de correo e invalidación de solicitudes reemplazadas.
- `ADR-0012`: PostgreSQL, bloqueos, concurrencia y transacciones.
- `ADR-0013`: Spring MVC, jOOQ/JDBC, Flyway, OpenAPI contract-first y controles de calidad.
- `ADR-0014`: módulos, esquemas, APIs Java y arquitectura hexagonal.
- `ADR-0015`: aplicación de autorización y `ActorContext` explícito.
- `ADR-0016`: secretos, observabilidad, copias y operación en Azure.

Si este documento contradice una fuente aceptada, prevalece el ADR o la línea base y deberá corregirse el diseño antes de implementar.

## Razonamiento de diseño

1. La cuenta es una identidad de acceso, no el perfil del corredor. Mezclarlas impediría conservar responsabilidades, retenciones y ciclos de vida distintos.
2. El rol se fija al crear la cuenta y no cambia. Así se materializa la decisión funcional y se evita convertir una reasignación de rol en una migración implícita de permisos e historial.
3. Las operaciones que emiten un enlace y su solicitud de correo deben confirmarse en la misma transacción. Una invitación sin notificación persistida, o un correo sobre un secreto no confirmado, serían estados incoherentes.
4. Los secretos y sesiones se validan mediante verificadores, nunca mediante valores en claro persistidos. El dato imprescindible para renderizar un enlace asíncrono se cifra para el worker y se elimina al finalizar o caducar.
5. La autorización se decide en aplicación con el actor explícito y el recurso cargado. Las reglas no se confían al frontend, a filtros genéricos ni a consultas accidentales.
6. El PMV prioriza reglas verificables sobre funciones defensivas no justificadas: no incluye MFA, reautenticación adicional ni límite de sesiones simultáneas, y conserva esas mejoras en un backlog explícito.

## Supuestos e incertidumbres

| Elemento | Supuesto o incertidumbre | Confianza | Tratamiento |
| --- | --- | --- | --- |
| Escala | Un solo club, más de `500` corredores y picos iniciales inferiores a `100` usuarios concurrentes. | Alta | PostgreSQL cubre sesiones y límites; no se introduce Redis. |
| Correo | Brevo puede recibir desde el worker el contenido mínimo descifrado y no necesita acceder a secretos persistidos. | Alta | Adaptador aislado, payload cifrado en reposo y revisión de privacidad antes de datos reales. |
| Cambio de correo | El correo anterior puede no ser entregable cuando se confirme el nuevo. | Alta | La verificación del nuevo es obligatoria; la notificación al anterior se intenta cuando sea posible, pero no bloquea el cambio. |
| Dirección IP | La IP ayuda a limitar abuso, pero es dato personal y puede estar compartida o cambiar. | Alta | Se usa un identificador HMAC rotatorio para contadores y se evita conservar la IP completa en eventos ordinarios. |
| Eliminación legal | La supresión puede impedir reactivar la misma identidad y puede requerir conservar evidencia mínima. | Media | El flujo de privacidad manda; una cuenta ya suprimida no se reactiva y se crea una identidad nueva cuando proceda. |
| UUID | No existe requisito de orden temporal de identificadores de cuenta o sesión. | Alta | Se usa UUID aleatorio; cualquier cambio de estrategia requiere evidencia de rendimiento, no otro significado de negocio. |

## Límite del módulo

`identity-access` es propietario de:

- cuenta, correo canónico, rol inmutable y estado de acceso;
- hash de contraseña y política de credenciales;
- desafíos de activación, reactivación, recuperación y cambio de correo;
- sesiones opacas y su revocación;
- declaraciones de mayoría de edad ligadas a la identidad;
- eventos de seguridad y contadores de abuso;
- bootstrap y recuperación operativa de la única cuenta administradora.

`runner-management` es propietario del perfil del corredor y de su vínculo con una cuenta. Para dar de alta a un corredor, ese módulo coordina la creación del perfil y consume la API Java publicada por `identity-access` dentro de la misma transacción. `identity-access` no consulta el esquema de corredores y no crea perfiles.

`identity-access` consume la API publicada por `notification-delivery` para crear solicitudes autocontenidas. No accede a sus tablas ni llama al proveedor de correo dentro de una transacción de negocio.

## Lenguaje y modelo de estado

### Cuenta

Una `Cuenta` identifica a una persona que puede autenticarse. Tiene un único `Rol` entre `administrador`, `entrenador` y `corredor`; el rol es obligatorio e inmutable durante toda la vida de esa cuenta.

Los estados son:

| Estado | Significado | Entradas permitidas | Salidas permitidas |
| --- | --- | --- | --- |
| `pending_activation` | Invitación inicial vigente o renovable; todavía no existe acceso. | Creación administrativa o bootstrap. | `active`, `cancelled`. |
| `active` | Cuenta autenticable, sujeta a credenciales y autorización. | Activación o reactivación completada. | `disabled`. |
| `disabled` | Acceso bloqueado administrativamente; los datos retenidos aún existen. | Desactivación de una cuenta activa. | `pending_reactivation`. |
| `pending_reactivation` | Reactivación autorizada pendiente de nueva contraseña. | Reactivación administrativa de una cuenta desactivada. | `active`, `disabled`. |
| `cancelled` | Invitación no activada cancelada; nunca fue una cuenta activa. | Cancelación de `pending_activation`. | Ninguna; una nueva invitación crea otra cuenta. |

No existe transición de rol. Una invitación con rol incorrecto se cancela y se crea otra cuenta. Una cuenta activa con otro rol no puede reactivarse ni convertirse: se necesita una identidad distinta y un correo disponible.

Una cuenta cuyos datos ya se hayan suprimido por privacidad no se reactiva. Si vuelve a necesitar acceso, se crea una nueva identidad y se aplican otra vez las declaraciones y el flujo de alta.

### Correo

El correo introducido se normaliza en Unicode NFC, se eliminan espacios exteriores y se obtiene una forma canónica en minúsculas para comparar. Se conserva por separado la forma de presentación confirmada. No se aplican reglas específicas de proveedores como eliminar puntos o sufijos `+`.

La forma canónica debe ser única entre correos actuales y cambios pendientes. La reserva se realiza bajo una restricción única parcial de PostgreSQL; una comprobación previa solo mejora el mensaje y no decide la concurrencia. Un correo histórico puede conservarse durante su retención sin mantener la reserva.

- Si una invitación pendiente tiene un correo incorrecto, se cancela y se crea otra invitación.
- Cancelar una invitación libera la reserva para que el mismo correo pueda usarse en una cuenta nueva con el rol correcto. La evidencia histórica conservada no participa en la unicidad.
- Una cuenta activa puede solicitar su cambio y un administrador puede iniciarlo para otra cuenta.
- El correo actual no cambia hasta consumir un desafío enviado al correo nuevo.
- Al confirmar se vuelve a verificar unicidad, se reemplaza el correo, se revocan todas las sesiones y se exige iniciar sesión con el correo nuevo.
- Se solicita una notificación al correo anterior cuando sea posible. Su fallo no revierte un cambio ya confirmado.
- El identificador, rol, declaraciones e historial de la cuenta no cambian.

### Mayoría de edad

No se almacena fecha de nacimiento ni documento identificativo.

- El administrador declara que la persona es mayor de `18` años al crear la invitación.
- La persona invitada realiza una segunda declaración durante la activación inicial.
- Cada declaración conserva cuenta, actor, origen, instante y versión inmutable del texto mostrado.
- El bootstrap registra la declaración del primer administrador como realizada por el operador de despliegue y exige que la persona la confirme al activarse.
- Una reactivación conserva las declaraciones existentes y no las recrea.

La ausencia de cualquiera de las dos declaraciones impide completar una activación inicial. Estas declaraciones no sustituyen las evidencias o controles jurídicos que determine la revisión de privacidad.

## Credenciales y desafíos

Las contraseñas tienen entre `12` y `128` caracteres, se normalizan en NFC y se rechazan si pertenecen a la lista versionada de valores comprometidos o previsibles. Se almacenan con Argon2id y los parámetros mínimos aceptados. El hash conserva parámetros para permitir rehash transparente tras un inicio correcto.

Cada desafío usa `32` bytes del CSPRNG, expone el secreto como `base64url` y persiste solo su verificador SHA-256. Contiene propósito, generación, creación, caducidad, consumo y cuenta. Solo la última generación vigente de cada propósito y cuenta puede consumirse.

| Propósito | Vigencia | Resultado |
| --- | --- | --- |
| `activation` | `72` horas | Registra declaración del invitado, establece contraseña y activa la cuenta. |
| `reactivation` | `72` horas por defecto | Establece contraseña nueva y reactiva la cuenta conservada. |
| `password_recovery` | `1` hora | Sustituye la contraseña y revoca todas las sesiones. |
| `email_change` | `1` hora por defecto | Confirma y sustituye el correo y revoca todas las sesiones. |

La reemisión invalida la generación anterior y cierra como reemplazada su solicitud de correo pendiente en la misma transacción. Un desafío consumido, caducado, reemplazado o de una cuenta incompatible responde con el mismo error público.

Las vigencias nuevas se configuran como duraciones ISO-8601 mediante `pmv.identity.challenge.reactivation-ttl`, con valor predeterminado `PT72H`, y `pmv.identity.challenge.email-change-ttl`, con valor predeterminado `PT1H`. Una propiedad ausente usa su valor predeterminado; la aplicación rechaza al arrancar cualquier valor explícito que no sea una duración positiva. Son propiedades de despliegue leídas al arrancar, no ajustes del producto ni configuración dinámica. Modificar un valor exige revisión de seguridad, pruebas con la configuración de despliegue, reinicio controlado y textos de correo coherentes, pero no requiere otro ADR mientras conserve caducidad y uso único.

Al emitir un desafío se calcula y persiste su `expires_at`. Un cambio posterior de configuración solo afecta a desafíos nuevos y nunca amplía ni reduce retroactivamente la vigencia de enlaces ya emitidos.

El cambio autenticado de contraseña exige la contraseña actual, aplica la misma política, reemplaza el hash y revoca todas las sesiones, incluida la actual. No inicia otra sesión automáticamente. Un administrador nunca establece, recibe ni conoce la contraseña de otra persona.

## Sesiones y protección web

Cada inicio correcto crea una sesión opaca independiente. No se cuentan ni limitan sesiones simultáneas en el PMV.

- El identificador tiene `32` bytes aleatorios en `base64url`; solo se persiste su verificador SHA-256.
- La cookie es `__Host-pmv_session`, `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/` y sin `Domain`.
- La sesión caduca tras `12` horas de inactividad o `7` días desde su creación, lo que ocurra primero.
- Cerrar sesión revoca la sesión actual.
- Cambiar o recuperar contraseña, confirmar un cambio de correo, desactivar o iniciar una reactivación revoca todas las sesiones de la cuenta.
- La sesión referencia la cuenta, pero el rol, estado y permisos se consultan de nuevo para cada operación protegida.

Las operaciones que cambian estado requieren un token CSRF asociado al contexto del navegador y validación de origen cuando el navegador lo envía. La SPA obtiene el token mediante una operación del mismo origen y lo devuelve en `X-CSRF-TOKEN`; el valor no se incluye en URL ni logs. Para peticiones anónimas se emite una cookie `__Host-pmv_csrf`, `Secure`, `SameSite=Lax`, `Path=/`, sin `Domain` y legible por la SPA. El servidor exige que cookie y cabecera coincidan y rota el valor al autenticar y cerrar sesión. Activación, recuperación e inicio de sesión usan esta protección sin deshabilitar CSRF globalmente.

## Límites de abuso

Los límites aceptados son:

- inicio de sesión: `5` fallos por cuenta y `20` por IP en `15` minutos;
- activación o recuperación: `3` solicitudes por cuenta y `10` por IP en una hora.

Los contadores se guardan en PostgreSQL mediante ventanas y actualizaciones atómicas. La clave de cuenta es un HMAC de la forma canónica y la clave de IP es un HMAC rotatorio del valor observado en el borde de confianza. La clave HMAC procede del gestor de secretos. El diseño del despliegue debe fijar qué proxies son confiables antes de aceptar `Forwarded` o `X-Forwarded-For`.

Las respuestas de acceso y recuperación no revelan si una cuenta existe, su estado, su rol ni cuál de los límites se alcanzó. Los errores internos sí generan métricas normalizadas sin correo, token, contraseña o IP completos.

## Operaciones HTTP previstas

La tabla define la semántica que deberá materializar OpenAPI `3.1`. No sustituye el contrato contract-first ni autoriza implementar controladores antes de aprobarlo.

La API será REST y seguirá estas reglas:

- las rutas representan cuentas, sesiones, invitaciones, credenciales, direcciones de correo, desafíos de acceso y tokens CSRF mediante nombres en plural;
- las rutas no contienen verbos, nombres de acciones, roles ni mecanismos de autenticación como `/admin` o `/auth`;
- `GET` consulta sin cambiar estado, `POST` crea un recurso subordinado, `PATCH` modifica parcialmente un recurso y `DELETE` elimina la sesión actual;
- las transiciones se expresan modificando el estado del recurso correspondiente, no creando recursos nominales de activación, restablecimiento o confirmación;
- solo identificadores estables aparecen en la ruta; contraseñas, secretos y tokens se envían exclusivamente en el cuerpo;
- el contrato define representaciones, cabeceras, estados HTTP, idempotencia observable y Problem Details de cada operación.

| Actor | Operación | Propósito |
| --- | --- | --- |
| Anónimo | `GET /api/csrf-tokens/current` | Obtener la representación del token CSRF del contexto actual. |
| Anónimo | `POST /api/sessions` | Crear una sesión con respuesta genérica ante credenciales o estado inválidos. |
| Anónimo | `PATCH /api/invitations/{invitationId}` | Cambiar la invitación a `accepted` consumiendo el secreto, la declaración de mayoría de edad y la contraseña; cubre activación y reactivación. |
| Anónimo | `POST /api/access-challenges` | Crear un desafío con `purpose: password_reset`; responde `202` de forma indistinguible. |
| Anónimo | `PATCH /api/accounts/{accountId}/credentials/current` | Sustituir la contraseña mediante el identificador y secreto del desafío de recuperación. |
| Anónimo | `PATCH /api/accounts/{accountId}/email-addresses/{emailAddressId}` | Cambiar la dirección pendiente a `verified` mediante su secreto y convertirla en la dirección actual. |
| Autenticado | `GET /api/sessions/current` | Consultar identidad, rol y estado de la sesión actual. |
| Autenticado | `DELETE /api/sessions/current` | Revocar la sesión actual. |
| Autenticado | `PATCH /api/accounts/me/credentials/current` | Sustituir la contraseña presentando la actual. Solo admite los campos definidos para ese cambio. |
| Autenticado | `POST /api/accounts/me/email-addresses` | Crear una dirección pendiente y solicitar su verificación. |
| Administrador | `GET /api/accounts` | Buscar cuentas mediante paginación por cursor y filtros acotados. |
| Administrador | `GET /api/accounts/{accountId}` | Consultar estado, rol y metadatos administrativos. |
| Administrador | `POST /api/accounts` | Crear una cuenta pendiente solo con rol `administrador` o `entrenador`. |
| Administrador | `POST /api/accounts/{accountId}/invitations` | Crear una nueva invitación para una cuenta pendiente e invalidar la anterior. |
| Administrador | `PATCH /api/accounts/{accountId}` | Modificar exclusivamente `status`: cancelar una cuenta pendiente, desactivar una activa o iniciar la reactivación de una desactivada. |
| Administrador | `POST /api/accounts/{accountId}/email-addresses` | Crear una dirección pendiente para otra cuenta y solicitar su verificación. |

`me` es un alias estable de la cuenta del actor autenticado y se resuelve en el backend; no acepta ni confía en un identificador enviado por el cliente. Las rutas no conceden permisos: cada operación aplica las capacidades del actor y el alcance del recurso conforme a `ADR-0004` y `ADR-0015`.

La cuenta de corredor se crea únicamente mediante el futuro caso de uso de alta de corredor de `runner-management`, que llama a la API Java de identidad dentro de su transacción. Permitir crearla directamente mediante `POST /api/accounts` produciría cuentas de corredor huérfanas y queda prohibido.

El `PATCH /api/accounts/{accountId}` no es una modificación genérica ni permite cambiar rol, correo u otros atributos. OpenAPI define un cuerpo cerrado con `status`, el servidor valida la transición contra el estado actual y una repetición que ya alcanzó el estado solicitado no vuelve a emitir desafíos ni notificaciones.

`Invitation`, `Credential`, `EmailAddress` y `AccessChallenge` son recursos del contrato, no nombres alternativos de comandos. Una representación REST no tiene que corresponder uno a uno con una tabla: una invitación se materializa mediante la cuenta pendiente y su desafío de acceso vigente. Sus representaciones exponen estado, vigencia y enlaces permitidos sin incluir hashes, secretos ni datos de otras cuentas. Los UUID de cuenta, invitación y dirección no conceden acceso; el servidor valida además el secreto de un solo uso y responde de forma indistinguible cuando corresponda.

No existen endpoints para cambiar rol, establecer contraseñas ajenas, solicitar una baja desde el producto, eliminar la propia cuenta, limitar sesiones ni ejecutar recuperación operativa.

Todos los errores usan `application/problem+json`, `type` estable y un código de aplicación documentado. Como mínimo se distinguen validación (`400`), autenticación ausente o inválida (`401`), autorización insuficiente (`403`), recurso administrativo inexistente (`404`), conflicto de estado o unicidad (`409`) y límite de solicitudes (`429`). Los flujos anónimos sustituyen el detalle por una respuesta indistinguible cuando revelarlo permitiría enumeración.

## API Java publicada

`identity-access` publica casos de uso, no repositorios ni entidades persistentes:

```text
AccountProvisioningApi
  provisionRunnerAccount(command, actorContext) -> ProvisionedAccount
  cancelProvisioning(accountId, actorContext)

ActorContext
  accountId
  role
  actorClass
```

`ProvisionRunnerAccount` contiene correo, declaración administrativa de mayoría de edad y correlación, pero fija internamente el rol `corredor`; el consumidor no puede elegirlo. El resultado expone únicamente el identificador estable y el estado necesario para que `runner-management` cree su vínculo.

La resolución del verificador de sesión es un puerto de entrada interno usado por el `SecurityContextRepository`; no se publica a otros módulos. El adaptador de seguridad convierte su resultado en el `ActorContext` explícito definido por `ADR-0015`. Ningún módulo recibe el hash de contraseña, verificadores, sesiones ni tipos jOOQ de identidad.

## Transacciones e invariantes

| Caso de uso | Trabajo atómico |
| --- | --- |
| Invitar | Reservar correo, crear cuenta y rol, registrar declaración administrativa y auditoría, emitir desafío cifrable y crear solicitud de notificación. |
| Reenviar | Bloquear cuenta, invalidar desafío y solicitud pendientes, emitir nueva generación y crear nueva solicitud. |
| Cancelar invitación | Bloquear cuenta pendiente, pasar a `cancelled`, invalidar desafío y solicitud pendientes y liberar la reserva del correo. |
| Activar | Bloquear cuenta y desafío, validar vigencia, registrar declaración del invitado, guardar hash, consumir desafío y pasar a `active`. |
| Solicitar recuperación | Aplicar límites y, solo para cuenta activa, emitir desafío y notificación sin alterar la respuesta pública. |
| Recuperar contraseña | Bloquear desafío y cuenta, sustituir hash, consumir desafío y revocar todas las sesiones. |
| Cambiar contraseña | Verificar contraseña actual, sustituir hash y revocar todas las sesiones. |
| Solicitar cambio de correo | Reservar correo nuevo, reemplazar desafío previo y crear notificación al correo nuevo. |
| Confirmar correo | Bloquear reserva, desafío y cuenta, sustituir correo actual, consumir desafío, revocar sesiones y solicitar notificación al correo anterior. |
| Desactivar | Bloquear cuenta, impedir auto-desactivación o último administrador, pasar a `disabled`, revocar sesiones y desafíos y registrar auditoría. |
| Reactivar | Bloquear cuenta, validar que los datos existen y el rol coincide, pasar a `pending_reactivation`, revocar restos de acceso, emitir desafío y crear notificación. |

Las restricciones únicas y bloqueos deciden la concurrencia. Los checks previos no sustituyen la traducción determinista de violaciones SQL a conflictos de negocio. Ninguna llamada a Brevo se ejecuta dentro de estas transacciones.

## Datos persistidos

Todas las tablas pertenecen al esquema `identity_access` y solo su adaptador jOOQ puede acceder a ellas.

| Tabla | Datos principales e invariantes |
| --- | --- |
| `account` | UUID, rol inmutable, estado, hash y parámetros Argon2id, instantes, versión optimista. Check de rol y estado; no contiene datos de corredor. |
| `account_email` | Cuenta, correo de presentación, forma canónica, uso `current`, `pending_change` o `released`, confirmación y caducidad. Unicidad parcial global de forma canónica solo para `current` y `pending_change`, y una fila reservada de cada uso por cuenta. |
| `access_challenge` | Cuenta, propósito, generación, verificador SHA-256, caducidad, consumo y reemplazo. Índice único parcial para una generación vigente por cuenta y propósito. |
| `access_session` | Verificador SHA-256, cuenta, creación, último uso, caducidad absoluta, revocación y motivo. Índices por verificador y por cuenta activa. |
| `adult_declaration` | Cuenta, actor, origen, instante y versión de texto. Unicidad por cuenta y origen de declaración. |
| `security_event` | Actor, cuenta afectada, tipo, resultado, instante, correlación y metadatos mínimos no secretos. |
| `auth_rate_limit_bucket` | Tipo, clave HMAC, inicio y fin de ventana y contador. Unicidad por tipo, clave y ventana; limpieza por TTL. |
| `bootstrap_execution` | Marca única del bootstrap, operador, cuenta, instante y correlación; impide crear un segundo bootstrap. |

Los identificadores son UUID aleatorios, los instantes son `timestamptz` en UTC y las comparaciones de caducidad usan un reloj inyectable respaldado por el tiempo de base de datos dentro de las transacciones críticas.

No se persisten contraseñas, secretos de desafío, identificadores de sesión, tokens CSRF, cuerpos de correo ni direcciones IP en claro en estas tablas.

## Entrega segura de enlaces

La entrega asíncrona introduce una necesidad real: el worker debe obtener una vez el secreto en claro para formar el enlace, aunque identidad solo conserve su verificador. Persistir ese secreto sin protección violaría la línea base.

La solicitud a `notification-delivery` incluye un payload mínimo cifrado mediante AEAD antes de confirmar la transacción. El sobre conserva versión de clave, nonce, texto cifrado y autenticación; la clave reside en Azure Key Vault y nunca en PostgreSQL. La versión activa de la clave se carga en memoria durante el arranque y se rota de forma controlada, por lo que cifrar dentro de la transacción es una operación local y no llama a Key Vault. El worker descifra el payload solo en memoria inmediatamente antes de renderizar y enviar. El contenido cifrado se elimina al alcanzar estado terminal, ser reemplazado o caducar el desafío, sin esperar la retención de metadatos técnicos.

La rotación conserva en memoria las versiones anteriores mientras exista un payload no terminal que las referencie y el arranque falla de forma segura si falta una versión necesaria. La clave idempotente, correlación, plantilla y destino pueden persistir según `ADR-0011`, pero logs, trazas, métricas, errores y eventos no contienen el secreto ni el enlace completo. Las páginas que reciben secretos aplican `Referrer-Policy: no-referrer` y no cargan terceros.

## Desactivación, privacidad y recuperación operativa

Solo un administrador puede desactivar cuentas desde el producto. No existe solicitud de baja del corredor ni autoeliminación. Esto no elimina el canal obligatorio para ejercer derechos de privacidad: las solicitudes recibidas por ese canal externo activan el flujo de exportación, conservación o supresión definido por `ADR-0010`.

Una desactivación bloquea acceso, revoca sesiones y secretos, registra actor, motivo, fecha y correlación e inicia el tratamiento de retención aplicable. Un administrador no puede desactivarse a sí mismo ni dejar el sistema sin al menos otro administrador activo.

La recuperación excepcional del único administrador se ejecuta mediante un comando disponible solo en el entorno de despliegue:

- recupera una cuenta administradora existente y unívoca; no crea cuentas ni cambia roles;
- exige operador y motivo, permite actualizar a un correo canónico disponible, revoca sesiones y desafíos y deja la cuenta en `pending_reactivation`;
- emite una invitación nueva y registra operador, motivo, cuenta, fecha y correlación;
- falla si el objetivo no es unívoco, no es administrador o las precondiciones no se cumplen;
- no tiene endpoint HTTP ni interfaz en el producto.

## Autorización

Los adaptadores autentican, pero los servicios de aplicación autorizan cada caso de uso con `ActorContext` y el recurso cargado.

- `administrador` gestiona cuentas y hereda las capacidades globales de entrenador.
- `entrenador` no gestiona cuentas y opera solo las capacidades globales permitidas por `ADR-0004`.
- `corredor` solo accede a recursos vinculados a su propio perfil mediante las APIs propietarias.
- Un UUID válido ajeno produce denegación aunque el frontend o la consulta hayan intentado filtrarlo.
- Proceder de otro módulo no evita la autorización ni concede confianza implícita.

Las pruebas cubren la matriz rol-operación, propiedad, estados, auto-desactivación, último administrador y creación transaccional del vínculo de corredor.

## Paquetes previstos

```text
com.vgrunning.identityaccess/
  api/
    command/
    query/
  application/
    service/
    port/out/
  domain/
    account/
    credential/
    session/
  adapter/in/web/
  adapter/in/command/
  adapter/out/persistence/jooq/
  adapter/out/security/
```

El dominio no depende de Spring, OpenAPI, jOOQ o JDBC. El adaptador web mapea el contrato generado; el adaptador de comando aloja bootstrap y recuperación excepcional. No se crean repositorios CRUD genéricos ni un modelo de dominio espejo de las tablas.

## Observabilidad y conservación

Se registran métricas de intentos, límites alcanzados, activaciones, recuperaciones, cambios, revocaciones y sesiones activas agregadas. Las alertas cubren aumento de fallos, bloqueo sostenido, errores de cifrado o descifrado y uso del comando excepcional.

Los eventos de seguridad son estructurados y contienen identificadores opacos, tipo, resultado y correlación; excluyen correo completo, IP completa, contraseña, token, cookie y payload cifrado. El acceso administrativo, las operaciones operativas y los cambios de identidad quedan auditados.

Las retenciones y supresiones siguen `ADR-0010`. Las tareas de limpieza eliminan sesiones caducadas, desafíos terminales, reservas vencidas y contadores cuando dejan de ser necesarios. La conservación de evidencia de auditoría se documenta en el inventario y registro de actividades antes de producción.

## Validación prevista

### Dominio y aplicación

- Probar todas las transiciones permitidas y prohibidas de cuenta, incluida concurrencia.
- Probar rol inmutable, cancelación por rol incorrecto y prohibición de cuentas de corredor huérfanas.
- Probar doble declaración de mayoría de edad y conservación durante reactivación.
- Probar normalización, reserva y unicidad concurrente del correo.
- Probar auto-desactivación, último administrador y recuperación excepcional.
- Probar la matriz completa de autorización con recurso propio, ajeno e inexistente.

### Seguridad

- Probar política y rehash de contraseña sin exponer entradas.
- Probar generación, caducidad, consumo único, reemplazo y comparación constante de verificadores.
- Probar límites por cuenta e IP y respuestas indistinguibles.
- Probar atributos de cookie, expiración inactiva y absoluta, CSRF, origen y revocaciones masivas.
- Probar que un número no acotado de sesiones puede coexistir sin alterar la caducidad individual.
- Probar ausencia de secretos, correos e IP completos en logs, métricas, trazas, URL de salida y errores.

### Persistencia e integración

- Aplicar Flyway desde PostgreSQL vacío y generar jOOQ reproduciblemente.
- Probar restricciones, índices parciales, bloqueos y rollback con Testcontainers.
- Probar atomicidad entre cuenta, desafío, auditoría y outbox a través de módulos y esquemas.
- Probar cifrado, rotación de clave, descifrado solo en worker y eliminación del payload terminal o caducado.
- Probar que `identity-access` no importa jOOQ ni paquetes internos de otros módulos.

### Contrato

- Crear y aprobar OpenAPI `3.1` antes de implementar las operaciones de esta sección.
- Validar con Spectral, generar servidor y cliente, y probar Problem Details, CSRF, estados y ejemplos.
- Ejecutar pruebas de contrato con MockMvc, pruebas negativas con Schemathesis y `oasdiff` frente a `main`.
- Trazar cada operación y prueba a los criterios de `RF-01` y a la parte de cuentas y roles de `RF-02`.

## Alternativas descartadas en este diseño

- Unificar cuenta y corredor: mezcla identidad de acceso con perfil operativo y crea ciclos de propiedad.
- Permitir cambio de rol: contradice la decisión funcional y transforma permisos e historial sin una migración definida.
- Crear corredores desde el endpoint genérico de cuentas: permite identidades sin perfil y rompe la invariante transaccional.
- Guardar secretos en claro para el worker: convierte una lectura de base de datos en toma de cuenta.
- Enviar correo dentro de la transacción: acopla el commit a una red externa y no resuelve caídas ambiguas.
- Introducir Redis para sesiones o límites: añade infraestructura sin una necesidad de escala medida.
- Incorporar MFA, reautenticación o límite de sesiones al PMV: añade flujos no aprobados; permanecen en [Mejoras futuras](future-improvements.md).

## Consecuencias

- La implementación puede avanzar por casos de uso sin decidir reglas de producto durante la codificación.
- La separación entre cuenta y corredor exige una API Java y pruebas transaccionales entre módulos, pero evita propiedad duplicada.
- El cambio de correo y la reactivación añaden estados y reservas que deben limpiarse y probarse bajo concurrencia.
- Cifrar el payload de notificación evita persistir enlaces utilizables en claro, pero introduce rotación de claves y un fallo operativo adicional que debe observarse.
- Permitir sesiones ilimitadas simplifica el PMV, pero acepta una superficie mayor ante una cuenta comprometida y exige limpieza eficiente.
- No incluir MFA ni reautenticación deja un riesgo relevante en cuentas privilegiadas; está aceptado para el PMV, no eliminado.

## Decisiones pendientes

No quedan decisiones funcionales o arquitectónicas pendientes dentro del alcance de identidad y acceso de este documento.

Antes de implementar todavía deben producirse y revisarse los siguientes artefactos, cuyos criterios ya están decididos:

- contrato OpenAPI inicial de las operaciones previstas;
- migraciones Flyway e índices concretos;
- catálogo versionado de Problem Details y eventos de seguridad;
- textos versionados de declaración de mayoría de edad y plantillas de correo;
- inventario de datos, retención y evidencias operativas bloqueantes para producción exigidas por `ADR-0010`, `ADR-0011` y `ADR-0016`.

Estos trabajos no reabren el diseño salvo que revelen una contradicción o requieran cambiar una decisión aceptada.
