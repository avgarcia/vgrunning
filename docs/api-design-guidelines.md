# Guía de diseño de API HTTP

**Estado:** Vigente
**Fecha:** 2026-08-15

## Propósito

Materializar las reglas operativas de `ADR-0017` para diseñar, revisar e implementar la API HTTP pública. Esta guía complementa OpenAPI contract-first de `ADR-0013`; no sustituye las reglas funcionales, de autorización o privacidad de cada módulo.

Si una regla de esta guía contradice un ADR aceptado, prevalece el ADR. Cambiar el modelo arquitectónico requiere otro ADR; ampliar ejemplos o controles sin cambiar la decisión puede modificar esta guía.

## Nivel adoptado

El PMV usa una API HTTP orientada a recursos con semántica del nivel `2` del modelo de Richardson. HATEOAS no es obligatorio. No debe describirse como REST completo en sentido estricto.

## Identificar el recurso

Antes de proponer una ruta, responde:

1. ¿Qué concepto del dominio o de seguridad tiene identidad propia?
2. ¿Qué representación observable tiene?
3. ¿Qué estados y transiciones válidas gobierna el módulo propietario?
4. ¿La ruta seguiría teniendo sentido si cambiara el nombre del caso de uso?
5. ¿Se está creando un sustantivo únicamente para ocultar un comando?

Un proceso solo es recurso cuando tiene identidad, estado y ciclo de vida reales. `AccessChallenge` cumple la regla porque tiene propósito, generación, vigencia, consumo y reemplazo. `PasswordReset` no la cumple si solo nombra el acto de cambiar una contraseña.

## Convenciones de rutas

- Prefijo común: `/api`.
- Segmentos de colecciones en inglés, minúsculas, plural y `kebab-case`.
- Identificadores estables entre llaves en OpenAPI, como `{accountId}`.
- `/me` identifica la cuenta del actor y `/current` el recurso vigente del contexto.
- No usar verbos, nombres de acciones ni sufijos como `-requests`, `-confirmations`, `-resets` o `-activations` salvo que el concepto supere expresamente el test de recurso.
- No usar roles o mecanismos como `/admin`, `/trainer`, `/runner`, `/auth` o `/secured`.
- No incluir detalles de módulos, tablas, proveedores o tecnología en la URL.
- No incluir contraseñas, secretos o tokens en path o query.

La API inicial no incluye `/v1` ni otro segmento de versión. Una incompatibilidad futura se bloquea con `oasdiff` hasta que un ADR defina transición, coexistencia y retirada; no se introduce un prefijo de versión de forma preventiva ni silenciosa.

### Ejemplos

| Evitar | Usar | Motivo |
| --- | --- | --- |
| `POST /api/auth/account-activations` | `PATCH /api/invitations/{invitationId}` | Se modifica el estado de una invitación real. |
| `POST /api/auth/password-resets` | `PATCH /api/accounts/{accountId}/credentials/current` | Se sustituye la credencial identificada. |
| `POST /api/auth/email-change-confirmations` | `PATCH /api/accounts/{accountId}/email-addresses/{emailAddressId}` | Se verifica una dirección pendiente. |
| `POST /api/auth/password-reset-requests` | `POST /api/access-challenges` | Se crea un desafío con estado y caducidad propios. |
| `POST /api/admin/accounts/{accountId}/deactivations` | `PATCH /api/accounts/{accountId}` | Se cambia el `status` de la cuenta. |
| `GET /api/admin/accounts` | `GET /api/accounts` | El rol se aplica mediante autorización, no mediante la ruta. |

## Métodos y respuestas

| Método | Uso | Respuesta normal | Reglas |
| --- | --- | --- | --- |
| `GET` | Consultar recurso o colección. | `200 OK`; `304 Not Modified` cuando aplique caché condicional. | Seguro, idempotente y sin cuerpo de petición. |
| `POST` | Crear un recurso en una colección. | `201 Created` con `Location`; `202 Accepted` cuando no se pueda revelar creación o el procesamiento sea asíncrono. | No representa un verbo remoto. La repetición y su idempotencia deben documentarse. |
| `PUT` | Reemplazar por completo un recurso conocido. | `200 OK` o `204 No Content`. | Idempotente; no usar para actualizaciones parciales. |
| `PATCH` | Modificar propiedades o estado documentados. | `200 OK` o `204 No Content`. | Cuerpo cerrado; valida transición y concurrencia. No reejecuta efectos al repetir el estado alcanzado. |
| `DELETE` | Eliminar o revocar el recurso dirigido. | `204 No Content`. | Resultado idempotente desde el punto de vista del cliente. |

La creación devuelve una representación cuando el cliente la necesita. Una respuesta sin cuerpo usa `204`, no `200` vacío. Los flujos que ocultan existencia pueden devolver `202` aunque no creen ningún recurso observable.

## Estado, concurrencia e idempotencia

- Las transiciones se expresan con propiedades como `status`, no con rutas nuevas.
- OpenAPI enumera estados aceptados y el servidor rechaza transiciones inválidas con `409 Conflict`.
- Repetir el estado ya alcanzado no crea otra notificación, desafío o publicación.
- Una modificación donde la concurrencia pueda provocar pérdida de actualizaciones expone `ETag` y exige `If-Match`.
- `POST` con riesgo de duplicado lógico define clave idempotente, unicidad natural o tratamiento explícito de repetición.
- Los efectos externos se coordinan mediante las transacciones y outbox aceptadas, no mediante promesas de exactamente una vez en HTTP.

## Colecciones

- Las colecciones potencialmente grandes usan cursor opaco y límite acotado.
- Filtros y orden se expresan mediante parámetros documentados, no mediante rutas específicas por consulta.
- El cursor fija criterio y desempate estables y no incorpora datos personales legibles.
- La autorización se aplica dentro de consultas, conteos y cursores antes de devolver resultados.
- Una respuesta de colección conserva la misma forma aunque no existan elementos.

## Seguridad

- OpenAPI declara la sesión y CSRF de cada operación protegida.
- La URL nunca concede acceso; el backend valida actor, capacidad, propiedad y estado.
- Los UUID pueden localizar candidatos, pero no sustituyen autorización ni secreto.
- Contraseñas y secretos solo aparecen en cuerpos y nunca en ejemplos reales, logs, métricas, trazas o Problem Details.
- Un flujo anónimo sensible no diferencia cuenta inexistente, estado incompatible, secreto inválido o límite alcanzado cuando hacerlo permita enumeración.
- `/me` se resuelve desde la sesión; nunca acepta un `accountId` enviado como prueba de identidad.

## Problem Details

Todos los errores usan `application/problem+json` con:

- `type` estable y documentado;
- `status` HTTP coherente;
- `title` no sensible;
- código de aplicación estable cuando el cliente necesite decidir comportamiento;
- correlación opaca cuando sea útil para soporte.

No se devuelven trazas, excepciones, SQL, nombres de tablas, secretos ni detalles que permitan enumeración. Los errores de validación identifican campos solo cuando sea seguro hacerlo.

## Contrato OpenAPI

Cada operación define antes de implementar:

- recurso, método, ruta y `operationId` estable;
- seguridad, CSRF y alcance esperado;
- parámetros, cuerpos cerrados y restricciones;
- respuestas de éxito, error y límite;
- Problem Details y ejemplos sintéticos;
- idempotencia, concurrencia y cabeceras;
- paginación y filtros cuando corresponda;
- trazabilidad con requisito, diseño y ADR.

Los modelos generados se limitan a adaptadores. El contrato no expone entidades de dominio, tipos jOOQ ni estructuras de persistencia.

## Controles automáticos

El primer cambio que cree `api/openapi/` debe incorporar la configuración de Spectral. Como mínimo comprobará:

- formato de rutas y prefijos prohibidos;
- parámetros sensibles prohibidos en path y query;
- ausencia de request body en `GET`;
- `operationId` únicos;
- respuestas mínimas y Problem Details;
- esquemas cerrados donde el contrato no admita extensión;
- seguridad y CSRF declarados;
- ejemplos válidos y sin secretos.

El build ejecutará además generación de servidor y cliente, `oasdiff`, pruebas de contrato y las validaciones de `ADR-0013`.

## Revisión humana obligatoria

La revisión humana debe comprobar lo que el lint no puede decidir:

- existe un recurso real y no una acción nominalizada;
- la ruta no refleja un rol, pantalla, módulo o caso de uso;
- método, estado y efectos laterales son coherentes;
- autorización, concurrencia e idempotencia están completas;
- una excepción tiene ADR y transición explícita.

El informe de PR usará:

```markdown
## Revisión de API HTTP
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Evidencia: <operaciones OpenAPI y recursos afectados>
- Hallazgos: <acciones nominalizadas, semántica, seguridad o ninguno>
- Acción requerida: <cambio, ADR o ninguna>
- Revisor humano: Revisor de arquitectura
```

## Excepciones

Si un caso no puede expresarse con estas reglas sin deformar el dominio, no se resuelve inventando un verbo o un `status` artificial. Se documentan necesidad, alternativas, compatibilidad, seguridad y coste, y se aprueba otro ADR antes de incorporarlo al contrato.
