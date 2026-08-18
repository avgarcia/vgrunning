# Diseño detallado de gestión de corredores — Fase 2

**Estado:** Validado para diseño y desarrollo con datos sintéticos
**Fecha:** 2026-08-17
**Responsable de revisión:** Revisor de arquitectura
**Restricción:** Prohibido tratar datos personales reales hasta completar la revisión especializada de privacidad exigida por `ADR-0010` y `ADR-0018`

## Propósito y alcance

Diseñar `runner-management`, propietario del perfil operativo del corredor, de su ciclo de vida y del vínculo uno a uno con la cuenta de acceso. El documento materializa la parte de perfil de `RF-02` y proporciona la identidad de corredor que necesitan `RF-03`, `RF-16`, `RF-17`, `RF-18` y `RF-19`.

El alcance incluye:

- alta atómica de perfil y cuenta de corredor;
- nombre, apellidos y vínculo estable con identidad;
- estados de activación, actividad, inactividad y reactivación;
- búsqueda operativa mínima y permisos;
- elegibilidad publicada para los demás módulos;
- retención, supresión y auditoría del perfil;
- recursos HTTP, modelo persistente, concurrencia y pruebas previstas.

No incluye credenciales, correo, rol ni sesiones, gobernados por `identity-access`; etiquetas, segmentos y excepciones, gobernados por `classification-segmentation`; grupos, gobernados por `planning`; publicaciones, seguimiento ni la experiencia del portal. Tampoco almacena teléfono, dirección, fecha de nacimiento, documentos, frecuencia cardiaca, zonas o marcas deportivas.

## Estado y fuentes de decisión

Este diseño aplica:

- [Requisitos de Fase 1](phase-1-requirements.md), [criterios de aceptación](phase-1-acceptance-criteria.md) y [matriz de decisiones](phase-1-decision-matrix.md), especialmente `RF-02`, `D-01` y `D-08`;
- `ADR-0003`: identidad, invitación, activación y recuperación;
- `ADR-0004`: autorización por roles, con la precisión de permisos aceptada en `ADR-0018`;
- `ADR-0010`: privacidad y retención, con el cambio arquitectónico de plazo aceptado en `ADR-0018` y todavía bloqueado para datos reales;
- `ADR-0012`: PostgreSQL, transacciones, bloqueos, índices y migraciones;
- `ADR-0013`: Java, Spring MVC, JDBC, jOOQ, Flyway y OpenAPI contract-first;
- `ADR-0014`: límites modulares, dependencia hacia `identity-access` y esquema propio;
- `ADR-0015`: actor explícito y resolución del corredor desde la cuenta autenticada;
- `ADR-0017`: recursos y semántica de la API HTTP;
- `ADR-0018` aceptado: perfil mínimo, ciclo de vida, permisos, inactividad y reactivación;
- `ADR-0019` aceptado: coordinación desde `planning`, validación de la clasificación conservada y reserva hipotética durante `pending_reactivation`.

`ADR-0018` cambia decisiones aceptadas sobre permisos y conservación. Su aceptación permite validar e implementar este diseño exclusivamente con datos ficticios, sintéticos o anonimizados de forma irreversible. No autoriza tratar datos de corredores reales ni salir a producción: si la revisión de privacidad rechaza la conservación automática, deberán reemplazarse conjuntamente el ADR, este diseño y la trazabilidad de Fase 2 antes de levantar esa restricción.

## Decisiones confirmadas

1. El perfil separa `givenName` y `familyName`; ambos son obligatorios y no únicos.
2. No se duplican correo, rol, credenciales ni estado de sesión.
3. No se almacenan referencias deportivas personales; los objetivos relativos se interpretan fuera de este módulo.
4. Solo el administrador crea, modifica, da de baja y reactiva perfiles.
5. El entrenador consulta únicamente corredores activos y gestiona clasificación mediante su módulo propietario.
6. Un corredor pendiente no participa en la operación hasta completar la activación.
7. Una activación inicial pendiente tiene un máximo absoluto de `30` días, no renovable mediante reenvíos.
8. La baja excluye inmediatamente al corredor de cualquier conjunto operativo nuevo.
9. Cuenta, perfil y clasificación se conservan automáticamente hasta `24` meses para reactivación, con acceso exclusivo del administrador y supresión anticipada por el canal de privacidad.
10. Reactivar exige revisión administrativa; el grupo anterior nunca se restaura automáticamente.
11. Los plazos de publicaciones y seguimiento no se reinician por baja o reactivación.
12. El PMV incluye búsqueda paginada por nombre y apellidos. Buscar por etiquetas o grupos queda aplazado como `MF-004`.

## Riesgos y controles

| Riesgo | Impacto | Control de diseño |
| --- | --- | --- |
| Crear cuenta sin perfil o perfil sin cuenta | Identidad huérfana e invariantes rotas. | Un único caso de uso coordinador y una transacción compartida. |
| Activar identidad sin reflejar todavía la elegibilidad | Retraso en acceso operativo. | Proyección segura por defecto, evento confirmado idempotente y reconciliación; el retraso produce exclusión, nunca acceso prematuro. |
| Mantener elegible a un corredor dado de baja | Nuevos planes, correos o exposición indebida. | Baja síncrona con identidad y API canónica de elegibilidad consultada por consumidores. |
| Restaurar etiquetas o grupos obsoletos | Asignación incorrecta, conflicto entre grupos y posibles planes no deseados. | Revisión administrativa, validación hipotética coordinada por `planning` y ausencia de restauración automática del grupo. |
| Conservar datos reales durante `24` meses sin fundamento suficiente | Incumplimiento y exposición innecesaria. | Desarrollo limitado a datos sintéticos; revisión especializada obligatoria antes de datos reales, acceso restringido, plazo no renovable y supresión anticipada. |
| Buscar inactivos desde un rol entrenador | Exposición de datos fuera de la operación. | Política por rol y estado aplicada en consulta, no filtrado posterior. |
| Borrar el perfil rompiendo historia o claves foráneas | Pérdida de trazabilidad o fallos de integridad. | Anonimización irreversible del vínculo identificable y limpieza idempotente por módulos propietarios. |
| Dos transiciones concurrentes de baja o reactivación | Estados divergentes y efectos duplicados. | Bloqueo de fila, versión optimista, transición cerrada e idempotencia observable. |

## Lenguaje y modelo de estado

### Perfil de corredor

Un **perfil de corredor** representa a la persona dentro de la operación deportiva. No es una cuenta, una clasificación ni un expediente legal. Contiene únicamente identidad visible mínima y estado de relación.

`givenName` representa el nombre y `familyName` todos los apellidos como un único texto. Se eliminan espacios exteriores, se rechazan valores vacíos y se normaliza Unicode a NFC. La presentación conserva mayúsculas, diacríticos y espacios interiores introducidos. Ningún campo es único.

Para búsqueda se mantienen formas derivadas sin distinguir mayúsculas ni diacríticos. Son datos técnicos reconstruibles y no sustituyen el texto de presentación. La consulta admite prefijo por uno o varios términos; no incorpora similitud difusa, búsqueda fonética ni extensiones PostgreSQL en el PMV.

### Estados observables

| Estado | Entrada | Salida | Regla principal |
| --- | --- | --- | --- |
| `pending_activation` | Alta administrativa atómica. | `active`, `cancelled`. | No elegible; máximo `30` días desde la creación. |
| `active` | Activación inicial o reactivación completada. | `inactive`. | Único estado elegible para operación. |
| `inactive` | Baja administrativa. | `pending_reactivation`, supresión. | Sin acceso ni operación; conservación temporal restringida. |
| `pending_reactivation` | Revisión administrativa completada e invitación emitida. | `active`, `inactive`. | Continúa sin elegibilidad hasta aceptar la reactivación. |
| `cancelled` | Cancelación o vencimiento de un alta nunca activada. | Supresión. | Terminal y no recuperable. |

`erased` podrá existir como marca técnica no observable para mantener integridad referencial después de eliminar nombre, apellidos y vínculo con la cuenta. No permite búsqueda, recuperación ni reactivación y no se presenta como estado funcional.

### Estado de cuenta y estado operativo

La cuenta continúa siendo propiedad de `identity-access`. El estado observable del corredor se proyecta a partir del perfil y de hechos confirmados de identidad, sin permitir que identidad dependa de `runner-management`.

- El alta llama sincrónicamente a `ProvisionRunnerAccount` y crea ambos recursos en la misma transacción.
- La activación de cuenta confirma primero identidad. Un evento publicado por su API permite que `runner-management` cambie idempotentemente a `active` después del commit.
- Mientras el evento no se procese, el corredor sigue no elegible. Esta consistencia eventual produce una denegación temporal segura.
- Una reconciliación consulta por lotes el estado publicado de las cuentas pendientes y corrige eventos retrasados o perdidos.
- La baja y el inicio de reactivación se coordinan desde `runner-management` mediante llamadas Java síncronas a identidad dentro de la transacción que modifica el perfil.

Los consumidores nunca infieren elegibilidad desde una cuenta ni desde la presencia de una fila. Usan la API canónica publicada por `runner-management`.

## Modelo persistente

El esquema `runner_management` contendrá inicialmente:

| Tabla | Contenido e invariantes |
| --- | --- |
| `runner` | UUID, `account_id` único mientras sea identificable, nombre, apellidos, formas de búsqueda, estado, fechas de creación, activación, baja, vencimiento y versión optimista. Nombre y apellidos obligatorios salvo anonimización terminal. |
| `runner_lifecycle_audit` | Corredor o referencia anonimizada, actor, transición, motivo, instante y correlación. No duplica correo, nombre, comentarios ni contenido deportivo. |

La clave foránea desde `runner.account_id` hacia `identity_access.account` sigue la dependencia permitida y solo garantiza integridad; el adaptador no lee tablas de identidad. Al ejecutar supresión irreversible se elimina el vínculo y los campos identificativos, manteniendo únicamente un identificador opaco cuando otra retención todavía necesite trazabilidad no identificable.

Restricciones mínimas:

- unicidad de `account_id` cuando no sea nulo;
- `given_name` y `family_name` no vacíos en estados identificables;
- `retention_until` obligatorio solo en `inactive` y coherente con `deactivated_at + 24 meses`;
- `pending_activation_expires_at` fijado al crear y nunca ampliado;
- fechas obligatorias según estado y transiciones cerradas en aplicación;
- versión optimista para actualizaciones y bloqueo pesimista en transiciones con efectos entre módulos.

Los índices iniciales cubrirán cuenta, estado y vencimientos. La búsqueda usará las formas canónicas y un cursor ordenado por apellidos canónicos, nombre canónico e identificador. Los índices concretos se justificarán con `EXPLAIN ANALYZE`; no se añade `pg_trgm` sin evidencia.

## Casos de uso

### Alta

1. Un administrador envía nombre, apellidos, correo y declaración administrativa de mayoría de edad, con `Idempotency-Key`.
2. `runner-management` normaliza el perfil y comprueba únicamente errores de presentación; la unicidad real del correo pertenece a identidad.
3. Dentro de la misma transacción llama a `ProvisionRunnerAccount`, que fija rol `corredor` y crea cuenta, desafío y solicitud de correo.
4. Crea el perfil `pending_activation` con `account_id`, vencimiento absoluto a `30` días y misma correlación.
5. Confirma ambos recursos o revierte todo. La respuesta devuelve el corredor y enlaces permitidos, nunca secreto ni estado interno del correo.

Repetir la misma clave con el mismo cuerpo devuelve el resultado anterior; reutilizarla con otro cuerpo produce conflicto. Un correo ya reservado devuelve un conflicto estable que permite al administrador localizar la cuenta desde identidad sin revelar datos a roles no autorizados.

### Activación y cancelación

La aceptación de invitación sigue el contrato de identidad. Tras el evento confirmado, el perfil pasa de `pending_activation` a `active` una sola vez. Un evento repetido, tardío o relativo a otro estado no duplica efectos.

El administrador puede cancelar antes del vencimiento. Una tarea automática reclama por lotes perfiles vencidos con bloqueo y `SKIP LOCKED`, invoca la cancelación idempotente de identidad, marca `cancelled` y ejecuta la supresión del perfil provisional. Reenviar una invitación no modifica `pending_activation_expires_at`.

### Modificación del perfil

Solo el administrador modifica nombre o apellidos. El cuerpo es cerrado y no admite correo, rol, etiquetas, grupo, estado de cuenta ni referencias deportivas. La actualización requiere versión o `If-Match`; un conflicto concurrente devuelve Problem Details sin sobrescribir silenciosamente.

Cada cambio registra actor, instante, campos modificados y correlación, pero no conserva copias históricas de nombre y apellidos como mecanismo de auditoría. Los consumidores obtienen la presentación vigente mediante la API publicada.

### Baja

1. El administrador solicita `status: inactive` e indica un motivo operativo no sensible.
2. El caso de uso bloquea el perfil y rechaza estados incompatibles o repeticiones con otro efecto.
3. Llama sincrónicamente a identidad para deshabilitar la cuenta, revocar sesiones y desafíos.
4. Registra `deactivated_at`, `retention_until = deactivated_at + 24 meses`, transición y actor.
5. Confirma una única transacción. Desde ese commit, la API de elegibilidad lo excluye.

No se modifican físicamente etiquetas, excepciones, grupos, publicaciones ni seguimiento durante la baja. Cada módulo debe usar elegibilidad para operación y su propia política para retención. Una publicación ya confirmada conserva su instantánea histórica.

### Reactivación

El administrador consulta el perfil inactivo y, mediante las APIs propietarias y la interfaz compuesta, revisa nombre, apellidos, etiquetas, excepciones y grupo de referencia. `runner-management` registra actor y fecha de revisión sin copiar el contenido gobernado por otros módulos.

Solo una revisión vigente del ciclo actual permite solicitar `pending_reactivation`. El adaptador de corredores invoca el puerto de coordinación definido por `runner-management`; la implementación de `planning` adquiere el bloqueo global y la revisión de clasificación del corredor, evalúa la clasificación conservada como si estuviera activo y rechaza la operación completa si aparecería en varios grupos. Cuando el resultado es válido, esa misma transacción llama a la API publicada por `runner-management`, que vuelve a autorizar la transición y solicita a identidad el desafío de reactivación. Aceptarlo publica el hecho que devuelve el perfil a `active`. Si el desafío caduca o el administrador cancela, vuelve a `inactive` sin ampliar `retention_until`.

`runner-management` no importa `planning`: conoce únicamente su puerto. La aplicación deberá fallar al arrancar si no existe exactamente una implementación coordinada, y las pruebas de arquitectura impedirán una implementación local que omita la validación de grupos.

Mientras el perfil permanezca en `pending_reactivation`, `planning` lo incluye como reserva hipotética en toda validación de exclusividad. La reserva se deriva del estado publicado por este módulo, no convierte al corredor en miembro efectivo ni congela su grupo y termina cuando la invitación se acepta, cancela o caduca. Así, cambios posteriores solo se confirman si la futura activación seguiría siendo válida.

Reactivar antes del vencimiento cancela la tarea de supresión y elimina `retention_until`. Una baja posterior inicia un periodo nuevo de `24` meses. No se pueden reactivar perfiles ya anonimizados o suprimidos.

### Supresión por vencimiento o solicitud

Una tarea idempotente reclama perfiles vencidos, deshabilita cualquier resto de acceso y elimina nombre, apellidos, formas de búsqueda y vínculo con la cuenta. Publica un hecho durable de supresión para que cada módulo propietario aplique su política sin acceso SQL cruzado.

Clasificación elimina asignaciones y excepciones identificables; planificación elimina la referencia administrativa al último grupo; publicaciones y seguimiento conservan, anonimizan o suprimen según su propio evento y plazo. Un identificador opaco sin vínculo recuperable puede permanecer para integridad mientras exista historia legítimamente conservada.

Una solicitud anticipada usa el procedimiento externo de derechos de `ADR-0010`; el producto no decide automáticamente excepciones jurídicas. Una decisión aprobada por el responsable encola la misma operación de supresión con su correlación y motivo.

## Autorización

| Operación | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Crear perfil | Permitida | Denegada | Denegada |
| Consultar lista de activos | Permitida | Permitida | Denegada |
| Consultar inactivos o pendientes | Permitida | Denegada | Denegada |
| Consultar un perfil activo | Permitida | Permitida | Denegada de forma directa |
| Modificar perfil | Permitida | Denegada | Denegada |
| Dar de baja, revisar o reactivar | Permitida | Denegada | Denegada |
| Resolver el corredor propio desde cuenta | Mediante API interna autorizada | Mediante API interna autorizada | Derivada de la cuenta, nunca elegida por el cliente |

La política se aplica antes de leer. Para un entrenador, un identificador de corredor no activo responde igual que uno inexistente. Solo el administrador distingue estados no operativos. Los endpoints no aceptan roles ni `accountId` como forma de ampliar alcance.

## Contratos entre módulos

### API consumida de identidad

`runner-management` necesita contratos Java publicados para:

- provisionar atómicamente una cuenta de corredor;
- consultar por lotes estado de cuentas vinculadas para reconciliación;
- deshabilitar acceso dentro de la baja coordinada;
- iniciar o cancelar una reactivación;
- suprimir o desvincular una identidad conforme a una decisión de privacidad aprobada;
- consumir hechos confirmados de activación con identificador, estado y correlación, sin correo ni secretos.

Estos contratos amplían la API de identidad en el mismo commit de implementación y no autorizan lectura de su esquema.

### API publicada por gestión de corredores

Los demás módulos podrán:

- resolver el `runnerId` asociado a la cuenta del actor, como exige `ADR-0015`;
- consultar si uno o varios identificadores son `active` mediante una operación por lotes y, solo para coordinación autorizada de `planning`, obtener los `pending_reactivation` que deben tratarse como reservas;
- obtener presentación mínima de corredores activos para interfaces autorizadas;
- obtener, solo para administración, la presentación de un inactivo durante revisión;
- consumir hechos de baja, reactivación y supresión para actualizar datos derivados o ejecutar retención.

Para iniciar una reactivación, `runner-management` publicará además el caso de uso interno que valida la revisión vigente y realiza la transición. Solo la implementación del puerto coordinador en `planning` lo invocará desde el adaptador HTTP; ningún controlador podrá usarlo directamente como atajo.

La consulta interna de reservas no tendrá adaptador HTTP y devolverá a `planning` únicamente UUID opacos y estado, nunca nombre ni apellidos. Su resultado sirve para proteger la invariante y no amplía lo que el actor puede recibir en la respuesta.

La presentación mínima contiene identificador, nombre y apellidos; nunca correo, motivo de baja o metadatos de privacidad. Clasificación, planificación y seguimiento no duplican esa presentación como fuente de verdad.

## API HTTP propuesta

El contrato OpenAPI `3.1` deberá modelar como mínimo:

| Actor | Operación | Semántica |
| --- | --- | --- |
| Administrador | `POST /api/runners` | Crear atómicamente perfil y cuenta pendiente. |
| Administrador o entrenador | `GET /api/runners` | Listar corredores visibles con cursor y búsqueda por nombre o apellidos; solo administrador puede filtrar estados no activos. |
| Administrador o entrenador | `GET /api/runners/{runnerId}` | Consultar un perfil visible según rol y estado. |
| Administrador | `PATCH /api/runners/{runnerId}` | Modificar nombre, apellidos o solicitar una transición de estado documentada. |

La representación expone `id`, `givenName`, `familyName`, estado visible, versión y enlaces o propiedades permitidas. No expone `accountId`, correo, fechas internas de retención a entrenadores ni motivos de privacidad.

El `PATCH` usa un cuerpo cerrado. La solicitud de `pending_reactivation` atraviesa el puerto coordinado de `ADR-0019`; una transición repetida que ya alcanzó el estado no vuelve a emitir invitaciones ni prolonga retención. Si la revisión de reactivación necesita representación propia durante OpenAPI, deberá superar el test de recurso de `ADR-0017`; no se añadirá una ruta nominalizada como `/reactivate`.

La búsqueda `q` acepta texto de nombre, no secretos ni identificadores de identidad. `status` solo está disponible para administrador. El cursor es opaco, ligado a los filtros y con límite máximo documentado. Los errores usan `application/problem+json` y distinguen validación, autenticación, capacidad, no encontrado indistinguible, conflicto de estado o versión y límite de solicitudes.

## Concurrencia e idempotencia

- El alta exige `Idempotency-Key` y una reserva única de correo gobernada por identidad.
- Las transiciones bloquean la fila del perfil y validan estado y versión antes de invocar otro módulo.
- Baja, inicio de reactivación y cancelación son idempotentes respecto al estado alcanzado; repetir no reinicia plazos ni emite nuevos mensajes.
- La activación por evento usa identificador de evento o correlación única para evitar doble transición.
- Las tareas de caducidad y supresión reclaman lotes mediante lease o `FOR UPDATE SKIP LOCKED`, reanudan tras caída y registran resultado terminal.
- Las búsquedas no prometen instantánea entre páginas; el cursor evita duplicados por empate mediante el UUID y documenta que una modificación concurrente del nombre puede mover el recurso entre páginas.

## Privacidad, auditoría y observabilidad

La categoría mínima de datos de este módulo es nombre, apellidos, vínculo con cuenta, estado y fechas de ciclo. El módulo no registra correo, secretos, datos deportivos personales ni contenido de seguimiento.

Los logs usan identificadores opacos y correlación. No incorporan nombre, apellidos ni motivo libre. La auditoría registra alta, cambio, baja, acceso excepcional a inactivos, revisión, reactivación y supresión. Su plazo sigue la categoría de auditoría de `ADR-0010`, no el plazo del perfil.

Métricas agregadas:

- perfiles por estado;
- altas pendientes próximas a caducar;
- retraso y fallos de reconciliación de activación;
- bajas, reactivaciones y supresiones por periodo;
- tareas de retención vencidas, fallidas o reintentadas;
- accesos administrativos a inactivos, sin identificar al corredor en etiquetas de métrica.

Alertas: acumulación de pendientes vencidos, discrepancias persistentes con identidad, supresiones fuera de plazo y fallos sostenidos del consumidor de eventos.

El tratamiento de datos personales reales y la producción permanecen bloqueados hasta obtener la revisión especializada de `ADR-0018` y actualizar inventario, base jurídica, información al interesado, registro de actividades, EIPD, automatización y pruebas. Los entornos de desarrollo y prueba no podrán importar ni copiar información de corredores reales.

## Búsqueda y evolución

El PMV incluye únicamente búsqueda por prefijo normalizado de nombre o apellidos y paginación por cursor. El administrador puede filtrar por estado; el entrenador recibe implícitamente `active` y no puede modificar el filtro.

Buscar corredores por etiquetas o grupos requiere componer información de `classification-segmentation` y `planning`. Queda registrado como `MF-004` y no se resolverá mediante joins cruzados, duplicación de propiedad ni filtros improvisados en este módulo.

## Validación prevista

### Dominio y persistencia

- Probar obligatoriedad, Unicode NFC, límites y ausencia de unicidad de nombre y apellidos.
- Probar unicidad del vínculo cuenta-corredor y rechazo de perfiles o cuentas huérfanos.
- Probar máquina de estados, fechas obligatorias, versión optimista y bloqueos de transiciones.
- Probar índices, cursores, vencimientos y rollback con PostgreSQL y Testcontainers.
- Probar que `runner-management` no importa jOOQ ni paquetes internos de identidad, clasificación o planificación.

### Alta, activación y permisos

- Probar alta atómica, idempotencia, conflicto de correo y rollback de solicitud de notificación.
- Probar activación eventual, evento repetido, reconciliación y exclusión segura mientras exista retraso.
- Probar cancelación y máximo absoluto de `30` días aunque se reenvíe la invitación.
- Probar la matriz completa de rol, estado y operación, incluidas listas y acceso directo por UUID.
- Probar que el entrenador nunca observa pendientes o inactivos ni modifica datos identificativos.

### Baja, reactivación y retención

- Probar deshabilitación de acceso y baja dentro de una transacción, incluidas carreras con publicación.
- Probar que todos los módulos excluyen estados no activos de conjuntos efectivos y nuevas publicaciones.
- Probar acceso administrativo auditado y supresión anticipada mediante decisión aprobada.
- Probar revisión obligatoria, rechazo atómico del reingreso que produciría varios grupos, reserva durante `pending_reactivation`, ausencia de restauración automática de grupo y vencimiento de desafíos sin ampliar retención.
- Ejecutar carreras entre aceptación, cancelación, caducidad y cambios de clasificación o grupos, sin revelar reservas al entrenador.
- Probar `24` meses exactos desde cada baja, nuevo periodo tras una reactivación real y ausencia de renovación por consultas.
- Probar anonimización o supresión coordinada, reintentos, copias restauradas e independencia de los plazos históricos.

### Contrato

- Crear y aprobar OpenAPI `3.1` antes de implementar operaciones HTTP.
- Revisar recursos, métodos, estados, seguridad e idempotencia contra `ADR-0017` y la guía de API.
- Validar con Spectral, generación de servidor y cliente, MockMvc, Schemathesis y `oasdiff` frente a `main`.
- Trazar operaciones y pruebas a `RF-02` y a las dependencias de `RF-03`, `RF-16`, `RF-17`, `RF-18` y `RF-19`.

### Privacidad

- Impedir y probar importaciones o copias de datos personales reales en desarrollo y pruebas mientras permanezca el bloqueo.
- Obtener revisión humana especializada de la conservación automática antes de tratar datos personales reales.
- Probar que únicamente el administrador accede a inactivos y que cada acceso queda auditado.
- Probar información versionada de retención al alta y comunicación de la baja.
- Probar derechos, supresión anticipada, vencimiento automático y ausencia de datos identificativos en logs y métricas.

## Alternativas descartadas

- Unificar perfil y cuenta: mezcla ciclos, datos y propietarios y crea dependencias desde identidad.
- Usar únicamente el correo como presentación: obliga a exponer identidad a módulos deportivos y dificulta minimizar acceso.
- Guardar nombre completo en un campo: se descarta por decisión de producto; nombre y apellidos se administran por separado.
- Guardar marcas o zonas personales: amplía privacidad y no es necesario para expresar objetivos relativos.
- Hacer operativo al corredor antes de activar: permite planes y notificaciones que todavía no puede consultar.
- Permitir al entrenador administrar perfil o baja: amplía privilegios y choca con el alta de cuenta reservada al administrador.
- Borrar clasificación al dar de baja: dificulta la vuelta y no es necesario si se excluye por elegibilidad.
- Restaurar automáticamente el grupo: puede aplicar una decisión obsoleta después de meses o años.
- Buscar por grupos o etiquetas desde SQL de este módulo: viola propiedad y dependencias modulares.
- Conservar perfiles pendientes o inactivos sin vencimiento: impide demostrar limitación del plazo.

## Consecuencias

- Los módulos deportivos reciben una identidad de corredor estable sin acceder a cuentas ni correos.
- La consistencia eventual de activación acepta un retraso seguro; nunca concede operación antes de confirmar identidad.
- Separar elegibilidad de la existencia física permite conservar datos restringidos sin incluirlos en el trabajo diario.
- La búsqueda mínima es viable para más de `500` corredores sin introducir filtros cruzados ni infraestructura adicional.
- La reactivación preserva información útil, pero exige trabajo administrativo y puede encontrar historia ya vencida.
- Una reactivación pendiente puede bloquear cambios posteriores que crearían un conflicto, aunque el corredor todavía no aparezca en resultados efectivos.
- La retención de `24` meses es una ampliación material de tratamiento: no bloquea el diseño con datos sintéticos, pero bloquea cualquier dato personal real hasta su revisión especializada.
- La implementación necesitará coordinación idempotente de eventos y retención entre módulos, además de pruebas temporales y de concurrencia.

## Decisiones pendientes

- No quedan decisiones de producto o arquitectura pendientes dentro de `runner-management`; las decisiones sobre búsqueda por etiquetas o grupos están aplazadas explícitamente a `MF-004`.
- **Bloqueante antes de tratar datos personales reales y para producción:** revisar la finalidad, base jurídica, necesidad y proporcionalidad de la conservación automática durante `24` meses. Responsable: responsable del tratamiento con Revisor de privacidad o DPO. Tratamiento: usar exclusivamente datos ficticios, sintéticos o anonimizados de forma irreversible hasta obtener la revisión y corregir ADR, diseño y trazabilidad si no confirma la política.
- Antes de implementar deben producirse OpenAPI, migraciones Flyway, índices medidos, catálogo de Problem Details, eventos publicados, textos de información y tareas verificables de retención.
