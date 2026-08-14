# ADR-0012: Persistencia relacional y estrategia transaccional

**Estado:** Aceptado
**Fecha:** 2026-08-13
**Fecha de aceptación:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

Los módulos de identidad, taxonomías, segmentación, planificación, publicación, seguimiento y notificaciones necesitan compartir invariantes y participar en transacciones de una única aplicación modular. Los ADRs `ADR-0005` a `ADR-0009` exigen, entre otras garantías, exclusividad efectiva de grupos, publicación atómica, versionado inmutable, un único plan activo por corredor y semana, seguimiento único e históricos cronológicos. `ADR-0011` añade una outbox en la misma persistencia y un worker recuperable.

El PMV opera para un único club, con más de `500` corredores y picos iniciales inferiores a `100` usuarios concurrentes. No necesita distribución geográfica, réplicas de lectura, búsqueda externa ni una caché distribuida. Sí necesita evitar que dos operaciones concurrentes validen sobre estados incompatibles y confirmen resultados que violen las reglas aceptadas.

La elección debe funcionar igual en producción, desarrollo y pruebas de integración. Este ADR no elige runtime, framework, proveedor gestionado, plataforma de despliegue ni estrategia de copias de seguridad; esas decisiones deben respetar las garantías aquí establecidas.

## Decisión

### Motor y fuente de verdad

El PMV usará **PostgreSQL como única base de datos primaria de producción**. Desarrollo y pruebas de integración usarán PostgreSQL, en una versión principal compatible con producción; no se usará SQLite ni otro motor como sustituto de compatibilidad.

PostgreSQL será la fuente de verdad para datos de negocio, sesiones, secretos verificables, versiones publicadas, destinatarios, seguimiento y outbox. El PMV no incorporará Redis, caché distribuida, réplicas de lectura, motor de búsqueda externo ni broker de mensajes. Una caché futura será siempre derivada, deberá demostrar invalidación equivalente a la regla canónica y solo se añadirá ante un problema medido mediante una decisión posterior.

`ADR-0016` define el proveedor de PostgreSQL y su operación junto con la plataforma de despliegue. Esa elección no podrá reducir las garantías transaccionales, de bloqueo, restricciones, migraciones o recuperación definidas aquí.

### Integridad y autorización

Las invariantes locales que PostgreSQL pueda expresar se reforzarán con `NOT NULL`, `FOREIGN KEY`, `UNIQUE`, `CHECK`, restricciones de exclusión o índices únicos parciales, según corresponda. Esto incluye como mínimo las unicidades ya decididas para valor por etiqueta y corredor, excepciones, grupo y semana, entrenamiento y día, versión, destinatario activo por corredor y semana, seguimiento y clave idempotente de outbox.

Las reglas que dependen de varias filas o de la evaluación dinámica de segmentos se validarán en servicios de aplicación dentro de una transacción. Ninguna regla de integridad dependerá exclusivamente de la interfaz. Las restricciones de base de datos son la última defensa ante carreras, no un sustituto de mensajes de error de dominio comprensibles.

PostgreSQL Row-Level Security no se usará en el PMV. La autorización seguirá aplicándose obligatoriamente en cada caso de uso del backend conforme a `ADR-0004`, con denegación por defecto y pruebas de aislamiento. Esta decisión evita mantener dos modelos de autorización parcialmente solapados sobre una única identidad técnica del pool de conexiones. Adoptar RLS en el futuro requerirá otro ADR y no permitirá retirar la autorización de aplicación.

### Aislamiento y coordinación

El nivel de aislamiento ordinario será `READ COMMITTED`. Las operaciones sencillas se apoyarán en actualizaciones condicionadas y restricciones únicas. Las operaciones que calculen invariantes sobre varias filas tomarán bloqueos explícitos y seguirán un orden estable.

Existirá una fila persistente única de coordinación de planificación para el club. Toda modificación que pueda cambiar la pertenencia efectiva a grupos bloqueará esa fila mediante `SELECT ... FOR UPDATE` antes de leer y validar el estado afectado. Esto comprende cambios de etiquetas de corredores, criterios y excepciones de segmentos, asociaciones de segmentos a grupos y excepciones de grupos. La primera publicación de un plan adquirirá el mismo bloqueo antes de resolver sus destinatarios. La transacción rechazará por completo el cambio si produce pertenencia a varios grupos.

Serializar estas escrituras de planificación reduce concurrencia, pero es deliberado: son operaciones administrativas poco frecuentes y la escala single-club hace preferible una garantía sencilla y verificable frente a bloqueos distribuidos por predicados dinámicos. Las lecturas ordinarias no adquieren este bloqueo.

La publicación bloqueará también la fila del plan. La primera publicación resolverá y persistirá versión, destinatarios y solicitudes de notificación dentro de la misma transacción. Una restricción única física impedirá más de un destinatario activo por corredor y semana. La republicación conservará los destinatarios originales, bloqueará el plan y sustituirá atómicamente la versión activa sin volver a evaluar grupos. Cualquier conflicto abortará toda la publicación.

Cuando una operación necesite varios bloqueos, el orden será: coordinación del club, planes por identificador estable y después filas dependientes por identificador estable. La aplicación reintentará de forma acotada únicamente transacciones completas que sea seguro repetir ante interbloqueos o fallos de serialización; nunca incluirá llamadas de red dentro del reintento transaccional.

### Reserva recuperable de la outbox

El worker de `ADR-0011` reclamará lotes pequeños de solicitudes elegibles mediante una transacción corta con `FOR UPDATE SKIP LOCKED`. La selección respetará `siguiente_intento`, prioridad temporal y el orden por plan, destinatario y versión; una versión no será elegible mientras exista una versión anterior no liberada según `ADR-0008` y `ADR-0011`.

Al reclamar una solicitud, el worker la cambiará a `procesando` y persistirá:

- un `lease_token` UUID aleatorio e irrepetible por reclamación;
- el identificador de la instancia `lease_owner`;
- el instante `lease_until`, calculado con el reloj de PostgreSQL;
- el contador y los datos de planificación exigidos por `ADR-0011`.

La transacción confirmará antes de llamar a Brevo. No se mantendrán conexiones ni bloqueos de base de datos durante la llamada HTTP. El lease será mayor que el timeout máximo de la llamada con margen operativo y el lote no contendrá más trabajo del que la instancia pueda iniciar dentro de ese plazo.

Una solicitud `procesando` con lease caducado podrá recuperarse sin crear otra solicitud lógica. La nueva reclamación asignará otro token. Toda transición posterior al resultado del proveedor será una actualización condicionada por identificador, estado `procesando`, token vigente y lease no caducado. Un worker que reaparezca después de perder su lease no podrá sobrescribir el resultado del propietario actual.

Esta mecánica evita el procesamiento concurrente ordinario, pero no promete exactamente un envío físico. Si la aplicación cae después de que Brevo acepte y antes de persistir la respuesta, se aplicarán la misma clave idempotente, la reconciliación y el cierre por resultado desconocido definidos en `ADR-0011`.

### Índices, consultas y paginación

Cada migración añadirá los índices necesarios para las restricciones y recorridos conocidos, incluidos:

- relaciones y criterios usados para evaluar segmentos y grupos;
- planes, versiones y destinatarios por grupo, corredor y semana;
- historial y seguimiento ordenados por corredor o ámbito de revisión;
- solicitudes de outbox elegibles, leases caducados y correlación idempotente.

No se crearán índices genéricos sobre toda columna ni se asumirá que una `FOREIGN KEY` queda indexada automáticamente. Los índices se justificarán mediante el patrón de consulta, cardinalidad y planes obtenidos con `EXPLAIN (ANALYZE, BUFFERS)` sobre datos representativos. Los índices parciales podrán limitarse a estados activos o no terminales cuando la consulta use el mismo predicado.

Los históricos y colas cronológicas usarán paginación por cursor con una ordenación total y estable, como `(fecha, id)`. Los filtros formarán parte del contrato del cursor y no se aceptará un cursor emitido para otro conjunto de filtros. `OFFSET` solo se permitirá en catálogos o listados pequeños y acotados donde no se necesite estabilidad ante inserciones concurrentes.

### Evolución del esquema

El esquema evolucionará exclusivamente mediante migraciones versionadas, ordenadas y conservadas en el mismo repositorio que el código. Cada cambio de aplicación que dependa de una migración la incluirá y revisará conjuntamente. No se realizarán cambios manuales de esquema en producción.

Las migraciones deberán ser verificables desde una base vacía y sobre una copia sintética representativa del esquema anterior. Un cambio destructivo exigirá un plan explícito de compatibilidad, conservación o recuperación antes de ejecutarse. Las correcciones ordinarias avanzarán mediante una migración nueva; no se reescribirá una migración ya aplicada.

## Alternativas consideradas

### Alternativa A: MySQL o MariaDB

Podrían cubrir la mayor parte del modelo relacional y la concurrencia prevista. Se descartan porque no aportan una ventaja concreta para este PMV y obligarían a adaptar restricciones, índices parciales y patrones operativos que PostgreSQL cubre de forma coherente con las decisiones existentes. No se considera una alternativa inviable, sino una segunda elección sin beneficio compensatorio.

### Alternativa B: SQLite en desarrollo o producción

Se descarta. Su modelo de un único escritor y sus diferencias de restricciones, concurrencia y bloqueo no representan el comportamiento que debe probarse para publicación y outbox. Usarlo solo en pruebas produciría una validación engañosa de las garantías más importantes.

### Alternativa C: Base documental o NoSQL

Se descarta porque el dominio contiene relaciones, unicidades e invariantes transaccionales entre grupos, planes, versiones, destinatarios y seguimiento. Desnormalizar esos datos trasladaría integridad al código y complicaría consultas e históricos sin una necesidad de escala que lo justifique.

### Alternativa D: Nivel `SERIALIZABLE` para todas las operaciones

Se descarta como valor global. Aporta garantías fuertes, pero exige reintentos generales y no elimina la necesidad de diseñar restricciones y flujos externos. `READ COMMITTED` con bloqueos explícitos sobre filas de coordinación y restricciones únicas hace visible qué operaciones se serializan y limita la contención al ámbito necesario.

### Alternativa E: RLS como autorización principal

Se descarta porque las capacidades del entrenador y administrador incluyen acciones y transiciones que no se expresan únicamente como filtros de fila. Además, una identidad técnica compartida por el pool exige propagar contexto de seguridad correctamente en cada transacción. La autorización del backend ya es obligatoria y comprobable bajo `ADR-0004`.

### Alternativa F: Redis, broker o caché distribuida desde el PMV

Se descarta porque introduciría otra fuente operativa, despliegue, credenciales, recuperación y observabilidad sin evidencia de necesidad. PostgreSQL cubre la escala inicial y la outbox recuperable; cualquier incorporación posterior deberá partir de mediciones.

## Consecuencias

- La aplicación obtiene una única frontera transaccional para todos sus módulos y la outbox.
- Las pruebas de integración necesitan PostgreSQL real; las pruebas unitarias podrán seguir aislando lógica que no dependa de persistencia.
- Las escrituras que alteran pertenencias de planificación quedan serializadas para todo el club. Se acepta menor concurrencia administrativa a cambio de impedir conflictos difíciles de representar con una restricción local.
- `READ COMMITTED` no protege por sí solo consultas complejas; omitir los bloqueos o restricciones prescritos sería un defecto de integridad.
- El worker puede escalar a varias instancias sin que todas reclamen ordinariamente el mismo trabajo y puede recuperar leases abandonados.
- El lease y la idempotencia reducen duplicados, pero no convierten una llamada HTTP en parte de la transacción de PostgreSQL ni prometen entrega exactamente una vez.
- La paginación por cursor mantiene recorridos cronológicos estables, pero no ofrece saltar de forma eficiente a una página arbitraria.
- No usar RLS concentra la autorización en el backend y sus pruebas; una omisión en un caso de uso no tendrá una segunda barrera por fila en la base de datos.
- No incorporar caché ni servicios auxiliares reduce complejidad operativa, pero exige vigilar planes de consulta e índices antes de escalar.
- Las migraciones pasan a ser parte obligatoria y revisable de cada cambio de modelo.
- `ADR-0013` concreta driver, pool y migraciones; `ADR-0016` define proveedor, versión principal y operación. Todos deberán conservar las garantías de esta decisión.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20`.
- Especialmente `RF-03`, `RF-05` a `RF-10`, `RF-14`, `RF-15` y `RF-17` a `RF-20`.

## Decisiones de Fase 1 relacionadas

- `D-01` a `D-08`.

## Validación prevista

- Ejecutar las migraciones desde una base vacía y desde la versión anterior sin cambios manuales.
- Probar restricciones físicas para cada unicidad e invariante local definida por los ADRs de dominio.
- Ejecutar dos transacciones concurrentes que intenten situar al mismo corredor en grupos incompatibles y comprobar que solo una confirma.
- Cambiar simultáneamente etiquetas, segmentos, asociaciones y excepciones de grupo para comprobar que todas las rutas adquieren la misma coordinación y rechazan estados incompatibles.
- Publicar mientras cambia la pertenencia del grupo y comprobar que la versión captura un conjunto coherente de destinatarios.
- Publicar simultáneamente planes distintos para el mismo corredor y semana y comprobar que la restricción física aborta una publicación completa.
- Republicar concurrentemente el mismo plan y comprobar orden de versiones, destinatarios inmutables y una única versión activa.
- Ejecutar varios workers y comprobar que `SKIP LOCKED` distribuye solicitudes sin reclamación concurrente ordinaria.
- Detener un worker antes y después de la llamada externa, caducar su lease y comprobar recuperación con un token nuevo y protección frente al worker obsoleto.
- Probar que ninguna llamada a Brevo mantiene abierta la transacción de reclamación.
- Probar orden de versiones de outbox y todos los casos de reconciliación de `ADR-0011`.
- Validar cursores ante empates, inserciones concurrentes, cambio de filtros y recorridos hacia delante sin duplicados ni omisiones dentro de la semántica documentada.
- Cargar datos representativos de más de `500` corredores, revisar los planes de las consultas críticas y fijar umbrales antes de producción.
- Ejecutar pruebas de autorización del backend para cada rol y confirmar que la ausencia de RLS no permite accesos cruzados.
- Comprobar en CI que las pruebas de integración usan PostgreSQL y no un motor alternativo.

## Decisiones pendientes

No quedan decisiones arquitectónicas pendientes para aceptar este ADR.

- **Resuelto por `ADR-0013` (Aceptado):** driver JDBC, HikariCP, Flyway, proceso imperativo del worker y reintentos transaccionales sin modificar las garantías anteriores.
- **Tratado por `ADR-0016` (Aceptado), sin bloquear este ADR:** PostgreSQL `18` administrado mediante Azure Database for PostgreSQL Flexible Server en `West Europe`, junto con su configuración operativa, copias, recuperación y observabilidad. Las garantías anteriores permanecen vigentes.

## Referencias

- [PostgreSQL: aislamiento de transacciones](https://www.postgresql.org/docs/18/transaction-iso.html).
- [PostgreSQL: `SELECT`, bloqueos de fila y `SKIP LOCKED`](https://www.postgresql.org/docs/18/sql-select.html).
- [PostgreSQL: restricciones](https://www.postgresql.org/docs/18/ddl-constraints.html).
- [PostgreSQL: índices parciales](https://www.postgresql.org/docs/18/indexes-partial.html).
- [PostgreSQL: políticas de seguridad por fila](https://www.postgresql.org/docs/18/ddl-rowsecurity.html).
- [SQLite: transacciones](https://www.sqlite.org/lang_transaction.html).
