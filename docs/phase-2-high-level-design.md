# Diseño funcional y técnico de alto nivel — Fase 2

**Estado:** Validado — diseño cerrado; únicamente autorizada la preparación técnica con datos sintéticos
**Fecha:** 2026-08-24
**Cierre documental:** [Evidencia y condiciones posteriores](phase-2-closure.md)

## Propósito

Materializar el contrato de entrada de Fase 1 en un diseño de alto nivel trazable. Este documento delimita los componentes lógicos, los flujos, los datos y las decisiones técnicas que deben resolverse antes de implementar el PMV.

`ADR-0013` define runtime, framework, acceso JDBC, frontend conjunto y contrato API; `ADR-0017` define las convenciones HTTP orientadas a recursos. `ADR-0014` concreta la estructura modular y `ADR-0015` el mecanismo técnico de autorización. `ADR-0012` define PostgreSQL y la estrategia transaccional; la entrega de correo se concreta en `ADR-0011`. `ADR-0016` define Microsoft Azure `West Europe` para despliegue y operación, refinado por los objetivos y custodia de `ADR-0023`. Los ADR relacionados y el diseño de Fase 2 están validados; la PR de cierre conserva las puertas documentales, la autorrevisión y la aceptación del riesgo sin revisión independiente.

## Alcance y restricciones heredadas

- Aplicación web adaptable para un único club, con más de 500 corredores registrados y picos iniciales inferiores a 100 usuarios concurrentes.
- Roles operativos: administrador, entrenador y corredor. El corredor solo accede a sus propios datos; el entrenador accede globalmente solo a corredores `active`; el administrador dispone de acceso auditado a estados no operativos.
- La modalidad se expresa mediante una etiqueta controlada y cada entrenamiento tiene modalidad propia; ambas son informativas e independientes. El lugar de encuentro es texto libre opcional por entrenamiento presencial y no se limita a El Retiro.
- Las reglas de segmentos se limitan a condiciones de etiquetas con operador Y, varios valores por etiqueta e inclusiones o exclusiones manuales.
- Los segmentos pueden solaparse. Los grupos de planificación combinan segmentos y excepciones persistentes, y un corredor pertenece como máximo a un grupo activo; los grupos inactivos solo conservan una proyección sin miembros ni reservas.
- No forman parte del PMV multiclub, aplicaciones nativas, mensajería, integraciones deportivas, reservas, pagos ni datos de salud especiales.

## Principios de diseño

- Mantener una única fuente de verdad para planes, publicaciones y seguimiento. El correo solo notifica disponibilidad o cambios.
- Resolver destinatarios en la primera publicación del plan y conservarlos en todas sus versiones. Los cambios posteriores de etiquetas, segmentos, grupos o pertenencias solo afectan a planes todavía no publicados.
- Resolver conflictos de pertenencia al mantener grupos de planificación, no al crear cada plan semanal.
- Separar el borrador inédito de cada versión publicada. Después de publicar no se acumulan cambios en servidor: una sesión local sustituye atómicamente contenido vigente y versión activa.
- Aplicar autorización en cada operación y no solo en la interfaz. El aislamiento de cada corredor es una regla de datos y de acceso.
- Representar el seguimiento con valores estructurados y comentario opcional, deshabilitado hasta obtener el consentimiento explícito y separado definido por `ADR-0010`; no solicitar campos de salud ni ampliar ese ámbito, sin ignorar que el texto libre puede contenerlos.
- Registrar eventos internos durables con Spring Modulith JDBC, consumidores idempotentes y recuperación ordenada antes de considerar un broker externo.

## Componentes lógicos confirmados

| Componente | Responsabilidad | Requisitos principales | Datos lógicos que gobierna |
| --- | --- | --- | --- |
| Identidad y acceso | Cuentas de administrador, entrenador y corredor; invitación, activación, inicio, restablecimiento y rol. | `RF-01`, `RF-02`, `RF-16`, `RF-18`, `RF-19` | Cuenta, rol, estado de activación, credencial y sesión. |
| Gestión de corredores | Perfil y ciclo de vida operativo del corredor, sin credenciales, roles ni clasificación. | `RF-02`, `RF-03`, `RF-16` a `RF-19` | Corredor y su vínculo con la cuenta. |
| Clasificación y segmentación | Taxonomías, asignaciones, reglas dinámicas y excepciones manuales que producen segmentos reutilizables y solapables. | `RF-02` a `RF-06`, `RF-08` | Etiqueta, valor permitido, asignación, segmento, criterio, inclusión y exclusión. |
| Planificación | Gestión de grupos exclusivos, planes semanales, fases, bloques, catálogo, objetivos, modalidad, lugar de encuentro e historial operativo. | `RF-04`, `RF-07`, `RF-08`, `RF-11`, `RF-12`, `RF-13`, `RF-14` | Grupo de planificación, reconfiguración, excepción de grupo, plan semanal, entrenamiento, fase, bloque, tipo, objetivo, modalidad, ubicación y cambio de planificación. |
| Publicación | Candidatura, captura de miembros, publicación y actualización atómicas, reglas temporales, versiones, destinatarios, visibilidad y cobertura semanal derivada. | `RF-08`, `RF-09`, `RF-10`, `RF-14`, `RF-15`, `RF-16`, `RF-20`, `RF-21` | Candidatura derivada, publicación actual, versión publicada, día cambiado, miembro efectivo y cobertura semanal. |
| Entrega de notificaciones | Outbox, entrega de correo, leases, reintentos, webhooks y supresión. | `RF-01`, `RF-15`, `RF-20` | Solicitud y eventos técnicos de notificación. |
| Seguimiento y revisión | Registro de ejecución, historial de respuesta y consulta global por entrenador. | `RF-17`, `RF-18`, `RF-19` | Registro de seguimiento vinculado a corredor, entrenamiento y publicación. |
| Portal del corredor | Fachada móvil de consulta de planes, entrenamientos, ubicación e historial propios. | `RF-16`, `RF-18` | Vista derivada sin datos propios inicialmente. |

Estos límites son lógicos. `ADR-0002` aceptado define que se materializan como módulos de una única aplicación desplegable, sin introducir multiclub fuera de alcance. `ADR-0014` define ocho módulos, sus APIs y un esquema PostgreSQL por módulo con estado, conservando una única base y transacciones compartidas. `ADR-0013` define Spring MVC, JDBC, jOOQ y despliegue conjunto del frontend; persistencia y transacciones se rigen por `ADR-0012` aceptado. `ADR-0016` define la plataforma y mantiene explícitos los bloqueantes operativos previos a producción.

## Flujos de alto nivel

### Acceso y administración

1. El administrador invita al corredor por correo y declara que es mayor de edad.
2. El corredor confirma durante la activación inicial que tiene al menos `18` años, define contraseña y puede iniciar sesión o solicitar restablecimiento; se conservan ambas declaraciones, no documentos o fecha de nacimiento.
3. El administrador crea o invita cuentas, incluidas las de entrenador, asigna su rol inicial inmutable y gestiona el perfil operativo de los corredores.
4. El administrador mantiene las taxonomías cerradas; identidad aporta el rol asignado y cada módulo aplica en el backend la capacidad y el alcance del recurso.

### Segmentación y planificación

1. El entrenador configura un segmento mediante reglas permitidas y excepciones manuales.
2. El entrenador crea un grupo activo con composición válida o uno inactivo para prepararlo sin miembros efectivos.
3. Los traslados se aplican mediante una reconfiguración multigrupo atómica; el sistema rechaza cualquier estado final que sitúe a un corredor en dos grupos y muestra solo el detalle autorizado.
4. El entrenador crea como máximo un plan para la pareja grupo-semana y añade como máximo un entrenamiento por día.
5. Cada entrenamiento declara modalidad, calentamiento por duración, una parte principal con bloques y objetivos relativos comunes, enfriamiento por duración y ubicación opcional solo cuando sea presencial. Las modalidades pueden mezclarse sin bloqueo o aviso.
6. Antes de publicar, el sistema valida el plan y resuelve los miembros efectivos actuales del grupo.

### Publicación y republicación

1. Antes de la primera publicación, el entrenador confirma grupo, semana, entrenamientos, conteo y lista exacta de destinatarios activos; una revisión obsoleta obliga a repetir la confirmación.
2. Una publicación válida hace visible el plan completo y congela destinatarios en una única operación lógica; no admite retirada ni primera publicación con entrenamientos de hoy o anteriores.
3. Después de publicar, administrador o entrenador abre una sesión local, modifica uno o varios días futuros y confirma una sustitución completa. Nombre, grupo, semana, hoy y pasado permanecen inmutables.
4. Cada guardado confirmado crea una sola versión completa y una solicitud individual para cada destinatario congelado. No existe borrador persistente, cambios pendientes, restauración ni historial visible de versiones.
5. `ADR-0011` define la entrega asíncrona mediante outbox, worker interno y Brevo. En el primer procesamiento, `ADR-0021` resuelve conjuntamente actividad y correo verificado vigente: omite al inactivo, fija el correo para toda la solicitud elegible o cierra a creación `+120` minutos si no puede resolverlo.
6. Administrador y entrenador consultan por semana la cobertura derivada de todos los corredores activos, con conteos y lista paginada; los estados son informativos y no bloquean operación.

### Consulta, seguimiento y revisión

1. El corredor autenticado consulta únicamente sus publicaciones visibles, adaptadas a móvil.
2. Desde la fecha del entrenamiento y durante siete días naturales, el corredor registra `realizado` con esfuerzo `1 Muy suave` a `5 Muy intenso` y sensación obligatorios o `no-realizado` sin ellos; el comentario es opcional.
3. El historial incluye todos los entrenamientos que llegaron a publicarse, diferencia `sin-seguimiento`, `no-realizado` y `retirado`, y conserva la versión de referencia fijada al responder.
4. El entrenador consulta y analiza seguimiento y ausencias de corredores activos por corredor, plan semanal o entrenamiento, sin modificar, responder ni marcar como revisado; no se crea titularidad, SLA, prioridad, prueba de lectura ni garantía de que cada elemento haya sido leído.

## Modelo lógico consolidado

| Concepto | Relación o invariante de diseño |
| --- | --- |
| Usuario y corredor | Un usuario recibe un único rol inmutable al crear la cuenta. El corredor se asocia a sus etiquetas, publicaciones visibles e información de seguimiento. |
| Etiqueta y valor permitido | Una etiqueta posee un conjunto cerrado de valores. La modalidad es una de estas etiquetas; no existe un sistema paralelo de modalidad. |
| Segmento | Evalúa criterios sobre etiquetas y aplica inclusiones o exclusiones manuales. Su resultado es dinámico y puede solaparse con otros segmentos. |
| Grupo de planificación | Un grupo activo combina por unión uno o varios segmentos, inclusiones y exclusiones persistentes. Un grupo inactivo puede estar vacío, no tiene miembros y conserva una proyección. Un corredor puede quedar sin grupo, pero no pertenecer a dos grupos activos. |
| Plan semanal y entrenamiento | Un grupo tiene como máximo un plan por semana. El plan agrupa como máximo un entrenamiento completo por día; después de publicar su contenido solo cambia junto con una nueva versión y únicamente en días futuros. |
| Grupo y publicación | El plan no recibe asignaciones directas. Una publicación captura una versión y la instantánea de miembros efectivos del grupo, con un único plan por corredor y semana. |
| Seguimiento | Un corredor registra una única respuesta estructurada por entrenamiento publicado durante su ventana de siete días; ausencia, no realización y retirada son estados diferenciados. |
| Notificación | Se origina exclusivamente por publicación o actualización; referencia versión, miembro congelado, contenido requerido y estado técnico. La publicación nace sin correo y lo fija al resolver por primera vez `active(currentVerifiedEmail)`; cambios posteriores solo afectan a solicitudes futuras. |

El modelo es conceptual y no sustituye los modelos detallados, tablas, identificadores, retención o límites de cada módulo. `ADR-0012` define el motor, las garantías transaccionales y la estrategia de índices; los ocho diseños detallados concretan la propiedad y los artefactos que deben producirse antes de implementar.

## Trazabilidad de requisitos

Los criterios de validación citados son los de [Criterios de aceptación — Fase 1](phase-1-acceptance-criteria.md). Cada estado enlaza el diseño detallado que materializa el requisito; los bloqueos posteriores se distinguen del cierre de diseño.

| Requisito | Flujo y componente | Modelo lógico o regla principal | Decisiones de Fase 1 | ADR relacionado o candidato | Validación prevista | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| `RF-01` | Acceso; Identidad y acceso | Invitación, doble declaración de mayoría de edad, activación, reactivación, credencial, sesiones, cambio de correo y restablecimiento sin revelar cuentas existentes. | `D-10` | `ADR-0003`, `ADR-0010` a `ADR-0017` | Criterios de `RF-01`; pruebas de declaraciones, estado, secretos en fragmento, caducidad, revocación, concurrencia, enumeración, aceptación del proveedor y entrega controlada. | Identidad y entrega diseñadas; revisión documental en curso y producción bloqueada |
| `RF-02` | Administración; Identidad, Gestión de corredores y Clasificación | Rol inicial inmutable, perfil de corredor y taxonomías cerradas administrados solo por administrador; modificar roles queda descartado en Fase 2. | `D-01`, `D-08` | `ADR-0003`, `ADR-0004`, `ADR-0005`, `ADR-0014`, `ADR-0015`, `ADR-0017`, `ADR-0018`, `ADR-0019` | Criterios de `RF-02`, ajustados para probar asignación inicial y rechazo de cambios; pruebas de autorización. | Identidad, roles, perfil y clasificación validados como diseño; solo se autoriza preparación técnica con datos sintéticos hasta cerrar Fase 2 |
| `RF-03` | Clasificación y segmentación | Etiquetas controladas asignadas a corredores alimentan segmentos dinámicos y solapables. | `D-01`, `D-02` | `ADR-0005`, `ADR-0019` | Criterios de `RF-03`; pruebas de evaluación dinámica, lotes atómicos y solapamiento permitido. | Diseñado y validado en [Diseño detallado de clasificación y segmentación](phase-2-detailed-design-classification-segmentation.md) |
| `RF-04` | Planificación; Clasificación y segmentación; Portal del corredor | Modalidad del corredor como etiqueta y modalidad propia del entrenamiento, informativas e independientes; ubicación libre opcional solo en presencial. | `D-02`, `D-04` | `ADR-0005`, `ADR-0006`, `ADR-0019`, `ADR-0020` (Aceptados) | Criterios de `RF-04`; pruebas de mezcla válida, ubicación ausente, rechazo de ubicación en línea y consulta propia. | Diseñado en clasificación, planificación y [Portal del corredor](phase-2-detailed-design-runner-portal.md) |
| `RF-05` | Clasificación y segmentación | Semántica limitada a Y, varios valores por etiqueta y sin expresiones libres. | `D-05` | `ADR-0005`, `ADR-0019` | Criterios de `RF-05`; pruebas de reglas aceptadas y rechazadas y evaluación explicada. | Diseñado y validado en [Diseño detallado de clasificación y segmentación](phase-2-detailed-design-classification-segmentation.md) |
| `RF-06` | Clasificación y segmentación | Excepciones manuales se aplican sobre el resultado dinámico antes de resolver destinatarios. | `D-01`, `D-05` | `ADR-0005`, `ADR-0019` | Criterios de `RF-06`; pruebas de inclusión, exclusión e historial inmutable. | Diseñado y validado en [Diseño detallado de clasificación y segmentación](phase-2-detailed-design-classification-segmentation.md) |
| `RF-07` | Planificación | Cada grupo tiene como máximo un plan por semana; el plan admite como máximo un entrenamiento completo por día de lunes a domingo. | — | `ADR-0006` (Aceptado), `ADR-0020` (Aceptado) | Criterios de `RF-07`; pruebas de ciclo de vida, semana, revisión global, unicidad grupo-semana y unicidad diaria. | Diseñado y validado en [Diseño detallado de planificación](phase-2-detailed-design-planning.md) |
| `RF-08` | Clasificación, Planificación y Publicación | Un grupo activo combina segmentos e inclusiones o exclusiones persistentes; una reconfiguración protege traslados y la candidatura confirma miembros activos antes de congelarlos. La mezcla de modalidades es válida. | `D-01`, `D-02` | `ADR-0005` a `ADR-0007`, `ADR-0019` a `ADR-0021` (Aceptados) | Criterios de `RF-08`; pruebas de fórmula, mezcla de modalidades, estados, reconfiguración, candidatura, exclusividad y captura. | Diseñado en [publicación](phase-2-detailed-design-publication.md) |
| `RF-09` | Publicación | Validar, versionar y activar plan y destinatarios dentro de una única transacción con coordinación de grupos, revisiones y fecha local. | `D-01`, `D-06` | `ADR-0007`, `ADR-0019` a `ADR-0021` (Aceptados) | Criterios de `RF-09`; pruebas de fallo sin visibilidad parcial, candidatura obsoleta y carreras con clasificación, grupos o fecha. | Diseñado y validado en [publicación](phase-2-detailed-design-publication.md) |
| `RF-10` | Publicación | Cada versión conserva contenido completo, nombre de grupo y destinatarios congelados desde la primera publicación; cambios posteriores no recalculan el conjunto. | `D-01`, `D-06` | `ADR-0007`, `ADR-0019` a `ADR-0021` (Aceptados) | Criterios de `RF-10`; pruebas ante cambios posteriores de contenido, actividad, grupo o clasificación. | Diseñado y validado en [publicación](phase-2-detailed-design-publication.md) |
| `RF-11` | Planificación | El tipo de la parte principal usa el catálogo cerrado; calentamiento y enfriamiento son siempre `rodaje`. | — | `ADR-0006` (Aceptado), `ADR-0020` (Aceptado) | Criterios de `RF-11`; pruebas de catálogo, fases fijas y entrenamiento completo. | Diseñado y validado en [Diseño detallado de planificación](phase-2-detailed-design-planning.md) |
| `RF-12` | Planificación | Objetivos comunes `Z1..Z5` o ritmo relativo con referencias externas; no se almacenan ni calculan datos deportivos personales y desconocerlos no bloquea. | — | `ADR-0006` (Aceptado), `ADR-0020` (Aceptado) | Criterios de `RF-12`; pruebas de textos exactos, escalas, unidades, ausencia de personalización y recuperación. | Diseñado en [Diseño detallado de planificación](phase-2-detailed-design-planning.md) |
| `RF-13` | Planificación y Portal del corredor | Cada entrenamiento declara modalidad; la ubicación libre opcional se conserva y solo se admite en presencial. | `D-04` | `ADR-0006` (Aceptado), `ADR-0020` (Aceptado) | Criterios de `RF-13`; pruebas de captura, ausencia, rechazo en línea y consulta posterior. | Diseñado y validado en planificación y [Portal del corredor](phase-2-detailed-design-runner-portal.md) |
| `RF-14` | Planificación y Publicación | Estados visibles `borrador` y `publicado`; tras publicar no hay borrador persistente, retirada ni restauración y nombre, grupo y semana quedan fijos. | `D-06` | `ADR-0006`, `ADR-0007`, `ADR-0020`, `ADR-0021` (Aceptados) | Criterios de `RF-14`; pruebas de transición irreversible, concurrencia y ausencia de cambios pendientes o restauración. | Diseñado y validado en planificación y [publicación](phase-2-detailed-design-publication.md) |
| `RF-15` | Publicación y Entrega de notificaciones | Una sesión local confirma como una versión completa cualquier diferencia visible futura; no-op, hoy, pasado y metadatos técnicos quedan fuera. | `D-06` | `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0020`, `ADR-0021` (Aceptados) | Criterios de `RF-15`; pruebas del catálogo exhaustivo de cambios, temporalidad, atomicidad, prioridad, orden y entrega. | Diseñado en [Publicación](phase-2-detailed-design-publication.md) y [Entrega de notificaciones](phase-2-detailed-design-notification-delivery.md) |
| `RF-16` | Portal del corredor; Identidad y acceso | Todo el PMV web cumple WCAG `2.2 AA`, incluida la vista propia de planes, objetivos, modalidad y ubicación. | `D-04`, `D-08`, `D-11` | `ADR-0002`, `ADR-0004`, `ADR-0006`, `ADR-0020` (Aceptados) | Criterios de `RF-16`; pruebas automáticas y manuales a `320` CSS px, zoom `400 %`, texto `200 %`, teclado, foco, errores y objetivos `24 × 24` CSS px. | Diseñado en [Portal del corredor](phase-2-detailed-design-runner-portal.md) |
| `RF-17` | Seguimiento y revisión | Registro único editable durante siete días; `realizado` exige la escala `1 Muy suave` a `5 Muy intenso` sin valor por defecto y sensación; solo se persiste el entero. | `D-07` | `ADR-0004`, `ADR-0009`, `ADR-0010`, `ADR-0022` (Aceptados) | Criterios de `RF-17`; pruebas de pregunta, etiquetas, extremos, combinaciones, consentimiento, ventana y concurrencia. | Diseñado en [Seguimiento y revisión](phase-2-detailed-design-tracking-review.md) y [Portal del corredor](phase-2-detailed-design-runner-portal.md) |
| `RF-18` | Portal del corredor y Seguimiento | Historial propio de todo entrenamiento publicado, incluidos `sin-seguimiento` y `retirado`, con versión de respuesta e aislamiento. | `D-07`, `D-08` | `ADR-0004` (Aceptado), `ADR-0009` (Aceptado) | Criterios de `RF-18`; pruebas de conjunto histórico, versiones, retirados y acceso indebido. | Diseñado y validado en [Seguimiento y revisión](phase-2-detailed-design-tracking-review.md) y [Portal del corredor](phase-2-detailed-design-runner-portal.md) |
| `RF-19` | Seguimiento y revisión | Administrador y entrenador consultan; el entrenador solo ve activos. No existe workflow, SLA, prueba de lectura ni garantía de revisión individual. | `D-07`, `D-08` | `ADR-0004` (Aceptado), `ADR-0009` (Aceptado) | Criterios de `RF-19`; pruebas de filtros, activos, solo lectura y ausencia de estados o efectos por consultar. | Diseñado en [Seguimiento y revisión](phase-2-detailed-design-tracking-review.md) |
| `RF-20` | Publicación y Entrega de notificaciones | Cada versión crea una solicitud por miembro; el primer procesamiento fija el correo vigente si está activo, omite al inactivo o falla a `+120 min` si no resuelve elegibilidad. | `D-06` | `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0021` (Aceptados) | Criterios de `RF-20`; pruebas de fijación, cambio posterior, omisión, límite, orden, proveedor y webhook. | Diseñado en [Publicación](phase-2-detailed-design-publication.md) y [Entrega de notificaciones](phase-2-detailed-design-notification-delivery.md); controles previos a producción pendientes |
| `RF-21` | Publicación; cobertura semanal | Consulta derivada de todos los activos con un estado principal y `sin-modalidad` independiente; no bloquea ni persiste proyección. | `D-09` | `ADR-0014`, `ADR-0017`, `ADR-0021` (Aceptados) | Criterios de `RF-21`; pruebas de exhaustividad, exclusión de no activos, conteos, paginación, estados y cálculo canónico. | Diseñado en [Publicación](phase-2-detailed-design-publication.md) |

## Trazabilidad de decisiones de Fase 1

| Decisión | Tratamiento en este diseño | ADR relacionado o candidato |
| --- | --- | --- |
| `D-01` | Taxonomías, segmentos solapables, grupos exclusivos, reconfiguración atómica, excepciones e instantáneas de versión y destinatarios efectivos. | `ADR-0005` (Aceptado), `ADR-0006` (Aceptado), `ADR-0007` (Aceptado), `ADR-0019` (Aceptado), `ADR-0020` (Aceptado) |
| `D-02` | Modalidad del corredor dentro de la taxonomía controlada y modalidad propia del entrenamiento sin duplicar clasificación. | `ADR-0005` (Aceptado), `ADR-0019` (Aceptado), `ADR-0020` (Aceptado) |
| `D-03` | Límite de un único club en todos los componentes lógicos, materializado como una aplicación única modular. | `ADR-0002` (Aceptado) |
| `D-04` | Ubicación libre opcional por entrenamiento con modalidad presencial explícita. | `ADR-0006` (Aceptado), `ADR-0020` (Aceptado) |
| `D-05` | Gramática limitada de reglas de segmentos. | `ADR-0005` (Aceptado), `ADR-0019` (Aceptado) |
| `D-06` | Borrador solo antes de publicar; después, sesión local y sustitución atómica de días futuros, versiones completas, solicitudes para destinatarios congelados y entrega condicionada a actividad actual. | `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0020`, `ADR-0021` (Aceptados) |
| `D-07` | Seguimiento estructurado, esfuerzo percibido `1..5`, historial y revisión global de solo lectura. | `ADR-0009`, `ADR-0022` (Aceptados) |
| `D-08` | Permisos globales de entrenador y aislamiento del corredor. | `ADR-0004` (Aceptado), `ADR-0019` (Aceptado) |
| `D-09` | Cobertura semanal derivada y no bloqueante para dar visibilidad a entrenador y administrador. | `ADR-0014`, `ADR-0017`, `ADR-0021` (Aceptados) |
| `D-10` | PMV solo para adultos, con doble declaración y evidencia mínima sin documentos. | `ADR-0003`, `ADR-0010` (Aceptados) |
| `D-11` | WCAG `2.2 AA` como contrato de todo el PMV web. | Línea base transversal y diseños de frontend; no requiere un ADR adicional. |

## Decisiones de arquitectura y bloqueos posteriores

| ADR o pregunta | Impacto | Bloquea | Responsable | Tratamiento |
| --- | --- | --- | --- | --- |
| `ADR-0003`: identidad, autenticación e invitación | Seguridad de acceso y flujo de activación. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con línea base de seguridad de acceso. |
| `ADR-0004`: autorización y aislamiento | Permisos, consultas y datos visibles. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con jerarquía explícita e inmutabilidad del rol. |
| `ADR-0005`: taxonomías y segmentación | Modelo de datos y semántica de segmentos. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con un único valor por definición y corredor, modalidad protegida y segmentos dinámicos solapables. |
| `ADR-0006`: grupos, planes y entrenamientos | Grupos exclusivos, modelo semanal, fases, bloques, objetivos y ubicación. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con grupos estables, un plan por grupo-semana, un entrenamiento por día y estructura obligatoria de tres fases. |
| `ADR-0007`: publicación, versiones y destinatarios | Consistencia, historial, captura de contenido y miembros del grupo y garantía de un plan por corredor y semana. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con visibilidad inmediata, grupo no vacío, contenido completo inmutable y destinatarios congelados desde la primera publicación. |
| `ADR-0008`: notificaciones de publicación | Solicitudes transaccionales, destinatarios, contenido, idempotencia lógica y orden. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con solicitud individual para todos los destinatarios, sin estado visible ni reintento manual. |
| `ADR-0009`: seguimiento e historial | Identidad del registro, ventana de actualización, versiones, conjunto histórico y revisión global. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con siete días, versión fijada al responder, retirados históricos y lectura global sin flujo de revisión. |
| `ADR-0010`: privacidad, retención y derechos | Responsable persona física, bases por finalidad, consentimiento del comentario, mayores de edad, conservación, bloqueo, derechos, encargados, EIPD y evidencias. | Salida a producción y cualquier cambio de alcance derivado. | Responsable del tratamiento con asesoramiento de privacidad | Aceptado; las evidencias jurídicas y operativas siguen bloqueando producción y no se consideran satisfechas por la aceptación arquitectónica. |
| `ADR-0011`: correo transaccional | Brevo por API REST, outbox persistente, worker interno, reintentos por tipo, webhooks, supresión y observabilidad. | No bloquea implementación; decisión aceptada. Dominio, revisión de privacidad y alertas bloquean producción. | Revisor de arquitectura | Aceptado con reconciliación dentro del TTL, entrega posterior gestionada por Brevo y orden liberado al aceptar el proveedor. |
| `ADR-0012`: persistencia y transacciones | PostgreSQL, restricciones, coordinación de planificación, publicación, outbox recuperable, índices, cursores y migraciones. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con PostgreSQL compartido, `READ COMMITTED`, bloqueos explícitos, restricciones físicas, outbox con lease, cursor y migraciones versionadas. |
| `ADR-0013`: runtime y contrato API | Java, Spring MVC, JDBC, HikariCP, jOOQ, Flyway, frontend conjunto, OpenAPI y gates de calidad. | OpenAPI, build y gates bloquean comenzar la implementación; la elección arquitectónica está aceptada. | Revisor de arquitectura | Producir y revisar los artefactos contract-first antes de controladores o código de negocio. |
| `ADR-0014`: módulos, hexagonal y DDD | Límites de código y datos, dependencias, comunicación, puertos, esquemas y modelado de dominio. | El scaffolding y las verificaciones modulares bloquean desarrollo que pueda violar límites; la decisión está aceptada. | Revisor de arquitectura | Materializar un proyecto Gradle único, ocho módulos y reglas Spring Modulith y ArchUnit antes de ampliar código. |
| `ADR-0015`: autorización de aplicación | Propagación del actor, políticas de aplicación, alcance en consultas y pruebas. | Las políticas y pruebas bloquean cualquier caso de uso protegido; la decisión está aceptada. | Revisor de arquitectura | Implementar actor explícito, políticas Java canónicas e identidad de sistema mínima desde el primer caso de uso. |
| `ADR-0016`: despliegue y operación | Microsoft Azure `West Europe`, artefactos, promoción, PostgreSQL, copias, observabilidad, secretos y recuperación. | Despliegue de staging y producción. | Revisor de arquitectura y persona operadora | Aceptado; completar evidencias previas a producción. |
| `ADR-0017`: API HTTP orientada a recursos | Recursos, rutas, métodos, transiciones, seguridad y controles del contrato HTTP. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado; nivel 2 de Richardson sin HATEOAS obligatorio y con revisión semántica humana. |
| `ADR-0018`: ciclo de vida, inactividad y reactivación del corredor | Perfil mínimo, permisos, elegibilidad, retención automática durante 24 meses y reactivación revisada. | Datos personales reales, funcionalidades de negocio y producción; no bloquea la preparación técnica con datos sintéticos. | Revisor de arquitectura; Revisor de privacidad o DPO antes de datos reales | Aceptado como decisión de arquitectura; la revisión de base jurídica, necesidad y proporcionalidad sigue pendiente y prohíbe usar datos reales. |
| `ADR-0019`: coordinación, ciclo de vida e historial de clasificación | Coordinador transaccional, segmentos inactivos, lotes, impacto, evaluación explicada, historial y reservas de reactivación. | Datos personales reales, funcionalidades de negocio y producción; no bloquea la preparación técnica con datos sintéticos. | Revisor de arquitectura; Revisor de privacidad o DPO antes de datos reales | Aceptado; no quedan decisiones de producto o arquitectura pendientes y OpenAPI, migraciones y límites medidos preceden a la implementación funcional. |
| `ADR-0020`: ciclo de vida, objetivos e historial de planificación | Grupos activos e inactivos, reconfiguración multigrupo, borradores, objetivos, modalidad, historial y purga. | Datos personales reales y producción; OpenAPI, migraciones y pruebas antes de implementar. | Revisor de arquitectura; Revisor de privacidad o DPO antes de datos reales | Aceptado con decisiones de planificación validadas explícitamente; no levanta el bloqueo de privacidad. |
| `ADR-0021`: edición de publicaciones y elegibilidad de notificaciones | Regla temporal, sesión local de varios días, ausencia de retirada y de borrador persistente, autoría mínima y omisión de correo a inactivos. | OpenAPI, migraciones y pruebas transaccionales antes de implementar; datos personales reales, proveedor de correo y producción continúan bloqueados. | Revisor de arquitectura | Aceptado; no quedan decisiones de producto o arquitectura pendientes y sustituye normativamente solo las partes incompatibles que identifica. |
| `ADR-0022`: escala de esfuerzo percibido | Intervalo entero `1..5`, ausencia cuando no se realiza y prohibición de conversiones implícitas. | OpenAPI, restricción física, interfaz y pruebas antes de implementar seguimiento. | Revisores de producto y arquitectura | Aceptado; reemplaza únicamente el intervalo `1..10` de `ADR-0009`. |
| `ADR-0023`: recuperación y custodia independiente | Objetivos por escenario, backup externo, cifrado, custodia de clave privada, credenciales y simulacros. | Restauración externa completa, runbooks y evidencias antes de producción. | Revisor de arquitectura, propietario y custodio de recuperación | Aceptado; refina recuperación y salida de `ADR-0016` sin introducir replicación multicloud continua. |

## Riesgos y mitigaciones

- Una publicación parcialmente visible o sin destinatarios congelados rompería la trazabilidad. Mitigación: tratar publicación, versión y destinatarios como una misma decisión en `ADR-0007`.
- Un control de acceso solo de interfaz expondría datos de corredores. Mitigación: `ADR-0004` define reglas de autorización aplicadas en las operaciones de datos.
- Convertir reglas de segmentos en un lenguaje genérico ampliaría el alcance. Mitigación: conservar la gramática de `D-05` y rechazar expresiones libres.
- Un cambio de etiquetas, segmentos o excepciones podría situar a un corredor activo o a una reserva `pending_reactivation` en dos grupos. Mitigación: aplicar `ADR-0019`, bloquear la coordinación global de `planning`, validar el estado final y revertir la operación completa mostrando solo el detalle permitido por el alcance del actor.
- Un cambio de grupo posterior a una publicación podría intentar incluir al corredor en otro plan de la misma semana. Mitigación: los planes ya publicados conservan destinatarios y la primera publicación de otro plan comprueba unicidad transaccional por corredor y semana.
- Trasladar corredores mediante cambios secuenciales podría permitir una primera publicación sobre un estado intermedio. Mitigación: `ADR-0020` exige reconfiguración multigrupo atómica bajo la coordinación global.
- Un borrador abandonado podría conservar ubicación, aclaraciones e historia sin finalidad. Mitigación: `ADR-0020` purga conjuntamente el plan nunca publicado y su historial `90` días después de terminar su semana.
- Implementar parcialmente la semántica de republicación puede producir correos duplicados, omitidos o fuera de orden. Mitigación: aplicar conjuntamente `ADR-0007`, `ADR-0008` y `ADR-0011`.
- Una edición publicada podría alterar días ya alcanzados o acumular un borrador distinto de lo visible. Mitigación: `ADR-0021` aceptado usa fecha local del servidor, bloquea hoy y pasado y confirma contenido y versión en una sola transacción.
- Una baja o cambio de correo posterior a fijar una solicitud podría no impedir su entrega. Mitigación: resolver estado y correo juntos al iniciar el procesamiento, fijar el destino para esa solicitud y aceptar la carrera residual; cambios posteriores rigen solicitudes futuras.
- Una publicación errónea no podrá retirarse. Mitigación: candidatura con grupo, semana, contenido, conteo y lista exacta, revisión estable y confirmación explícita; el riesgo residual se acepta como coste de excluir retirada.
- Una pérdida total de Azure inutilizaría PITR y cualquier clave privada alojada allí. Mitigación: backup diario en Scaleway, custodia de clave privada y credenciales fuera de Azure y restauración externa semestral conforme a `ADR-0023`.
- El comentario libre puede contener datos de salud aunque el PMV no los solicite. Mitigación: mantener los campos acotados, no presentar el seguimiento como clínico y resolver base jurídica, información, retención y derechos en `ADR-0010` antes de producción.

## Resultado de la revisión de Fase 2

La trazabilidad funcional, los ocho diseños detallados, `RF-01` a `RF-21`, `D-01` a `D-11` y los `23` ADR aceptados están actualizados y validados. La PR de cierre registra las puertas de calidad, la autorrevisión del mantenedor único, la ausencia de revisión independiente y el riesgo aceptado.

Mientras Fase 2 permanezca en revisión solo se autoriza preparación técnica con datos sintéticos: proyecto base, pipeline, OpenAPI, Problem Details, Flyway, jOOQ, generación de cliente y fixtures. La implementación funcional posterior se planificará por funcionalidades verticales en Linear; cada slice deberá aprobar contrato, modelo y migraciones, autorización, transacciones y pruebas antes de código de negocio. Los datos personales reales, el proveedor de correo y producción continúan bloqueados por las evidencias de privacidad, seguridad, recuperación y operación aplicables.
