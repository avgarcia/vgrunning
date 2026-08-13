# ADR-0014: Arquitectura modular, hexagonal y DDD selectivo

**Estado:** Propuesto
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0002` decide un monolito modular de despliegue único y el diseño de alto nivel identifica siete componentes lógicos. Esa decisión no fija cómo materializar límites, dependencias, puertos, modelo de dominio ni propiedad de datos. Sin reglas estructurales, la aplicación puede degradarse en paquetes técnicos compartidos, acceso cruzado a tablas y ciclos difíciles de detectar.

`ADR-0013` propone Gradle y un stack reactivo. Este ADR debe convertir los límites funcionales existentes en módulos verificables sin introducir microservicios, duplicar modelos por ceremonia ni aplicar patrones tácticos de DDD donde solo existe CRUD.

## Decisión

El backend se organizará como un monolito modular basado en paquetes dentro de un único proyecto Gradle y un único ejecutable Spring Boot. Spring Modulith descubrirá y verificará los módulos; no se usarán JPMS ni un subproyecto Gradle por módulo durante el PMV.

Los módulos de aplicación serán:

| Módulo | Responsabilidad y datos gobernados |
| --- | --- |
| `identity-access` | Cuentas, rol, activación, credenciales, sesiones e invitación o recuperación. |
| `administration-taxonomies` | Corredores, definiciones de etiquetas, valores permitidos y asignaciones. |
| `segmentation` | Segmentos, criterios e inclusiones o exclusiones manuales. |
| `planning` | Grupos exclusivos, excepciones, planes, entrenamientos, fases, bloques, tipos y objetivos. |
| `publication` | Validación publicable, versiones inmutables, destinatarios congelados y visibilidad. |
| `notification-delivery` | Solicitudes de outbox, leases, entrega, reintentos, webhooks y supresiones para publicación e identidad. |
| `runner-query` | Fachada de lectura móvil de publicaciones e historial propios; no será fuente de verdad de negocio. |
| `tracking-review` | Seguimiento del corredor, historial de respuesta y consultas globales de revisión. |

La separación de `publication` y `notification-delivery` refina el componente lógico «Publicación y notificación» del diseño de alto nivel: publicación gobierna la decisión de negocio y notificaciones gobierna una capacidad operativa reutilizada también por identidad. Ambas permanecen dentro de la misma transacción PostgreSQL cuando se crea una solicitud de outbox.

Cada módulo tendrá un paquete raíz bajo el paquete base de la aplicación. Su API pública se declarará mediante el paquete raíz o `@NamedInterface`; el resto será interno. `@ApplicationModule(allowedDependencies = ...)` o configuración equivalente expresará dependencias permitidas y `ApplicationModules.verify()` rechazará ciclos, accesos a internos y dependencias no declaradas.

### Dependencias permitidas

Las dependencias seguirán el flujo de capacidades, no una arquitectura por capas global:

- `identity-access` podrá solicitar entregas a `notification-delivery`;
- `administration-taxonomies` podrá crear o consultar identidades mediante la API de `identity-access` para vincular cuenta y corredor;
- `segmentation` podrá consultar la API necesaria de `administration-taxonomies`;
- `planning` podrá consultar `segmentation` y `administration-taxonomies`;
- `publication` podrá pedir a `planning` la resolución efectiva del grupo y solicitar outbox a `notification-delivery`, sin acceder directamente a `segmentation`;
- `tracking-review` podrá consultar publicaciones y destinatarios mediante la API de `publication`;
- `runner-query` compondrá exclusivamente APIs de lectura de `publication` y `tracking-review`;
- `notification-delivery` no dependerá de módulos de negocio: recibirá solicitudes autocontenidas y referencias opacas.

Las dependencias inversas y los ciclos quedan prohibidos. Cuando un flujo parezca necesitarlos, se moverá la responsabilidad al módulo que gobierna la regla o se definirá un contrato de aplicación; no se resolverá con acceso a paquetes internos, eventos circulares ni un paquete `common` genérico.

### Arquitectura hexagonal

Cada módulo aplicará arquitectura hexagonal en sus fronteras significativas:

```text
<module>/
  api/                     contrato entre módulos
  application/             casos de uso, políticas y transacciones
  domain/                  reglas y tipos de dominio cuando aporten valor
  adapter/in/web/          adaptación desde OpenAPI/WebFlux
  adapter/in/scheduling/   entradas programadas cuando correspondan
  adapter/out/persistence/ jOOQ/R2DBC
  adapter/out/provider/    servicios externos cuando correspondan
```

Los puertos serán específicos del caso de uso o capacidad. No se crearán interfaces espejo de cada clase, repositorios CRUD genéricos ni DTOs duplicados sin una frontera real. Los tipos generados por OpenAPI y jOOQ permanecerán en adaptadores; no serán el modelo canónico de aplicación o dominio.

Las transacciones se delimitarán en servicios de aplicación. Los adaptadores de entrada no decidirán invariantes y los adaptadores de salida no coordinarán casos de uso. Un módulo que recibe una llamada interna aplicará sus propias invariantes y autorización igual que ante una entrada HTTP.

Las colaboraciones síncronas directas se usarán cuando el resultado o la atomicidad sean necesarios para confirmar el caso de uso. Los eventos de aplicación se reservarán para efectos que toleren ejecución posterior o fallo independiente; no sustituirán las llamadas requeridas para publicación, destinatarios, autorización u outbox atómica.

### Aplicación de DDD

Se aplicará DDD estratégico a todos los módulos: límite explícito, lenguaje ubicuo, propiedad de reglas y contratos publicados. Los nombres de código deberán corresponder a los términos validados en los ADRs y documentos de fase.

DDD táctico será selectivo. Se usarán entidades, value objects, agregados, servicios de dominio y eventos solo donde protejan invariantes o reduzcan ambigüedad. Plan, publicación, destinatario, seguimiento, segmento y grupo son candidatos a modelos ricos; catálogos y proyecciones de lectura podrán permanecer como modelos simples. Una tabla no implica una entidad de dominio y un agregado no tiene por qué reproducir todas sus relaciones en memoria.

Los agregados no se usarán para fingir atomicidad sobre grandes grafos. Las invariantes que `ADR-0012` coordina mediante consultas, bloqueos y restricciones podrán residir en servicios de aplicación y políticas de dominio respaldadas por PostgreSQL. El dominio no dependerá de Reactor, Spring, jOOQ, OpenAPI ni clases de infraestructura; la capa de aplicación podrá usar tipos reactivos en sus puertos para conservar el flujo no bloqueante.

No existirá un `shared-kernel` de negocio inicial. Solo se compartirán tipos técnicos mínimos, estables y sin semántica de un módulo concreto, como reloj o identificadores base. Cualquier ampliación de ese núcleo requerirá demostrar que no roba propiedad a un módulo.

## Alternativas consideradas

### Alternativa A: Paquetes por capas globales

Se descarta porque agrupar todos los controladores, servicios y repositorios facilita dependencias cruzadas y oculta qué capacidad gobierna cada regla.

### Alternativa B: Un subproyecto Gradle por módulo

Ofrece fronteras de compilación más fuertes, pero se descarta inicialmente por su coste de configuración, publicación de contratos y gestión de dependencias dentro de un único ejecutable. Spring Modulith y ArchUnit proporcionarán verificación suficiente; una degradación medida de límites podrá justificar la separación física posterior.

### Alternativa C: DDD táctico uniforme

Se descarta porque produciría agregados, repositorios y objetos de dominio ceremoniales para catálogos y consultas simples. DDD se aplicará donde existan reglas y lenguaje que proteger.

### Alternativa D: Arquitectura hexagonal para cada clase

Se descarta. Crear una interfaz por servicio o repositorio sin alternativa real no desacopla el sistema; aumenta navegación, mocks y coste de cambio. Los puertos se reservan para fronteras de módulo y tecnología.

### Alternativa E: Eventos para toda comunicación entre módulos

Se descarta porque varios flujos necesitan respuesta inmediata y una única transacción. Forzar asincronía interna ocultaría errores, complicaría consistencia y podría contradecir publicación y outbox atómicas.

## Consecuencias

- Los límites de negocio podrán verificarse automáticamente sin separar despliegues.
- Ocho módulos de aplicación refinan los siete componentes lógicos existentes al separar publicación de entrega de notificaciones.
- La propiedad de tablas y reglas quedará concentrada, pero la base seguirá siendo físicamente compartida según `ADR-0012`.
- Las APIs internas requieren diseño y compatibilidad, aunque no sean contratos HTTP públicos.
- El uso selectivo de DDD evita sobreingeniería, pero exige criterio de revisión para distinguir reglas de dominio de simple transformación de datos.
- El módulo `runner-query` podrá componer lecturas sin convertirse en propietario de los datos ni acceder directamente a tablas internas.
- Mantener un único proyecto Gradle simplifica el build, pero sus límites dependen de verificaciones obligatorias y disciplina de paquetes.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20`.

## Decisiones de Fase 1 relacionadas

- `D-01`: taxonomías, segmentos y destinatarios mantienen propietarios distintos y colaboración explícita.
- `D-03`: los módulos pertenecen a un único monolito single-club.
- `D-06`: publicación y solicitud de notificación colaboran dentro de una transacción.
- `D-07`: seguimiento y lectura del corredor conservan modelos y responsabilidades separados.
- `D-08`: cada módulo aplica autorización sin introducir titularidad por entrenador.

## Validación prevista

- Ejecutar `ApplicationModules.verify()` y reglas ArchUnit en cada build.
- Generar documentación de módulos y revisar dependencias reales frente a la lista permitida.
- Hacer fallar una prueba introduciendo un ciclo, acceso a paquete interno y dependencia no permitida.
- Revisar que cada tabla, caso de uso e invariante tenga un módulo propietario único.
- Confirmar que tipos OpenAPI y jOOQ no aparecen en el dominio.
- Probar publicación y creación de outbox en una sola transacción aun atravesando módulos.
- Revisar una muestra de modelos simples y ricos para comprobar que DDD táctico responde a invariantes y no a una plantilla uniforme.

## Decisiones pendientes

- **Bloqueante para aceptar este ADR:** confirmar los ocho módulos, en especial la separación entre `publication` y `notification-delivery`, y la dirección de dependencias propuesta. Responsable: revisor de arquitectura. Tratamiento: revisión explícita del mapa modular.
- **Bloqueante para aceptar este ADR:** confirmar un único proyecto Gradle con módulos por paquetes, en lugar de un subproyecto por módulo. Responsable: revisor de arquitectura. Tratamiento: aceptar el coste y la suficiencia de Spring Modulith y ArchUnit.
- **Bloqueante para aceptar este ADR:** confirmar que DDD táctico y puertos hexagonales serán selectivos y no obligatorios para toda tabla o clase. Responsable: revisor de arquitectura. Tratamiento: validar los criterios descritos antes de crear la estructura inicial.
