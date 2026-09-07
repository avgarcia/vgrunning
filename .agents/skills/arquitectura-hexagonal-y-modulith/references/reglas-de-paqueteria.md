# Reglas de paquetería

## Estructura canónica

```text
com.vgrunning.<module>/
  package-info.java
  api/
    <concepto>/
  domain/
    <concepto>/
      aggregate/
      entity/
      valueobject/
      policy/
      service/
      event/
      exception/
  application/
    port/
      in/
      out/
    mapper/
    service/
    exception/
  infrastructure/
    input/
      web/
      scheduling/
      command/
    output/
      persistence/jooq/
      provider/
    security/
      session/
      csrf/
      ratelimit/
      password/
    configuration/
      web/
      synthetic/
```

No crees directorios vacíos. Dentro de `domain.<concepto>` añade `aggregate`, `entity`, `valueobject`, `policy`, `service`, `event` o `exception` únicamente cuando exista un tipo real de esa categoría.

## Contratos y modelos

- `api.<concepto>` contiene las interfaces públicas entre módulos y los tipos estables de sus contratos.
- Cada paquete concreto `api.<concepto>` publicado contiene un `package-info.java` con `@NamedInterface("api")`; no presupongas que la anotación de `api` publica sus subpaquetes de forma recursiva.
- `application.port.in` contiene interfaces de casos de uso consumidos únicamente por entradas del propio módulo.
- `application.port.in` y `application.port.out` contienen únicamente interfaces.
- Una firma de `api` solo puede usar tipos JDK o de la propia `api`; nunca expone tipos de `domain`, `application` o `infrastructure`.
- Un puerto local puede usar tipos JDK, de `domain` o de la propia `api` cuando expresen correctamente el contrato.
- Un comando o resultado exclusivo de un puerto puede declararse como tipo anidado en su interfaz.
- Si una interfaz o un tipo forma parte de un contrato intermodular, colócalo en `api.<concepto>`.
- No uses `application.model`, `dto`, `common`, `shared`, `helper` o `util` como cajones genéricos.

## Mapeo en fronteras

- Un mapeo hacia una representación HTTP se llama `toResponse`; el parámetro conserva el nombre del tipo fuente.
- Un mapeo hacia un tipo de dominio se llama `toDomain`.
- Para colecciones usa el plural correspondiente: `toResponses` y `toDomains`.
- No añadas sufijos de infraestructura a tipos de dominio para resolver colisiones de nombres; resuélvelas en la frontera mediante imports o nombres totalmente cualificados.

## Dominio

- `aggregate`: raíz que protege invariantes y consistencia de un conjunto de cambios.
- `entity`: concepto con identidad y ciclo de vida que no es raíz del agregado.
- `valueobject`: valor inmutable definido por sus atributos y validación.
- `policy`: regla de dominio que combina conceptos y no pertenece naturalmente a una entidad.
- `service`: operación de dominio sin estado que coordina varios conceptos y no pertenece naturalmente a una entidad.
- `event`: hecho de negocio ocurrido y expresado en pasado.
- `exception`: incumplimiento de una invariante del dominio.

Organiza primero por concepto y después por tipo. Una tabla no implica una entidad y una clase de datos no implica un value object.

## Aplicación

- Los puertos son interfaces, nunca implementaciones ni modelos.
- `application.service` implementa puertos de entrada, coordina dominio y puertos de salida y puede usar `@Transactional`.
- `application.mapper` contiene exclusivamente interfaces MapStruct que convierten contratos internos; no tiene componentes Spring ni efectos secundarios.
- `application.exception` expresa fallos semánticos sin `HttpStatus`, `ProblemDetail`, URI, cabeceras o tipos del framework.
- Aplicación no gestiona cookies, sesión HTTP, CSRF, rate limiting, serialización ni `SecurityContext`.

## Infraestructura

- `infrastructure.input.web`: controladores, tipos OpenAPI, validación del protocolo y mapeadores HTTP.
- `infrastructure.output.persistence.jooq`: implementaciones de puertos de persistencia, consultas y mapeadores jOOQ.
- `infrastructure.output.provider`: clientes de proveedores externos.
- `infrastructure.security`: Spring Security, Spring Session, CSRF, Bucket4j y criptografía.
- `infrastructure.configuration`: `@Configuration`, `@Bean`, `@ConfigurationProperties` y traducción global de excepciones de Spring MVC.

Un controlador puede depender directamente de un componente técnico de infraestructura. No añadas un puerto entre ambos salvo que el núcleo necesite realmente esa capacidad.

## Matriz de dependencias permitidas

| Origen | Dependencias permitidas |
| --- | --- |
| `api` | JDK y tipos de la propia `api` |
| `domain` | JDK y tipos del propio `domain` |
| `application.port.in` | JDK, `api` y `domain` del módulo |
| `application.port.out` | JDK, `api` y `domain` del módulo |
| `application.service` | puertos, `api` y `domain` del módulo; `@Transactional` |
| `application.mapper` | MapStruct, JDK, `api`, `domain` y contratos locales del módulo |
| `infrastructure.input` | `application.port.in`, `api`, tipos de protocolo y componentes técnicos concretos de `infrastructure.security` |
| `infrastructure.output` | el `application.port.out` que implementa, `api`, `domain` y la tecnología externa correspondiente |
| `infrastructure.security` | Spring Security y, según responsabilidad, un puerto de entrada que invoca o un puerto de salida que implementa |
| `infrastructure.configuration` | todas las piezas del módulo necesarias para componerlas |

Una entrada no invoca puertos de salida, repositorios ni jOOQ. Una salida no invoca puertos de entrada ni servicios de aplicación y no coordina casos de uso. La dependencia directa desde `infrastructure.input.web` hacia `infrastructure.security` se limita al ciclo técnico de autenticación, sesión, CSRF y rate limiting.

## Dependencias prohibidas

```text
domain        -> application
domain        -> infrastructure
application   -> infrastructure
application   -> OpenAPI, jOOQ, Servlet o Spring Security
module A      -> internos de module B
```

La única dependencia de Spring permitida en aplicación es `org.springframework.transaction.annotation.Transactional` en servicios de aplicación. “Solo interfaces” significa que todas las declaraciones Java de nivel superior bajo `application.port.in` y `application.port.out` son interfaces; se permiten tipos miembro que formen parte del contrato de una de ellas.

## Pruebas

- `domain`: JUnit puro.
- `application`: JUnit puro para reglas; prueba transaccional cuando use `@Transactional`.
- `infrastructure.input.web`: `@WebMvcTest` y contrato HTTP.
- `infrastructure.output.persistence.jooq`: integración con PostgreSQL real.
- `infrastructure.security`: pruebas de integración de Spring Security.
- límites: ArchUnit y `ApplicationModules.verify()`.
