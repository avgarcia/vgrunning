# ADR-0026: Paquetería hexagonal bajo infraestructura

**Estado:** Aceptado
**Fecha:** 2026-09-02
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0014](0014-modular-hexagonal-ddd-architecture.md)

## Contexto

`ADR-0014` separa físicamente `adapter` e `infrastructure`, sitúa los puertos de entrada publicados bajo `api` y permite modelos auxiliares en aplicación. La implementación inicial de `identity-access` mostró que esa taxonomía facilita duplicar paquetes técnicos, confundir configuración con adaptación y convertir `application.model` en un contenedor sin responsabilidad arquitectónica.

La arquitectura necesita una regla mecánica para clasificar cada clase antes de crearla. El objetivo no es añadir capas, sino mantener un núcleo compuesto por dominio y aplicación, y reunir todas las entradas, salidas y dependencias del framework bajo infraestructura.

## Decisión

Cada módulo conservará `api`, `domain`, `application` e `infrastructure` como paquetes raíz internos. Solo `api` publicará interfaces y tipos de contratos entre módulos. Cada paquete concreto `api.<concepto>` declarará `@NamedInterface("api")` en su propio `package-info.java`; no se asumirá que la anotación del paquete padre publica recursivamente los subpaquetes.

El dominio se organizará primero por concepto de negocio y después, cuando existan tipos reales, por `aggregate`, `entity`, `valueobject`, `policy`, `service`, `event` y `exception`. No se crearán esos subpaquetes vacíos ni se aplicará DDD táctico por plantilla.

Todas las declaraciones Java de nivel superior de `application.port.in` y `application.port.out` serán interfaces, además de sus metadatos de paquete. Esas interfaces podrán declarar tipos miembro exclusivos de su contrato. Los puertos de entrada locales se ubicarán en `application.port.in`; los publicados a otros módulos, en `api.<concepto>`. Los servicios que implementan puertos de entrada vivirán en `application.service`. No existirá un paquete genérico `application.model`: se usarán tipos de dominio o JDK, tipos anidados en el puerto cuando sean exclusivos de él y tipos bajo `api.<concepto>` cuando formen parte de contratos intermodulares.

Los adaptadores seguirán existiendo como rol hexagonal, pero no como paquete físico. Todas las entradas y salidas estarán bajo `infrastructure.input` e `infrastructure.output`. Los controladores OpenAPI vivirán en `infrastructure.input.web`; las implementaciones jOOQ, en `infrastructure.output.persistence.jooq` del módulo propietario.

La configuración y las properties de Spring vivirán en `infrastructure.configuration`. Spring Security, Spring Session, CSRF, rate limiting y criptografía vivirán en `infrastructure.security`. Un controlador podrá depender directamente de un componente técnico de infraestructura; no se introducirá un puerto entre ambos si aplicación no necesita esa capacidad.

Una entrada de negocio invocará exclusivamente un puerto de entrada y no accederá a puertos de salida, repositorios o jOOQ. Una salida implementará un puerto de salida y no invocará puertos de entrada ni coordinará servicios de aplicación. Las firmas de `api` usarán únicamente JDK y tipos de la propia `api`; nunca filtrarán tipos internos del dominio, aplicación o infraestructura.

Los servicios de aplicación podrán usar `@Transactional` para delimitar el caso de uso. Aplicación no importará otras APIs de Spring, Servlet, OpenAPI, jOOQ o infraestructura. Dominio permanecerá libre de framework y dependerá únicamente de Java y de su propio modelo.

La Skill versionada `arquitectura-hexagonal-y-modulith` se aplicará antes de crear, renombrar o mover clases Java. Una revisión posterior mediante otro agente comprobará responsabilidad, paquete e imports; ArchUnit y Spring Modulith seguirán siendo la autoridad ejecutable.

## Alternativas consideradas

### Alternativa A: Mantener `adapter` e `infrastructure` como raíces separadas

Se descarta porque ambas representan detalles externos al núcleo y su separación física ha producido responsabilidades y nombres duplicados sin reforzar una frontera útil.

### Alternativa B: Publicar todos los puertos de entrada bajo `api`

Se descarta. `api` se reserva para contratos intermodulares y `application.port.in` para interfaces de casos de uso. Publicar cada entrada ampliaría innecesariamente la superficie del módulo.

### Alternativa C: Prohibir cualquier dependencia de Spring en aplicación

Se descarta. `@Transactional` expresa correctamente la frontera atómica del caso de uso y ya forma parte de la decisión de runtime. Esta excepción no se extiende a seguridad, web, persistencia ni configuración.

## Consecuencias

- La ubicación física refleja núcleo frente a infraestructura sin una raíz `adapter` paralela.
- Cada puerto es identificable como interfaz y desaparece el cajón genérico `application.model`.
- Los controladores y componentes técnicos pueden colaborar directamente dentro de infraestructura sin interfaces ceremoniales.
- El dominio gana una taxonomía consistente por concepto y tipo, pero solo se materializan categorías con contenido real.
- Será necesario mover las clases existentes y actualizar imports, pruebas ArchUnit, documentación y reglas de generación que todavía mencionan `adapter`.
- La decisión está aceptada y la migración de clases, imports, documentación y gates queda aplicada en F01.1.
- La revisión mediante agente aporta una segunda lectura, pero no reemplaza gates ni constituye aprobación independiente de una PR.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-21`.

## Decisiones de Fase 1 relacionadas

- `D-01`: cada concepto y regla conserva un propietario inequívoco.
- `D-03`: la aplicación continúa siendo un único monolito modular.
- `D-06`: las transacciones compartidas permanecen delimitadas por casos de uso.

## Validación prevista

- Verificar con `ApplicationModules.verify()` que solo `api` es visible entre módulos.
- Verificar que cada `api.<concepto>` publicado declara explícitamente `@NamedInterface("api")` y que sus firmas no filtran tipos internos.
- Añadir reglas ArchUnit para las dependencias permitidas y los imports prohibidos.
- Verificar que `application.port.in` y `application.port.out` contienen únicamente interfaces.
- Verificar que no existen paquetes raíz `adapter` ni `application.model`.
- Confirmar que OpenAPI solo se consume en `infrastructure.input.web` y jOOQ solo en `infrastructure.output.persistence.jooq`.
- Ejecutar pruebas dirigidas, `check`, `docsCheck` y un único `qualityGate` final con la migración de paquetes aplicada.

## Decisiones pendientes

- Mantener la estructura migrada y sus gates ArchUnit en las siguientes slices.
