# ADR-0014: Arquitectura modular, hexagonal y DDD selectivo

**Estado:** Aceptado
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0002` decide un monolito modular de despliegue único y el diseño de alto nivel identifica componentes lógicos. Esa decisión no fija cómo materializar límites, dependencias, puertos, modelo de dominio ni propiedad de datos. Sin reglas estructurales, la aplicación puede degradarse en paquetes técnicos compartidos, acceso cruzado a tablas y ciclos difíciles de detectar.

`ADR-0013` define Gradle y un stack imperativo con Spring MVC, jOOQ y JDBC. Este ADR convierte los límites funcionales existentes en módulos verificables sin introducir microservicios, duplicar modelos por ceremonia ni aplicar patrones tácticos de DDD donde solo existe CRUD.

`ADR-0012` exige una única frontera PostgreSQL y transacciones que pueden atravesar planificación, publicación, destinatarios y outbox. La modularidad debe preservar esas garantías y, al mismo tiempo, impedir que compartir base de datos convierta cualquier tabla en una API interna.

## Decisión

El backend se organizará como un monolito modular basado en paquetes dentro de un único proyecto Gradle y un único ejecutable Spring Boot. Spring Modulith descubrirá y verificará los módulos; no se usarán JPMS ni un subproyecto Gradle por módulo durante el PMV.

### Mapa modular y propiedad

Los módulos de aplicación serán:

| Módulo | Responsabilidad y datos gobernados |
| --- | --- |
| `identity-access` | Cuentas de administrador, entrenador y corredor; rol, activación, credenciales, sesiones e invitación o recuperación. |
| `runner-management` | Perfil y ciclo de vida operativo del corredor, sin credenciales, rol ni taxonomías. |
| `classification-segmentation` | Definiciones de etiquetas, valores permitidos, asignaciones a corredores, segmentos, criterios e inclusiones o exclusiones manuales. |
| `planning` | Grupos exclusivos, excepciones, planes, entrenamientos, fases, bloques, tipos, objetivos y ubicación. |
| `publication` | Validación publicable, versiones inmutables, destinatarios congelados y visibilidad. |
| `notification-delivery` | Solicitudes de outbox, leases, entrega, reintentos, webhooks y supresiones para publicación e identidad. |
| `tracking-review` | Seguimiento del corredor, versión de referencia, historial de respuesta y consultas globales de revisión. |
| `runner-portal` | Fachada de lectura de planes, entrenamientos e historial propios; no será fuente de verdad de negocio. |

Los entrenadores no forman un módulo ni un perfil independiente porque el PMV solo los modela como cuentas con rol `entrenador` y capacidades globales. No existen asignaciones, cartera de corredores, atributos profesionales ni ciclo de vida propio. Incorporar alguno de esos conceptos exigiría revisar el límite de `identity-access` y los requisitos afectados.

Taxonomías y segmentación forman una única capacidad de clasificación: los segmentos se expresan exclusivamente con definiciones, valores y asignaciones controladas. `runner-management` conserva la identidad operativa del corredor, mientras `classification-segmentation` gobierna cómo se clasifica; así, cambiar la gramática de segmentación no modifica el ciclo de vida del corredor.

`planning` gobierna borradores mutables y reglas de composición del plan. `publication` permanece separado porque gobierna la transición atómica a versiones inmutables, destinatarios congelados y visibilidad. Ambos colaboran dentro de la misma transacción cuando se publica, pero sus modelos, ciclos de vida e invariantes son distintos.

`runner-portal` permanece separado de `runner-management` porque es una fachada de consulta que compone datos de publicación y seguimiento. No gobierna el corredor ni adquiere propiedad sobre datos ajenos. Unirlos convertiría el módulo de perfiles en consumidor de varias capacidades y facilitaría ciclos con clasificación, planificación y seguimiento.

> **Evolución propuesta:** `ADR-0021` mantiene este mapa de dependencias mediante un puerto de elegibilidad definido por `notification-delivery` e implementado por `publication`, que consulta `runner-management` antes de un intento de correo. No se añade una dependencia desde entrega hacia módulos de negocio.

### APIs y dependencias permitidas

Cada módulo tendrá un paquete raíz bajo el paquete base de la aplicación. Su API pública se declarará mediante `@NamedInterface`; el resto será interno. `@ApplicationModule(allowedDependencies = ...)` o configuración equivalente expresará dependencias permitidas y `ApplicationModules.verify()` rechazará ciclos, accesos a internos y dependencias no declaradas.

Las dependencias seguirán el flujo de capacidades:

| Módulo consumidor | APIs que puede consumir |
| --- | --- |
| `identity-access` | `notification-delivery` |
| `runner-management` | `identity-access` |
| `classification-segmentation` | `runner-management` |
| `planning` | `classification-segmentation`, `runner-management` |
| `publication` | `planning`, `runner-management`, `notification-delivery` |
| `tracking-review` | `publication`, `runner-management` |
| `runner-portal` | `runner-management`, `publication`, `tracking-review` |
| `notification-delivery` | Ninguna API de negocio; recibe solicitudes autocontenidas y referencias opacas. |

Las dependencias inversas y los ciclos quedan prohibidos. Cuando un flujo parezca necesitarlos, se moverá la responsabilidad al módulo que gobierna la regla o se definirá un contrato de aplicación. No se resolverá con acceso a paquetes internos, eventos circulares ni un paquete `common` genérico.

### Comunicación entre módulos

Una colaboración que necesite resultado inmediato, validación conjunta o la misma transacción usará una llamada Java síncrona a una interfaz publicada en `api`. No habrá HTTP, REST, mensajería ni serialización interna entre módulos del mismo ejecutable.

Los eventos de aplicación se reservarán para hechos ya confirmados cuyos consumidores puedan ejecutarse después y fallar independientemente. No sustituirán las llamadas necesarias para publicación, resolución de destinatarios, autorización ni creación atómica de outbox. Un evento no concederá acceso a paquetes o tablas internas del emisor.

La transacción se delimitará en el servicio de aplicación que coordina el caso de uso. Una llamada síncrona a otro módulo participará en esa misma transacción mediante el gestor definido por `ADR-0013`. El módulo receptor aplicará sus invariantes y autorización igual que ante una entrada HTTP; proceder de otro módulo no será una relación de confianza implícita.

Las APIs internas no tendrán versionado independiente porque todos los módulos compilan y se despliegan juntos. Un cambio de contrato deberá actualizar en el mismo commit sus consumidores y pruebas; no se mantendrán versiones paralelas sin una transición explícita.

### Propiedad y evolución de datos

Habrá una única base PostgreSQL, un único `DataSource` HikariCP y un único usuario técnico de runtime. Cada módulo con estado tendrá un esquema PostgreSQL propio:

| Módulo o infraestructura | Esquema |
| --- | --- |
| `identity-access` | `identity_access` |
| `runner-management` | `runner_management` |
| `classification-segmentation` | `classification_segmentation` |
| `planning` | `planning` |
| `publication` | `publication` |
| `notification-delivery` | `notification_delivery` |
| `tracking-review` | `tracking_review` |
| Historial común de Flyway | `platform` |

`runner-portal` no tendrá esquema inicial porque solo compone consultas mediante APIs. Si una necesidad medida exige una proyección persistida, ese módulo podrá gobernar su propio esquema y datos derivados mediante una revisión de este ADR; nunca convertirá la proyección en fuente de verdad.

Los esquemas expresan propiedad y reducen accesos accidentales, pero no son una frontera de seguridad: el único usuario técnico tendrá permisos sobre todos ellos para conservar transacciones compartidas. Introducir usuarios, pools o bases separadas por módulo requeriría otra decisión y no podrá degradar las garantías de `ADR-0012`.

Solo el adaptador de persistencia propietario podrá leer o modificar tablas de su esquema. Quedan prohibidos el acceso SQL directo y los joins a tablas de otro módulo, aunque compartan `DataSource`. Una necesidad de lectura cruzada se resolverá mediante una API de consulta o, si existe evidencia de rendimiento, mediante una proyección explícita con propietario, actualización y consistencia documentadas.

Se permitirán claves foráneas entre esquemas cuando una invariante aceptada necesite integridad física. La referencia deberá seguir una dependencia permitida, apuntar a un identificador estable y declararse en la migración del módulo consumidor. Una clave foránea no concede permiso para consultar o modificar la tabla referenciada.

Flyway gestionará todos los esquemas con una única tabla `platform.flyway_schema_history`, porque comparten versión, despliegue y ciclo de vida. Las migraciones se organizarán por módulo en ubicaciones diferenciadas, usarán versiones globalmente únicas y crearán objetos con nombres de esquema explícitos. Una migración que afecte a varios esquemas deberá declarar el módulo coordinador y revisarse con todos los propietarios afectados.

jOOQ se generará desde todos los esquemas después de aplicar Flyway a PostgreSQL efímero. Los tipos generados quedarán separados por esquema y solo serán visibles para el adaptador de persistencia de su módulo. ArchUnit impedirá que un módulo importe las clases jOOQ generadas de otro.

### Arquitectura hexagonal y paquetes

La estructura lógica de cada módulo será:

```text
com.vgrunning.<module>/
  package-info.java                  @ApplicationModule y dependencias permitidas
  api/
    package-info.java                @NamedInterface("api")
    command/                         puertos de entrada de escritura y comandos
    query/                           puertos de entrada de lectura y resultados
  application/
    service/                         implementaciones de casos de uso y transacciones
    port/out/                        puertos de salida requeridos por la aplicación
  domain/
    <concepto>/                      agregados, entidades, value objects y políticas
  adapter/in/web/                    adaptación OpenAPI/Spring MVC a puertos de entrada
  adapter/in/scheduling/             entradas programadas cuando correspondan
  adapter/out/persistence/jooq/      implementación JDBC, consultas y mapeadores
  adapter/out/provider/              implementación de proveedores externos
```

Los puertos de entrada y salida se escribirán manualmente; no serán código generado. Los puertos de entrada publicados vivirán en `api` y expresarán casos de uso, comandos, consultas y resultados con tipos del módulo. Sus implementaciones vivirán en `application/service`. Los puertos de salida vivirán en `application/port/out` y describirán únicamente capacidades que la aplicación necesita de persistencia, reloj, correo u otro sistema.

OpenAPI Generator producirá interfaces y modelos HTTP en el source set generado del build. Los adaptadores `in/web` implementarán o delegarán esas interfaces y mapearán sus modelos a comandos o consultas de `api`. jOOQ producirá tipos SQL en otro source set generado; los adaptadores `out/persistence/jooq` los usarán y mapearán a tipos de aplicación o dominio. Ningún generador creará casos de uso, puertos o lógica de negocio y el código generado no se versionará.

Dentro de `domain`, los paquetes se organizarán por concepto de negocio, como `plan`, `planninggroup`, `segment` o `publication`, no por categorías genéricas globales como `entities`, `services` o `valueobjects`. El dominio no dependerá de Spring, jOOQ, OpenAPI, JDBC ni adaptadores.

Los adaptadores de entrada no decidirán invariantes y los adaptadores de salida no coordinarán casos de uso. Los puertos serán específicos de una capacidad; no se crearán interfaces espejo de cada clase, repositorios CRUD genéricos ni DTOs duplicados sin una frontera real.

### Aplicación de DDD

Se aplicará DDD estratégico a todos los módulos: límite explícito, lenguaje ubicuo, propiedad de reglas y contratos publicados. Los nombres de código deberán corresponder a los términos validados en los ADRs y documentos de fase.

DDD táctico será selectivo. Se usarán entidades, value objects, agregados, servicios de dominio y eventos solo donde protejan invariantes o reduzcan ambigüedad. Plan, publicación, destinatario, seguimiento, segmento y grupo son candidatos a modelos ricos; catálogos y proyecciones de lectura podrán permanecer como modelos simples. Una tabla no implica una entidad de dominio y un agregado no tiene por qué reproducir todas sus relaciones en memoria.

Los agregados no se usarán para fingir atomicidad sobre grandes grafos. Las invariantes que `ADR-0012` coordina mediante consultas, bloqueos y restricciones podrán residir en servicios de aplicación y políticas de dominio respaldadas por PostgreSQL.

No existirá un `shared-kernel` de negocio inicial. Solo se compartirán tipos técnicos mínimos, estables y sin semántica de un módulo concreto, como reloj o identificadores base. Cualquier ampliación de ese núcleo requerirá demostrar que no roba propiedad a un módulo.

### Mantenimiento y verificación

Cada módulo será propietario de sus casos de uso, dominio, APIs, adaptadores, tablas, migraciones y pruebas. Un cambio deberá permanecer dentro de ese límite salvo que modifique explícitamente un contrato publicado.

El build ejecutará `ApplicationModules.verify()` y reglas ArchUnit para comprobar como mínimo:

- ausencia de ciclos y respeto de `allowedDependencies`;
- acceso entre módulos solo mediante `api`;
- dominio independiente de framework y adaptadores;
- tipos OpenAPI limitados a adaptadores de entrada;
- tipos jOOQ limitados al adaptador propietario de cada esquema;
- ausencia de SQL o imports que atraviesen la propiedad modular.

Cada módulo tendrá pruebas unitarias de dominio y aplicación, pruebas de adaptadores y pruebas de integración con `@ApplicationModuleTest` o mecanismo equivalente. Los flujos transaccionales entre módulos tendrán además pruebas de integración conjuntas sobre PostgreSQL real.

La documentación de módulos y sus dependencias se generará desde el modelo de Spring Modulith y se revisará cuando cambie una API, dependencia o esquema. No se considerará mantenible un módulo cuya propiedad de datos o dependencias solo pueda deducirse leyendo su implementación.

## Alternativas consideradas

### Alternativa A: Paquetes por capas globales

Se descarta porque agrupar todos los controladores, servicios y repositorios facilita dependencias cruzadas y oculta qué capacidad gobierna cada regla.

### Alternativa B: Un subproyecto Gradle o base de datos por módulo

Ofrece fronteras físicas más fuertes, pero se descarta inicialmente por su coste de build, credenciales, migraciones y transacciones. Contradiría la simplicidad operativa y la frontera transaccional única del PMV sin aportar independencia de despliegue real.

### Alternativa C: Un único esquema `public`

Se descarta porque no expresa propiedad física y facilita consultas o joins accidentales entre módulos. Los esquemas separados introducen una barrera de arquitectura verificable sin romper la transacción compartida.

### Alternativa D: DDD táctico uniforme

Se descarta porque produciría agregados, repositorios y objetos de dominio ceremoniales para catálogos y consultas simples. DDD se aplicará donde existan reglas y lenguaje que proteger.

### Alternativa E: Arquitectura hexagonal para cada clase

Se descarta. Crear una interfaz por servicio o repositorio sin una frontera real no desacopla el sistema; aumenta navegación, mocks y coste de cambio.

### Alternativa F: Eventos para toda comunicación entre módulos

Se descarta porque varios flujos necesitan respuesta inmediata y una única transacción. Forzar asincronía interna ocultaría errores, complicaría consistencia y podría contradecir publicación y outbox atómicas.

### Alternativa G: Unir planificación y publicación

Se descarta porque mezcla borradores mutables con versiones inmutables, destinatarios congelados y visibilidad. La transacción común no exige que ambas capacidades pertenezcan al mismo módulo.

### Alternativa H: Unir corredor, clasificación y portal

Se descarta porque combina ciclo de vida, clasificación administrativa y composición de consultas. El resultado concentraría demasiadas dependencias y facilitaría ciclos con planificación, publicación y seguimiento.

## Consecuencias

- Los límites de negocio, código y datos podrán verificarse automáticamente sin separar despliegues.
- Ocho módulos refinan los componentes lógicos iniciales: separan perfil de corredor de clasificación, unen taxonomías con segmentación y mantienen publicación y portal como capacidades propias.
- Compartir base de datos conserva atomicidad, pero los esquemas no aíslan por seguridad porque existe un único usuario técnico.
- Las claves foráneas entre esquemas preservan integridad a costa de hacer explícitas algunas dependencias de migración.
- Prohibir joins cruzados puede exigir varias consultas o una proyección cuando aparezca una necesidad de lectura compleja; esa optimización no se anticipará sin medición.
- Las APIs internas requieren diseño y pruebas, aunque no tengan versionado ni despliegue independiente.
- El uso selectivo de DDD evita sobreingeniería, pero exige criterio de revisión para distinguir reglas de dominio de simple transformación de datos.
- `runner-portal` podrá componer lecturas sin adquirir tablas ni convertirse en propietario de los datos mostrados.
- Mantener un único proyecto Gradle simplifica el build, pero sus límites dependen de verificaciones obligatorias y disciplina de paquetes.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20`.

## Decisiones de Fase 1 relacionadas

- `D-01`: taxonomías, segmentos, grupos y destinatarios mantienen propietarios explícitos y colaboración controlada.
- `D-03`: los módulos pertenecen a un único monolito single-club.
- `D-06`: planificación, publicación y solicitud de notificación colaboran dentro de una transacción.
- `D-07`: seguimiento y portal del corredor conservan modelos y responsabilidades separados.
- `D-08`: entrenador se modela como rol global y cada módulo aplica autorización sin introducir titularidad.

## Validación prevista

- Ejecutar `ApplicationModules.verify()` y reglas ArchUnit en cada build.
- Generar documentación de módulos y revisar dependencias reales frente a la lista permitida.
- Hacer fallar una prueba introduciendo un ciclo, acceso a paquete interno y dependencia no permitida.
- Revisar que cada tabla, caso de uso e invariante tenga un módulo y esquema propietario único.
- Confirmar que tipos OpenAPI y jOOQ no aparecen en dominio o aplicación.
- Probar que un adaptador no puede importar tipos jOOQ de otro esquema.
- Aplicar todas las ubicaciones Flyway sobre PostgreSQL vacío y verificar una única historia en `platform`.
- Probar claves foráneas entre esquemas, nombres cualificados y ausencia de acceso SQL cruzado.
- Probar publicación y creación de outbox en una sola transacción aun atravesando módulos y esquemas.
- Probar composición de `runner-portal` exclusivamente mediante APIs de `publication` y `tracking-review`.
- Revisar una muestra de modelos simples y ricos para comprobar que DDD táctico responde a invariantes y no a una plantilla uniforme.

## Decisiones pendientes

- **Resuelto:** se confirma un único proyecto Gradle con módulos por paquetes verificados mediante Spring Modulith y ArchUnit, sin subproyectos por módulo.
- **Resuelto:** se confirma que DDD táctico y puertos hexagonales serán selectivos y se aplicarán donde protejan invariantes o fronteras reales.
- **Resuelto:** se confirman los ocho módulos, sus responsabilidades, comunicación síncrona o por eventos y dependencias permitidas.
- **Resuelto:** se confirma una base PostgreSQL, un esquema por módulo con estado, un único `DataSource`, usuario técnico e historial Flyway, y `runner-portal` sin esquema inicial.
- **Resuelto:** se permiten claves foráneas entre esquemas según dependencias declaradas y se prohíben acceso SQL directo y joins entre módulos.
- **Resuelto por `ADR-0019`:** los adaptadores de clasificación y corredores dependerán de puertos de coordinación definidos por sus módulos propietarios e implementados por `planning`; así se conserva la dirección de dependencias sin permitir atajos locales.
