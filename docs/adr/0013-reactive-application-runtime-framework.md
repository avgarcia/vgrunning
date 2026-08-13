# ADR-0013: Runtime reactivo, framework de aplicación y contrato API

**Estado:** Propuesto
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0002` define una aplicación web modular y de despliegue único. `ADR-0012` fija PostgreSQL, una frontera transaccional compartida, bloqueos explícitos y migraciones versionadas. `ADR-0011` exige un worker interno recuperable para la outbox. Falta seleccionar un stack que materialice esas decisiones sin introducir microservicios, broker, caché distribuida ni un segundo runtime de producción.

El PMV prevé más de `500` corredores y picos iniciales inferiores a `100` usuarios concurrentes. La elección de un stack reactivo no se justifica por esa carga, que también podría resolverse con un modelo imperativo más sencillo. Se adopta deliberadamente para mantener un flujo no bloqueante de extremo a extremo y se acepta su mayor coste de implementación, diagnóstico y pruebas.

Este ADR elige runtime, framework, acceso reactivo a datos, migraciones, frontend, contrato API y controles mínimos del build. `ADR-0014` debe concretar los módulos, la arquitectura hexagonal y el uso de DDD. `ADR-0015` debe concretar el mecanismo técnico de autorización sin cambiar la política funcional de `ADR-0004`.

## Decisión

### Runtime y build

El backend se implementará exclusivamente en Java `25` LTS con Spring Boot `4.1` y Spring WebFlux. Gradle Wrapper será la entrada canónica del build y usará Java Toolchains para fijar Java `25`. El código de producción no combinará Spring MVC y WebFlux ni adoptará un modelo de hilo por solicitud.

El repositorio será un monorepo con backend, frontend y contrato OpenAPI. El backend se construirá como una única aplicación Spring Boot ejecutable. Node.js `24` LTS y npm se usarán únicamente durante desarrollo, pruebas y build del frontend; no existirá un proceso Node en producción.

### Persistencia reactiva y jOOQ

Todo acceso a PostgreSQL durante la ejecución de casos de uso, consultas, sesiones y worker será no bloqueante mediante el driver PostgreSQL R2DBC y un pool R2DBC. No se configurará un `DataSource`, HikariCP ni JDBC en el proceso de aplicación de producción.

jOOQ OSS será el constructor SQL tipado y se configurará explícitamente sobre una `ConnectionFactory` R2DBC. Las consultas se consumirán como `Publisher`, `Mono` o `Flux`; no se usarán DAO generados ni APIs de jOOQ que dependan de JDBC.

`R2dbcTransactionManager` y `TransactionalOperator` serán el mecanismo transaccional canónico. El gestor operará sobre el pool real y jOOQ recibirá un `TransactionAwareConnectionFactoryProxy` sobre ese mismo pool para reutilizar la conexión vinculada al contexto Reactor. No se mezclarán transacciones de jOOQ independientes con transacciones Spring en el mismo caso de uso. Las pruebas deberán demostrar que todas las consultas de una operación comparten conexión, commit y rollback.

Las operaciones con bloqueos de `ADR-0012`, incluidas `SELECT ... FOR UPDATE`, `SKIP LOCKED` y actualizaciones condicionadas, deberán probarse contra PostgreSQL real usando el mismo driver R2DBC de producción. Un adaptador que invoque una API bloqueante no podrá ejecutarse en el event loop; cualquier excepción inevitable deberá aislarse explícitamente, medirse y justificarse, pero no se admite JDBC como excepción para lógica de negocio.

### Flyway y generación de jOOQ

Flyway gestionará migraciones SQL versionadas y PostgreSQL será el único dialecto objetivo. Las migraciones aplicadas no se modificarán; las correcciones avanzarán mediante nuevas migraciones conforme a `ADR-0012`.

Flyway y la generación de código jOOQ podrán usar JDBC porque se ejecutarán fuera del flujo de negocio:

- en desarrollo y pruebas, una tarea de preparación aplicará Flyway automáticamente antes de arrancar la aplicación o las pruebas;
- en producción, un paso de despliegue ejecutará y validará Flyway antes de iniciar la nueva versión de la aplicación;
- el build generará los tipos jOOQ desde una instancia efímera de PostgreSQL creada desde todas las migraciones;
- el código generado será un artefacto derivado dentro del directorio de build y no se versionará;
- el build completo requerirá Docker para provisionar PostgreSQL efímero y fallará si migraciones y generación no son reproducibles desde una base vacía.

El driver JDBC requerido por Flyway y codegen quedará en configuraciones de build, migración o prueba y no en el classpath de ejecución de producción.

### Frontend y despliegue conjunto

El frontend será una SPA con React `19`, TypeScript en modo `strict`, Vite, Node.js `24` LTS y npm. No se usará Next.js, SSR ni un servidor Node de producción.

Gradle ejecutará el build de Vite y empaquetará sus recursos estáticos dentro del ejecutable Spring Boot. WebFlux servirá la SPA y la API bajo el mismo origen. Las rutas de cliente tendrán fallback a `index.html` sin interceptar rutas `/api`, recursos estáticos ni respuestas de error de la API. Backend y frontend se versionarán, desplegarán y revertirán como una única unidad.

Esta decisión evita CORS y un despliegue frontend independiente, pero no impide extraerlo mediante otro ADR si aparecen requisitos de SSR, CDN, escalado u operación separada.

### API contract-first

OpenAPI `3.1` será la fuente de verdad del contrato HTTP y se almacenará en `api/openapi/`. El contrato se diseñará antes de implementar cada operación. OpenAPI Generator producirá interfaces y modelos Spring para WebFlux y el cliente TypeScript consumido por React. El código generado no se editará ni versionará.

La implementación adaptará las interfaces generadas a casos de uso; no se generará lógica de negocio. El contrato incluirá seguridad, token CSRF, estados HTTP, errores con Problem Details, restricciones de entrada, cursores, formatos temporales, ejemplos y `operationId` estables.

El build validará la especificación, regenerará ambos extremos y fallará si backend o frontend no compilan. Spectral aplicará una guía de estilo propia y `oasdiff` comparará el contrato con `main`; un cambio incompatible deberá bloquear el merge salvo decisión explícita que defina transición o versionado.

### Seguridad y sesiones

Spring Security para WebFlux materializará autenticación, cookies, CSRF y cabeceras. Las sesiones opacas y sus verificadores se persistirán mediante R2DBC en PostgreSQL según `ADR-0003` y la línea base de seguridad. No se usará Spring Session JDBC ni una sesión en memoria como fuente de verdad.

Argon2id se configurará explícitamente con al menos `19 MiB`, `2` iteraciones y paralelismo `1`; no se aceptarán valores por defecto inferiores. El cálculo de contraseña, por ser intensivo y bloqueante para el event loop, se ejecutará en un scheduler acotado dedicado a criptografía. El resultado volverá al flujo Reactor sin transportar la contraseña a logs, métricas ni otros hilos no controlados.

La autorización se regirá por `ADR-0004` y su mecanismo reactivo se definirá en `ADR-0015`.

### Worker y reintentos transaccionales

El worker de correo permanecerá dentro de la aplicación y se activará mediante Spring Scheduling con una cadena reactiva. El scheduler solo iniciará la suscripción; selección R2DBC, llamadas HTTP con `WebClient` y persistencia de resultados seguirán siendo no bloqueantes. No se incorporarán Quartz, Redis, Kafka ni otro broker.

Los valores iniciales configurables serán: sondeo cada `5` segundos, lote máximo de `20` solicitudes, concurrencia máxima de `4` envíos, timeout de conexión de `3` segundos, timeout total de respuesta de `10` segundos y lease de `90` segundos. El worker aplicará backpressure y no reclamará más trabajo del que pueda iniciar dentro del lease. Cambiar estos valores no requerirá otro ADR si conserva las garantías de `ADR-0011` y `ADR-0012` y se sustenta en métricas.

Las transacciones completas, seguras para repetición y sin llamadas de red podrán reintentarse hasta `3` intentos ante interbloqueo o fallo de serialización, con espera exponencial y jitter. La transacción se reconstruirá en cada intento; no se reintentará solo una sentencia ni errores de integridad o negocio.

### Calidad de código, pruebas y API

El merge quedará bloqueado por los siguientes controles, excluyendo código generado de métricas de cobertura y análisis que no pueda corregirse:

- formato reproducible con Spotless y compilación sin avisos aceptados silenciosamente;
- análisis estático con SpotBugs y NullAway, con anotaciones de nulabilidad explícitas;
- verificación de módulos y reglas de arquitectura conforme a `ADR-0014`;
- JUnit 5, AssertJ, Reactor Test y Testcontainers sobre PostgreSQL real;
- JaCoCo con al menos `80 %` de líneas y `70 %` de ramas globales, y `90 %` de líneas y `80 %` de ramas en dominio y aplicación críticos;
- PIT con al menos `70 %` de mutation score en reglas críticas;
- TypeScript `strict`, ESLint, Vitest y Playwright para el frontend;
- validación OpenAPI, Spectral, detección de incompatibilidades con `oasdiff`, compilación de clientes generados y pruebas de contrato con `WebTestClient`;
- pruebas negativas y generativas de la API con Schemathesis, y análisis dinámico con OWASP ZAP antes de producción.

La cobertura no sustituirá pruebas de comportamiento. Serán obligatorias pruebas específicas de autorización, concurrencia, bloqueos, rollback, idempotencia, recuperación de leases, orden de versiones, backpressure y ausencia de llamadas bloqueantes en el event loop.

## Alternativas consideradas

### Alternativa A: Spring MVC, JDBC y HikariCP

Es la alternativa recomendada por simplicidad para la carga prevista y encaja directamente con jOOQ y las transacciones de `ADR-0012`. Se descarta por la decisión explícita de mantener un flujo reactivo completo. Se acepta que WebFlux aumenta la complejidad sin que la escala actual demuestre una mejora de rendimiento.

### Alternativa B: WebFlux con JDBC aislado en un pool de trabajadores

Se descarta. Aunque puede evitar bloquear el event loop si se aplica correctamente, mantiene dos modelos de concurrencia, dificulta propagar transacciones y convierte el stack en reactivo solo en la superficie HTTP.

### Alternativa C: Spring Data R2DBC sin jOOQ

Es viable y ofrece integración directa con Spring, pero se descarta porque jOOQ aporta SQL tipado, control explícito de consultas PostgreSQL, generación desde el esquema y mejor visibilidad para bloqueos, cursores y consultas complejas. No se usarán abstracciones de repositorio que oculten esas operaciones.

### Alternativa D: Liquibase

Se descarta frente a Flyway. Sus changesets, precondiciones, contextos y formatos declarativos son útiles para múltiples motores o despliegues condicionales complejos, pero el PMV usa únicamente PostgreSQL y necesita revisar SQL específico. Flyway reduce la capa de abstracción y encaja con la generación de jOOQ desde migraciones SQL.

### Alternativa E: Frontend desplegado por separado o con SSR

Se descarta para el PMV porque introduce otra unidad de despliegue, CORS o proxy, coordinación de versiones y operación de Node sin un requisito de SEO o renderizado en servidor. El empaquetado conjunto conserva el despliegue único de `ADR-0002`.

### Alternativa F: Code-first para la API

Se descarta. Generar OpenAPI desde controladores convierte la implementación en fuente de verdad y permite que frontend y backend diverjan hasta compilación o ejecución. Contract-first hace revisable la compatibilidad antes de implementar.

## Consecuencias

- El flujo de negocio será reactivo de extremo a extremo y no podrá reutilizar librerías bloqueantes sin un análisis explícito.
- jOOQ sobre R2DBC exige configuración y disciplina adicionales; los DAO JDBC generados y la auto-configuración orientada a `DataSource` no serán aplicables.
- Flyway y codegen seguirán usando JDBC fuera del runtime, por lo que el repositorio tendrá dependencias JDBC acotadas a tooling.
- El build completo dependerá de Docker y será más lento, pero demostrará que migraciones y tipos jOOQ son reproducibles.
- Backend y frontend se desplegarán juntos y compartirán origen, cookie y política CSRF; no podrán escalarse ni publicarse de manera independiente sin cambiar la decisión.
- OpenAPI será una entrada del desarrollo y cualquier cambio incompatible será visible antes de implementar.
- Los umbrales de cobertura y mutación elevan el coste inicial del build; se aceptan para proteger reglas transaccionales y de autorización de alto impacto.
- El worker no necesita infraestructura adicional, pero comparte recursos y ciclo de vida con la aplicación HTTP y deberá exponer métricas de saturación, leases y entrega.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20`.

## Decisiones de Fase 1 relacionadas

- `D-01`: taxonomías, segmentación y destinatarios se implementan sobre una fuente relacional común.
- `D-03`: el PMV sigue siendo single-club y de despliegue único.
- `D-06`: publicación y solicitud de correo conservan una frontera transaccional reactiva.
- `D-07`: seguimiento e historial se exponen mediante contratos verificables.
- `D-08`: el aislamiento del corredor se aplicará en el backend reactivo.

## Validación prevista

- Ejecutar el build completo desde un checkout limpio con Java `25`, Node.js `24`, npm y Docker.
- Crear PostgreSQL vacío, aplicar todas las migraciones Flyway y generar y compilar jOOQ sin artefactos versionados.
- Verificar que el classpath de producción no contiene HikariCP, un `DataSource` configurado ni acceso JDBC de negocio.
- Detectar llamadas bloqueantes en pruebas de integración de los flujos WebFlux, worker y persistencia.
- Ejecutar las pruebas concurrentes y transaccionales exigidas por `ADR-0012` usando R2DBC.
- Construir la SPA, empaquetarla en el ejecutable y comprobar rutas de cliente, `/api`, recursos, cookies y CSRF bajo el mismo origen.
- Regenerar servidor y cliente desde OpenAPI, ejecutar Spectral y bloquear con `oasdiff` un cambio incompatible de prueba.
- Hacer fallar deliberadamente cada gate de formato, arquitectura, cobertura, mutación, frontend y contrato para confirmar que bloquea el build.
- Medir event loops, pool R2DBC, latencia, backpressure y worker con la concurrencia objetivo antes de producción.

## Decisiones pendientes

- **Delegada a `ADR-0014`, bloqueante antes de implementar dominio:** confirmar módulos, dependencias permitidas, arquitectura hexagonal y uso táctico de DDD. Responsable: revisor de arquitectura. Tratamiento: aceptar el ADR específico antes de estructurar el backend.
- **Delegada a `ADR-0015`, bloqueante antes de implementar casos de uso protegidos:** confirmar propagación del actor, políticas de aplicación y pruebas del alcance por recurso en WebFlux. Responsable: revisor de arquitectura. Tratamiento: aceptar el ADR específico sin cambiar `ADR-0004`.
- **Bloqueante para producción, no para aceptar este ADR:** seleccionar plataforma de despliegue, operación de PostgreSQL, secretos, copias de seguridad, alertas y observabilidad. Responsable: revisor de arquitectura y persona operadora. Tratamiento: ADR de despliegue y runbooks antes de producción.
