# ADR-0019: Coordinación, ciclo de vida e historial de clasificación

**Estado:** Aceptado
**Fecha:** 2026-08-17
**Responsable de revisión:** Revisor de arquitectura
**Validación documental:** Decisiones `CS-01` a `CS-08` aceptadas explícitamente por el responsable el 2026-08-17

## Contexto

`ADR-0005` define taxonomías controladas, asignaciones con un único valor por definición, segmentos dinámicos y excepciones manuales. `ADR-0006` exige rechazar cualquier cambio de clasificación que sitúe a un corredor en dos grupos efectivos y `ADR-0012` materializa esa garantía con una fila única de coordinación de planificación y una transacción PostgreSQL.

La dirección de dependencias de `ADR-0014` permite que `planning` consuma las APIs de `classification-segmentation` y `runner-management`, pero prohíbe la dependencia inversa. Por tanto, el módulo propietario de la clasificación no puede consultar grupos para validar por sí mismo la exclusividad sin introducir un ciclo.

El diseño tampoco había decidido el ciclo de vida de los segmentos, cuándo se clasifica a un corredor todavía no operativo, si existen operaciones masivas, cómo se informa de su impacto, qué detalle ofrece la evaluación ni qué historial operativo se conserva. Resolver estas cuestiones solo durante la implementación ocultaría decisiones que afectan a API, permisos, transacciones, datos personales y pruebas.

## Decisión

### Coordinación transaccional

`classification-segmentation` seguirá siendo propietario de definiciones, valores, asignaciones, segmentos, criterios, excepciones e historial de clasificación. `planning` será propietario del caso de uso coordinador para toda mutación que pueda cambiar la pertenencia efectiva a grupos.

El coordinador de `planning` ejecutará, dentro de una única transacción:

1. autorización de la capacidad solicitada;
2. bloqueo de la fila de coordinación del club definida por `ADR-0012`;
3. captura del estado anterior necesario para explicar el impacto;
4. invocación síncrona de la API publicada por `classification-segmentation` para aplicar provisionalmente la mutación;
5. nueva evaluación de los corredores y grupos afectados;
6. rechazo completo si existe pertenencia a varios grupos, mostrando únicamente los corredores y grupos que el actor esté autorizado a conocer;
7. registro del cambio y confirmación de la transacción si el estado final es válido.

El módulo receptor volverá a autorizar su propia operación con el mismo `ActorContext`. No habrá acceso a tablas ajenas, dependencias inversas, eventos previos al commit ni compensaciones. Crear, renombrar, activar o desactivar definiciones y valores, y cambiar el estado de un segmento sin alterar sus relaciones existentes, permanecerán como casos de uso locales de clasificación porque no modifican por sí mismos la pertenencia efectiva.

Los adaptadores HTTP permanecerán junto al recurso propietario. Para una mutación coordinada dependerán de un puerto de coordinación definido por el módulo propietario e implementado en `planning`. La implementación importa las APIs publicadas de clasificación y corredores, dirección ya permitida por `ADR-0014`; los módulos propietarios solo conocen su propio puerto y no importan `planning`. Spring Modulith y ArchUnit verificarán esa dirección y las pruebas impedirán una implementación local que omita la coordinación.

La misma coordinación validará la reincorporación de un corredor inactivo. `planning` evaluará su clasificación conservada como si el corredor volviera a estar activo y solo después invocará a `runner-management` para iniciar `pending_reactivation`.

Mientras permanezca en `pending_reactivation`, `planning` lo incluirá como una reserva hipotética en toda validación de exclusividad. Las mutaciones coordinadas evaluarán conjuntamente corredores `active` y reservas `pending_reactivation` y rechazarán cualquier estado que situaría a uno de ellos en varios grupos. La reserva se deriva del estado publicado por `runner-management`, que entrega a la coordinación identificadores opacos y nunca presentaciones de corredores no visibles: no materializa miembros ni congela un grupo y termina cuando la invitación se acepta, cancela o caduca. El corredor seguirá fuera de segmentos y grupos efectivos hasta alcanzar `active`, pero aceptar la invitación no necesitará otra confirmación ni descubrirá tardíamente un conflicto permitido por el sistema.

Una mutación podrá cambiar el grupo hipotético de la reserva si el resultado conserva la exclusividad, igual que cambiaría dinámicamente el de un corredor activo. Si el actor es entrenador, un conflicto causado por una reserva se devolverá sin identidad ni datos del corredor no visible y requerirá intervención administrativa; el administrador sí podrá consultar el detalle dentro del flujo auditado de reactivación.

### Ciclo de vida del segmento

Cada segmento tendrá estado `active` o `inactive` y no se eliminará físicamente. Un segmento inactivo:

- no podrá asociarse a grupos nuevos ni modificar nombre, regla o excepciones;
- continuará evaluándose para los grupos que ya lo referencien, evitando cambios silenciosos;
- podrá retirarse explícitamente de esos grupos;
- podrá reactivarse por administrador o entrenador.

Desactivar o reactivar un segmento no añade ni retira miembros por sí mismo. La modificación explícita de un grupo sigue perteneciendo a `planning` y usa la coordinación de `ADR-0012`.

### Estado del corredor y clasificación

Un corredor en `pending_activation` inicial no admitirá asignaciones ni excepciones. Tras alcanzar `active`, administrador o entrenador podrán clasificarlo. La ausencia de modalidad u otras etiquetas será válida: el corredor no cumplirá los criterios que las requieran y podrá quedar sin grupo o plan.

Las asignaciones y excepciones de un corredor `inactive` o `pending_reactivation` permanecerán conservadas pero no participarán en resultados efectivos. Solo el administrador podrá revisarlas y modificarlas dentro del flujo de reactivación. Las modificaciones mientras esté `inactive` serán locales porque permanecen dormidas; durante `pending_reactivation` atravesarán la coordinación porque pueden cambiar la reserva. Antes de emitir la invitación, `planning` validará el posible reingreso contra los grupos actuales. El entrenador nunca localizará ni modificará corredores no activos.

### Operaciones individuales y por lote

El PMV permitirá asignar, sustituir o retirar un valor para un corredor activo y aplicar la misma modificación a un lote explícito de corredores activos seleccionados. No incluirá importación CSV ni selección implícita mediante filtros por etiquetas o grupos.

El lote será atómico: adquirirá una sola vez la coordinación de planificación, aplicará el conjunto completo y validará el estado final. Un identificador inválido, un corredor no activo, una precondición obsoleta o un conflicto entre grupos rechazará todo el lote; no existirán resultados parciales.

### Aplicación e impacto

Las mutaciones válidas se aplicarán inmediatamente y no crearán un flujo previo de simulación o confirmación en servidor. La interfaz confirmará el conjunto de corredores y el valor elegido antes de enviar la solicitud.

La respuesta confirmada incluirá un resumen del estado anterior y posterior: asignaciones modificadas, segmentos cuyo resultado cambió y grupos efectivos de los corredores afectados. Un rechazo por exclusividad devolverá los conflictos sin persistir clasificación ni historial de cambio.

### Evaluación explicada

Administrador y entrenador podrán consultar, con paginación, la evaluación completa de un segmento para cada corredor activo. Cada resultado indicará:

- valor asignado y cumplimiento de cada criterio;
- resultado base de la regla;
- excepción manual existente, aunque sea redundante en ese momento;
- resultado efectivo final.

La evaluación será derivada y no se persistirá como fuente de verdad. Los corredores no activos quedarán fuera de esta consulta operativa; el administrador revisará su clasificación únicamente desde el flujo individual de reactivación.

### Historial operativo

Cada mutación confirmada registrará de forma inmutable actor, instante, correlación, recurso, operación y estado anterior y posterior. El historial cubrirá definiciones, valores, asignaciones, segmentos, reglas, excepciones y cambios de estado. No admitirá comentarios ni texto libre. Será consultable por administrador y entrenador, pero el entrenador solo verá configuración no personal y cambios de corredores actualmente activos; los inactivos o pendientes permanecerán bajo acceso administrativo auditado.

Los registros vinculados a un corredor seguirán la conservación, supresión anticipada y anonimización de su clasificación conforme a `ADR-0018`. Los cambios de configuración no vinculados a una persona identificable se conservarán durante `24` meses desde el evento. El historial no sustituirá las instantáneas de destinatarios de `ADR-0007` ni permitirá reconstruir o alterar una publicación.

Hasta completar la revisión de privacidad exigida por `ADR-0010` y `ADR-0018`, el diseño y sus pruebas usarán exclusivamente datos ficticios, sintéticos o anonimizados de forma irreversible.

## Alternativas consideradas

### Alternativa A: `classification-segmentation` depende de `planning`

Se descarta porque crea la dependencia inversa de la ya necesaria para evaluar grupos, contradice `ADR-0014` e introduce un ciclo entre propietarios de datos.

### Alternativa B: Unir clasificación y planificación

Se descarta porque elimina un límite modular aceptado para resolver una única coordinación transversal. Mezclaría taxonomías reutilizables con grupos, planes y entrenamientos.

### Alternativa C: Permitir conflictos temporales entre grupos

Se descarta porque contradice `ADR-0006`, deja estados operativos inválidos y traslada el error a publicación o consulta.

### Alternativa D: Eliminar segmentos o retirarlos automáticamente de grupos

Se descarta porque perdería trazabilidad o cambiaría pertenencias sin una decisión explícita del entrenador. El estado inactivo evita usos nuevos sin modificar relaciones existentes.

### Alternativa E: Limitar las asignaciones a cambios individuales

Se descarta porque operar corredor por corredor no es coherente con una escala superior a `500` y perpetuaría trabajo repetitivo.

### Alternativa F: Exigir previsualización persistente antes de cada cambio

Se descarta para el PMV porque introduce un segundo recurso, caducidad y resolución de obsolescencia. La transacción inmediata con impacto explicado cubre el riesgo aceptado.

### Alternativa G: Mostrar solo miembros efectivos o una explicación resumida

Se descarta porque oculta qué criterio falló y dificulta corregir reglas, asignaciones o excepciones persistentes.

### Alternativa H: Conservar solo el estado actual

Se descarta porque varios actores globales podrían cambiar indirectamente los grupos sin dejar evidencia operativa suficiente para investigar el resultado.

### Alternativa I: Revalidar únicamente al aceptar la reactivación

Se descarta porque permitiría que cambios posteriores invalidaran la revisión administrativa y dejaran una cuenta ya activada con el corredor todavía fuera de la operación hasta otra intervención. La reserva hipotética mantiene la invariante durante la espera sin exigir otra confirmación al corredor.

## Consecuencias

- Se conserva el grafo de dependencias: `planning` coordina y consume clasificación y corredores; los módulos propietarios no dependen de él.
- Los casos de uso coordinados atraviesan módulos, pero siguen dentro de una transacción local y cada receptor autoriza y protege sus invariantes.
- La desactivación de segmentos evita nuevos usos sin convertir el estado en una operación destructiva o silenciosa.
- La activación inicial permanece desacoplada de planificación; la reactivación, que ya contiene revisión administrativa, valida antes el posible reingreso.
- Una reactivación pendiente puede hacer que una mutación posterior sea rechazada aunque el corredor todavía no sea operativo; el conflicto se mantiene reservado hasta aceptación, cancelación o caducidad.
- El entrenador no recibe la identidad de una reserva conflictiva y necesitará que el administrador revise el caso.
- Los lotes reducen trabajo repetitivo, pero aumentan el tamaño del impacto y exigen límites de petición, planes de consulta medidos y errores completos.
- La evaluación criterio por criterio mejora explicabilidad a costa de consultas más pesadas; deberá paginarse y medirse con datos representativos.
- El historial facilita investigación y responsabilidad operativa, pero añade datos personales derivados, volumen y obligaciones de retención y acceso.
- Las publicaciones continúan siendo históricas e inmutables; ningún cambio de clasificación modifica destinatarios ya congelados.

## Requisitos relacionados

- `RF-02`
- `RF-03`
- `RF-04`
- `RF-05`
- `RF-06`
- `RF-08`
- `RF-09`
- `RF-10`

## Decisiones de Fase 1 relacionadas

- `D-01`: segmentos dinámicos, grupos exclusivos y destinatarios publicados inmutables.
- `D-02`: modalidad dentro de la taxonomía controlada.
- `D-05`: gramática acotada y excepciones manuales.
- `D-08`: el entrenador opera globalmente solo sobre corredores `active`; el administrador conserva el ciclo completo y cada corredor queda aislado.

## Validación prevista

- Probar que toda mutación capaz de afectar grupos entra por el coordinador de `planning`, adquiere el bloqueo y usa la API publicada de clasificación.
- Inyectar fallos antes y después de la mutación provisional y comprobar rollback completo sin historial huérfano.
- Probar cambios individuales y lotes, incluidas sustitución, retirada, identificadores inválidos, estados no activos, precondiciones obsoletas y conflictos múltiples.
- Probar que desactivar un segmento bloquea nuevos usos y ediciones sin cambiar grupos existentes, y que reactivarlo recupera su capacidad de edición.
- Probar que un alta pendiente no admite clasificación y que un corredor activo sin modalidad no cumple criterios de modalidad.
- Probar revisión administrativa de clasificación inactiva, validación hipotética del reingreso e inicio de reactivación solo cuando no exista conflicto.
- Probar que todas las mutaciones con impacto en grupos incluyen reservas `pending_reactivation`, que una carrera no puede crear solapamiento y que la reserva termina al aceptar, cancelar o caducar.
- Probar que el conflicto de una reserva oculta identidad y datos al entrenador, pero ofrece detalle al administrador autorizado.
- Probar la respuesta de impacto con estados anterior y posterior y ausencia total de cambios cuando la operación se rechaza.
- Probar evaluación paginada por corredor con detalle de todos los criterios, resultado base, excepción redundante o efectiva y resultado final.
- Probar que un cambio entre páginas invalida una evaluación ligada a otra revisión del segmento y no mezcla reglas distintas.
- Probar historial inmutable, autorización, ausencia de texto libre, retención y supresión o anonimización de referencias personales.
- Medir asignaciones por lote, cambios de reglas y evaluación completa con más de `500` corredores mediante `EXPLAIN (ANALYZE, BUFFERS)`.
- Probar que publicaciones anteriores y sus republicaciones conservan exactamente sus destinatarios congelados.
- Verificar con Spring Modulith y ArchUnit que no aparecen ciclos, imports internos ni SQL entre esquemas.
- Verificar que los adaptadores HTTP usan el puerto coordinado y que la implementación efectiva pertenece a `planning`, sin rutas duplicadas ni atajos locales.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes para aceptar este ADR.

- Antes de implementar deberán producirse el contrato OpenAPI, migraciones Flyway, límites de lote medidos, consultas jOOQ, catálogo de Problem Details y pruebas de integración transaccional.
- Antes de tratar datos personales reales seguirán siendo obligatorias las evidencias y revisiones de privacidad de `ADR-0010`, `ADR-0018` y este ADR.
