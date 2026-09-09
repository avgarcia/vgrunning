# Auditoría de la estructura de paquetería

**Alcance:** los 63 paquetes de `src/main/java/com/vgrunning/`, contrastados contra sus tres normas propias: `ADR-0026` (decisión aceptada), `.agents/skills/arquitectura-hexagonal-y-modulith/references/reglas-de-paqueteria.md` (árbol canónico y matriz de dependencias) y las 14 reglas ArchUnit de `src/test/java/com/vgrunning/`.

---

## Veredicto

**La estructura es válida y no necesita rediseño.** Es coherente con su propio ADR, `ApplicationModules.verify()` la acepta sin ciclos, y la taxonomía `input`/`output` demuestra que generaliza más allá de web y persistencia: `runnermanagement/infrastructure/input/event/RunnerInvitationActivationListener` es una entrada que no es HTTP y encaja sin forzar el árbol.

El problema no está en el diseño de la estructura, sino en la **cobertura de su enforcement**, y la correlación es exacta:

> De las diez filas de la matriz de dependencias de la skill, ArchUnit ejecuta tres completas y dos parciales. **Las cuatro divergencias reales que he encontrado están todas en filas que ningún gate comprueba.** La estructura no se ha degradado donde hay regla ejecutable; se ha degradado donde solo hay prosa.

Eso convierte la propuesta en un problema de gates, no de reorganización. Mover paquetes sería tratar el síntoma.

---

## Cobertura real de la matriz de dependencias

`reglas-de-paqueteria.md:97-108` define diez filas. Contrastadas con las reglas ArchUnit existentes:

| Fila de la matriz | Regla ejecutable | Divergencia encontrada |
| --- | --- | --- |
| `api` | — | 6 paquetes `api` vacíos (H4) |
| `domain` | Parcial · `DomainIndependenceTest` + `domainMustNotDependOnApplicationOrInfrastructure` | — |
| `application.port.in` | — | **Sí** (H3) |
| `application.port.out` | — | **Sí** (H3) |
| `application.service` | Parcial · `applicationMayNotDependOnFrameworksOrPersistence` | — |
| `application.mapper` | Sí · `applicationMappersMustRemainPure` | — |
| `infrastructure.input` | Sí · `infrastructureInputMustNotUseOutputPorts` | — |
| `infrastructure.output` | Sí · `infrastructureOutputMustNotUseInputPorts` | **Sí** (H5), no detectable por esa regla |
| `infrastructure.security` | — | Sin violación hoy, sin gate (H2) |
| `infrastructure.configuration` | n/a (puede depender de todo) | Reparto ambiguo (H1) |

Las filas `domain` y `application.service` figuran como parciales porque las reglas prohíben frameworks y capas, pero no el calificador **«del módulo»** que la matriz sí exige. Esa diferencia es justo por donde entran H3 y H5.

---

## Divergencias verificadas

### H1 · La única regla de clasificación que ofrece una elección sin criterio

`SKILL.md:30-39` es un procedimiento de nueve reglas para decidir el paquete de una clase antes de crearla. Ocho son deterministas. La novena no:

> `9.` Spring, seguridad, properties o composición: `infrastructure.security` **o** `infrastructure.configuration`.

No hay criterio para elegir entre las dos. El resultado es observable: la seguridad de `identityaccess` vive partida en dos raíces.

- En `infrastructure.configuration`: `SecurityConfiguration`, `RequestSecurityInfrastructureConfiguration`, `SessionSecurityInfrastructureConfiguration`.
- En `infrastructure.security`: los diez componentes que esas tres clases construyen (`OriginValidationFilter`, `ActorContextRequestFilter`, `CurrentSessionIdentityResolver`, `SessionActorMapper`, `LoginRateLimiter`, `SessionPrincipal`, `Argon2PasswordHasher`, `AesGcmInvitationPayloadProtector`, y dos excepciones).

El reparto que el código aplica de facto es correcto y consistente —`@Configuration`/`@Bean` a un lado, lo que se instancia al otro—, pero **no está escrito en ninguna parte**. Nadie lo decidió: emergió.

### H2 · `infrastructure.security` es la única fila de la matriz sin regla ejecutable

Contenido real por rol, verificado siguiendo los usos:

| Rol | Clases |
| --- | --- |
| Lado de entrada | `OriginValidationFilter`, `ActorContextRequestFilter` (ambos `OncePerRequestFilter`), `CurrentSessionIdentityResolver`, `SessionActorMapper`, `LoginRateLimiter`, `SessionPrincipal`, `AuthenticationRequiredException`, `CsrfValidationException`, `RateLimitedException` |
| Lado de salida | `Argon2PasswordHasher` → implementa `application.port.out.PasswordHasher`; `AesGcmInvitationPayloadProtector` → implementa `application.port.out.InvitationPayloadProtector` |

**No hay violación hoy.** La matriz (`reglas-de-paqueteria.md:107`) permite explícitamente ambos roles: *«un puerto de entrada que invoca o un puerto de salida que implementa»*. El paquete cumple.

Lo que falta es el gate. `infrastructureInputMustNotUseOutputPorts` e `infrastructureOutputMustNotUseInputPorts` apuntan a `..infrastructure.input..` y `..infrastructure.output..`; ninguna alcanza `..infrastructure.security..`. Nada impide que mañana una clase de ese paquete invoque `application.service` directamente o cruce los dos sentidos en la misma clase, que es precisamente lo que la fila 107 prohíbe en prosa.

Conviene ser exacto sobre el alcance del hueco: `JooqBoundaryTest` y `OpenApiBoundaryTest` **sí** cubren `infrastructure.security`, porque restringen todo lo que queda fuera de su paquete permitido. Son exactamente dos reglas direccionales las que no llegan.

### H3 · Puertos tipados con la `api` de otro módulo

La matriz (`reglas-de-paqueteria.md:101-102`) permite a `application.port.in` y `application.port.out` usar «JDK, `api` y `domain` **del módulo**». Dos puertos incumplen ese calificador:

```java
// identityaccess/application/port/out/InvitationPayloadProtector.java
import com.vgrunning.notificationdelivery.api.request.EncryptedValue;
EncryptedValue protect(String plaintext);

// runnermanagement/application/port/in/CreateRunnerUseCase.java
import com.vgrunning.identityaccess.api.actor.ActorContext;
CreatedRunner create(ActorContext actor, UUID idempotencyKey, CreateRunner command);
```

Los dos casos son distintos y merecen respuestas distintas:

- **`ActorContext` en `port.in` es sano.** Es el tipo de actor que atraviesa todo el sistema, publicado como contrato precisamente para eso. Aquí la matriz es más estricta que la práctica que pretendía describir: es la **regla** la que necesita ajuste, no el código.
- **`EncryptedValue` en `port.out` no lo es.** `identityaccess` no puede cifrar nada sin `notification-delivery` en el classpath, y el sobre AEAD genérico (`keyId`, `nonce`, `ciphertext`) lo posee el módulo que lo *consume*, no el que lo *produce*. La dirección de propiedad está invertida.

Restricción a tener en cuenta antes de proponer nada: **el tipo no se puede mover a `identityaccess.api`**, porque `identity-access` ya declara `allowedDependencies = {"notification-delivery::api"}` y se crearía un ciclo Modulith. Y `reglas-de-paqueteria.md:56` prohíbe expresamente un cajón `shared` o `common`. La única salida compatible con ambas restricciones es que cada módulo posea su tipo y la conversión ocurra en el punto de llamada.

### H4 · Ocho `@NamedInterface("api")`, tres publican tipos

Solo `identityaccess.api.actor`, `identityaccess.api.provisioning` y `notificationdelivery.api.request` contienen tipos. Los otros cinco paquetes anotados (`classificationsegmentation`, `planning`, `publication`, `runnerportal`, `trackingreview`) contienen únicamente su `package-info.java`, igual que los dos padres vacíos `identityaccess.api` y `notificationdelivery.api`.

Y el grafo declarado apunta a ellos: cinco módulos declaran `allowedDependencies` sobre `runner-management::api`, que hasta hoy es un paquete sin un solo tipo.

**Resuelto empíricamente.** Las dos primeras ejecuciones fueron inválidas (`compileJava`/`test` en `UP-TO-DATE`, Modulith leyendo bytecode anterior). Una tercera con `./gradlew clean test --tests "com.vgrunning.ApplicationModularityTest"` forzó la invalidación completa: `clean` + regeneración de jOOQ/OpenAPI + recompilación, `12/12` tareas ejecutadas, ninguna `UP-TO-DATE`. Con `runnermanagement/api/package-info.java` retirado del árbol, **`BUILD SUCCESSFUL`**: `ApplicationModules.verify()` no falla, y `recognizesExactlyTheEightAcceptedModules` sigue reconociendo los ocho módulos (el módulo lo define el `package-info.java` de su raíz, no el de `api`).

**Conclusión: los `api` vacíos no son load-bearing.** Modulith no comprueba en `verify()` que un `allowedDependencies` señale un `@NamedInterface` con contenido real, solo que no se crucen internos. Cinco módulos pueden declarar `allowedDependencies = {"runner-management::api", ...}` apuntando a un paquete sin una sola interfaz y el grafo se acepta igual.

Aplica la segunda rama de la propuesta: **son ruido, se pueden retirar.**

### H5 · Un contrato intermodular servido desde `infrastructure.output`

`notificationdelivery` tiene dos de las cuatro raíces (`api` e `infrastructure`), sin `application` ni `domain`, y su `infrastructure` no tiene rama `input`. Su interfaz publicada `api.request.NotificationRequestApi` la implementa directamente `infrastructure.output.persistence.jooq.JooqNotificationRequestApi`.

`SKILL.md:56` dice: *«Una salida implementa `application.port.out`»*. Esta implementa un tipo de `api`. La regla ArchUnit correspondiente no puede verlo, porque solo prohíbe que una salida dependa de `application.port.in`, y aquí no hay ningún `application`.

Las consecuencias de diseño de esto están analizadas en [auditoría de arquitectura hexagonal](auditoria-arquitectura-hexagonal.md); a efectos de paquetería basta con registrar que es el único módulo cuya `api` no tiene detrás una capa de aplicación, y que `ADR-0031` ya prevé construir ese worker, momento en el que la rama `application` aparece de forma natural.

### H6 · La taxonomía de cuatro raíces solo existe completa en un módulo

| Módulo | `api` | `domain` | `application` | `infrastructure` |
| --- | --- | --- | --- | --- |
| `identityaccess` | ✔ | ✔ | ✔ | ✔ |
| `runnermanagement` | ✔ (vacío) | — | ✔ | ✔ |
| `notificationdelivery` | ✔ | — | — | ✔ |
| `runnerportal` | ✔ (vacío) | — | — | ✔ |
| Los otros 4 | ✔ (vacío) | — | — | — |

`ADR-0026:17` dice que cada módulo «conservará» las cuatro raíces, y la skill añade «no crees directorios vacíos» (`:44`), referido a los subpaquetes de `domain`. Las dos frases juntas no dicen si un módulo sin dominio real debe tener el paquete o no. En la práctica se ha aplicado el criterio razonable —no crearlo vacío— y no hay nada que corregir aquí salvo la ausencia de dominio en `runnermanagement`, que es un hallazgo de diseño, no de paquetería, y ya está recogido en el otro informe.

---

## Propuesta

Ordenada por relación coste/beneficio. Ninguna de las tres primeras mueve un solo fichero ni requiere un ADR nuevo: convierten en ejecutable lo que las normas ya dicen en prosa.

### 1. Cerrar el hueco de `infrastructure.security` con dos reglas ArchUnit

Es la fila 107 de la matriz, escrita como gate. Añadir a `HexagonalLayeringTest`:

```java
@ArchTest
static final ArchRule securityMustNotCoordinateApplicationServices =
        noClasses()
                .that()
                .resideInAnyPackage("..infrastructure.security..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..application.service..", "..infrastructure.output.persistence..")
                .allowEmptyShould(false);
```

**Coste:** una regla. **Beneficio:** el tercer brazo de `infrastructure` deja de ser el único sin gate direccional. Pasa hoy sin cambiar nada, que es exactamente lo que debe hacer un gate que fija el statu quo correcto.

### 2. Fijar el criterio que resuelve la regla 9

Añadir a `reglas-de-paqueteria.md` la frase que falta, escribiendo lo que el código ya hace:

> `@Configuration`, `@Bean` y `@ConfigurationProperties` van siempre a `infrastructure.configuration`, aunque su materia sea seguridad. Los componentes que esas clases construyen van a `infrastructure.security`.

**Coste:** una frase en un documento operativo versionado. **No requiere ADR:** la skill es la norma operativa, no una decisión aceptada, y esto desambigua `ADR-0026:25` sin contradecirlo. **Beneficio:** la novena regla de clasificación deja de ser la única que admite dos respuestas.

### 3. Devolver a `identityaccess` la propiedad de su sobre cifrado (H3)

Declarar `SealedPayload(String keyId, byte[] nonce, byte[] ciphertext)` en `identityaccess.domain` y cambiar la firma del puerto:

```java
SealedPayload protect(String plaintext);
```

`ProvisionRunnerAccountService` convierte a `EncryptedValue` al construir el `CreateNotificationRequest`, que es una llamada legítima a la `api` de otro módulo.

**Precisión sobre lo que esto compra:** no elimina el acoplamiento —el servicio seguirá importando `notificationdelivery.api.request` para invocar esa API, y debe hacerlo—. Lo que se limpia es la **firma del puerto**: `InvitationPayloadProtector` deja de exigir `notification-delivery` en el classpath para expresar un concepto criptográfico genérico.

**Coste:** un record y tres líneas. **No requiere ADR:** cumple una regla de la skill que hoy se incumple.

### 4. Relajar el calificador «del módulo» para `api` en la matriz

`ActorContext` en `CreateRunnerUseCase` es correcto y debe seguir. La matriz debería decir «`api` del módulo, o la `api` de un módulo declarado en `allowedDependencies`», que es lo que Modulith ya verifica. Si no se ajusta, la regla queda como una norma que se incumple a propósito, que es peor que no tenerla.

**Coste:** una frase. **Beneficio:** hace posible convertir las filas `port.in`/`port.out` en reglas ArchUnit, hoy inescribibles porque el texto vigente prohibiría un patrón sano.

### 5. Retirar los `api` vacíos (confirmado empíricamente, ver H4)

`ApplicationModules.verify()` acepta el grafo sin `runner-management/api/package-info.java`, confirmado con `clean test` de invalidación forzada. No son estructura portante: son ruido que desdibuja una señal útil, porque hoy es imposible distinguir de un vistazo un módulo que publica contrato de uno que solo reserva el sitio.

**Acción:** borrar los seis `api` vacíos (`classificationsegmentation`, `planning`, `publication`, `runnerportal`, `trackingreview`, y `runnermanagement`) y los dos padres vacíos (`identityaccess.api`, `notificationdelivery.api` como paquete propio, no sus subpaquetes con contenido). Conservar `allowedDependencies` intacto en los `package-info.java` de módulo: son declaraciones válidas de intención arquitectónica aunque el contrato aún no exista, y `ADR-0014` las respalda. Recrear cada `api.<concepto>` cuando el módulo publique su primer tipo real, tal como ya ocurrió en `identityaccess` y `notificationdelivery`.

**Coste:** ocho ficheros borrados, cero ADR, cero cambio de comportamiento verificado. **Beneficio:** un `api` presente vuelve a significar «este módulo publica contrato», no «este módulo existe».

### Alternativa evaluada y descartada: disolver `infrastructure.security`

Se puede argumentar que `infrastructure.security` es un cajón por rol —siete clases de entrada y dos de salida— y repartirlo en `infrastructure.input.web.security` e `infrastructure.output.security`. El resultado sería que toda clase caería bajo una rama ya gobernada por las reglas direccionales existentes, sin necesidad de la regla nueva del punto 1.

**Se descarta.** El coste es mover diez clases de producción más cuatro de prueba (`OriginValidationFilterTest`, `CurrentSessionIdentityResolverTest`, `LoginRateLimiterTest`, `Argon2PasswordHasherTest`), actualizar sus imports **y refinar `ADR-0026`**, cuya línea 25 asigna explícitamente Spring Security, Spring Session, CSRF, rate limiting y criptografía a `infrastructure.security`. Sería un cambio de decisión, no una aclaración, y exigiría el ADR correspondiente conforme a la norma de `docs/adr/README.md`.

El punto 1 compra la misma garantía con una regla y cero movimientos. Además, el paquete es coherente **por concern** aunque sea mixto por rol: el ciclo técnico de autenticación, sesión, CSRF y rate limiting es una unidad que la matriz reconoce explícitamente (`reglas-de-paqueteria.md:110`).
