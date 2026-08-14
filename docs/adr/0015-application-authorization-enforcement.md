# ADR-0015: Aplicación de autorización y alcance por recurso

**Estado:** Aceptado
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0004` define roles, capacidades, denegación por defecto y aislamiento del corredor. También decide que cada módulo autorice sus casos de uso y deja pendiente el mecanismo concreto del framework. `ADR-0012` descarta Row-Level Security como mecanismo del PMV y `ADR-0013` define Spring MVC, jOOQ y JDBC.

Autenticar una ruta no autoriza automáticamente todos los recursos alcanzados por el caso de uso. La política debe proteger también llamadas internas y consultas por identificador sin confiar en datos enviados por el cliente ni filtrar resultados después de leerlos.

Este ADR materializa técnicamente `ADR-0004`; no modifica roles, jerarquía, capacidades, tratamiento de no encontrado ni alcance global del entrenador.

## Decisión

### Autenticación y actor

Spring Security autenticará cada solicitud mediante la sesión opaca de `ADR-0003`. Un `SecurityContextRepository` propio resolverá exclusivamente el verificador persistido mediante jOOQ/JDBC y producirá una identidad autenticada con identificador de cuenta, rol vigente y estado. `identity-access` no resolverá ni poseerá la vinculación entre cuenta y corredor.

El adaptador HTTP transformará esa identidad en un `ActorContext` inmutable emitido por `identity-access`, compuesto por identificador de cuenta, rol y clase de actor. El actor se pasará explícitamente a cada caso de uso protegido; la capa de aplicación y el dominio no leerán `SecurityContextHolder`, cookies, cabeceras ni clases de Spring Security. El contexto de seguridad asociado al hilo quedará limitado a filtros y adaptadores.

Cuando un caso de uso necesite el corredor asociado, el módulo consumidor resolverá el identificador mediante la API Java publicada por `runner-management`, propietario de esa vinculación. La consulta partirá del identificador de cuenta del actor, nunca de un `runnerId` suministrado por el cliente. `identity-access` no dependerá de `runner-management`; esta dirección evita un ciclo entre autenticación y gestión de corredores.

Las entradas sin usuario, como el worker, usarán una identidad de sistema explícita y acotada a su capacidad técnica. No podrán construir un actor administrador ni invocar casos de uso interactivos.

### Políticas en la capa de aplicación

Cada API de aplicación declarará una acción y aplicará denegación por defecto antes de leer o modificar recursos. Las capacidades simples de rol podrán reutilizar una política común, pero el módulo propietario evaluará el alcance que depende de sus datos.

Las anotaciones `@PreAuthorize` y reglas de rutas podrán actuar como primera barrera para capacidades gruesas. No serán la única autorización ni contendrán en SpEL las reglas de negocio o pertenencia. La decisión canónica residirá en políticas Java explícitas, tipadas y comprobables de forma aislada.

Una llamada entre módulos transportará el mismo `ActorContext` y el módulo receptor volverá a autorizar su propia acción. Proceder de otro módulo, de un controlador autenticado o de un proceso interno no concederá permisos.

### Alcance por recurso y consultas

Para operaciones de corredor, su identificador se derivará de la cuenta del actor autenticado mediante `runner-management` y no se aceptará como selector de propiedad desde la solicitud. Un identificador de plan, publicación, entrenamiento o seguimiento recibido del cliente será solo un criterio de localización.

El alcance se aplicará en la consulta jOOQ/JDBC o en una actualización condicionada, por ejemplo mediante destinatario efectivo y corredor autenticado. No se recuperarán filas globales para filtrarlas posteriormente. Las listas, conteos, cursores, búsquedas y relaciones anidadas usarán el mismo predicado de autorización que las consultas individuales.

Las políticas que necesiten comprobar varias condiciones lo harán dentro de la misma transacción que la operación protegida cuando una modificación concurrente pueda invalidar la decisión. No se separará la autorización de la mutación mediante una comprobación previa fuera de esa transacción.

Una falta de capacidad independiente del recurso devolverá acceso denegado. Un recurso inexistente o fuera del alcance del corredor devolverá el mismo resultado de no encontrado conforme a `ADR-0004`, sin revelar existencia mediante mensaje, estado, tiempo deliberado ni conteos laterales.

### Contrato y frontend

OpenAPI declarará requisitos de sesión y CSRF, pero no intentará describir como suficientes las reglas de autorización por recurso. El frontend podrá ocultar acciones según capacidades devueltas por el backend, pero esas capacidades serán solo una ayuda de presentación y no una concesión reutilizable.

No se enviará el rol en una cabecera controlada por el cliente ni se confiará en datos almacenados por la SPA. Cambios de estado de cuenta o revocación de sesión se comprobarán en servidor según `ADR-0003`.

### Pruebas y observabilidad

Cada caso de uso protegido tendrá una matriz de pruebas con rol, acción, propiedad, estado del recurso y resultado. Las pruebas cubrirán acceso directo por identificador, listas, cursores, búsquedas, relaciones, llamadas internas y carreras entre comprobación y modificación.

Los eventos de seguridad registrarán actor, acción, módulo, resultado y correlación cuando sea necesario para investigar denegaciones, sin registrar cookies, secretos ni contenido personal innecesario. Las métricas no incluirán identificadores de corredor como etiquetas de alta cardinalidad.

## Alternativas consideradas

### Alternativa A: `@PreAuthorize` y SpEL como política completa

Se descarta porque dispersa reglas, dificulta probar consultas por alcance y acopla la política de dominio al framework. Las anotaciones se admiten únicamente como defensa gruesa complementaria.

### Alternativa B: Obtener la identidad desde `SecurityContextHolder` dentro de cada servicio

Se descarta como contrato de aplicación. Es una dependencia global y oculta que dificulta probar casos de uso, ejecutar procesos internos y reconocer qué operaciones requieren actor. El adaptador extraerá el actor y lo pasará explícitamente.

### Alternativa C: Autorizar solo en controladores o filtros

Se descarta porque las APIs de aplicación también se invocan entre módulos y desde entradas no HTTP. Una ruta autenticada no protege accesos internos ni garantiza alcance por recurso.

### Alternativa D: Filtrar después de consultar

Se descarta porque puede exponer datos en memoria, métricas, paginación o errores y desperdicia trabajo. El alcance debe formar parte de la consulta o actualización.

### Alternativa E: Motor externo de políticas o PostgreSQL RLS

Se descarta para el PMV. No existe una matriz dinámica que justifique otro lenguaje o servicio y `ADR-0012` ya descarta RLS. Incorporarlos exigiría otro ADR y no permitiría retirar la política de aplicación.

## Consecuencias

- Los casos de uso expresarán explícitamente que operan en nombre de un actor.
- Las políticas podrán probarse sin levantar HTTP ni depender de contexto global.
- Las consultas jOOQ deberán incorporar predicados de alcance; omitirlos será un defecto de seguridad que las pruebas de matriz deben detectar.
- La doble barrera de ruta y aplicación añade algo de duplicación, pero la primera será gruesa y la segunda canónica.
- Las llamadas internas no serán privilegiadas por defecto y deberán propagar actor, lo que hace visibles sus requisitos de seguridad.
- Los módulos que necesiten alcance de corredor dependerán de la API de `runner-management`; no podrán leer su esquema ni duplicar la asociación cuenta-corredor.
- La identidad de sistema permitirá procesos internos sin simular usuarios, pero deberá mantenerse mínima y separada de capacidades interactivas.
- No usar RLS deja la última defensa en la aplicación y sus pruebas, riesgo ya aceptado en `ADR-0012`.

## Requisitos relacionados

- `RF-02`
- `RF-03`
- `RF-05`
- `RF-06`
- `RF-07`
- `RF-08`
- `RF-09`
- `RF-14`
- `RF-16`
- `RF-17`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-01`: administración, segmentación y publicación requieren capacidades explícitas.
- `D-03`: no existe alcance por organización porque el PMV es single-club.
- `D-06`: publicación y republicación están reservadas a entrenador y administrador.
- `D-07`: el corredor escribe solo su seguimiento y entrenador y administrador consultan globalmente.
- `D-08`: entrenador dispone de alcance global y cada corredor queda aislado.

## Validación prevista

- Probar cada fila de la matriz de `ADR-0004` para los tres roles y para usuario no autenticado.
- Probar que ningún endpoint de corredor acepta otro `runnerId` como concesión de acceso.
- Probar acceso directo por UUID ajeno, listas, filtros, conteos, cursores y recursos anidados sin diferencias que revelen existencia.
- Probar que cada módulo vuelve a autorizar llamadas internas con el actor recibido.
- Probar que la asociación cuenta-corredor se resuelve mediante la API de `runner-management` y que un cambio de asociación se refleja sin duplicar estado en `identity-access`.
- Verificar mediante ArchUnit que aplicación y dominio no dependen de Spring Security ni acceden a `SecurityContextHolder`.
- Ejecutar carreras entre autorización y modificación para confirmar que las comprobaciones sensibles comparten la transacción adecuada.
- Revisar logs y métricas para impedir secretos, datos personales innecesarios y cardinalidad no acotada.

## Decisiones pendientes

- **Resuelto:** `ActorContext` será un parámetro explícito de los casos de uso y `SecurityContextHolder` quedará limitado a filtros y adaptación técnica.
- **Resuelto:** `@PreAuthorize` y las reglas de ruta serán una defensa gruesa; las políticas Java de cada módulo serán canónicas.
- **Resuelto:** worker y procesos internos usarán una identidad de sistema mínima, sin privilegios administrativos y limitada a capacidades técnicas explícitas.
- **Tratado por `ADR-0010` (Aceptado), bloqueante para producción:** los eventos de seguridad tendrán acceso restringido, `12` meses de conservación activa y posterior supresión o bloqueo restringido cuando proceda. Deberán completarse las evidencias de privacidad antes de datos reales.
