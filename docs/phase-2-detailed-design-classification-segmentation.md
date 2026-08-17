# Diseño detallado de clasificación y segmentación — Fase 2

**Estado:** Validado para diseño y desarrollo con datos sintéticos
**Fecha:** 2026-08-17
**Responsable de revisión:** Revisor de arquitectura
**Restricción:** Prohibido tratar datos personales reales hasta completar la revisión especializada de privacidad exigida por `ADR-0010`, `ADR-0018` y `ADR-0019`

## Propósito y alcance

Diseñar `classification-segmentation`, propietario de las taxonomías controladas, las asignaciones a corredores, los segmentos dinámicos, sus reglas, excepciones e historial operativo. El documento materializa `RF-02` a `RF-06` y la contribución de clasificación a `RF-08`, `RF-09` y `RF-10`.

Incluye:

- definiciones de etiquetas y valores permitidos;
- modalidad protegida `en-linea` o `presencial`;
- asignación individual y por lote a corredores activos;
- segmentos activos e inactivos, reglas y excepciones;
- coordinación transaccional con grupos de planificación;
- evaluación completa por criterio para cada corredor activo;
- impacto anterior y posterior de las mutaciones;
- historial operativo, retención, permisos y auditoría;
- recursos HTTP, modelo persistente, concurrencia, observabilidad y pruebas.

No incluye perfiles, cuentas, grupos, planes, publicaciones ni seguimiento. Tampoco incluye importación CSV, reglas libres, operadores configurables, pertenencia materializada como fuente de verdad, búsqueda de corredores por etiquetas o grupos ni edición de datos reales antes de superar los gates de privacidad.

## Fuentes normativas

Este diseño aplica:

- [Requisitos de Fase 1](phase-1-requirements.md), [criterios de aceptación](phase-1-acceptance-criteria.md) y [matriz de decisiones](phase-1-decision-matrix.md), especialmente `RF-02` a `RF-06`, `RF-08`, `D-01`, `D-02`, `D-05` y `D-08`;
- `ADR-0004`: administrador y entrenador operan clasificación con los límites precisados por `ADR-0018`;
- `ADR-0005`: taxonomías controladas, cardinalidad, modalidad, regla y excepciones;
- `ADR-0006`: grupos exclusivos y rechazo completo de solapamientos;
- `ADR-0007`: destinatarios congelados desde la primera publicación;
- `ADR-0010`: privacidad, retención, derechos y datos sintéticos antes de producción;
- `ADR-0012`: PostgreSQL, coordinación, bloqueos, restricciones e índices;
- `ADR-0014`: propiedad modular y dependencias permitidas;
- `ADR-0015`: `ActorContext`, políticas de aplicación y autorización en consultas;
- `ADR-0017`: API HTTP orientada a recursos;
- `ADR-0018`: estados y elegibilidad del corredor;
- `ADR-0019`: coordinación operativa, ciclo de vida, lotes, evaluación e historial;
- [Guía de diseño de API HTTP](api-design-guidelines.md).

Si este documento contradice una fuente aceptada, prevalece el ADR y deberá corregirse el diseño antes de implementar.

## Decisiones confirmadas

1. Una definición y cada valor permitido tienen nombre normalizado, identificador estable y estado activo o inactivo.
2. Cada corredor tiene como máximo un valor por definición; sustituirlo es una única operación.
3. La modalidad usa una definición protegida y los valores protegidos `en-linea` y `presencial`; no es obligatoria para activar al corredor.
4. Un segmento tiene al menos un criterio, Y entre definiciones, uno o varios valores dentro de cada criterio y una única excepción por segmento y corredor.
5. El segmento es dinámico y no persiste miembros como fuente de verdad.
6. `planning` coordina toda mutación de clasificación que pueda cambiar miembros efectivos de grupos.
7. Los segmentos tienen estado `active` o `inactive`, no se borran físicamente y su desactivación no modifica grupos existentes.
8. La primera clasificación ocurre después de activar al corredor; una reactivación revisa y valida antes la clasificación conservada.
9. Existen asignaciones individuales y lotes explícitos, siempre atómicos.
10. Una mutación válida se aplica inmediatamente y devuelve su impacto anterior y posterior; no existe previsualización persistente.
11. La consulta del segmento explica todos los criterios para cada corredor activo, además del resultado base y la excepción.
12. Administrador y entrenador consultan un historial operativo inmutable sin texto libre.
13. Ningún cambio modifica destinatarios de publicaciones ya congelados.
14. Buscar corredores por etiquetas, segmentos o grupos permanece aplazado a `MF-004`.
15. Un corredor `pending_reactivation` reserva su pertenencia hipotética para todas las validaciones de exclusividad hasta que la invitación se acepte, cancele o caduque.

## Lenguaje ubicuo

- **Definición de etiqueta:** clasificación administrada con nombre visible y valores cerrados.
- **Valor permitido:** opción única dentro de una definición.
- **Asignación:** valor vigente de una definición para un corredor.
- **Regla de segmento:** conjunto completo de criterios que se evalúan con Y.
- **Criterio:** definición y conjunto no vacío de valores aceptados para ella.
- **Excepción:** modo persistente `inclusion` o `exclusion` para una pareja segmento-corredor.
- **Resultado base:** corredores activos que cumplen todos los criterios.
- **Resultado efectivo:** `(resultado base ∪ inclusiones) − exclusiones`.
- **Evaluación de corredor:** explicación derivada de criterios, resultado base, excepción y resultado efectivo.
- **Impacto de clasificación:** diferencia confirmada entre asignaciones, segmentos y grupos antes y después de una mutación.
- **Cambio de clasificación:** registro inmutable de una mutación confirmada.
- **Coordinación de planificación:** caso de uso de `planning` que protege la exclusividad de grupos.

No se usarán `tag`, `label`, `cohorte`, `lista` o `audiencia` como sinónimos de los términos de negocio en documentación o contratos públicos. Los nombres ingleses se reservarán para código, OpenAPI y persistencia.

## Límites modulares y dependencias

`classification-segmentation` gobierna exclusivamente su esquema `classification_segmentation`, sus reglas locales y sus consultas. Puede consumir la API de `runner-management` para:

- comprobar estado y elegibilidad de corredores;
- obtener presentaciones mínimas de corredores activos por lotes;
- obtener, solo para administración, un corredor inactivo durante reactivación.

`planning` consume las APIs publicadas de clasificación y corredores. En cada validación de exclusividad trata conjuntamente los corredores `active` y los `pending_reactivation` como reservas hipotéticas, aunque estos últimos no formen parte de resultados efectivos. Sus coordinadores son la única entrada de aplicación para:

- asignar, sustituir o retirar etiquetas cuando el corredor está activo;
- aplicar lotes de asignaciones;
- reemplazar reglas de segmentos;
- crear, modificar o retirar excepciones de segmentos;
- validar la clasificación conservada antes de iniciar una reactivación.

Las operaciones locales que no cambian miembros efectivos ni reservas permanecen en clasificación: administrar definiciones y valores, crear un segmento inicial todavía no asociado, cambiar el estado de un segmento, modificar la clasificación dormida de un `inactive` dentro de su revisión administrativa y consultar evaluaciones o historial. Modificar la clasificación de un `pending_reactivation` sí usa la coordinación porque puede cambiar su reserva.

No habrá llamadas HTTP internas, acceso SQL entre esquemas, imports de paquetes internos ni eventos usados para decidir una transacción. Los hechos posteriores al commit podrán alimentar métricas o documentación, pero no sustituirán la coordinación síncrona.

Los adaptadores HTTP de asignaciones, reglas y excepciones permanecerán en `classification-segmentation` y dependerán de un puerto de coordinación definido por ese módulo. `planning`, que ya puede importar su API, aportará la implementación del puerto y coordinará la transacción. El mismo patrón se aplicará al adaptador de reactivación de `runner-management`: el módulo define el puerto, `planning` lo implementa y ninguna clase de corredor o clasificación importa código de planificación.

## Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Mutar clasificación sin validar grupos | Un corredor queda en dos grupos efectivos. | Única entrada coordinada desde `planning`, bloqueo global y rollback completo. |
| Introducir un ciclo entre módulos | Frontera imposible de verificar y acoplamiento de datos. | `planning` consume clasificación; nunca se añade la dependencia inversa. |
| Desactivar un segmento y vaciar un grupo silenciosamente | Planes futuros con destinatarios inesperados. | El segmento inactivo sigue evaluándose en asociaciones existentes hasta retirarlo explícitamente. |
| Clasificar un alta todavía pendiente | La activación de acceso podría fallar por planificación. | Prohibir asignaciones y excepciones durante `pending_activation`. |
| Reactivar con etiquetas obsoletas | Conflicto con grupos actuales. | Revisión administrativa y evaluación hipotética antes de emitir la invitación. |
| Cambiar clasificación o grupos mientras una reactivación está pendiente | La validación previa queda obsoleta y la activación podría crear un conflicto. | Tratar `pending_reactivation` como reserva hipotética en toda validación coordinada. |
| Revelar al entrenador una reserva conflictiva | Exposición de un corredor no operativo. | Respuesta de conflicto sin identidad para entrenador y detalle solo para administrador auditado. |
| Lote parcialmente aplicado | Estado difícil de reparar y grupos incoherentes. | Transacción única, precondiciones comunes y rechazo total. |
| Impacto válido pero inesperado | Traslado operativo no detectado. | Confirmación de selección en interfaz y respuesta anterior/posterior completa. |
| Evaluación completa costosa | Latencia o presión sobre PostgreSQL. | Paginación por corredor, consultas por lotes, revisión ligada a versión e índices medidos. |
| Historial con datos excesivos | Exposición y retención innecesarias. | Esquema cerrado, sin texto libre, acceso por rol y política de supresión o anonimización. |
| Usar historial para reconstruir publicaciones | Incoherencia histórica. | Las instantáneas de `publication` son la única fuente de destinatarios publicados. |

## Arquitectura de aplicación

El módulo mantendrá puertos de entrada separados para administración local, mutaciones coordinadas y consultas. Las operaciones coordinadas no serán invocables mediante un atajo que omita `planning`.

Flujo común de una mutación coordinada:

1. el adaptador HTTP transforma la identidad en `ActorContext` y un comando cerrado;
2. el servicio coordinador de `planning` autoriza, adquiere la fila de coordinación y fija una correlación;
3. consulta mediante APIs publicadas el estado anterior y los corredores afectados;
4. llama al puerto de clasificación, que vuelve a autorizar y aplica sus invariantes locales;
5. `planning` calcula los grupos efectivos afectados con el nuevo estado dentro de la misma transacción;
6. ante conflicto lanza un error de dominio que revierte clasificación e historial;
7. si el estado es válido, clasificación registra el cambio inmutable y el coordinador devuelve el impacto;
8. el commit hace visible conjuntamente la clasificación y su historial.

Los comandos de clasificación no aceptarán nombres de actor, rol o estado de corredor enviados por el cliente. Esos datos se resolverán desde `ActorContext` y `runner-management`.

## Modelo persistente

El esquema `classification_segmentation` contendrá como mínimo:

| Relación | Contenido e invariantes principales |
| --- | --- |
| `tag_definition` | UUID, nombre visible, clave canónica única, estado, `system_key` opcional único y versión optimista. |
| `tag_value` | UUID, definición, nombre, clave canónica única dentro de la definición, estado, `system_key` opcional y versión. |
| `runner_tag_assignment` | Corredor, definición y valor coherente; una fila única por corredor y definición. |
| `runner_classification_state` | Corredor y revisión monotónica; serializa cualquier cambio de su clasificación con el inicio de reactivación. |
| `segment` | UUID, nombre y clave canónica únicos, estado `active` o `inactive`, revisión y metadatos mínimos. |
| `segment_criterion` | Segmento y definición; una fila única por pareja y orden estable de presentación. |
| `segment_criterion_value` | Criterio y valor perteneciente a su definición; una fila única por pareja. |
| `segment_exception` | Segmento, corredor y modo `inclusion` o `exclusion`; una fila única por pareja. |
| `tag_assignment_batch` | UUID, actor, instante, entrada inmutable, resultado confirmado y correlación; no admite reejecución ni modificación. |
| `classification_change` | UUID, correlación, lote opcional, actor, instante, tipo y UUID del recurso, operación y estados cerrados anterior/posterior. |

El historial podrá almacenar estados anterior y posterior en `JSONB` versionado porque sus formas difieren por recurso, pero el esquema lógico será cerrado y validado por código: solo identificadores, claves, estados y valores controlados. No admitirá campos arbitrarios, comentarios, nombres de corredores ni contenido deportivo.

Las relaciones hacia corredores usarán UUID opacos sin `FOREIGN KEY` física entre esquemas. La existencia, estado y acceso se comprobarán mediante la API de `runner-management`; no se consultará `runner_management.runner` desde jOOQ de clasificación.

La forma canónica de nombres se calculará en aplicación mediante Unicode NFC, eliminación de espacios exteriores y conversión independiente de la configuración regional. PostgreSQL aplicará unicidad sobre la clave persistida, no sobre el nombre visible.

La migración inicial insertará la definición protegida de modalidad y los valores `en-linea` y `presencial` mediante `system_key` estable. Reejecutar la migración no creará duplicados y ninguna operación pública podrá cambiar o eliminar esas claves.

## Invariantes

- Una definición activa puede recibir valores nuevos; una inactiva no.
- Una definición no se desactiva mientras conserve valores activos.
- Un valor inactivo no se usa en nuevas asignaciones o criterios, pero las referencias anteriores siguen evaluándose.
- Un corredor conserva como máximo una asignación por definición.
- Cada cambio de asignación o excepción de un corredor bloquea e incrementa su `runner_classification_state`.
- El valor de una asignación pertenece a la definición indicada.
- Un segmento se crea con nombre único y una regla completa con al menos un criterio.
- Cada segmento tiene como máximo un criterio por definición y cada criterio contiene al menos un valor activo de esa definición.
- Un segmento inactivo no admite cambios de nombre, regla o excepciones ni asociaciones nuevas a grupos.
- Una excepción conserva un único modo por segmento y corredor, incluso cuando sea redundante.
- Solo corredores `active` participan en resultados efectivos y operaciones ordinarias.
- Todo `pending_reactivation` participa como reserva hipotética en la validación de exclusividad, nunca como miembro efectivo.
- La reserva se deriva del estado del corredor, no persiste una pertenencia ni congela un grupo, y termina al aceptar, cancelar o caducar la invitación.
- Una operación coordinada nunca confirma un estado con más de un grupo efectivo por corredor.
- Una publicación congelada no se consulta ni modifica al recalcular clasificación.
- Cada cambio confirmado y su historial comparten transacción y correlación.

## Casos de uso

### Administrar taxonomías

Solo el administrador crea definiciones y valores, modifica nombres y cambia estados. Cada mutación exige `If-Match` cuando reemplaza una revisión existente. La respuesta informa del recurso actualizado y no calcula grupos porque estas operaciones preservan referencias existentes.

Desactivar un valor impide nuevos usos, pero no elimina asignaciones o criterios. La interfaz mostrará cuántas referencias activas permanecen para que el administrador pueda retirarlas de forma explícita. La definición protegida de modalidad y sus valores no admiten desactivación.

### Crear y cambiar el estado de segmentos

Administrador y entrenador crean un segmento activo enviando nombre y regla completa. La creación se rechaza si cualquier criterio es inválido; nunca persiste un segmento sin regla. Un segmento no asociado todavía a grupos puede crearse localmente porque no cambia pertenencias.

Cambiar `status` a `inactive` lo congela para edición y nuevos usos, pero no altera grupos existentes. Reactivarlo recupera la edición. Retirarlo de un grupo pertenece a `planning` y puede cambiar pertenencias, por lo que usa su coordinación.

### Asignar etiquetas

La asignación individual sustituye de forma idempotente el valor de una definición o elimina la relación. Solo admite corredores `active` en el flujo ordinario. `planning` captura asignación y grupo anteriores, aplica el cambio, valida y devuelve el impacto.

El lote recibe una definición, un valor o retirada y una colección explícita sin duplicados de corredores activos. El servidor limita el tamaño por contrato y rechaza todo el lote si un elemento no es válido. No interpreta un filtro como selección implícita y no continúa después de un error.

### Modificar reglas y excepciones

La regla se reemplaza completa, no se parchean criterios de forma independiente. Esto evita estados intermedios sin criterios y permite validar una única revisión mediante `If-Match`. `planning` vuelve a evaluar los corredores alcanzables por los grupos que referencian el segmento.

Una excepción se identifica por segmento y corredor. `PUT` crea o sustituye su modo y `DELETE` la retira idempotentemente. Las excepciones redundantes se conservan y aparecen en la evaluación completa.

### Revisar y reactivar un corredor

Un alta inicial `pending_activation` rechaza cualquier asignación o excepción. Después de la activación podrá permanecer sin modalidad, segmento o grupo hasta que administrador o entrenador lo clasifiquen.

Durante `inactive`, solo el administrador consulta y modifica la clasificación conservada desde la revisión del corredor. Esos cambios permanecen dormidos, se registran en el historial y no afectan a grupos actuales. Para iniciar `pending_reactivation`, `planning` bloquea `runner_classification_state`, evalúa al corredor como activo con una revisión estable, rechaza conflictos y, si es válido, invoca a `runner-management`, que emite la invitación mediante `identity-access` antes de liberar la transacción.

Mientras dure `pending_reactivation`, toda mutación coordinada vuelve a evaluar al corredor como reserva hipotética. Puede cambiar su grupo proyectado si el nuevo estado sigue siendo exclusivo, pero no puede quedar proyectado en varios grupos. La reserva no aparece en segmentos o grupos efectivos y termina automáticamente cuando `runner-management` publica `active` o vuelve a `inactive` por aceptación, cancelación o caducidad. Por ello la aceptación no necesita una segunda confirmación ni un estado manual de reparación.

Un entrenador cuya mutación choque con una reserva recibe un `409 Conflict` que identifica el tipo de conflicto y los grupos permitidos por su alcance, pero no el UUID ni la presentación del corredor. El administrador puede consultar el detalle desde el flujo auditado de reactivación.

### Consultar evaluación completa

La consulta parte de una página de corredores activos proporcionada por `runner-management` y evalúa esos UUID en clasificación mediante una consulta por lote. Cada fila contiene presentación mínima, asignaciones relevantes, resultado de todos los criterios, resultado base, excepción y resultado efectivo.

El cursor queda ligado al segmento, su revisión, filtros y orden. Si la regla cambia entre páginas, el cursor se rechaza como obsoleto para impedir mezclar evaluaciones de revisiones distintas. La consulta no promete una instantánea global frente a cambios de nombres de corredores, pero conserva orden total por presentación canónica e identificador.

### Consultar historial

Administrador y entrenador consultan cambios por recurso, corredor, actor, tipo o intervalo temporal mediante cursor. El entrenador solo recibe configuración no personal y cambios de corredores actualmente activos; el administrador puede revisar los inactivos conforme al flujo auditado de reactivación. La respuesta no ofrece restauración, deshacer ni edición. Los intentos rechazados pertenecen a observabilidad o seguridad y no crean `classification_change` de negocio.

## Permisos

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Crear o modificar definiciones y valores | Sí | No | No |
| Consultar taxonomías | Sí | Sí | No |
| Asignar etiquetas a corredores activos | Sí | Sí | No |
| Aplicar lotes sobre corredores activos | Sí | Sí | No |
| Crear y administrar segmentos | Sí | Sí | No |
| Gestionar reglas y excepciones de activos | Sí | Sí | No |
| Evaluar segmentos sobre corredores activos | Sí | Sí | No |
| Consultar historial operativo | Sí | Sí, solo configuración y corredores actualmente activos | No |
| Revisar y modificar clasificación de inactivos | Sí, desde reactivación | No | No |

Las políticas se aplican antes de leer o modificar y se repiten dentro de cada módulo llamado. Las listas, evaluaciones, conteos e historial usan el mismo predicado de autorización que el acceso individual.

## APIs internas publicadas

`classification-segmentation` publicará tipos propios para:

- consultar definiciones y valores activos o históricos autorizados;
- crear un segmento inicial con regla completa;
- aplicar provisionalmente asignaciones individuales o por lote;
- reemplazar una regla y crear, sustituir o retirar una excepción;
- evaluar corredores o segmentos con una revisión concreta;
- registrar el historial de una mutación confirmada;
- suprimir o anonimizar datos vinculados a un corredor conforme a retención.

`planning` publicará coordinadores para:

- modificar clasificación con validación de grupos;
- devolver impacto o conflictos;
- validar la reincorporación de un corredor antes de reactivarlo;
- incluir reservas `pending_reactivation` en toda validación hasta que su estado termine.

Los contratos transportarán `ActorContext`, correlación y revisiones explícitas. Los puertos coordinadores definidos por los propietarios tendrán una única implementación productiva en `planning`; los módulos no ofrecerán una implementación local permisiva. No expondrán jOOQ, entidades persistentes, nombres de esquema ni colecciones mutables compartidas.

## API HTTP prevista

OpenAPI será la fuente de verdad antes de implementar. Las operaciones previstas son:

| Actor | Método y recurso | Semántica |
| --- | --- | --- |
| Administrador o entrenador | `GET /api/tag-definitions` | Consultar definiciones y valores permitidos; el estado inactivo es un filtro autorizado. |
| Administrador | `POST /api/tag-definitions` | Crear una definición con identidad propia; `201` y `Location`, con idempotencia documentada. |
| Administrador | `PATCH /api/tag-definitions/{tagDefinitionId}` | Modificar nombre o estado con `If-Match`. |
| Administrador | `POST /api/tag-definitions/{tagDefinitionId}/values` | Crear un valor dependiente de la definición. |
| Administrador | `PATCH /api/tag-definitions/{tagDefinitionId}/values/{tagValueId}` | Modificar nombre o estado con `If-Match`. |
| Administrador o entrenador | `PUT /api/runners/{runnerId}/tag-assignments/{tagDefinitionId}` | Crear o sustituir idempotentemente la asignación conocida. |
| Administrador o entrenador | `DELETE /api/runners/{runnerId}/tag-assignments/{tagDefinitionId}` | Retirar idempotentemente la asignación. |
| Administrador o entrenador | `POST /api/tag-assignment-batches` | Crear un lote inmutable y su resultado confirmado; `201`, `Location` e idempotencia obligatoria. |
| Administrador o entrenador | `GET /api/tag-assignment-batches/{tagAssignmentBatchId}` | Consultar la entrada y el resultado inmutables de un lote visible. |
| Administrador o entrenador | `GET /api/segments` | Consultar segmentos paginados por nombre y estado. |
| Administrador o entrenador | `POST /api/segments` | Crear un segmento con nombre y regla completos. |
| Administrador o entrenador | `GET /api/segments/{segmentId}` | Consultar estado, regla, excepciones y revisión. |
| Administrador o entrenador | `PATCH /api/segments/{segmentId}` | Cambiar nombre o estado con `If-Match`; un inactivo solo admite reactivación. |
| Administrador o entrenador | `PUT /api/segments/{segmentId}/rule` | Reemplazar la regla completa con `If-Match`. |
| Administrador o entrenador | `PUT /api/segments/{segmentId}/exceptions/{runnerId}` | Crear o sustituir el modo de la excepción. |
| Administrador o entrenador | `DELETE /api/segments/{segmentId}/exceptions/{runnerId}` | Retirar la excepción de forma idempotente. |
| Administrador o entrenador | `GET /api/segments/{segmentId}/evaluations` | Consultar evaluación completa y paginada por corredor activo. |
| Administrador o entrenador | `GET /api/classification-changes` | Consultar historial inmutable con filtros y cursor. |

`tag-assignment-batch` es un recurso real porque tiene UUID, actor, instante, entrada inmutable, resultado y correlación con historial. No representa un comando nominalizado ni admite estados asíncronos. La respuesta de cada mutación incluye impacto confirmado; no existe `/preview`, `/apply`, `/activate` ni prefijo por rol.

Las rutas de asignaciones y excepciones admiten un corredor inactivo únicamente para el administrador y dentro de una revisión de reactivación vigente. En ese caso la mutación es local, permanece dormida y no calcula grupos; el entrenador y cualquier uso fuera de ese flujo reciben la misma denegación que para un recurso no visible.

Los `POST` con riesgo de duplicado usarán una clave idempotente y unicidad persistida. Los recursos mutables expondrán `ETag`; una precondición obsoleta devolverá `412 Precondition Failed`. Conflictos de reglas, estados o grupos devolverán `409 Conflict` mediante Problem Details estable y sin persistencia parcial. Las colecciones crecientes usarán cursor opaco y límite acotado.

## Concurrencia y consistencia

- Las operaciones locales usan actualizaciones condicionadas por revisión y restricciones únicas.
- Toda mutación individual de asignaciones o excepciones bloquea la revisión de clasificación del corredor; el inicio de reactivación conserva ese mismo bloqueo hasta confirmar `pending_reactivation`.
- Toda mutación que pueda cambiar grupos bloquea primero la fila de coordinación del club.
- La consulta de candidatos para exclusividad incluye en una misma transacción corredores `active` y reservas `pending_reactivation` obtenidos mediante la API publicada por `runner-management`.
- El orden posterior es segmentos, corredores y filas dependientes por UUID estable.
- El lote elimina duplicados de entrada antes de bloquear y procesa UUID en orden estable.
- La regla nueva se escribe y evalúa dentro de la misma transacción; un conflicto revierte la revisión.
- La evaluación de lectura no toma el bloqueo global, pero queda ligada a una revisión del segmento.
- El historial se inserta antes del commit y nunca se escribe en una transacción separada.
- Las publicaciones confirmadas no se bloquean ni recalculan; una primera publicación adquiere por su lado la misma coordinación conforme a `ADR-0012`.

## Consultas e índices

Índices mínimos candidatos:

- unicidad de claves canónicas de definiciones, valores por definición y segmentos;
- valores por definición y estado;
- asignación por corredor-definición y por valor-corredor;
- criterios por segmento-definición y valores por criterio;
- excepciones por segmento-corredor y por corredor;
- segmentos por estado y clave canónica;
- historial por instante e identificador, recurso, corredor, actor y correlación;
- índices parciales para elementos activos cuando el predicado coincida con la consulta.

La evaluación usará consultas relacionales y CTEs sobre la página de UUID recibida, sin cargar todas las filas en memoria. Los planes se medirán con más de `500` corredores, segmentos solapados, excepciones redundantes y reglas con varios criterios. No se añadirá caché antes de demostrar un cuello de botella.

## Retención y privacidad

Las asignaciones, excepciones, revisiones por corredor y lotes vinculados se conservan durante su inactividad conforme a `ADR-0018`, sin participar en conjuntos efectivos. La supresión anticipada o el vencimiento eliminan asignaciones, excepciones y `runner_classification_state` y suprimen o anonimizan irreversiblemente los identificadores de corredor presentes en lotes e historial cuando el evento no necesite seguir siendo personal.

Los cambios de definiciones, valores, segmentos y reglas sin referencia personal se conservan `24` meses desde el evento. El job de retención es idempotente, auditable y reaplica supresiones después de restaurar copias.

Hasta superar la revisión especializada, todos los ejemplos, migraciones de prueba, fixtures y cargas de rendimiento usarán datos sintéticos. Los estados anterior y posterior del historial se consideran datos de negocio restringidos, no contenido apto para logs o métricas.

## Observabilidad

Se registrarán métricas agregadas de:

- mutaciones individuales y por lote, tamaño y duración;
- conflictos por exclusividad, sin UUID de corredor como etiqueta;
- evaluaciones por tamaño de página, criterios y latencia;
- cambios por tipo de recurso;
- jobs de retención completados, fallidos o retrasados.

Las trazas podrán incluir correlación opaca, módulo y operación, pero no nombres, asignaciones, reglas completas ni estados anterior/posterior. Las alertas cubrirán conflictos sostenidos, latencia de evaluación, lotes fuera de umbral y fallos de retención.

## Validación prevista

### Taxonomías y modalidad

- Probar Unicode NFC, espacios exteriores, unicidad canónica y nombres vacíos.
- Probar cardinalidad por corredor y definición, sustitución y retirada.
- Probar definición y valores protegidos de modalidad, incluidas claves estables e idempotencia de migración.
- Probar estados activos e inactivos y conservación de referencias existentes.

### Segmentos y evaluación

- Probar creación con regla completa y rechazo sin criterios, criterios duplicados o referencias incompatibles.
- Probar Y entre criterios, varios valores por criterio y ausencia de asignación.
- Probar inclusiones, exclusiones y excepciones redundantes persistentes.
- Probar estado inactivo, congelación de edición, continuidad en grupos existentes y reactivación.
- Probar evaluación completa de cada criterio, resultado base y resultado efectivo para cada corredor activo.
- Probar cursor ligado a revisión y rechazo después de modificar la regla.

### Coordinación y lotes

- Probar que asignaciones, reglas y excepciones capaces de cambiar grupos pasan por `planning` y toman el bloqueo.
- Probar impacto anterior/posterior y conflictos con corredores y grupos afectados.
- Probar rollback inyectando fallo en cada paso, incluido el historial.
- Probar lote válido, duplicados, mezcla de estados, referencias inválidas, precondición obsoleta y varios conflictos.
- Ejecutar carreras entre lotes, reglas, grupos y primera publicación.

### Ciclo del corredor

- Rechazar clasificación durante `pending_activation` inicial.
- Permitir corredor activo sin modalidad y excluirlo de criterios que la requieran.
- Excluir todos los estados no activos de evaluaciones y grupos.
- Probar revisión administrativa de clasificación inactiva y reingreso hipotético antes de reactivar.
- Probar reserva durante `pending_reactivation`, cambio proyectado sin solapamiento, rechazo de conflictos y finalización por aceptación, cancelación o caducidad.
- Ejecutar una carrera entre modificación dormida de un `inactive` e inicio de reactivación y comprobar que la validación usa una revisión completa, nunca una mezcla.
- Ejecutar carreras entre aceptación, caducidad y mutaciones de asignaciones, reglas, excepciones y grupos.
- Probar supresión y anonimización coordinadas al vencer la retención.

### Permisos, API e historial

- Probar toda la matriz de rol, recurso y estado, incluidas listas, evaluaciones e historial.
- Probar que un conflicto con una reserva no revela el corredor al entrenador y sí ofrece detalle al administrador autorizado.
- Probar `ETag`, `If-Match`, idempotencia, cursores y Problem Details.
- Probar historial anterior/posterior, correlación, ausencia de texto libre y autorización.
- Revisar semántica de cada recurso HTTP y generar OpenAPI, servidor y cliente antes de implementar.
- Verificar límites modulares con Spring Modulith, ArchUnit y pruebas conjuntas sobre PostgreSQL.
- Probar que los adaptadores de clasificación y reactivación resuelven la implementación coordinada de `planning` y que la aplicación falla al arrancar si falta o se duplica.

### Rendimiento

- Cargar más de `500` corredores sintéticos y cardinalidades representativas.
- Medir asignación individual, lote máximo, cambio de regla, evaluación paginada e historial.
- Revisar `EXPLAIN (ANALYZE, BUFFERS)` y fijar límites de lote y página antes de cerrar OpenAPI.

## Alternativas descartadas

- Dependencia directa de clasificación hacia planificación: crea ciclo.
- Unir módulos: pierde propiedad explícita por evitar una coordinación ya soportada por el monolito modular.
- Conflictos temporales: contradicen la exclusividad aceptada.
- Borrado físico de segmentos: pierde intención e historial.
- Listas manuales o miembros persistidos como fuente de verdad: contradicen `ADR-0005`.
- Operación únicamente individual: no escala al volumen validado.
- Importación CSV: requiere otro diseño de formato, validación y errores.
- Previsualización persistente: añade un proceso de dos pasos sin requisito.
- Revalidar solo al aceptar una reactivación: permitiría una cuenta activada con el corredor bloqueado por un conflicto sobrevenido.
- Evaluación resumida: no satisface la explicación criterio por criterio confirmada.
- Estado actual sin historial: impide investigar cambios globales.
- SQL entre esquemas, triggers cruzados o caché canónica: rompen propiedad modular o la fuente de verdad.

## Conclusiones

- La semántica aceptada de `ADR-0005` queda materializada sin crear un motor genérico de reglas.
- `planning` protege la exclusividad de grupos sin invertir dependencias ni apropiarse de datos de clasificación.
- La operación individual y masiva comparte garantías, impacto y rollback.
- La evaluación completa y el historial priorizan explicabilidad, con coste de consulta y privacidad explícitamente asumido.
- La reactivación valida la clasificación conservada antes de emitir acceso y la activación inicial permanece desacoplada.
- Las reservas de reactivación mantienen la exclusividad durante la espera sin convertir al corredor en miembro efectivo ni congelar un grupo.
- Las publicaciones históricas no dependen de reconstruir taxonomías o segmentos actuales.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro de `classification-segmentation`.

- Antes de implementar deberán producirse OpenAPI, migraciones Flyway, tipos jOOQ, límites medidos de lote y página, catálogo de Problem Details y pruebas transaccionales.
- La búsqueda de corredores por etiquetas, segmentos o grupos continúa aplazada a `MF-004`.
- El tratamiento de datos personales reales y la producción continúan bloqueados hasta completar las evidencias de privacidad aplicables.
