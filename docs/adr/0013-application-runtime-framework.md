# ADR-0013: Runtime imperativo, framework de aplicación y contrato API

**Estado:** Aceptado
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura
**Refinado parcialmente por:** [ADR-0024](0024-hybrid-validation-ai-authority.md) — Aceptado; estrategia de activación y autoridad de validación, sin cambio de umbrales ni herramientas; [ADR-0025](0025-spring-session-jdbc-local-login-rate-limit.md) — Propuesto; sustituye el repositorio propio de sesiones por Spring Session JDBC y concreta Bucket4j local.

## Contexto

`ADR-0002` define una aplicación web modular y de despliegue único. `ADR-0012` fija PostgreSQL, una frontera transaccional compartida, bloqueos explícitos y migraciones versionadas. `ADR-0011` exige un worker interno recuperable para la outbox. Falta seleccionar un stack que materialice esas decisiones sin introducir microservicios, broker, caché distribuida ni un segundo runtime de producción.

El PMV prevé más de `500` corredores y picos iniciales inferiores a `100` usuarios concurrentes. Su carga está dominada por casos de uso transaccionales, consultas relacionales y un volumen moderado de llamadas externas. No existe una necesidad medida de streaming, composición masiva de I/O ni concurrencia que justifique asumir la complejidad de un flujo reactivo de extremo a extremo.

Este ADR elige runtime, framework, acceso a datos, migraciones, frontend, contrato API y controles mínimos del build. `ADR-0014` concreta los módulos, la arquitectura hexagonal y el uso de DDD. `ADR-0015` concreta el mecanismo técnico de autorización sin cambiar la política funcional de `ADR-0004`. `ADR-0017` complementa el contrato OpenAPI con las convenciones HTTP orientadas a recursos.

## Decisión

### Runtime y build

El backend se implementará exclusivamente en Java `25` LTS con Spring Boot `4.1` y Spring MVC. Gradle Wrapper será la entrada canónica del build y usará Java Toolchains para fijar Java `25`. El servidor embebido usará hilos de plataforma convencionales; no se habilitarán virtual threads inicialmente.

El código de producción no combinará Spring MVC y WebFlux. Se podrá usar un cliente HTTP asíncrono solo si un caso concreto lo justifica, pero eso no cambiará el modelo imperativo de los casos de uso ni de la persistencia.

El repositorio será un monorepo con backend, frontend y contrato OpenAPI. El backend se construirá como una única aplicación Spring Boot ejecutable. Node.js `24` LTS y npm se usarán únicamente durante desarrollo, pruebas y build del frontend; no existirá un proceso Node en producción.

### Persistencia, jOOQ y transacciones

Todo acceso a PostgreSQL durante la ejecución de casos de uso, consultas, sesiones y worker usará el driver PostgreSQL JDBC y HikariCP. jOOQ OSS será el constructor SQL tipado y la única abstracción general de persistencia; no se incorporarán JPA, Hibernate ni Spring Data JDBC.

La generación de jOOQ producirá tipos de esquema, tablas y registros. Los adaptadores podrán mapearlos a modelos de aplicación o dominio, pero no expondrán tipos generados fuera de la frontera de persistencia. No se usarán DAO CRUD generados como sustituto de consultas o puertos específicos del módulo.

`JdbcTransactionManager` será el gestor transaccional canónico. Los servicios de aplicación delimitarán transacciones mediante `@Transactional` o `TransactionTemplate`, y jOOQ participará en ellas usando el mismo `DataSource` gestionado por Spring. No se abrirán transacciones jOOQ independientes dentro de una transacción Spring.

Las operaciones con bloqueos de `ADR-0012`, incluidas `SELECT ... FOR UPDATE`, `SKIP LOCKED` y actualizaciones condicionadas, deberán probarse contra PostgreSQL real usando JDBC y la misma configuración transaccional de producción. Las llamadas de red nunca se ejecutarán dentro de una transacción que deba reintentarse.

### Flyway y generación de jOOQ

Flyway gestionará migraciones SQL versionadas y PostgreSQL será el único dialecto objetivo. Las migraciones aplicadas no se modificarán; las correcciones avanzarán mediante nuevas migraciones conforme a `ADR-0012`.

- En desarrollo y pruebas, una tarea de preparación aplicará Flyway automáticamente antes de arrancar la aplicación o las pruebas.
- En producción, un paso de despliegue ejecutará y validará Flyway antes de iniciar la nueva versión de la aplicación.
- El build generará los tipos jOOQ desde una instancia efímera de PostgreSQL creada desde todas las migraciones.
- El código generado será un artefacto derivado dentro del directorio de build y no se versionará.
- El build completo requerirá Docker para provisionar PostgreSQL efímero y fallará si migraciones y generación no son reproducibles desde una base vacía.

### Frontend y despliegue conjunto

El frontend será una SPA con React `19`, TypeScript en modo `strict`, Vite, Node.js `24` LTS y npm. No se usará Next.js, SSR ni un servidor Node de producción.

Gradle ejecutará el build de Vite y empaquetará sus recursos estáticos dentro del ejecutable Spring Boot. Spring MVC servirá la SPA y la API bajo el mismo origen. Las rutas de cliente tendrán fallback a `index.html` sin interceptar rutas `/api`, recursos estáticos ni respuestas de error de la API. Backend y frontend se versionarán, desplegarán y revertirán como una única unidad.

Esta decisión evita CORS y un despliegue frontend independiente, pero no impide extraerlo mediante otro ADR si aparecen requisitos de SSR, CDN, escalado u operación separada.

### API contract-first

OpenAPI `3.1` será la fuente de verdad del contrato HTTP y se almacenará en `api/openapi/`. El contrato se diseñará antes de implementar cada operación. OpenAPI Generator producirá interfaces y modelos Spring MVC y el cliente TypeScript consumido por React. El código generado no se editará ni versionará.

La implementación adaptará las interfaces generadas a casos de uso; no se generará lógica de negocio. El contrato incluirá seguridad, token CSRF, estados HTTP, errores con Problem Details, restricciones de entrada, cursores, formatos temporales, ejemplos y `operationId` estables.

El build validará la especificación, regenerará ambos extremos y fallará si backend o frontend no compilan. Spectral aplicará una guía de estilo propia y `oasdiff` comparará el contrato con `main`; un cambio incompatible deberá bloquear el merge salvo decisión explícita que defina transición o versionado.

### Seguridad y sesiones

Spring Security para aplicaciones servlet materializará autenticación, cookies, CSRF y cabeceras. Un repositorio propio de sesiones opacas persistirá mediante jOOQ/JDBC exclusivamente el verificador y los metadatos definidos por `ADR-0003`; no se usará una sesión HTTP en memoria como fuente de verdad.

Argon2id se configurará explícitamente con al menos `19 MiB`, `2` iteraciones y paralelismo `1`; no se aceptarán valores por defecto inferiores. Su coste se medirá en el entorno objetivo y los límites de intentos protegerán la capacidad del servidor.

La autorización se regirá por `ADR-0004` y su mecanismo se define en `ADR-0015`.

### Worker y reintentos transaccionales

El worker de correo permanecerá dentro de la aplicación. Spring Scheduling iniciará el sondeo y un ejecutor dedicado y acotado procesará los envíos bloqueantes sin consumir el pool de peticiones HTTP. La reclamación y persistencia usarán jOOQ/JDBC y las llamadas a Brevo usarán `RestClient` o un cliente HTTP imperativo equivalente. No se incorporarán Quartz, Redis, Kafka ni otro broker.

Los valores iniciales configurables serán: sondeo cada `5` segundos, lote máximo de `20` solicitudes, concurrencia máxima de `4` envíos, timeout de conexión de `3` segundos, timeout total de respuesta de `10` segundos y lease de `90` segundos. El worker no reclamará más trabajo del que pueda iniciar dentro del lease. Cambiar estos valores no requerirá otro ADR si conserva las garantías de `ADR-0011` y `ADR-0012` y se sustenta en métricas.

Las transacciones completas, seguras para repetición y sin llamadas de red podrán reintentarse hasta `3` intentos ante interbloqueo o fallo de serialización, con espera exponencial y jitter. La transacción se reconstruirá en cada intento; no se reintentará solo una sentencia ni errores de integridad o negocio.

### Calidad de código, pruebas y API

El merge quedará bloqueado por los siguientes controles, excluyendo código generado de métricas de cobertura y análisis que no pueda corregirse:

- formato reproducible con Spotless y compilación sin avisos aceptados silenciosamente;
- análisis estático con SpotBugs y NullAway, con anotaciones de nulabilidad explícitas;
- verificación de módulos y reglas de arquitectura conforme a `ADR-0014`;
- JUnit 5, AssertJ, Mockito y Testcontainers sobre PostgreSQL real;
- JaCoCo con al menos `80 %` de líneas y `70 %` de ramas globales, y `90 %` de líneas y `80 %` de ramas en dominio y aplicación críticos;
- PIT con al menos `70 %` de mutation score en reglas críticas;
- TypeScript `strict`, ESLint, Vitest y Playwright para el frontend;
- validación OpenAPI, Spectral, detección de incompatibilidades con `oasdiff`, compilación de clientes generados y pruebas de contrato con MockMvc;
- pruebas negativas y generativas de la API con Schemathesis, y análisis dinámico con OWASP ZAP antes de producción.

La cobertura no sustituirá pruebas de comportamiento. Serán obligatorias pruebas específicas de autorización, concurrencia, bloqueos, rollback, idempotencia, recuperación de leases, orden de versiones y saturación de pools.

## Alternativas consideradas

### Alternativa A: Spring WebFlux, R2DBC y jOOQ reactivo

Se descarta. Permite un flujo no bloqueante, pero añade composición reactiva, propagación de contexto y una integración transaccional menos directa sin una necesidad de carga o streaming que compense ese coste. Adoptarlo por preferencia tecnológica sería sobreingeniería para el PMV actual.

### Alternativa B: Spring MVC con JPA o Hibernate

Se descarta porque las decisiones existentes necesitan SQL explícito, bloqueos PostgreSQL, índices parciales, cursores y actualizaciones condicionadas. jOOQ ofrece control y tipado sin introducir un modelo de estado persistente o consultas implícitas.

### Alternativa C: Spring Data JDBC

Es más simple que JPA, pero se descarta como abstracción principal porque jOOQ cubre tanto operaciones sencillas como las consultas y bloqueos avanzados necesarios. Mantener ambos aumentaría formas de acceso sin una responsabilidad diferenciada.

### Alternativa D: Liquibase

Se descarta frente a Flyway. Sus changesets, precondiciones, contextos y formatos declarativos son útiles para múltiples motores o despliegues condicionales complejos, pero el PMV usa únicamente PostgreSQL y necesita revisar SQL específico. Flyway reduce la capa de abstracción y encaja con la generación de jOOQ desde migraciones SQL.

### Alternativa E: Frontend desplegado por separado o con SSR

Se descarta para el PMV porque introduce otra unidad de despliegue, CORS o proxy, coordinación de versiones y operación de Node sin un requisito de SEO o renderizado en servidor. El empaquetado conjunto conserva el despliegue único de `ADR-0002`.

### Alternativa F: Code-first para la API

Se descarta. Generar OpenAPI desde controladores convierte la implementación en fuente de verdad y permite que frontend y backend diverjan hasta compilación o ejecución. Contract-first hace revisable la compatibilidad antes de implementar.

## Consecuencias

- El modelo imperativo reduce complejidad accidental y encaja directamente con jOOQ, JDBC y las transacciones de `ADR-0012`.
- Cada petición ocupa un hilo mientras espera a PostgreSQL o a una dependencia externa. La escala prevista lo permite, pero deberán vigilarse los pools y timeouts.
- HikariCP y el pool de hilos deben dimensionarse conjuntamente para no crear más trabajo concurrente del que PostgreSQL puede atender.
- El build completo dependerá de Docker y será más lento, pero demostrará que migraciones y tipos jOOQ son reproducibles.
- Backend y frontend se desplegarán juntos y compartirán origen, cookie y política CSRF; no podrán escalarse ni publicarse de manera independiente sin cambiar la decisión.
- OpenAPI será una entrada del desarrollo y cualquier cambio incompatible será visible antes de implementar.
- Los umbrales de cobertura y mutación elevan el coste inicial del build; se aceptan para proteger reglas transaccionales y de autorización de alto impacto.
- El worker no necesita infraestructura adicional, pero comparte proceso y base de datos con la aplicación HTTP y deberá usar un ejecutor separado y métricas propias.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-21`.

## Decisiones de Fase 1 relacionadas

- `D-01`: taxonomías, segmentación y destinatarios se implementan sobre una fuente relacional común.
- `D-03`: el PMV sigue siendo single-club y de despliegue único.
- `D-06`: publicación y solicitud de correo conservan una frontera transaccional común.
- `D-07`: seguimiento e historial se exponen mediante contratos verificables.
- `D-08`: el aislamiento del corredor se aplicará en el backend.

## Validación prevista

- Ejecutar el build completo desde un checkout limpio con Java `25`, Node.js `24`, npm y Docker.
- Crear PostgreSQL vacío, aplicar todas las migraciones Flyway y generar y compilar jOOQ sin artefactos versionados.
- Verificar que no se incorporan WebFlux, R2DBC, JPA, Hibernate ni Spring Data JDBC al runtime.
- Demostrar que todas las consultas jOOQ de una operación comparten commit y rollback mediante el gestor transaccional Spring.
- Ejecutar las pruebas concurrentes y transaccionales exigidas por `ADR-0012` usando JDBC.
- Construir la SPA, empaquetarla en el ejecutable y comprobar rutas de cliente, `/api`, recursos, cookies y CSRF bajo el mismo origen.
- Regenerar servidor y cliente desde OpenAPI, ejecutar Spectral y bloquear con `oasdiff` un cambio incompatible de prueba.
- Hacer fallar deliberadamente cada gate de formato, arquitectura, cobertura, mutación, frontend y contrato para confirmar que bloquea el build.
- Medir pools HTTP, HikariCP, PostgreSQL y worker con la concurrencia objetivo antes de producción.

## Decisiones pendientes

- **Resuelto por `ADR-0014` (Aceptado):** módulos, dependencias permitidas, arquitectura hexagonal y uso táctico selectivo de DDD.
- **Resuelto por `ADR-0015` (Aceptado):** propagación del actor, políticas de aplicación y pruebas del alcance por recurso.
- **Tratado por `ADR-0016` (Aceptado), bloqueante para producción:** plataforma de despliegue, operación de PostgreSQL, secretos, copias, alertas y observabilidad. Las evidencias operativas indicadas por `ADR-0016` deberán completarse antes de producción.
