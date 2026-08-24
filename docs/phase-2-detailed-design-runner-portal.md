# Diseño detallado del portal del corredor — Fase 2

**Estado:** Validado como diseño — únicamente autorizada la preparación técnica con datos sintéticos
**Fecha:** 2026-08-24
**Fecha de validación:** 2026-08-24
**Responsable de revisión:** Revisor de arquitectura
**Validación documental:** Decisiones del portal confirmadas explícitamente por el responsable y orden de validar y fusionar registrada el 2026-08-24
**Restricción:** Prohibido tratar datos personales reales hasta completar las evidencias de privacidad, seguridad y operación exigidas por `ADR-0010`, `ADR-0016` y `ADR-0018`
**Ámbito:** `runner-portal` y su composición de `runner-management`, `publication` y `tracking-review`

## Propósito

Materializar la experiencia móvil de `RF-16`, completar la consulta de ubicación de `RF-13`, la captura propia de `RF-17` y el historial de `RF-18` sin duplicar las reglas ni los datos de los módulos propietarios. El diseño concreta navegación, representaciones, estados vacíos, detalle del entrenamiento, seguimiento, consentimiento y contratos HTTP del corredor.

## Resultado funcional

- Un corredor `active` abre la semana actual, identifica hoy y consulta únicamente su plan publicado vigente.
- Si no existe plan esta semana, ve un estado vacío y puede acceder explícitamente al siguiente plan futuro publicado sin que el portal cambie de semana por sí solo.
- La semana resume cada entrenamiento en una tarjeta y el detalle completo queda a una pulsación.
- `Mi plan` reúne publicaciones actuales, anteriores y futuras; `Historial` reúne solo entrenamientos cuya fecha ya comenzó.
- El historial presenta primero lo más reciente, agrupa por semana y permite cargar más sin buscador ni filtros en el PMV.
- El corredor registra o corrige seguimiento mediante un formulario con guardado explícito y puede retirar en cualquier momento el consentimiento del comentario.
- El portal no posee datos de negocio ni proyecciones persistidas; aplica autorización y compone respuestas desde las APIs Java de sus módulos propietarios.

## Fuentes normativas

- [Requisitos de Fase 1](phase-1-requirements.md), [criterios de aceptación](phase-1-acceptance-criteria.md) y [matriz de decisiones](phase-1-decision-matrix.md), especialmente `RF-13`, `RF-16`, `RF-17`, `RF-18`, `D-04`, `D-07` y `D-08`.
- [Diseño de alto nivel](phase-2-high-level-design.md).
- [Diseño detallado de planificación](phase-2-detailed-design-planning.md), que define el entrenamiento, sus fases, cargas, objetivos, modalidad y ubicación.
- [Diseño detallado de publicación](phase-2-detailed-design-publication.md), que gobierna versiones activas, destinatarios congelados y representación autorizada.
- [Diseño detallado de seguimiento y revisión](phase-2-detailed-design-tracking-review.md), que gobierna registro, historial, ventana, estados y consentimiento.
- [Diseño detallado de gestión de corredores](phase-2-detailed-design-runner-management.md), que gobierna vínculo, estado `active`, baja y reactivación.
- `ADR-0004`: aislamiento estricto del corredor y autorización en backend.
- `ADR-0006`, `ADR-0007`, `ADR-0009`, `ADR-0020` y `ADR-0021`: contenido publicado, versión visible, historial, seguimiento y edición de publicaciones.
- `ADR-0010`: minimización, retención, derechos, comentario y bloqueo de datos reales.
- `ADR-0013`: SPA React adaptable y contrato OpenAPI `3.1`.
- `ADR-0014`: fachada `runner-portal` sin esquema inicial y dependencias permitidas.
- `ADR-0015`: `ActorContext`, políticas de autorización y alcance dentro de consultas.
- `ADR-0016`: despliegue, seguridad, observabilidad y operación.
- `ADR-0017` y la [guía de API HTTP](api-design-guidelines.md): recursos, rutas, métodos, filtros y errores.
- `ADR-0018`: solo el corredor `active` participa en portal y seguimiento; la reactivación no recupera datos vencidos.

Si este documento contradice un ADR aceptado, prevalece el ADR y deberá corregirse el diseño antes de implementar.

## Alcance

Incluye:

- navegación principal `Mi plan` e `Historial`;
- semana actual, navegación entre publicaciones y estado sin plan;
- tarjetas diarias y detalle completo de entrenamiento;
- presentación propia de estados de seguimiento y acceso al formulario;
- consentimiento justo a tiempo y acceso permanente a su retirada;
- enlaces de correo que regresan al plan autorizado después de autenticarse;
- API HTTP del portal, composición modular, seguridad, privacidad, accesibilidad y pruebas.

Quedan fuera:

- aplicaciones móviles nativas, instalación PWA y uso sin conexión prioritario;
- actualización en tiempo real, WebSocket, polling, banners de nueva versión o recuperación especial de formularios por republicación concurrente;
- edición de perfil, correo, contraseña, clasificación, grupos o preferencias deportivas;
- búsqueda, filtros, calendario mensual, exportación o analítica del historial;
- mensajería, respuesta del entrenador, notificaciones distintas del correo ya aprobado y estados de revisión;
- comentarios generales del plan, asistencia, reservas, pagos o integraciones deportivas;
- datos persistentes, caché compartida o proyecciones propias de `runner-portal`.

## Razonamiento de diseño

1. La semana es la unidad del plan, publicación, correo y exclusividad; abrir otra unidad obligaría al corredor a reconstruir el contexto.
2. Separar plan e historial evita presentar futuros como ausencias y permite que el mismo entrenamiento cambie de capacidad al comenzar su fecha.
3. Una tarjeta breve permite comparar hasta siete días en móvil; el detalle conserva toda la información sin comprimirla ni ocultarla definitivamente.
4. Guardar seguimiento de forma explícita evita estados parciales y coincide con la sustitución atómica de `tracking-review`.
5. El comentario es opcional y potencialmente sensible; su retirada debe estar disponible aunque no exista un entrenamiento editable.
6. Componer APIs preserva propietarios y autorización. Copiar publicaciones o seguimiento al portal crearía otra fuente de verdad y otra política de retención.
7. La actualización concurrente de una publicación será rara según el responsable. El PMV acepta una recarga manual y posible pérdida del formulario no guardado antes que añadir sincronización y recuperación específica.

## Decisiones confirmadas

1. `Mi plan` abre siempre la semana local actual, de lunes a domingo, y destaca el día de hoy.
2. El portal no salta automáticamente a una semana futura. Si la actual no tiene publicación, muestra un estado vacío y un acceso explícito al siguiente plan futuro publicado, cuando exista.
3. El corredor puede navegar por semanas anteriores y futuras que tengan publicaciones propias.
4. La navegación principal separa `Mi plan` de `Historial`.
5. `Historial` incluye solo entrenamientos cuya fecha local ya comenzó, agrupados por semana y ordenados del más reciente al más antiguo.
6. El historial no tendrá buscador ni filtros en el PMV y se recorrerá mediante carga progresiva.
7. Un seguimiento editable puede registrarse o corregirse desde `Mi plan` y desde `Historial`.
8. La ausencia se presenta como `Pendiente de seguimiento` mientras la ventana siga abierta y como `Sin seguimiento` después de cerrarse; se conservan `Realizado`, `No realizado` y `Retirado`.
9. El formulario usa `Registrar seguimiento` o `Editar seguimiento`, no guarda automáticamente y ofrece `Guardar` y `Cancelar`. Tras el cierre solo existe lectura.
10. La semana muestra una tarjeta por entrenamiento con fecha, tipo, modalidad, lugar cuando exista y estado. Una pulsación abre el contenido completo.
11. El consentimiento del comentario se solicita justo al intentar usarlo. Su retirada está siempre disponible desde `Privacidad de comentarios` y también enlazada desde el formulario.
12. Antes de retirar se informa de que se deshabilitan nuevos comentarios, los anteriores no se recuperan y el seguimiento estructurado permanece.
13. No se implementan avisos ni actualización especial por una republicación mientras la pantalla está abierta. Cada entrada o recarga obtiene la versión vigente; un conflicto de escritura exige recargar.

## Supuestos e incertidumbres

| Elemento | Supuesto o incertidumbre | Confianza | Tratamiento |
| --- | --- | --- | --- |
| Unidad semanal | Existe como máximo un plan publicado por corredor y semana. | Alta | Restricción física y transacción de `publication`; el portal nunca resuelve conflictos. |
| Zona horaria | Portal, publicación y seguimiento comparten una única zona IANA del club. | Alta | El servidor entrega semana, fecha local y capacidades; el navegador no redefine reglas. |
| Formulario y republicación | Una primera respuesta concurrente con una republicación puede quedar obsoleta. | Media | Rechazo seguro en backend, mensaje genérico y recarga; no hay recuperación especial en el PMV. |
| Navegación futura | Puede haber semanas sin publicación entre la actual y la siguiente disponible. | Alta | Los contratos devuelven referencias de navegación autorizadas y la interfaz no inventa planes vacíos. |
| Historial | La retención limita el volumen, pero puede superar una sola página. | Alta | Cursor opaco y botón `Cargar más`; no se añaden filtros sin evidencia de uso. |
| Consentimiento | Una nueva versión material puede dejar el consentimiento anterior insuficiente. | Media | `tracking-review` entrega estado y versión efectiva; el portal vuelve a solicitarlo cuando corresponda. |
| Datos reales | La validación documental no demuestra licitud, EIPD, operación ni seguridad productiva. | Alta | Mantener el gate de datos sintéticos y producción de los ADR aplicables. |

## Lenguaje ubicuo

| Término | Significado |
| --- | --- |
| `Mi plan` | Sección que consulta publicaciones semanales propias, incluida la semana actual y otras publicadas. |
| Semana actual | Semana de lunes a domingo que contiene `today` en la zona del club. |
| Tarjeta de entrenamiento | Resumen navegable de un entrenamiento publicado; no es otra entidad ni una copia persistida. |
| Detalle de entrenamiento | Representación completa autorizada de fases, bloques, objetivos, modalidad, ubicación y seguimiento propio. |
| `Historial` | Colección cronológica propia de entrenamientos cuya fecha ya comenzó, incluido su seguimiento o ausencia. |
| `Pendiente de seguimiento` | Presentación de `sin-seguimiento` mientras la ventana de respuesta permanece abierta. |
| `Sin seguimiento` | Presentación de la misma ausencia después del cierre de la ventana. |
| `Privacidad de comentarios` | Vista propia para consultar y retirar el consentimiento opcional del comentario. |
| Carga progresiva | Obtención de una página posterior mediante cursor y acción explícita `Cargar más`. |

En código y OpenAPI se usarán `own weekly plan`, `own training history`, `workout card`, `workout detail` y los estados canónicos publicados por los módulos propietarios. No se usarán `dashboard`, `feed`, `calendar event`, `completed` ni `reviewed` para renombrar conceptos existentes.

## Límite modular

`runner-portal` gobierna:

- representaciones HTTP y composición para el corredor;
- navegación semanal, estados vacíos y carga progresiva;
- adaptación visual de estados ya calculados;
- rutas SPA, accesibilidad y control de datos visibles;
- coordinación de consultas sin adquirir las invariantes llamadas.

Consume `runner-management` para resolver el corredor propio y verificar estado `active`; `publication` para semanas, versiones activas y detalles autorizados; y `tracking-review` para historial, seguimiento propio, capacidades y consentimiento.

```text
runner-portal ──> runner-management
runner-portal ──> publication
runner-portal ──> tracking-review
```

No consume `planning`, `identity-access` ni `notification-delivery`. `ActorContext` llega desde la infraestructura común; no se consulta identidad por SQL o mediante una dependencia inversa.

`runner-portal` no tendrá esquema PostgreSQL, tablas, migraciones, repositorios ni trabajos de retención iniciales. Tampoco mantendrá una proyección en memoria entre peticiones como fuente de verdad. Si mediciones futuras justifican persistencia, deberá revisarse `ADR-0014` antes de añadirla.

## Arquitectura de información

La navegación principal ofrece:

1. `Mi plan`, opción inicial después de acceder;
2. `Historial`;
3. `Privacidad de comentarios`, dentro del menú personal y enlazada desde el formulario cuando el comentario esté disponible;
4. salida de sesión mediante el recurso de sesión de `identity-access`, sin duplicar su implementación.

No existe una sección de perfil editable. Nombre, correo, etiquetas o grupo no se presentan como configuraciones del corredor.

## `Mi plan`

La primera consulta solicita la semana local actual. La cabecera muestra intervalo, nombre del plan y grupo publicado. Hoy queda destacado solo cuando pertenece a la semana visible.

Si existe publicación, los entrenamientos aparecen de lunes a domingo conservando huecos sin inventar sesiones. Si no existe, se muestra `No tienes un plan publicado para esta semana`. Cuando haya otra publicación futura autorizada, se ofrece un enlace con su semana; nunca se selecciona automáticamente.

La navegación anterior y siguiente recorre semanas publicadas propias. Una semana futura muestra contenido, pero ninguno de sus entrenamientos tiene estado ni acción de seguimiento antes de comenzar su fecha.

Cada tarjeta contiene:

- día de la semana y fecha;
- tipo de la parte principal;
- modalidad `presencial` o `en-linea`;
- lugar de encuentro cuando fue publicado y la modalidad es presencial;
- estado de seguimiento solo desde la fecha del entrenamiento;
- affordance accesible para abrir el detalle.

No calcula ni muestra distancia, duración, ritmo o esfuerzo totales si el modelo no los publica como magnitud canónica. No resume objetivos de forma que pueda alterar su intervalo o unidad.

## Detalle del entrenamiento

El detalle reproduce la versión autorizada con:

- plan, grupo, día, fecha, modalidad, ubicación y aclaraciones publicadas;
- calentamiento, parte principal y enfriamiento en orden;
- bloques, repeticiones, cargas por duración o distancia, recuperaciones y tipo;
- objetivos `Z1..Z5` o ritmo relativo con distancia e intervalo exactos;
- estado y contenido del seguimiento propio cuando la privacidad lo permita;
- capacidad `Registrar seguimiento`, `Editar seguimiento` o solo lectura calculada por `tracking-review`.

El corredor no ve destinatarios, autoría, números internos de versión, entrega de correo, nombres de tablas ni metadatos administrativos. El lugar ausente se muestra como ausencia; no se inventa, reutiliza ni completa desde otro entrenamiento.

Los objetivos se muestran literalmente como «Zx según las zonas que utilizas con tu entrenador» o «ritmo de distancia +/− segundos por km, usando tu marca de referencia acordada con tu entrenador». El portal no calcula, valida ni solicita zonas, marcas o ritmos personales. Desconocer la referencia no bloquea ninguna acción: el corredor la consulta por el canal externo habitual con su entrenador, sin chat ni registro adicional en el producto.

Un enlace de correo abre la semana de la publicación activa. Si exige autenticación, la ruta de retorno se conserva de forma opaca y se aplica solo después de una sesión válida. El enlace nunca concede acceso: si el corredor no es destinatario `active`, el recurso sigue sin estar disponible.

## `Historial`

El historial usa la consulta propia de `tracking-review`. Solo incluye fechas iniciadas, ordena por fecha descendente e identificador estable y agrupa visualmente por semana. La primera página se carga al entrar; `Cargar más` conserva posición y añade la página siguiente.

Cada elemento resume fecha, tipo, modalidad y presentación del estado. Abrirlo muestra el entrenamiento y seguimiento asociados al contexto histórico:

- con respuesta, la versión de referencia fijada al crearla;
- sin respuesta y no retirado, el contexto publicado vigente aplicable;
- retirado, el último contexto histórico permitido y la marca `Retirado`;
- comentario solo si continúa vigente y autorizado.

No existen búsqueda, filtros por fecha, tipo o estado, exportación, orden alternativo ni calendario mensual en el PMV. Una página vacía muestra un estado informativo y no consulta borradores o entrenamientos futuros para rellenarla.

## Seguimiento y consentimiento

`runner-portal` no reimplementa validaciones. Solicita a `tracking-review` la capacidad y representación actuales y usa sus recursos de escritura.

Estados visibles:

| Contexto | Presentación | Acción |
| --- | --- | --- |
| Fecha futura | Sin estado de seguimiento | Ninguna |
| Sin registro y ventana abierta | `Pendiente de seguimiento` y fecha límite | `Registrar seguimiento` |
| Registro y ventana abierta | `Realizado` o `No realizado` | `Editar seguimiento` |
| Sin registro y ventana cerrada | `Sin seguimiento` | Ninguna |
| Registro y ventana cerrada | `Realizado` o `No realizado` | Ninguna |
| Entrenamiento retirado | `Retirado`, conservando respuesta si existía | Solo lectura o edición del registro existente mientras su ventana fijada siga abierta |

El formulario es una representación completa. `Cancelar` vuelve sin petición y conserva el último registro; `Guardar` envía una sola operación con la precondición recibida. Un error de campo mantiene valores locales y no sustituye el registro anterior. Una recarga manual puede perder valores no guardados; no se incorpora borrador local persistente.

Cuando `performed=true`, el formulario pregunta exactamente «¿Cuánto esfuerzo te supuso este entrenamiento?» sin preselección y ofrece `1 Muy suave`, `2 Suave`, `3 Moderado`, `4 Intenso` y `5 Muy intenso`. Las mismas etiquetas se usan en lectura e historial; la interfaz no lo presenta como máximo fisiológico ni medición clínica.

Cuando el corredor intenta escribir un comentario sin consentimiento vigente, se presenta información separada, versión y advertencia para no introducir diagnósticos, lesiones ni otra información de salud. Rechazarla mantiene habilitado el guardado estructurado.

`Privacidad de comentarios` muestra estado y versión vigentes. Retirar exige una confirmación explícita con sus tres consecuencias ya validadas. La operación no depende de que exista plan o ventana abierta. Volver a aceptar no recupera textos anteriores.

## Actualización y errores

Cada entrada, cambio de semana, apertura de detalle o recarga consulta la versión autorizada vigente. El PMV no mantiene una suscripción ni consulta periódicamente si apareció otra versión.

Si una primera respuesta pierde la carrera con una republicación, `tracking-review` rechaza la escritura. El portal muestra un mensaje genérico que indica que el entrenamiento cambió y exige recargar; no mezcla automáticamente el formulario con el contenido nuevo ni reintenta sin confirmación.

Otros estados observables:

- `200 OK` con colección vacía cuando no existe plan para la semana o no hay historial;
- recurso no disponible sin distinguir inexistencia, propiedad, inactividad o retención cuando revelarlo permitiría enumeración;
- sesión caducada con retorno al acceso y, tras autenticarse, solo a una ruta todavía autorizada;
- fallo temporal conservando la pantalla anterior cuando sea seguro y ofreciendo reintento explícito;
- ventana cerrada durante la edición con rechazo completo y recarga en modo lectura.

No se prometen recuperación de formulario tras cierre, sincronización entre pestañas ni comportamiento sin conexión.

## Composición de consultas

### Abrir la semana

1. El adaptador recibe `ActorContext` y resuelve el corredor propio `active` mediante `runner-management`.
2. `publication` obtiene la publicación activa propia para `weekStart` y referencias autorizadas anterior y siguiente.
3. `tracking-review` obtiene por lote los estados propios de los entrenamientos cuya fecha comenzó.
4. El portal compone la representación y elimina cualquier campo administrativo antes de responder.

### Abrir un detalle

1. `publication` autoriza plan, destinatario y versión visible.
2. `tracking-review` aporta contexto histórico o registro y capacidad de escritura.
3. El portal usa el contexto que corresponde: versión activa para plan futuro o sin respuesta, versión fijada para una respuesta histórica.
4. Ningún identificador recibido sustituye la comprobación del actor.

### Recorrer historial

`tracking-review` entrega una página ya aislada y un cursor opaco. El portal adapta la presentación sin volver a ordenar, filtrar ni reconstruir ausencias. El cursor queda ligado al corredor, orden y alcance y no contiene datos personales legibles.

## APIs Java consumidas

De `runner-management`:

- resolución del corredor vinculado al actor y comprobación `active`;
- aplicación coordinada de supresión o anonimización cuando proceda, sin persistencia propia del portal.

De `publication`:

- consulta propia por corredor y semana con navegación entre publicaciones;
- detalle activo autorizado del plan y entrenamiento;
- contexto histórico mínimo solicitado por `tracking-review`.

De `tracking-review`:

- contexto y capacidad de seguimiento propio;
- consulta de historial propio por cursor;
- creación o sustitución de seguimiento;
- consulta, concesión y retirada de consentimiento.

Los contratos transportan `ActorContext`, identificadores opacos, fechas locales, capacidades y revisiones. No exponen modelos OpenAPI, entidades internas, tablas ni tipos jOOQ.

## API HTTP prevista

OpenAPI `3.1` será la fuente de verdad antes de implementar.

| Actor | Método y recurso | Semántica |
| --- | --- | --- |
| Corredor | `GET /api/runners/me/weekly-plans` | Consulta la semana indicada por `weekStart`; si se omite usa la actual. Devuelve cero o un plan y navegación autorizada. |
| Corredor | `GET /api/runners/me/weekly-plans/{weeklyPlanId}` | Obtiene la representación completa publicada propia. |
| Corredor | `GET /api/runners/me/training-history-items` | Recorre historial propio con cursor y sin filtros de producto. |
| Corredor | `GET /api/runners/me/training-history-items/{historyItemId}` | Obtiene detalle histórico propio. |

Las escrituras de seguimiento continúan en `PUT /api/workouts/{workoutId}/tracking-records/current`; el consentimiento en `GET` y `PUT /api/runners/me/tracking-comment-consents/current`. `runner-portal` usa esos contratos de `tracking-review` y no crea rutas proxy o verbos alternativos.

`/runners/me` identifica una relación contextual real permitida por `ADR-0017`; no es un prefijo de autorización por rol. `weekly-plans`, `training-history-items`, `tracking-records` y `tracking-comment-consents` son recursos con representación y ciclo de vida definidos, no acciones nominalizadas.

Semántica mínima:

- colecciones propias: `200 OK` aun vacías, cursor opaco y límite acotado;
- detalle autorizado: `200 OK`; candidato no disponible: respuesta no enumerable según `ADR-0015`;
- `weekStart` usa fecha ISO del lunes y se rechaza si no identifica una semana válida en la zona del club;
- respuestas personales usan `Cache-Control: private, no-store`;
- errores usan Problem Details estable sin contenido del plan, comentario o identidad;
- `GET` no recibe cuerpo y no crea estado;
- las precondiciones, CSRF y resultados de `PUT` permanecen definidos por `tracking-review`.

No existirán `/runner/dashboard`, `/my-calendar`, `/load-more`, `/complete`, `/comment-consent/withdraw`, rutas por versión histórica ni secretos en URL o query.

## Autorización y aislamiento

Todas las capacidades exigen sesión de corredor y vínculo operativo `active`. El identificador `me` se resuelve desde `ActorContext`; nunca se acepta `runnerId`, `accountId` o correo del cliente como identidad.

Cada consulta aplica el predicado de destinatario dentro de `publication` y el de propietario dentro de `tracking-review` antes de datos, conteos, navegación y cursores. `runner-portal` vuelve a minimizar la respuesta, pero su filtrado no sustituye las políticas de los propietarios.

La baja revoca el acceso y excluye inmediatamente el portal. La reactivación permite consultar solo publicaciones y seguimiento que sigan retenidos y para los que el corredor conserve destinatario histórico; no recalcula asignaciones ni recupera datos suprimidos.

Se probarán acceso directo, manipulación de identificadores, cursores, retorno tras login, enlaces de correo y respuestas de error para impedir enumeración o exposición cruzada.

## Diseño adaptable y accesibilidad

La interfaz se diseña primero para una columna móvil y se amplía sin perder orden semántico. Ninguna representación esencial exige desplazamiento horizontal. Las tablas administrativas no se reutilizan en el portal.

Todo el PMV web cumplirá WCAG `2.2` nivel `AA`. La validación incluye reflow a `320` CSS px sin desplazamiento bidimensional salvo excepciones esenciales, zoom al `400 %`, texto al `200 %` y objetivos de puntero de al menos `24 × 24` CSS px o una excepción oficial aplicable y documentada. Controles, estados y errores tendrán:

Las reglas se interpretan conforme a las explicaciones oficiales de W3C para [Reflow](https://www.w3.org/WAI/WCAG21/Understanding/reflow.html) y [Target Size (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html); una excepción no se presume, se documenta y prueba.

- etiquetas textuales; color e iconos nunca serán la única señal;
- navegación y activación mediante teclado;
- foco visible y retorno de foco al cerrar formularios o detalles;
- encabezados, listas y botones semánticos;
- errores asociados al campo y resumen cuando existan varios;
- fechas con día, mes y semana comprensibles, sin depender solo de formato numérico;
- zonas táctiles suficientes para evitar activar otra tarjeta accidentalmente;
- conservación del zoom y ausencia de texto incrustado como imagen.

La validación cubrirá tamaños móviles reducidos, orientación vertical y horizontal y escritorio. `Adaptable` no significa reducir la versión de escritorio hasta hacer ilegible su contenido: se verificará acceso a todas las fases, cargas, objetivos, ubicación y acciones.

## Rendimiento y caché

La primera vista realiza una composición acotada para una semana y obtiene estados en lote; no ejecuta una llamada por tarjeta. El historial pagina mediante cursor y no precarga toda la retención.

Las respuestas personales no se almacenan en cachés compartidas ni Service Worker. Se permite estado efímero en memoria del navegador durante la navegación, pero entrar, recargar o volver desde segundo plano consulta de nuevo al servidor. No se ofrece garantía offline.

Las cardinalidades, tamaño máximo de página y consultas se medirán con publicaciones e historial representativos. Una optimización no puede introducir SQL cruzado, estado personal persistente en `runner-portal` ni una fuente de verdad duplicada.

## Privacidad y observabilidad

El portal presenta plan, ubicación y seguimiento propios, todos datos personales. El comentario puede contener información de salud y conserva las restricciones de `ADR-0010`; no se envía a analítica, logs, trazas, métricas, informes de error o cachés.

La ubicación publicada se muestra solo a destinatarios autorizados y sigue la retención del entrenamiento. El portal no amplía finalidades, plazos, exportaciones ni derechos. Las solicitudes de acceso o supresión continúan por el canal de privacidad y se ejecutan en los módulos propietarios.

Métricas agregadas permitidas:

- aperturas y fallos de `Mi plan`, detalle e historial por resultado técnico;
- latencia y tamaño de página sin dimensiones de corredor, plan, entrenamiento o comentario;
- conflictos y ventanas cerradas por código normalizado;
- fallos de composición por módulo sin payload ni identificadores personales.

No se registra semana consultada, ubicación, objetivos, seguimiento, comentario, nombre, correo, ruta de retorno completa ni cursor. Los eventos de seguridad siguen el catálogo mínimo de `ADR-0010` y acceso restringido.

Hasta completar responsable, bases, información, proveedores, retención, derechos, medidas, EIPD y evidencias operativas, todos los entornos usan datos ficticios, sintéticos o anonimizados irreversiblemente. Validar este diseño no autoriza producción.

## Paquetes previstos

```text
com.vgrunning.runnerportal/
  api/
    query/
  application/
    service/
    port/out/
  adapter/in/web/
  adapter/out/module/
```

No existe `domain` rico inicial porque el portal no gobierna invariantes ni estado. Las reglas de composición y minimización viven en aplicación; los adaptadores traducen OpenAPI y APIs Java. Si aparecen invariantes propias reales, se añadirá dominio por concepto, no por plantilla.

Spring Modulith y ArchUnit verificarán `allowedDependencies = {"runner-management::api", "publication::api", "tracking-review::api"}`, ausencia de esquema, SQL, jOOQ, dependencias inversas e imports de paquetes internos.

## Validación prevista

### `RF-13` y `RF-16` — Plan y consulta móvil

- Abrir semana actual con plan, destacar hoy y mostrar de lunes a domingo sin inventar entrenamientos.
- Abrir semana actual vacía con y sin publicación futura y comprobar que nunca cambia automáticamente.
- Navegar semanas publicadas anteriores y futuras, incluidos huecos entre ellas.
- Mostrar tarjeta con fecha, tipo, modalidad, ubicación existente y estado; abrir el detalle completo con una sola activación.
- Probar ubicación ausente y modalidad `en-linea` sin texto ficticio ni dato reutilizado.
- Reproducir fases, bloques, repeticiones, cargas, recuperaciones, objetivos y aclaraciones sin pérdida ni reinterpretación.
- Probar enlace de correo con sesión activa, sesión caducada, corredor ajeno e inactivo.
- Verificar `320` CSS px, zoom `400 %`, texto `200 %`, objetivos `24 × 24` CSS px o excepción oficial documentada, ambas orientaciones, teclado, foco, etiquetas, errores y ausencia de desplazamiento bidimensional no esencial.

### `RF-17` — Seguimiento desde el portal

- Presentar futuro sin acción, ventana abierta como `Pendiente de seguimiento` y ventana cerrada como `Sin seguimiento`.
- Registrar desde `Mi plan` e `Historial`; editar dentro de ventana y dejar solo lectura después.
- Cancelar sin petición, guardar una sola representación completa y mantener valores ante error de campo.
- Cambiar entre `Realizado` y `No realizado` respetando pregunta exacta, ausencia de valor por defecto, escala `1 Muy suave` a `5 Muy intenso`, esfuerzo entero, sensación, comentario, normalización y límites de `tracking-review`.
- Simular republicación concurrente antes de la primera respuesta y comprobar rechazo, mensaje y recarga sin mezcla automática.
- Comprobar que no existen guardado automático, borrador persistente, recuperación especial, polling ni WebSocket.

### Consentimiento y comentario

- Intentar escribir comentario sin consentimiento y mostrar información separada, versión y advertencia sanitaria.
- Rechazar y guardar seguimiento estructurado válido.
- Aceptar una vez, reutilizar mientras siga vigente y volver a solicitar una versión material nueva.
- Acceder a `Privacidad de comentarios` sin plan ni ventana abierta.
- Retirar después de confirmar consecuencias, deshabilitar comentario y comprobar que los textos anteriores no reaparecen tras volver a consentir.
- Revisar todas las vistas, errores, cachés y telemetría para impedir exposición del comentario.

### `RF-18` — Historial

- Excluir entrenamientos futuros e incluir uno exactamente al comenzar su fecha local.
- Agrupar por semana y ordenar descendente con desempate estable.
- Cargar varias páginas sin duplicados, omisiones, cambio de posición ni filtros ocultos.
- Mostrar `Realizado`, `No realizado`, `Pendiente de seguimiento`, `Sin seguimiento` y `Retirado` según contexto.
- Abrir una respuesta contra su versión de referencia y un retirado contra su contexto histórico permitido.
- Probar historial vacío, retención vencida, baja, reactivación y aislamiento entre corredores.

### Arquitectura, API y privacidad

- Ejecutar `ApplicationModules.verify()` y ArchUnit para verificar dependencias y ausencia de persistencia propia.
- Probar composición por lotes y fallos aislados de cada módulo sin datos parciales de otro corredor.
- Revisar recursos, métodos, filtros, seguridad, caché, cursores, errores y ausencia de acciones nominalizadas contra `ADR-0017`.
- Generar servidor y cliente desde OpenAPI, ejecutar Spectral, pruebas de contrato y `oasdiff`.
- Medir semana e historial con volumen representativo y comprobar ausencia de consultas por tarjeta.
- Revisar datos personales, retorno de login, logs, métricas, trazas, cachés y entornos sintéticos.

## Alternativas descartadas

- **Abrir automáticamente el siguiente plan:** se descarta porque una semana futura podría parecer aplicable hoy.
- **Una única cronología con futuro e historial:** se descarta porque convertiría entrenamientos futuros en elementos comparables con respuestas y ausencias.
- **Mostrar todo el entrenamiento en la semana:** se descarta porque dificulta comparar días en móvil; el detalle queda a una pulsación.
- **Guardado automático del seguimiento:** se descarta por riesgo de estados parciales y cambios accidentales.
- **Buscar y filtrar el historial:** se descarta en el PMV por falta de necesidad validada y retención acotada.
- **Retirada solo desde un entrenamiento:** se descarta porque impediría revocar consentimiento sin una ventana abierta.
- **Actualización en tiempo real y recuperación de formulario:** se descarta por coste desproporcionado para un caso que el responsable considera excepcional.
- **Persistir una proyección del portal:** se descarta sin evidencia de rendimiento porque duplicaría datos, retención y consistencia.
- **Exponer APIs administrativas al corredor:** se descarta porque contienen representaciones y capacidades innecesarias aunque la ruta pudiera autorizarse.
- **Aplicación nativa o modo offline:** se descarta por alcance explícito de Fase 1.

## Cambios de alcance y riesgos aceptados

El diseño completa la experiencia ya exigida por `RF-13`, `RF-16`, `RF-17` y `RF-18`; no añade otra capacidad de negocio ni modifica ADR. `Privacidad de comentarios` materializa la retirada ya obligatoria y no se convierte en una sección general de preferencias.

Riesgos aceptados:

- una recarga o conflicto puede perder seguimiento no guardado porque no existe borrador persistente;
- el portal no avisa de una republicación hasta otra consulta o recarga;
- una semana vacía exige una acción adicional para abrir el siguiente plan;
- dos secciones pueden ofrecer acceso al mismo seguimiento mientras la ventana esté abierta;
- la ausencia de filtros puede exigir recorrer varias páginas de historial;
- componer tres módulos añade latencia y fallos parciales que deben manejarse sin duplicar datos;
- comentarios y ubicaciones elevan el impacto de una autorización incorrecta;
- el diseño validado continúa bloqueado para datos reales y producción por privacidad y operación.

No se crea un ADR porque las decisiones son de experiencia, representación y composición dentro de los límites aceptados. Incorporar persistencia, offline, notificaciones nuevas, analítica, perfil editable, actualización en tiempo real o exposición de datos adicionales requerirá revisar alcance y los ADR afectados.

## Conclusiones

- La semana actual y el historial separado ofrecen una navegación coherente con publicación y seguimiento.
- Las tarjetas mantienen la vista móvil breve y el detalle conserva toda la prescripción del entrenador.
- Los estados visibles explican si aún se puede responder sin cambiar el modelo `sin-seguimiento`.
- El formulario explícito y las precondiciones evitan guardados parciales; la sincronización avanzada queda fuera conscientemente.
- El consentimiento puede concederse cuando se necesita y retirarse en cualquier momento sin perder el seguimiento estructurado.
- `runner-portal` completa la experiencia sin tablas, SQL ni propiedad sobre datos de otros módulos.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro del diseño detallado de `runner-portal`.

Antes de implementar deberán producirse OpenAPI, componentes React, rutas SPA, catálogo común de Problem Details, límites de página medidos, pruebas de accesibilidad y pruebas de integración modular.

Bloqueantes para datos reales y producción:

| Bloqueante | Responsable | Tratamiento exigido |
| --- | --- | --- |
| Privacidad de plan, ubicación y seguimiento | Responsable del tratamiento con Revisor de privacidad o DPO | Confirmar bases, información, retención, derechos, acceso y tratamiento del comentario. |
| EIPD y acceso global | Responsable del tratamiento | Completar la EIPD, revisar riesgos de portal y aceptar riesgo residual antes de datos reales. |
| Seguridad de sesión y retorno | Revisor de seguridad | Probar aislamiento, CSRF, retorno tras login, caché, enlaces y respuestas no enumerables. |
| Operación y observabilidad | Persona operadora | Configurar alertas agregadas, revisar telemetría y demostrar ausencia de datos personales. |
| Evidencia técnica final | Revisor de arquitectura | Ejecutar pruebas de contrato, accesibilidad, volumen, retención y módulos sobre el stack real. |
