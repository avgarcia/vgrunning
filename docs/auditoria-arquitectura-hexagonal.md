# Auditoría de arquitectura hexagonal — Running Coach

**Alcance:** `src/main/java/com/vgrunning/` (8 módulos declarados, 3 con código: `identityaccess`, `runnermanagement`, `notificationdelivery`) y las fitness functions de `src/test/java/com/vgrunning/`. Todos los hallazgos citan fichero y línea verificados por lectura directa.

---

## Fugas de Dominio

### 1. El caso de uso de activación vive en SQL, no en la aplicación

`JooqInvitationRepository.accept()` (`:42-90`) no persiste: **ejecuta toda la transacción de negocio**. Decide la transición de estado (`pending_activation` → `active`), consume el desafío, confirma el correo de la cuenta, inserta la evidencia de declaración de mayoría de edad y registra la aceptación. Además lanza `InvitationNotAvailableException` (`:64`), una excepción de la capa de aplicación, desde infraestructura.

`AcceptInvitationService` (`:28-50`) solo valida entrada, compara el verificador y hashea la contraseña. El puerto se llama `InvitationRepository`, pero su contrato real es «haz la activación completa».

Peor: constantes de negocio y cumplimiento normativo quedan incrustadas en el adaptador jOOQ (`:77-80`):

```java
.set(ADULT_DECLARATION.ACTOR_KIND, "invitee")
.set(ADULT_DECLARATION.ORIGIN, "initial_activation")
.set(ADULT_DECLARATION.TEXT_VERSION, "ux-02-v0.1")
```

`ADR-0010` exige conservar «versión inmutable del texto mostrado» como evidencia jurídica. Esa versión se decide hoy en una clase de persistencia.

**Solución:** que `AcceptInvitationService` construya la evidencia y la transición, y que el puerto reciba datos ya decididos: `invitations.accept(invitation, passwordHash, AdultDeclaration, correlationId)`, donde `AdultDeclaration` es un record de aplicación o dominio con `actorKind`, `origin` y `textVersion`. El adaptador pasa a escribir lo que recibe. La excepción de negocio se lanza en el servicio a partir del recuento de filas devuelto por el puerto (`int` o `boolean`), no dentro del adaptador.

### 2. La regla de caducidad de la invitación solo existe en el `WHERE`

La vigencia se comprueba en SQL (`JooqInvitationRepository:32`, y otra vez en `:61`). `ActivationInvitation` transporta `expiresAt` hasta la aplicación (`InvitationRepository:42`), pero **`AcceptInvitationService` nunca lo lee**. Una regla temporal de negocio es hoy invisible para el dominio y no puede probarse sin base de datos.

**Solución:** comprobar la caducidad en el servicio contra un `Clock` inyectado (o un puerto `TimeProvider`) y dejar la condición SQL como defensa en profundidad, no como única fuente. Si no se va a comprobar arriba, eliminar `expiresAt` del record: hoy es un campo muerto que sugiere una validación que no ocurre.

### 3. Canonicalización de correo duplicada dentro del mismo método

`ProvisionRunnerAccountService:35-37` normaliza el mismo dato de entrada dos veces, con dos reglas distintas:

```java
EmailAddress email = EmailAddress.from(command.email());                          // strip → NFC → lower
String presentationEmail = Normalizer.normalize(command.email().strip(), NFC);    // strip → NFC, sin lower
```

Ambos valores se persisten como columnas separadas (`:47-48`). La mitad de `EmailAddress.canonicalize()` (`EmailAddress:24-27`) está reimplementada en línea porque el VO no expone la forma de presentación.

`CreateRunnerService:71-91` vuelve a normalizar y a pasar a minúsculas por su cuenta antes de delegar. No produce un valor divergente —el resultado acaba pasando por `EmailAddress`— pero mantiene una tercera copia de la misma regla.

**Solución:** que `EmailAddress` sea el único dueño de ambas formas: añadir `presentationValue()` al record y construirlo una sola vez (`EmailAddress.from(...)` devuelve las dos formas). `CreateRunnerService` deja de normalizar el correo y lo entrega tal cual.

### 4. `runnermanagement` no tiene capa de dominio

El módulo no tiene paquete `domain`. Las reglas de negocio viven como métodos estáticos privados del servicio: obligatoriedad de la declaración de mayoría de edad, no vacío tras normalizar, y la huella de idempotencia (`CreateRunnerService:71-106`). No son testeables ni reutilizables fuera de ese servicio.

**Solución:** un `RunnerName` (o `PersonName`) en `runnermanagement.domain` que valide y normalice, y mover la huella a un método del propio comando. Es también el sitio natural para el record compartido de la alerta de sobreingeniería nº1.

### 5. Política de contraseña sin dueño

`AcceptInvitationService:42-45` fija NFC y longitud `12..128` en línea, dentro del flujo de aceptación. Si mañana existe cambio de contraseña o recuperación, la regla se copia.

**Solución:** un `RawPassword` en `identityaccess.domain.account.valueobject` que normalice y valide en su constructor compacto, igual que ya hace `EmailAddress`.

### 6. Criptografía a medias entre puerto y método privado

Existe un puerto `PasswordHasher` para Argon2id, pero SHA-256 se resuelve con `MessageDigest.getInstance("SHA-256")` y su bloque `catch (NoSuchAlgorithmException) → IllegalStateException` **triplicado**: `AcceptInvitationService:52-59`, `ProvisionRunnerAccountService:72-79` y `CreateRunnerService:93-106`. La misma preocupación es un puerto en un caso y una estática privada en tres.

Además, la generación del secreto de invitación (`SecureRandom`, 32 bytes, Base64Url) está en la aplicación (`ProvisionRunnerAccountService:24, 66-70`).

**Solución:** un puerto `DigestPort`/`SecretGenerator` en `application.port.out` con su adaptador en `infrastructure.security`, junto a `Argon2PasswordHasher`. Elimina las tres copias y hace inyectable el aleatorio en pruebas.

### 7. El formato de la URL del frontend está en un servicio de aplicación

`ProvisionRunnerAccountService:61` construye `"/activar#i=%s&s=%s"`, y `:56` la clave lógica `"invitation:" + invitationId`. La ruta del SPA es conocimiento de presentación.

**Solución:** un puerto `ActivationLinkFactory` implementado en infraestructura, con la ruta en configuración. El servicio entrega `invitationId` y `secret`; el adaptador compone el enlace.

---

## Errores de Acoplamiento

**No hay fugas de tipos entre capas.** `JooqBoundaryTest` y `OpenApiBoundaryTest` impiden que los tipos jOOQ y los modelos OpenAPI salgan de sus paquetes, `HexagonalLayeringTest` cubre las nueve reglas de dirección, y las comprobé: no hay violación. El punto 3 del encargo (DTOs o modelos de BD atravesando fronteras) **está mecánicamente resuelto**.

Lo que esas reglas no capturan es el acoplamiento de *conocimiento*, no de tipos. Ahí sí hay tres problemas:

### 1. `notificationdelivery` implementa su API publicada directamente en jOOQ

`NotificationRequestApi` (`api/request/NotificationRequestApi.java:4-6`) es la interfaz que otros módulos consumen, y su única implementación es `JooqNotificationRequestApi` (`infrastructure/output/persistence/jooq/JooqNotificationRequestApi.java:14`). No hay capa de aplicación: el contrato intermodular se satisface desde persistencia.

El caso gemelo en `identityaccess` está resuelto al revés: `AccountProvisioningApi` lo implementa `ProvisionRunnerAccountService` (`application/service`, `:23`). Mismo rol arquitectónico, dos estratificaciones distintas. Cualquier regla futura (validación, cifrado, política de prioridad) no tiene dónde ir en `notificationdelivery` salvo dentro del adaptador SQL, que es exactamente cómo empezó el problema del hallazgo nº1 de la sección anterior.

**Solución:** `CreateNotificationRequestService` en `notificationdelivery.application.service` implementando `NotificationRequestApi`, con un `NotificationRequestRepository` en `application.port.out` que implemente el jOOQ actual. Es el mismo patrón ya escrito en `identityaccess`; no introduce concepto nuevo.

### 2. Las convenciones de MapStruct moldean la forma de los puertos

`InvitationRepository.ActivationInvitation` expone `accountId()` **y** `getAccountId()` para el mismo campo (`:30-36`), y `verifier` solo tiene `getVerifier()`. La duplicación existe porque MapStruct exige getters JavaBean; por eso la clase tampoco es un `record`. Lo mismo ocurre con `java.beans.@ConstructorProperties` en `RunnerCreationRepository.StoredCreation:22`.

Un requisito de la herramienta de mapeo está decidiendo la forma de un tipo de la capa de aplicación.

**Solución:** declarar los tipos de puerto como `record` y, donde MapStruct no resuelva el acceso, usar `@Mapping(source = "...", target = "...")` explícito o un método `default`. Si un tipo concreto no puede ser `record` por el `byte[]` defensivo, mantener un solo estilo de accesor (`getX()`), no los dos.

### 3. `ProvisionRunnerAccountService` promete atomicidad que no declara

Su Javadoc dice «deja una solicitud de correo en la misma transacción» (`:21`) y escribe en dos repositorios, uno de ellos de otro módulo (`:42` y `:53`). No declara `@Transactional`: depende de que **todos** sus llamantes lo hagan.

Hoy funciona —`CreateRunnerService:28` sí lo declara— pero la garantía es del llamante y nada la protege. Un segundo consumidor de `AccountProvisioningApi` sin transacción rompería la atomicidad en silencio.

**Solución:** anotar `provision()` con `@Transactional(propagation = MANDATORY)`. Documenta y **verifica** la exigencia en tiempo de ejecución sin abrir una transacción propia, y cumple la regla `transactionalBoundariesAreApplicationServices`.

---

## Alertas de Sobreingeniería

### 1. Un mapper MapStruct completo para copiar dos records idénticos

```java
record StoredRunner (UUID id, String givenName, String familyName, String status) {}  // port/out:37
record CreatedRunner(UUID id, String givenName, String familyName, String status) {}  // port/in:13
```

Componentes byte a byte idénticos. Para ir de uno a otro existen: la interfaz `RunnerCreationMapper`, su implementación generada, un `@Bean` de wiring y `RunnerCreationMapperTest`. Cuatro artefactos para una copia de cuatro campos.

**Alternativa simple:** un solo record en `runnermanagement.domain` (el módulo aún no tiene ese paquete; este sería su primer habitante legítimo, resolviendo también la fuga nº4), usado por ambos puertos. El mapper desaparece porque **deja de existir la frontera de representaciones que `ADR-0028` obliga a mapear**, no porque se incumpla el ADR.

> Nota sobre alternativas descartadas: que `RunnerCreationRepository.create()` devuelva `CreatedRunner` violaría `infrastructureOutputMustNotUseInputPorts`, y que `RunnerWebMapper` consuma `StoredRunner` violaría `infrastructureInputMustNotUseOutputPorts`. Un tipo en `domain` es la única de las tres opciones compatible con las reglas vigentes.

### 2. Un record que solo envuelve un `Optional`

`RunnerCreationRepository.Reservation` (`:16`) es `record Reservation(Optional<StoredCreation> existing) {}`. Su único uso es `reservation.existing()` (`CreateRunnerService:38`).

**Alternativa simple:** que `reserve(...)` devuelva `Optional<StoredCreation>`. Se elimina un tipo y una indirección sin perder nada.

### 3. Dos ficheros de fitness functions ejecutan las mismas dos reglas

`HexagonalPackageRootsTest` (`:21-38`) contiene:

- `noObsoleteHexagonalRootsRemain`, idéntica a `HexagonalLayeringTest.noObsoletePackagesRemain` (`:116-122`);
- `applicationPortsAreInterfaces`, idéntica a `HexagonalLayeringTest.applicationPortsAreTopLevelInterfaces` (`:95-104`).

Mismo `@AnalyzeClasses(packages = "com.vgrunning")`, mismas cláusulas. Su Javadoc anuncia un reparto de responsabilidades que no existe: es duplicación, y duplica coste de análisis en cada `fastGate`.

**Alternativa simple:** borrar `HexagonalPackageRootsTest.java`. `HexagonalLayeringTest` ya cubre ambas reglas. (`DomainIndependenceTest` **no** es duplicado: cubre dominio → frameworks, que `HexagonalLayeringTest` no cubre. Conservarlo.)

### 4. Once mappers MapStruct para dos flujos reales

Hay 11 interfaces `@Mapper` en `src/main/java` sirviendo, en la práctica, al alta de corredor y al inicio de sesión. El alta encadena cuatro saltos de mapeo (DTO OpenAPI → comando → record jOOQ → `StoredRunner` → `CreatedRunner` → DTO OpenAPI), de los cuales uno es la copia idéntica del punto 1.

**Alternativa simple:** aplicar el punto 1 y, en `AuthenticatedAccountMapper`, valorar si un constructor explícito no es más barato que interfaz + `@Bean` + `Mappers.getMapper()` + test para renombrar `id` → `accountId`. Este último sí es una proyección real (descarta `passwordHash` y `version`), así que es defendible mantenerlo; el del punto 1 no lo es.

Incoherencia menor de configuración: `AccountPersistenceMapper:13` usa `componentModel = "spring"` como literal mientras los otros seis usan `MappingConstants.ComponentModel.SPRING`.

### 5. Cinco de ocho módulos son cascarones vacíos

`classificationsegmentation`, `planning`, `publication`, `trackingreview` y `runnerportal` (salvo el resolver del SPA) solo contienen `package-info.java`. Es coherente con el estado del proyecto, pero conviene tenerlo presente al leer las métricas de cobertura y las reglas con `allowEmptyShould(true)`: varias pasan hoy de forma vacua.

**Alternativa simple:** ninguna acción sobre los módulos. Sí revisar, cuando se implementen, que `domainMustNotDependOnApplicationOrInfrastructure` y las dos reglas de frontera dejen de estar en `allowEmptyShould(true)`.

### Revisado y descartado como sobreingeniería

- **Interfaces `port/in` con una sola implementación.** No son ceremonia: `SessionHttpControllerTest:89-97` construye dobles a mano de `AuthenticateCredentialsUseCase` sin Mockito. La interfaz es la costura de prueba que hace eso trivial. Se mantienen.
- **Wiring manual con `@Bean` en `IdentityAccessInfrastructureConfiguration`.** Es el precio correcto —y consciente— de una capa de aplicación sin anotaciones de Spring, verificado por `applicationMayNotDependOnFrameworksOrPersistence`. Cambiarlo por `@Service` ahorraría líneas y perdería la propiedad que sostiene toda la estructura.
- **Separación `api/` vs `application/port/in`.** Responden a cosas distintas: `api/` es el contrato intermodular de Spring Modulith; `port/in`, el contrato local del caso de uso. La distinción se sostiene.
