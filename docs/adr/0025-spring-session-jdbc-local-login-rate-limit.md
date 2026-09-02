# ADR-0025: Spring Session JDBC y límite local de intentos de acceso

**Estado:** Propuesto
**Fecha:** 2026-09-02
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0003](0003-identity-authentication-invitation.md), [ADR-0013](0013-application-runtime-framework.md) y [ADR-0015](0015-application-authorization-enforcement.md)

## Contexto

`ADR-0003` decidió una sesión gestionada por el servidor y concretó un token opaco propio de 32 bytes con verificador `SHA-256`. `ADR-0013` trasladó esa decisión a un repositorio de sesiones propio sobre jOOQ/JDBC y `ADR-0015` a un `SecurityContextRepository` propio.

La implementación de F01.1 mostró que esa solución duplica responsabilidades que Spring Security y Spring Session JDBC ya cubren: identificador de sesión, persistencia, caducidad por inactividad, cookie y conservación del `SecurityContext`. Para el PMV de un único nodo, persistir además buckets propios de límite de intentos, HMAC y eventos técnicos de sesión añade migraciones, puertos, infraestructura y coordinación transaccional sin un requisito que lo justifique.

Esta decisión no modifica la identidad local, Argon2id, los flujos de invitación y recuperación, los roles ni la autorización por recurso. Sustituye únicamente la implementación técnica de sesiones y concreta el límite de intentos de inicio de sesión para la topología inicial.

## Decisión

Spring Security materializará la autenticación web y Spring Session JDBC persistirá las sesiones HTTP en PostgreSQL mediante sus tablas estándar `spring_session` y `spring_session_attributes`, creadas por Flyway dentro del esquema `identity_access`. Spring Boot no inicializará esas tablas automáticamente.

Spring Session generará y gestionará el identificador técnico de sesión. No se implementarán un token, un verificador `SHA-256`, un repositorio de sesiones ni un `SecurityContextRepository` propios. La cookie se llamará `__Host-pmv_session` y será `Secure`, `HttpOnly`, `SameSite=Lax`, con `Path=/` y sin `Domain`. La sesión caducará después de 12 horas de inactividad y el cierre de sesión invalidará la sesión HTTP actual.

Spring Security aplicará su integración CSRF para SPA y persistirá el `SecurityContext` en Spring Session. El controlador HTTP podrá depender directamente del componente técnico de sesión porque ambos pertenecen al perímetro de infraestructura; la capa de aplicación y el dominio no conocerán `HttpSession`, `SecurityContextHolder`, cookies ni clases de Spring Security.

Bucket4j aplicará el límite de inicio de sesión en memoria local: 5 fallos por cuenta y 20 por dirección IP durante 15 minutos. Una caché Caffeine acotará el proceso a 10.000 claves y retirará entradas tras 15 minutos de inactividad. Las claves no se persistirán ni se registrarán. Se ignorarán `Forwarded` y `X-Forwarded-For` mientras no exista una configuración explícita de proxies confiables.

Esta configuración solo es válida mientras el PMV se ejecute en un único nodo. Desplegar varias réplicas exige una decisión previa sobre un backend compartido compatible con Bucket4j y sobre la obtención confiable de la dirección de origen. No se introduce Redis ni otra infraestructura distribuida por anticipado.

Los servicios de aplicación podrán delimitar transacciones mediante `@Transactional`, conforme a `ADR-0013`. Esta concesión al framework no autoriza otras dependencias de Spring, Servlet, OpenAPI, jOOQ o infraestructura dentro de aplicación o dominio.

## Alternativas consideradas

### Alternativa A: Sesiones opacas y límites persistentes propios

Se descarta para el PMV. Permite controlar cada detalle del token, caducidad, revocación, contador y auditoría, pero duplica capacidades maduras del framework y obliga a mantener más modelo persistente, criptografía, puertos, infraestructura y pruebas sin una necesidad funcional o de escala demostrada.

### Alternativa B: Spring Session JDBC y Bucket4j con backend distribuido

Se descarta inicialmente. Facilitaría varias réplicas, pero exigiría introducir y operar un almacén compartido antes de que la topología o la carga lo requieran.

### Alternativa C: Sesiones exclusivamente en memoria del proceso

Se descarta porque un reinicio invalidaría todas las sesiones y no respetaría PostgreSQL como persistencia compartida del despliegue. Spring Session JDBC conserva una solución estándar sin añadir otro servicio.

## Consecuencias

- Se eliminan el token y verificador propios, las tablas de sesión y contadores diseñadas a medida y sus puertos, infraestructura y coordinación transaccional.
- La sesión adopta el modelo, esquema y ciclo de vida de Spring Session JDBC; sus identificadores técnicos no forman parte del dominio ni del contrato HTTP.
- El límite de intentos es sencillo y suficiente para un nodo, pero no es consistente entre réplicas y se reinicia con el proceso.
- Una expulsión por capacidad de la caché puede reiniciar un bucket antes de terminar su ventana; la saturación deberá observarse y obliga a revisar el almacenamiento antes de aumentar carga o réplicas.
- La invalidación futura de todas las sesiones de una cuenta deberá implementarse con las capacidades de consulta de Spring Session antes de completar recuperación, cambio de contraseña o desactivación.
- `@Transactional` continúa permitido en servicios de aplicación; las demás dependencias técnicas permanecen fuera del núcleo.
- Argon2id, verificación con hash ficticio, respuestas indistinguibles, CSRF y autorización por recurso siguen siendo controles obligatorios.

## Requisitos relacionados

- `RF-01`
- `RF-02`
- `RF-16`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-03`: el PMV opera como una única aplicación para un único club.
- `D-08`: autenticar una cuenta no sustituye las capacidades ni el aislamiento por corredor.
- `D-10`: los flujos de acceso conservan las declaraciones exigidas para participantes adultos.

## Validación prevista

- Aplicar Flyway desde PostgreSQL vacío y verificar las tablas e índices estándar de Spring Session dentro de `identity_access`.
- Probar el recorrido CSRF, inicio de sesión, consulta de sesión, cierre e invalidez posterior de la cookie.
- Verificar nombre y atributos de la cookie y la caducidad tras 12 horas de inactividad.
- Probar límites exactos de 5 fallos por cuenta y 20 por IP, respuesta indistinguible y eliminación de entradas de la caché.
- Comprobar que correo e IP no se persisten ni aparecen en logs o etiquetas de métricas.
- Verificar mediante ArchUnit que aplicación y dominio no dependen de Spring Security, Servlet, OpenAPI, jOOQ ni infraestructura, excepto el uso permitido de `@Transactional` en servicios de aplicación.
- Ejecutar las pruebas dirigidas de identidad, `backendCheck`, `apiCheck`, `docsCheck` y un único `qualityGate` final.

## Decisiones pendientes

- **Aplazada deliberadamente, no bloquea el nodo único:** selección de backend compartido de Bucket4j si se aprueba una topología con varias réplicas. Responsable: Revisor de arquitectura. Tratamiento: nuevo ADR antes de cambiar la topología.
- **Bloqueante para recuperación, cambio de contraseña y desactivación:** concretar y probar la invalidación de todas las sesiones asociadas a una cuenta mediante Spring Session. Responsable: Revisor de arquitectura. Tratamiento: cerrar la decisión antes de implementar esos casos de uso.
- **Bloqueante para confiar en cabeceras de origen:** definir proxies confiables y su configuración antes de interpretar `Forwarded` o `X-Forwarded-For`. Responsable: Responsable de plataforma. Tratamiento: revisión de despliegue previa a producción.
