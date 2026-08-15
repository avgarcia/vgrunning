# ADR-0017: API HTTP orientada a recursos y semántica REST

**Estado:** Aceptado
**Fecha:** 2026-08-15
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0013` establece OpenAPI `3.1` contract-first, generación de servidor y cliente, Problem Details, Spectral y `oasdiff`, pero no decide cómo identificar recursos ni cómo trasladar los casos de uso a HTTP. Durante el diseño detallado de identidad y acceso aparecieron rutas como `account-activations`, `password-resets` o `email-change-confirmations`: aunque estaban escritas como nombres, representaban acciones nominalizadas y producían una API RPC sobre HTTP.

Resolver cada módulo de forma independiente permitiría mezclar rutas orientadas a recursos con verbos, nombres de acciones, prefijos de rol y transiciones ad hoc. Esa inconsistencia afectaría al contrato, al cliente generado, a la autorización, a las pruebas y a cualquier evolución compatible de la API.

El PMV usa una SPA y no necesita que clientes desconocidos descubran dinámicamente todo el protocolo mediante hipermedia. Exigir HATEOAS completo añadiría representaciones, enlaces y estados de navegación sin una necesidad funcional o de integración que compense su coste.

## Decisión

La API HTTP propia del PMV seguirá un diseño orientado a recursos con la semántica del nivel `2` del modelo de madurez de Richardson: recursos identificables, métodos HTTP y códigos de estado coherentes. No se afirmará que implementa REST completo en sentido estricto y HATEOAS no será obligatorio durante el PMV.

[Guía de diseño de API HTTP](../api-design-guidelines.md) será la norma operativa que materializa este ADR. OpenAPI seguirá siendo la fuente de verdad de cada contrato conforme a `ADR-0013`.

### Ámbito

La decisión se aplica a todas las operaciones HTTP propias consumidas por la SPA o por futuros clientes del producto, con independencia del módulo que las implemente.

Quedan fuera:

- APIs Java internas entre módulos, regidas por `ADR-0014`;
- comandos de bootstrap o recuperación operativa sin endpoint HTTP;
- recursos estáticos, fallback de la SPA y endpoints técnicos estándar del framework;
- webhooks entrantes cuyo formato o ruta estén condicionados por un proveedor externo.

Una exclusión no permite exponer casos de uso de producto como RPC. Cualquier nueva excepción funcional requiere una decisión arquitectónica explícita.

### Recursos y rutas

- Cada ruta identifica un recurso o una colección con identidad, representación y ciclo de vida comprensibles.
- Los segmentos usan nombres en inglés, minúsculas, plural y `kebab-case`.
- Las rutas no contienen verbos ni nombres de acciones como `activate`, `deactivations`, `password-resets` o `email-change-confirmations`.
- Las rutas no codifican roles ni mecanismos mediante prefijos como `/admin`, `/trainer`, `/runner` o `/auth`. La autorización se declara en OpenAPI y se aplica en backend.
- `/me` y `/current` se permiten como identificadores contextuales de un recurso ya determinado por la cuenta o sesión autenticada.
- Una relación subordinada se expresa mediante anidamiento solo cuando el recurso hijo depende realmente del padre para identificarse o autorizarse.
- Un proceso puede modelarse como recurso únicamente si tiene identidad, estado y ciclo de vida reales y observables. Convertir un verbo en sustantivo no satisface esta regla.

Los recursos no tienen que corresponder uno a uno con tablas ni agregados. La representación HTTP pertenece al contrato externo; el módulo propietario decide cómo materializarla sin exponer su persistencia.

### Semántica HTTP

- `GET` consulta y es seguro e idempotente. No recibe cuerpo ni crea estado de negocio.
- `POST` crea un recurso dentro de una colección. Devuelve `201 Created` y `Location` cuando puede exponer la nueva identidad, o `202 Accepted` cuando la seguridad o asincronía exige una respuesta indistinguible.
- `PUT` reemplaza una representación completa conocida y es idempotente. No se usará para comandos.
- `PATCH` modifica parcialmente un recurso o solicita una transición mediante su estado. El cuerpo será cerrado y solo admitirá propiedades documentadas.
- `DELETE` elimina o revoca el recurso dirigido y se comporta de forma idempotente desde la perspectiva del cliente.

Las transiciones se validan contra el estado actual y sus invariantes. Repetir una petición que ya alcanzó el estado solicitado no vuelve a emitir desafíos, notificaciones ni otros efectos externos. Cuando una modificación concurrente pueda provocar pérdida de actualizaciones, el contrato expondrá `ETag` y exigirá `If-Match`.

### Seguridad y privacidad

- Un identificador en la ruta localiza un candidato, pero nunca concede acceso.
- Contraseñas, secretos y tokens de un solo uso solo aparecen en cuerpos protegidos; no se aceptan en rutas ni parámetros de consulta de la API.
- Las respuestas de flujos sensibles permanecen indistinguibles cuando revelar existencia, estado o propiedad permita enumeración.
- La autorización por rol y alcance sigue `ADR-0004` y `ADR-0015`; la misma ruta puede producir resultados distintos según las capacidades del actor sin duplicarse por rol.
- Logs, métricas, trazas, Problem Details y ejemplos no contienen secretos ni datos personales innecesarios.

### Representaciones, consultas y errores

- Los nombres de propiedades y valores enumerados son estables y usan el lenguaje ubicuo del módulo propietario.
- Colecciones usan paginación por cursor cuando puedan crecer y filtros explícitos; no se crean rutas distintas para cada búsqueda.
- Las respuestas de error usan `application/problem+json`, tipos estables y códigos de aplicación documentados.
- OpenAPI declara seguridad, CSRF, estados, restricciones, cabeceras, ejemplos y `operationId` estables.
- Los enlaces hipermedia pueden añadirse cuando aporten valor, pero no son condición general del PMV.

### Gobierno y controles

Antes de implementar una operación se revisarán recurso, representación, método, estado HTTP, seguridad, idempotencia y compatibilidad en OpenAPI.

Cuando se cree `api/openapi/`, el mismo cambio incorporará Spectral y sus reglas propias para comprobar al menos:

- segmentos en `kebab-case` y prefijos prohibidos;
- ausencia de secretos en ruta o query;
- ausencia de cuerpo en `GET`;
- `operationId` únicos y estables;
- Problem Details y respuestas mínimas por método;
- definición de seguridad y CSRF donde corresponda.

Spectral no puede decidir de forma fiable si un nombre es una acción nominalizada o si existe un recurso real. Esa comprobación seguirá siendo un gate humano obligatorio. `oasdiff`, generación de cliente y pruebas de contrato completarán los controles de `ADR-0013`.

Una excepción a esta guía deberá explicar por qué el caso no puede modelarse sin distorsionar un recurso y necesitará un ADR nuevo o la sustitución explícita de este.

### Versionado inicial

La primera versión no incluirá un segmento como `/v1`: todas las operaciones propias partirán de `/api`. Frontend y backend se despliegan juntos y `oasdiff` bloqueará incompatibilidades accidentales. Cuando aparezca una necesidad real de mantener clientes incompatibles, otro ADR deberá definir transición, coexistencia, retirada y estrategia de versionado antes de publicar la ruptura.

## Alternativas consideradas

### Alternativa A: RPC sobre HTTP con rutas de acciones

Se descarta. Expone casos de uso como comandos, multiplica rutas para cada transición y desplaza semántica desde HTTP hacia nombres convencionales que cada módulo puede inventar de forma distinta.

### Alternativa B: Acciones nominalizadas como recursos

Se descarta. Nombres como `password-resets` o `account-activations` cumplen una regla sintáctica, pero no describen necesariamente recursos con identidad y ciclo de vida. Ocultan RPC sin corregir el modelo.

### Alternativa C: Prefijos por rol o mecanismo

Se descarta. `/admin/accounts` y `/auth/sessions` duplican o agrupan recursos por quién los usa o cómo se protegen. La autorización pertenece al contrato de seguridad y a las políticas, no a la identidad del recurso.

### Alternativa D: REST completo con HATEOAS obligatorio

Se descarta para el PMV. Aportaría descubrimiento dinámico y menor acoplamiento a rutas, pero la SPA se genera desde OpenAPI y no existe integración pública o cliente desconocido que justifique el coste de hipermedia uniforme.

### Alternativa E: GraphQL o una API de comandos

Se descarta porque no resuelve una necesidad de consultas arbitrarias ni de mensajería y contradice el contrato OpenAPI HTTP ya aceptado. Introduciría otro modelo de contrato, autorización, caché y pruebas.

### Alternativa F: Convenciones independientes por módulo

Se descarta porque una única API pública no debe reflejar fronteras internas mediante dialectos diferentes. Los módulos conservan propiedad de sus recursos, pero comparten la misma interfaz HTTP.

## Consecuencias

- Los contratos de todos los módulos usarán una semántica coherente y revisable antes de implementar.
- El mismo recurso conservará la misma URL para distintos roles; la autorización deberá probarse con rigor porque la ruta no actúa como barrera accidental.
- Algunos flujos exigirán identificar recursos como invitaciones, credenciales, direcciones o desafíos que antes se ocultaban detrás de un comando.
- Diseñar recursos y transiciones requiere más trabajo inicial que publicar métodos de aplicación como endpoints, pero evita acoplar el contrato a cada caso de uso.
- Las representaciones HTTP podrán diferir del modelo persistente y exigir adaptación explícita.
- La mayoría de reglas sintácticas podrá automatizarse, pero la detección de acciones nominalizadas seguirá necesitando revisión humana.
- No exigir HATEOAS mantiene el cliente acoplado al contrato OpenAPI y sus rutas; esa limitación se acepta para el PMV.
- Las excepciones tendrán más coste documental, de forma deliberada, para impedir una degradación silenciosa hacia RPC.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20` que se expongan mediante HTTP.

## Decisiones de Fase 1 relacionadas

- `D-01` a `D-08`: los recursos HTTP materializan los conceptos y permisos aceptados sin cambiar sus reglas funcionales.

## Validación prevista

- Revisar cada diseño detallado y contrato OpenAPI contra la guía de API.
- Demostrar que una operación nueva se expresa mediante recurso, representación y método sin verbos, acciones nominalizadas o prefijos de rol.
- Hacer fallar Spectral con un prefijo prohibido, un secreto en query, un cuerpo en `GET`, un `operationId` duplicado y una respuesta sin Problem Details.
- Usar `oasdiff` para bloquear un cambio incompatible no acompañado de una transición explícita.
- Generar y compilar servidor Spring MVC y cliente TypeScript desde el contrato.
- Probar métodos, códigos, cabeceras, idempotencia, precondiciones, seguridad y respuestas indistinguibles.
- Registrar en cada PR de API el informe de revisión humana definido por la guía.

## Decisiones pendientes

No quedan decisiones pendientes dentro del alcance de este ADR.

- **Resuelto:** las rutas usan nombres en inglés, plural y `kebab-case`; `/me` y `/current` son identificadores contextuales permitidos.
- **Resuelto:** la API inicial usa `/api` sin `/v1`; una ruptura real exigirá decidir explícitamente su estrategia de versionado.
- **Resuelto:** `ETag` e `If-Match` se exigen solo cuando una modificación concurrente pueda provocar pérdida de actualizaciones.
- **Resuelto:** webhooks impuestos por proveedores, endpoints técnicos, recursos estáticos y comandos operativos quedan fuera del ámbito, sin permitir RPC para casos de uso de producto.
- **Resuelto:** repetir una transición hacia un estado ya alcanzado no vuelve a producir notificaciones, desafíos ni otros efectos externos.
