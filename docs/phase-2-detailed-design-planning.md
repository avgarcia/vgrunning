# Diseño detallado de planificación — Fase 2

**Estado:** Validado
**Fecha:** 2026-08-18
**Responsable de revisión:** Revisor de arquitectura
**Restricción:** Prohibido tratar datos personales reales hasta completar la revisión especializada de privacidad exigida por `ADR-0010`, `ADR-0018`, `ADR-0019` y `ADR-0020`
**Validación documental:** Decisiones de planificación aceptadas explícitamente por el responsable el 2026-08-19

## Propósito y alcance

Diseñar `planning`, propietario de los grupos de planificación, sus excepciones, los planes semanales, entrenamientos, fases, bloques, objetivos y borradores. El documento materializa `RF-04`, `RF-07`, `RF-11` a `RF-13` y la contribución de planificación a `RF-08` a `RF-10` y `RF-14` a `RF-16`.

Incluye:

- grupos activos e inactivos y su composición;
- reconfiguración multigrupo atómica y miembros explicados;
- excepciones de grupo durante reactivación;
- planes semanales y su borrador de trabajo;
- entrenamientos completos, fases, bloques, cargas y recuperaciones;
- modalidad, lugar de encuentro y aclaraciones;
- objetivos por zona cardiaca y ritmo relativo;
- historial operativo, retención, permisos y auditoría;
- recursos HTTP previstos, modelo persistente, concurrencia, consultas y pruebas.

No incluye versiones publicadas, destinatarios congelados, visibilidad del corredor, notificaciones, seguimiento ni reglas temporales de primera publicación durante una semana ya comenzada. Esas responsabilidades pertenecen respectivamente a `publication`, `notification-delivery`, `runner-portal` y `tracking-review`. Tampoco incluye duplicación de planes, plantillas, búsqueda global de corredores por grupos o etiquetas, datos deportivos personales ni tipos de entrenamiento administrables.

## Fuentes normativas

Este diseño aplica:

- [Requisitos de Fase 1](phase-1-requirements.md), [criterios de aceptación](phase-1-acceptance-criteria.md) y [matriz de decisiones](phase-1-decision-matrix.md), especialmente `RF-04`, `RF-07`, `RF-08`, `RF-11` a `RF-14`, `D-01`, `D-02`, `D-04`, `D-06` y `D-08`;
- `ADR-0004`: administrador y entrenador gestionan globalmente planes y borradores;
- `ADR-0005`: segmentos dinámicos y modalidad del corredor como etiqueta;
- `ADR-0006`: grupos exclusivos y modelo de plan, fases, bloques, catálogo y objetivos;
- `ADR-0007`: borrador separado, publicación atómica, versiones y destinatarios congelados;
- `ADR-0010`: privacidad, retención, derechos y datos sintéticos antes de producción;
- `ADR-0012`: PostgreSQL, coordinación global, bloqueos, restricciones, cursores e índices;
- `ADR-0014`: propiedad modular y dependencia permitida desde `publication` hacia `planning`;
- `ADR-0015`: `ActorContext`, autorización en aplicación y alcance por recurso;
- `ADR-0017`: API HTTP orientada a recursos;
- `ADR-0018`: estados y elegibilidad del corredor, grupo anterior no restaurable y retención;
- `ADR-0019`: coordinación desde `planning`, segmentos inactivos y reservas de reactivación;
- `ADR-0020`: ciclo de vida, reconfiguración, objetivos, historial y retención de planificación;
- [Guía de diseño de API HTTP](api-design-guidelines.md).

Si este documento contradice una fuente aceptada, prevalece el ADR y deberá corregirse el diseño antes de implementar.

## Decisiones confirmadas

1. Un grupo tiene estado `active` o `inactive`, no se borra físicamente y puede reactivarse.
2. El actor elige el estado inicial; un grupo activo exige al menos un segmento y uno inactivo puede estar vacío.
3. Un grupo inactivo no produce miembros ni reservas, pero admite cambios de composición y creación o edición de borradores.
4. La reactivación valida el estado completo y puede rechazarse por conflictos aparecidos durante la inactividad.
5. Los traslados usan una reconfiguración multigrupo inmediata, atómica y con impacto anterior/posterior.
6. El grupo inactivo impide primeras publicaciones, pero permite republicar destinatarios ya congelados.
7. La consulta explica miembros efectivos o proyectados sin habilitar búsqueda global por grupo.
8. El grupo anterior del corredor es referencia; una excepción para reactivación requiere confirmación administrativa nueva.
9. Solo se crean planes para la semana actual o una futura, bajo una única zona IANA del club.
10. Grupo y semana pueden cambiar antes de publicar y quedan inmutables después.
11. Un plan nunca publicado se puede eliminar; uno publicado no se elimina mediante planificación.
12. Cada entrenamiento guardado es completo; el plan sí puede estar vacío durante su preparación.
13. Un entrenamiento solo se mueve entre planes nunca publicados y el traslado es atómico.
14. Todo el plan comparte una única revisión optimista.
15. El borrador de un plan publicado puede restaurarse atómicamente desde su versión activa.
16. Cada entrenamiento declara `presencial` o `en-linea`; el lugar solo se admite en `presencial` y puede faltar.
17. Duraciones y distancias conservan presentación humana y tienen valores canónicos exactos y límites cerrados.
18. La recuperación configurada se ejecuta después de cada repetición, incluida la última.
19. Frecuencia cardiaca usa exactamente una zona `Z1` a `Z5`, sin cálculo ni descripción fisiológica.
20. Ritmo relativo usa distancia cerrada y un intervalo de `-60` a `+180` segundos por kilómetro.
21. Los bloques de trabajo comparten familia de objetivo; cada recuperación `rodaje` elige la suya.
22. El historial conserva antes/después completos durante `12` meses con acceso restringido.
23. Un plan nunca publicado y su historial de contenido se purgan `90` días después de terminar su semana.
24. La búsqueda de planes forma parte del PMV; duplicación y plantillas permanecen fuera.

## Lenguaje ubicuo

- **Grupo de planificación:** configuración estable que combina segmentos y excepciones para producir como máximo una pertenencia operativa por corredor.
- **Composición del grupo:** referencias de segmentos e inclusiones o exclusiones manuales vigentes para un grupo.
- **Miembro efectivo:** corredor `active` que pertenece al resultado de un grupo `active`.
- **Miembro proyectado:** corredor que pertenecería a un grupo `inactive` si se activase en la revisión consultada.
- **Reconfiguración multigrupo:** recurso inmutable que registra una sustitución atómica del estado final de uno o varios grupos.
- **Excepción de grupo:** inclusión o exclusión persistente ligada al ciclo operativo revisado de un corredor.
- **Plan semanal:** borrador de trabajo de un grupo y una semana, con identidad estable y como máximo un entrenamiento por día.
- **Entrenamiento:** sesión completa de un día, con modalidad, tres fases y datos complementarios.
- **Bloque principal:** unidad ordenada con repeticiones de trabajo y, cuando existe, recuperación posterior a cada repetición.
- **Carga:** duración o distancia exclusiva del trabajo o recuperación.
- **Objetivo cardiaco:** una única zona simbólica `Z1` a `Z5`.
- **Objetivo de ritmo relativo:** distancia de referencia e intervalo de desviación en segundos por kilómetro.
- **Cambios pendientes:** diferencia visible entre el borrador actual y la versión activa publicada.
- **Cambio de planificación:** registro inmutable anterior/posterior de una mutación confirmada.

No se usarán `cohorte`, `audiencia`, `lista manual`, `programa` o `sesión publicada` como sinónimos de grupo, plan o entrenamiento. Los nombres ingleses se reservarán para código, OpenAPI y persistencia.

## Límites modulares y dependencias

`planning` gobierna exclusivamente su esquema `planning`, sus borradores, reglas locales, historial y coordinación de grupos. Consume las APIs publicadas de:

- `classification-segmentation`, para validar segmentos, obtener resultados efectivos y aplicar provisionalmente mutaciones coordinadas;
- `runner-management`, para comprobar elegibilidad, ciclos operativos, presentaciones mínimas y reservas `pending_reactivation`.

`publication` consume la API publicada de `planning` para bloquear y leer un borrador consistente, validar su publicabilidad, obtener miembros del grupo en la primera publicación y registrar que una revisión se publicó. No habrá dependencia desde `planning` hacia `publication`.

La propiedad se mantiene mediante estos contratos:

- `planning` conserva `status`, `firstPublishedAt`, la revisión de trabajo y la huella canónica de la última revisión publicada porque esas propiedades gobiernan mutabilidad del borrador;
- `publication` conserva versiones, contenido congelado, nombre de grupo congelado, destinatarios y versión activa;
- al confirmar una publicación, `publication` llama dentro de la misma transacción a la API de `planning` para registrar estado y huella publicada;
- para descartar cambios, `publication` obtiene su versión activa y coordina una sustitución completa del borrador mediante la API de `planning`;
- `hasPendingChanges` se deriva comparando la huella canónica visible del borrador con la última huella registrada, sin leer tablas de publicación.

Los coordinadores implementados por `planning` para mutaciones de clasificación y reactivación siguen el patrón de `ADR-0019`: los módulos propietarios definen el puerto, `planning` lo implementa y no se crea una dependencia inversa. No habrá HTTP interno, SQL entre esquemas, imports de paquetes internos ni eventos usados para decidir una transacción.

## Riesgos y controles

| Riesgo | Impacto | Control de diseño |
| --- | --- | --- |
| Grupo inactivo que sigue bloqueando corredores | Reasignaciones rechazadas sin causa visible. | Los inactivos no producen miembros ni reservas; la reactivación vuelve a validar. |
| Traslado mediante varias llamadas | Publicación concurrente sobre una pertenencia intermedia. | Reconfiguración multigrupo, bloqueo global y commit único. |
| Reactivar con composición obsoleta | Doble pertenencia o grupo inesperado. | Proyección explicada, revisión administrativa y validación atómica final. |
| Restaurar automáticamente el grupo anterior | Decisión antigua aplicada meses después. | Referencia solo informativa y excepción nueva ligada al ciclo revisado. |
| Mezclar borradores y versiones | Cambios silenciosos para el corredor. | Propiedad modular separada, huella publicada y republicación obligatoria. |
| Guardar entrenamientos parciales | Estados nulos imposibles de validar consistentemente. | Cada entrenamiento persistido es completo; el formulario incompleto vive en cliente. |
| Sobrescritura concurrente | Pérdida de cambios en días o bloques. | Revisión única del plan y `If-Match` obligatorio. |
| Objetivo aparentemente personalizado | Instrucción errónea al no existir marca o zona personal. | Mostrar fórmula relativa, no calcular ni inventar equivalencias. |
| Conservar historial tras purgar borrador | Eliminación cosmética. | Purga conjunta del contenido y su historial; evidencia técnica minimizada. |
| Historial con texto o referencias personales | Exposición y retención excesivas. | Acceso restringido, `12` meses, anonimización anticipada y exclusión de telemetría. |

## Modelo de dominio

### Grupo de planificación

El grupo contiene UUID, nombre visible y canónico, `status`, revisión, fechas y composición. El nombre admite de `1` a `120` caracteres después de normalizar y es único sin distinguir mayúsculas, diacríticos equivalentes definidos por la forma canónica ni espacios exteriores.

Estados:

| Estado | Miembros o reservas | Mutaciones permitidas | Planes |
| --- | --- | --- | --- |
| `active` | Miembros efectivos y reservas hipotéticas; exclusividad obligatoria. | Nombre, composición o desactivación mediante coordinación. | Crear y editar borradores; primera publicación y republicación. |
| `inactive` | Ninguno; solo proyección consultable. | Nombre, composición o reactivación; puede quedar sin segmentos. | Crear y editar borradores; solo republicación de planes ya publicados. |

La composición conserva referencias a segmentos y excepciones. Un segmento inactivo ya asociado continúa evaluándose hasta que se retire; no puede asociarse por primera vez ni modificarse desde planificación. Activar exige al menos un segmento y el estado final exclusivo.

La excepción de grupo se identifica por grupo, corredor y ciclo operativo de corredor. Una excepción usada en un ciclo anterior queda histórica y no vuelve a ser efectiva cuando el corredor se reactiva. Durante la revisión, el administrador crea o confirma otra excepción vinculada al ciclo de reactivación; será dormida en `inactive`, reserva en `pending_reactivation` y efectiva cuando el mismo ciclo alcance `active`.

Esta vinculación evita modificar físicamente otros módulos al dar de baja y materializa la prohibición de restauración automática. La API de corredores aporta un identificador opaco de ciclo o revisión, no fechas, nombre ni motivo de baja.

### Reconfiguración multigrupo

Una reconfiguración es un recurso inmutable con UUID, actor, instante, clave idempotente, correlación, revisiones de entrada, configuraciones finales solicitadas, impacto confirmado y resultado. Solo se crea cuando la transacción confirma. Los rechazos devuelven Problem Details y no dejan un recurso de negocio parcial.

La configuración de cada grupo se reemplaza completa en la operación; no se aplican parches de asociaciones que puedan dejar un estado intermedio. Un grupo omitido no se modifica, aunque sí se evalúa cuando puede entrar en conflicto con el estado final.

El impacto contiene, según autorización:

- grupos creados, activados, desactivados o modificados;
- corredores que entran, salen o cambian de grupo;
- reservas que cambian de proyección;
- conflictos completos para administrador y detalle redactado para entrenador.

### Plan semanal

El plan contiene UUID, nombre, grupo, lunes de la semana, `status`, revisión, huella visible, huella de última publicación, fechas y metadatos. El nombre admite de `1` a `120` caracteres, usa la normalización de `ADR-0006` y es único dentro de la semana.

La fecha de semana se valida en `Europe/Madrid` inicialmente y nunca se deriva desde UTC. El servidor obtiene la fecha local mediante un reloj inyectable; una petición no elige zona horaria. Solo se crea para la semana actual o una futura.

Estados observables:

| Estado | Significado | Reglas |
| --- | --- | --- |
| `borrador` | Nunca se publicó. | Grupo y semana mutables; eliminación permitida; puede estar vacío. |
| `publicado` | Existe al menos una versión confirmada. | Grupo y semana inmutables; borrador editable; eliminación prohibida. |

`hasPendingChanges` no es un tercer estado. Se deriva de las huellas canónicas de contenido visible. Auditoría, revisiones, fechas técnicas y nombre actual del grupo no forman parte de esa huella. En la primera publicación, `publication` congela el nombre visible del grupo y lo reutiliza en todas las versiones del plan.

El plan puede contener de cero a siete entrenamientos. La publicación seguirá exigiendo al menos uno; esa validación se repite desde `publication` sobre una revisión bloqueada.

### Entrenamiento y fases

El entrenamiento tiene UUID, día `MONDAY` a `SUNDAY`, modalidad, aclaración, lugar y exactamente tres fases lógicas:

1. calentamiento `rodaje`, solo por duración y sin objetivo;
2. parte principal, con tipo cerrado y de uno a veinte bloques;
3. enfriamiento `rodaje`, solo por duración y sin objetivo.

La modalidad es `presencial` o `en-linea`. `presencial` admite lugar opcional; `en-linea` exige ausencia. La aclaración admite saltos de línea; el lugar no. Ambos son texto plano, Unicode NFC y sin marcado.

Un entrenamiento solo se guarda si la estructura completa satisface todas sus invariantes. La fecha se deriva del lunes y el día; no se persiste como una segunda fuente editable.

### Cargas, bloques y recuperaciones

Una carga es una unión exclusiva:

- duración con `totalSeconds` canónico entre `1` y `21.600`, presentada como minutos y segundos;
- distancia con `totalMeters` canónico entre `1` y `100.000` y unidad de presentación `m` o `km`.

La entrada en kilómetros admite hasta tres decimales y debe convertirse exactamente a metros enteros. No se aceptan redondeos silenciosos. Ejemplos equivalentes como `1 km` y `1.000 m` comparten valor canónico, pero cada uno conserva su unidad visible.

Cada bloque tiene orden denso desde `1`, repeticiones `1..100`, carga de trabajo, objetivo y recuperación opcional u obligatoria según `ADR-0006`. Si existe recuperación, su carga usa las mismas reglas y se ejecuta después de cada repetición, incluida la última. La modalidad de recuperación es `parado`, `andando` o `rodaje`; solo `rodaje` admite y exige objetivo.

### Objetivos

La familia de trabajo pertenece a la parte principal y es `frecuencia-cardiaca` o `ritmo-relativo`. Todos sus bloques usan esa familia, con valores propios. La recuperación `rodaje` guarda su familia de forma independiente.

Representación cardiaca:

```text
family = frecuencia-cardiaca
zone = Z1 | Z2 | Z3 | Z4 | Z5
```

Representación de ritmo:

```text
family = ritmo-relativo
referenceDistance = 1-km | 3-km | 5-km | 10-km | media-maraton | maraton
minOffsetSecondsPerKm = -60..180
maxOffsetSecondsPerKm = -60..180
minOffsetSecondsPerKm <= maxOffsetSecondsPerKm
```

Los signos se muestran de forma explícita cuando corresponda. Ninguna representación incluye `runnerId`, pulsaciones, marca, ritmo calculado, porcentaje ni descripción fisiológica.

## Modelo persistente

El esquema `planning` contendrá inicialmente:

| Tabla | Contenido e invariantes principales |
| --- | --- |
| `planning_coordination` | Fila única del club usada por `SELECT ... FOR UPDATE` para pertenencia y primera publicación. |
| `planning_group` | UUID, nombre y clave canónica únicos, estado, revisión y fechas; sin borrado físico. |
| `planning_group_segment` | Pareja única grupo-segmento, revisión de asociación y estado histórico necesario. |
| `planning_group_exception` | Grupo, corredor, ciclo operativo y modo único `inclusion` o `exclusion`; efectividad derivada del estado y ciclo del corredor. |
| `planning_group_reconfiguration` | UUID, actor, instante, idempotencia, correlación, entrada y resultado inmutables. |
| `weekly_plan` | UUID, nombre y clave, grupo, lunes, estado, revisión, huellas, primera publicación y fechas. |
| `workout` | UUID, plan, día único por plan, modalidad, tipo principal, aclaración y lugar. |
| `workout_phase_duration` | Duraciones canónicas de calentamiento y enfriamiento, una por fase y entrenamiento. |
| `workout_block` | UUID, entrenamiento, orden único, repeticiones, carga exclusiva y objetivo de trabajo. |
| `workout_recovery` | Relación uno a uno opcional por bloque, modalidad, carga exclusiva y objetivo solo para `rodaje`. |
| `planning_change` | Actor, instante, correlación, recurso, operación, revisiones y estados JSON estructurados anterior/posterior con versión de esquema. |
| `planning_purge_evidence` | Identificador opaco, instante, tipo de purga y resultado; nunca contenido del plan. |

Las columnas exclusivas de carga y objetivo usarán `CHECK` para exigir exactamente una variante coherente. Las formas JSON del historial no sustituirán tablas canónicas ni serán leídas para operar; su esquema versionado permitirá interpretar el periodo de retención sin depender de clases Java actuales.

Restricciones mínimas:

- nombre canónico único de grupo;
- pareja grupo-segmento única;
- pareja grupo-corredor-ciclo única para excepciones vigentes;
- pareja grupo-semana única para planes;
- nombre canónico único por semana;
- pareja plan-día única;
- orden de bloque único por entrenamiento;
- duraciones, distancias, repeticiones, offsets y longitudes dentro de escala;
- consistencia de modalidad y lugar;
- grupo y semana no modificables después de `first_published_at` mediante aplicación y pruebas de persistencia.

Las reglas globales de exclusividad y estado no se delegarán únicamente a `CHECK`: se protegen con coordinación, consultas propietarias y pruebas concurrentes.

## Casos de uso

### Crear y preparar un grupo

Administrador o entrenador crea nombre, estado inicial y composición completa opcional. Si solicita `active`, debe incluir al menos un segmento y la operación adquiere coordinación, valida miembros activos y reservas y confirma todo o nada. Si solicita `inactive`, puede quedar vacío y no afecta a otros grupos.

Nombre y estado se devuelven con `ETag`. Asociar un segmento inactivo se rechaza; conservar una referencia anterior es válido y aparece en la representación.

### Reconfigurar grupos

El actor envía una colección explícita de estados finales sin grupos duplicados y una `Idempotency-Key`. El servidor normaliza, ordena UUID, bloquea coordinación y grupos, sustituye provisionalmente composiciones, consulta clasificación y corredores por sus APIs y valida el resultado global.

Una reconfiguración válida crea su recurso e historial dentro de la misma transacción y devuelve `201 Created`, `Location`, configuraciones confirmadas e impacto. Una entrada inválida o conflicto no consume revisiones ni crea recurso. Repetir la misma clave con el mismo cuerpo devuelve el resultado original; reutilizarla con otro cuerpo se rechaza.

### Consultar miembros

La consulta usa cursor ligado a grupo, revisión, filtros y orden. Para un activo devuelve miembros efectivos; para un inactivo evalúa la configuración como hipotética sin crear reservas. Cada fila contiene presentación mínima autorizada, coincidencias por segmento, inclusión, exclusión y resultado.

Las reservas no visibles se usan para calcular conflictos, pero no se incorporan a una fila identificable para entrenador. Un cambio de revisión invalida el cursor para impedir mezclar configuraciones.

### Revisar una reactivación

El administrador abre el corredor inactivo mediante la interfaz compuesta y ve el último grupo solo como referencia. Puede incluir en una reconfiguración una excepción ligada a la revisión de reactivación vigente. La excepción permanece dormida.

Al iniciar `pending_reactivation`, el coordinador bloquea revisión de corredor, clasificación y planificación; valida la excepción y la pertenencia hipotética, crea la reserva y solo entonces invoca la transición de `runner-management`. Cancelación o caducidad elimina la reserva por estado; la excepción permanece dormida para revisión futura y no se reactiva sin confirmar otro ciclo. Aceptación hace efectiva la configuración correspondiente al ciclo activo.

### Crear y editar un plan

Administrador o entrenador crea nombre, grupo y lunes de semana. El grupo puede estar activo o inactivo; la semana debe ser actual o futura. El plan nace `borrador`, revisión `1` y puede no tener entrenamientos.

Antes de la primera publicación, `PATCH` puede cambiar nombre, grupo o semana con `If-Match`. Después solo el nombre sigue mutable. Cada entrenamiento se crea o reemplaza completo, cada mutación incrementa la revisión global y toda respuesta devuelve el nuevo `ETag`.

Eliminar un borrador nunca publicado borra plan y contenido en una transacción y conserva el cambio durante su política ordinaria. Si alguna vez se publicó, `DELETE` devuelve conflicto. Retirar un entrenamiento del borrador publicado es una edición válida que puede dejar cambios pendientes; nunca elimina versiones ni seguimiento.

### Mover un entrenamiento

El traslado recibe plan destino, día destino y revisiones esperadas. Bloquea origen y destino por UUID, exige que ambos sigan sin publicar y que el día esté libre, mueve el entrenamiento conservando su UUID e incrementa ambas revisiones en un único commit. Un fallo revierte ambos.

### Restaurar la versión activa

La operación se inicia desde la representación de publicación. `publication` bloquea su versión activa, obtiene la instantánea completa y llama a `planning` con plan, revisión esperada y contenido. `planning` bloquea el plan, verifica grupo y semana inmutables, reemplaza el borrador, calcula huella, incrementa revisión y registra antes/después. Ambos participan en la misma transacción y ninguna ruta de planificación lee el esquema de publicación.

El recurso HTTP exacto se cerrará en el diseño de publicación porque su representación de origen pertenece a ese módulo. No se añadirá `/reset`, `/restore` ni otra acción nominalizada sin superar `ADR-0017`.

### Purgar borradores vencidos

Una tarea idempotente reclama planes nunca publicados cuya semana terminó hace más de `90` días. Bloquea el plan, vuelve a comprobar estado, elimina contenido e historial asociado y registra evidencia minimizada. Usa lotes con `SKIP LOCKED`, reanuda tras caída y no toca planes publicados.

## Permisos

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Crear, modificar, activar o desactivar grupos | Sí | Sí | No |
| Reconfigurar varios grupos | Sí | Sí | No |
| Consultar grupos y miembros activos | Sí | Sí | No |
| Consultar proyección de grupos inactivos | Sí | Sí, sin reservas identificables | No |
| Configurar excepción para corredor inactivo | Sí, desde reactivación | No | No |
| Crear y editar planes o entrenamientos | Sí | Sí | No |
| Eliminar planes nunca publicados | Sí | Sí | No |
| Mover entrenamientos inéditos | Sí | Sí | No |
| Descartar cambios desde versión activa | Sí | Sí | No |
| Consultar historial | Sí | Sí, sin identidades no activas | No |

Las políticas se aplican antes de leer y de nuevo dentro de cada módulo llamado. Listas, conteos, impacto, historial y cursores usan el mismo predicado de autorización que el recurso individual. El corredor solo accederá posteriormente a representaciones propias desde `runner-portal` y `publication`, nunca a esta API administrativa.

## APIs internas publicadas

`planning` publicará tipos propios para:

- obtener y bloquear una revisión de borrador publicable;
- calcular su huella canónica y validar contenido;
- resolver miembros efectivos de un grupo activo bajo coordinación;
- registrar primera publicación o republicación confirmada sin guardar versiones ajenas;
- reemplazar el borrador desde una instantánea suministrada por `publication`;
- coordinar mutaciones de clasificación, grupos y reactivación;
- consultar pertenencia efectiva o proyectada con alcance autorizado;
- anonimizar o suprimir referencias personales conforme a retención.

Los contratos transportarán `ActorContext`, correlación, revisiones y resultados cerrados. No expondrán jOOQ, tablas, JSON de historial, entidades mutables ni tipos internos de segmentos, corredores o publicaciones.

`publication` no podrá marcar directamente columnas del esquema `planning`; usará el caso de uso publicado que aplica autorización técnica, bloqueo e invariantes dentro de la transacción coordinada.

## API HTTP prevista

OpenAPI será la fuente de verdad antes de implementar. Las operaciones de planificación previstas son:

| Actor | Método y recurso | Semántica |
| --- | --- | --- |
| Administrador o entrenador | `GET /api/planning-groups` | Consultar grupos por prefijo de nombre y estado mediante cursor. |
| Administrador o entrenador | `POST /api/planning-groups` | Crear grupo activo o inactivo; `201`, `Location` e idempotencia. |
| Administrador o entrenador | `GET /api/planning-groups/{planningGroupId}` | Consultar estado, composición, revisión y conteos autorizados. |
| Administrador o entrenador | `PATCH /api/planning-groups/{planningGroupId}` | Cambiar nombre o estado con `If-Match`; la transición valida composición. |
| Administrador o entrenador | `GET /api/planning-groups/{planningGroupId}/members` | Consultar miembros efectivos o proyectados y explicación paginada. |
| Administrador o entrenador | `POST /api/planning-group-reconfigurations` | Crear una reconfiguración inmutable y confirmada; atómica, idempotente y con impacto. |
| Administrador o entrenador | `GET /api/planning-group-reconfigurations/{reconfigurationId}` | Consultar entrada y resultado inmutables visibles. |
| Administrador o entrenador | `GET /api/weekly-plans` | Buscar por nombre, semanas, grupo, estado y cambios pendientes. |
| Administrador o entrenador | `POST /api/weekly-plans` | Crear un borrador para semana actual o futura. |
| Administrador o entrenador | `GET /api/weekly-plans/{weeklyPlanId}` | Consultar borrador completo, revisión y resumen de publicación permitido. |
| Administrador o entrenador | `PATCH /api/weekly-plans/{weeklyPlanId}` | Cambiar propiedades permitidas con `If-Match`. |
| Administrador o entrenador | `DELETE /api/weekly-plans/{weeklyPlanId}` | Eliminar idempotentemente solo un plan nunca publicado. |
| Administrador o entrenador | `POST /api/weekly-plans/{weeklyPlanId}/workouts` | Crear un entrenamiento completo en un día libre. |
| Administrador o entrenador | `GET /api/workouts/{workoutId}` | Consultar un entrenamiento del borrador visible. |
| Administrador o entrenador | `PUT /api/workouts/{workoutId}` | Reemplazar completamente día y contenido válido bajo revisión del plan. |
| Administrador o entrenador | `PATCH /api/workouts/{workoutId}` | Trasladar el entrenamiento entre planes inéditos con precondiciones de ambos. |
| Administrador o entrenador | `DELETE /api/workouts/{workoutId}` | Retirar el entrenamiento del borrador. |
| Administrador o entrenador | `GET /api/planning-changes` | Consultar historial por recurso, actor, operación e intervalo con cursor. |

`planning-group-reconfiguration` es un recurso real porque tiene UUID, actor, instante, configuraciones de entrada, resultado, impacto, idempotencia e historial consultable. No admite estados asíncronos ni una segunda confirmación.

Todos los recursos mutables expondrán `ETag`; los cambios exigirán `If-Match` y devolverán `412 Precondition Failed` cuando la revisión esté obsoleta. Las inconsistencias de estado, semana, composición o exclusividad devolverán `409 Conflict` mediante tipos estables de Problem Details. Los errores de campo usarán `422 Unprocessable Content` si esa convención queda fijada por el OpenAPI común; no se inventará el código durante la implementación.

Las creaciones y la reconfiguración usarán `Idempotency-Key` con unicidad persistida. Colecciones crecientes usarán cursor opaco, límite medido y orden total. La ruta de traslado mantiene un recurso real (`workout`) y modifica su relación; OpenAPI deberá documentar las dos precondiciones sin esconder un comando en otra ruta.

## Concurrencia y consistencia

- Toda mutación capaz de cambiar miembros adquiere primero `planning_coordination`.
- Después bloquea grupos, segmentos consultados, corredores y filas dependientes por UUID estable.
- La reconfiguración elimina duplicados de entrada antes de bloquear y valida el estado global final, no pasos intermedios.
- Crear, editar o eliminar contenido bloquea el plan y compara su revisión global.
- Mover un entrenamiento bloquea ambos planes en orden UUID y actualiza las dos revisiones.
- La primera publicación adquiere coordinación global y después bloqueo de plan; la republicación bloquea el plan pero no recalcula grupos.
- Registrar una publicación y su huella ocurre en la misma transacción que versión, destinatarios y outbox.
- El historial se inserta antes del commit y nunca mediante una transacción posterior.
- La consulta de miembros no toma el bloqueo global, pero su cursor queda ligado a la revisión del grupo y se rechaza tras cambios.
- La tarea de purga vuelve a comprobar que `first_published_at` sigue ausente después del bloqueo.

## Consultas e índices

Índices mínimos candidatos:

- unicidad y búsqueda por clave canónica de grupo;
- grupos por estado, clave canónica e identificador;
- asociaciones por grupo-segmento y segmento-grupo;
- excepciones por grupo-corredor-ciclo y corredor-ciclo;
- planes por grupo-semana, semana-nombre canónico, estado y cambios pendientes;
- entrenamientos por plan-día;
- bloques por entrenamiento-orden;
- reconfiguraciones e historial por instante e identificador, actor, recurso y correlación;
- borradores nunca publicados por fin de semana para la tarea de purga.

La consulta de miembros compone lotes mediante APIs publicadas, no joins entre esquemas. Se medirá con más de `500` corredores, grupos activos e inactivos, segmentos solapados, excepciones y reservas. Un problema probado podrá justificar una proyección explícita mediante otra decisión; no se añade caché ni duplicación canónica de miembros por anticipado.

La búsqueda de planes usa cursor ligado a filtros y orden por semana descendente, nombre canónico e identificador. No promete una instantánea global entre páginas; una modificación concurrente puede mover un borrador, pero el cursor y revisión evitan interpretar páginas de criterios distintos como una sola.

## Retención y privacidad

Grupos, excepciones, borradores, ubicaciones, aclaraciones, impacto e historial pertenecen a planificación y publicación según el catálogo de `ADR-0010`. Los entornos no productivos usarán únicamente datos ficticios, sintéticos o anonimizados de forma irreversible.

Política ejecutable:

- historial de mutaciones: `12` meses desde el evento;
- recursos confirmados de reconfiguración: `12` meses desde su confirmación;
- referencias personales: anonimización anticipada cuando venza la retención del corredor o se apruebe su supresión;
- plan nunca publicado: contenido e historial purgados `90` días después de terminar su semana;
- evidencia de purga: `12` meses, sin contenido ni identificador personal;
- planes publicados: fuera de la purga de planificación y sujetos a `24` meses desde el entrenamiento conforme a `ADR-0010` y al diseño de publicación.

La purga y anonimización serán idempotentes, auditables y reaplicables después de restaurar copias. El historial completo se considera dato restringido: no se exportará a logs, trazas, métricas, errores ni herramientas de analítica. Una solicitud de derechos será ejecutada por categoría y no permitirá alterar una publicación histórica como edición silenciosa.

La política de `12` meses y los datos reales siguen pendientes de revisión especializada. Este diseño no ofrece asesoramiento jurídico ni declara lícito el tratamiento.

## Observabilidad

Métricas agregadas:

- grupos por estado y reactivaciones confirmadas o rechazadas;
- tamaño, duración y conflictos de reconfiguraciones;
- miembros y proyecciones consultados por tamaño de página;
- planes creados, editados, eliminados y con cambios pendientes;
- entrenamientos y bloques por plan;
- conflictos de revisión y traslados;
- purgas completadas, retrasadas o fallidas.

Las etiquetas no incluirán nombres, UUID de corredor, ubicaciones, aclaraciones, objetivos completos ni estados anterior/posterior. Las trazas usarán correlación opaca, módulo y operación. Las alertas cubrirán conflictos sostenidos, reconfiguraciones fuera de umbral, purgas retrasadas y latencia de consultas de miembros.

## Validación prevista

### Grupos y composición

- Probar normalización, máximo `120`, unicidad y nombres vacíos.
- Probar creación activa o inactiva, inactivo vacío y rechazo de activo sin segmentos.
- Probar que un inactivo no produce miembros ni reservas y que su proyección queda ligada a revisión.
- Probar asociaciones nuevas a segmentos activos, conservación de referencias inactivas y retirada explícita.
- Probar reconfiguración de uno y varios grupos, impacto, idempotencia, precondiciones y rollback.
- Ejecutar traslados concurrentes y primera publicación para demostrar ausencia de estados intermedios.
- Probar que primera publicación requiere grupo activo y republicación acepta grupo inactivo.
- Probar nombre congelado del grupo y ausencia de cascada al renombrarlo.

### Reactivación

- Probar referencia del grupo anterior sin restauración automática.
- Probar excepción dormida ligada a revisión, reserva durante `pending_reactivation` y efectividad solo en el ciclo activo confirmado.
- Ejecutar carreras entre aceptación, cancelación, caducidad, reconfiguración y clasificación.
- Probar redacción de reserva para entrenador y detalle auditado para administrador.

### Planes y concurrencia

- Probar semana actual o futura en zona IANA y rechazo de semana terminada.
- Probar unicidad grupo-semana y nombre-semana.
- Probar cambio de grupo y semana antes de publicar e inmutabilidad posterior.
- Probar plan vacío válido como borrador e inválido para publicar.
- Probar revisión global, `If-Match`, dos editores y ausencia de última escritura gana.
- Probar borrado manual solo antes de publicar y conservación del historial correspondiente.
- Probar traslado atómico entre planes inéditos, día ocupado, revisiones obsoletas y bloqueo ordenado.
- Probar restauración completa desde versión activa sin dependencia inversa.

### Entrenamientos y objetivos

- Probar exactamente tres fases, orden fijo y tipos de calentamiento y enfriamiento.
- Probar de `1` a `20` bloques, de `1` a `100` repeticiones y carga exclusiva.
- Probar recuperación obligatoria u opcional según repeticiones y su ejecución tras la última.
- Probar minutos/segundos, límites `1..21.600`, kilómetros con tres decimales, conversión exacta, límites `1..100.000` y unidad conservada.
- Probar modalidad, ubicación ausente o presente, rechazo en línea y máximos de texto.
- Probar catálogo `Z1..Z5` sin porcentajes ni descripciones.
- Probar seis distancias, offsets `-60..180`, intervalo ordenado y extremos iguales.
- Probar familia común de bloques y familia independiente de recuperaciones `rodaje`.
- Rechazar HTML, espacios vacíos, escalas fuera de rango y combinaciones incompatibles.

### Consultas, historial y retención

- Probar miembros efectivos y proyectados con explicación completa y cursor obsoleto.
- Probar búsqueda de planes por todos los filtros, empates y cambios concurrentes.
- Probar historial anterior/posterior, actor, correlación, autorización y ausencia de restauración arbitraria.
- Probar caducidad de cada evento a `12` meses y anonimización personal anticipada.
- Probar purga conjunta a `90` días, carrera con publicación y evidencia técnica minimizada.
- Restaurar una copia sintética y reaplicar supresiones y purgas antes de abrirla.

### Arquitectura, API y rendimiento

- Revisar recursos, métodos, seguridad, idempotencia, precondiciones y Problem Details contra `ADR-0017`.
- Generar OpenAPI, servidor y cliente antes de implementar y ejecutar Spectral y `oasdiff`.
- Verificar límites con Spring Modulith y ArchUnit, incluida dependencia `publication -> planning` y prohibición inversa.
- Probar transacciones conjuntas con clasificación, corredores, publicación y outbox sobre PostgreSQL.
- Cargar datos sintéticos representativos y revisar `EXPLAIN (ANALYZE, BUFFERS)` antes de fijar límites de página y reconfiguración.

## Alternativas descartadas

- Borrar grupos o mantener sus reservas al desactivarlos: pierde historia o crea bloqueos invisibles.
- Cambios secuenciales de grupo: permiten estados intermedios observables.
- Previsualización o borrador de reconfiguración: duplica el espacio de preparación del grupo inactivo.
- Guardar entrenamientos parciales: convierte errores de formulario en estados persistentes.
- Revisión por día o última escritura gana: complica publicación o pierde cambios.
- Cambiar grupo o semana después de publicar: rompe identidad, fechas y destinatarios.
- Mover un entrenamiento publicado: rompe versiones y seguimiento histórico.
- Porcentajes, pulsaciones o marcas personales: amplían datos y no producen un objetivo común verificable.
- Rangos de zonas o listas discontinuas: se confirma exactamente una zona por objetivo cardiaco.
- Unidades decimales canónicas: introducen redondeos y pierden la presentación elegida.
- Resolver modalidad desde ubicación o corredores: no distingue una sesión presencial sin lugar y depende de estado dinámico.
- Conservar borradores abandonados o su historial tras purga: retención indefinida o eliminación cosmética.
- Duplicación y plantillas: alcance opcional sin necesidad para `RF-01` a `RF-20`.
- SQL cruzado, miembros materializados canónicos o dependencia hacia publicación: violan propiedad modular.

## Conclusiones

- El grupo inactivo es un espacio de preparación real, sin pertenencia ni reservas, y la reactivación es la única entrada a operación.
- La reconfiguración multigrupo protege traslados y primeras publicaciones frente a estados parciales.
- Los planes conservan un borrador editable sin mezclarlo con versiones publicadas y fijan su identidad temporal al publicar.
- Cada entrenamiento persistido es completo y usa magnitudes y objetivos verificables sin almacenar referencias deportivas personales.
- El historial completo es temporal y restringido; los borradores nunca publicados desaparecen junto con su contenido a los `90` días.
- `publication` puede coordinar publicar, republicar y restaurar sin invertir dependencias ni leer tablas de planificación.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro de `planning`.

- `publication` deberá decidir antes de implementarse la primera publicación cuando la semana ya haya comenzado y materializar el recurso HTTP de restauración desde la versión activa.
- Antes de implementar deberán producirse OpenAPI, migraciones Flyway, tipos jOOQ, límites medidos, catálogo de Problem Details y pruebas transaccionales.
- La búsqueda global de corredores por etiquetas, segmentos o grupos continúa aplazada a `MF-004`.
- La duplicación de planes y las plantillas continúan aplazadas a `MF-005`.
- Los datos personales reales y la producción continúan bloqueados hasta completar las evidencias de privacidad aplicables.
