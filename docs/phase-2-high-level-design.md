# Diseño funcional y técnico de alto nivel — Fase 2

**Estado:** Propuesto
**Fecha:** 2026-08-12

## Propósito

Materializar el contrato de entrada de Fase 1 en un diseño de alto nivel trazable. Este documento delimita los componentes lógicos, los flujos, los datos y las decisiones técnicas que deben resolverse antes de implementar el PMV.

No selecciona framework, base de datos, proveedor de identidad, proveedor de correo ni estrategia de despliegue. Esas decisiones deben registrarse en ADRs antes de cerrar el diseño afectado.

## Alcance y restricciones heredadas

- Aplicación web adaptable para un único club, con más de 500 corredores registrados y picos iniciales inferiores a 100 usuarios concurrentes.
- Roles operativos: administrador, entrenador y corredor. El corredor solo accede a sus propios datos; administrador y entrenador acceden a todos los datos operativos del PMV.
- La modalidad se expresa mediante una etiqueta controlada. El lugar de encuentro es texto libre por entrenamiento presencial y no se limita a El Retiro.
- Las reglas de segmentos se limitan a condiciones de etiquetas con operador Y, varios valores por etiqueta e inclusiones o exclusiones manuales.
- Los segmentos pueden solaparse. Los grupos de planificación combinan segmentos y excepciones persistentes, y un corredor pertenece como máximo a uno.
- No forman parte del PMV multiclub, aplicaciones nativas, mensajería, integraciones deportivas, reservas, pagos ni datos de salud especiales.

## Principios de diseño

- Mantener una única fuente de verdad para planes, publicaciones y seguimiento. El correo solo notifica disponibilidad o cambios.
- Resolver destinatarios en la primera publicación del plan y conservarlos en todas sus versiones. Los cambios posteriores de etiquetas, segmentos, grupos o pertenencias solo afectan a planes todavía no publicados.
- Resolver conflictos de pertenencia al mantener grupos de planificación, no al crear cada plan semanal.
- Separar el borrador de un plan de cada versión publicada. Una republicación no es una edición silenciosa.
- Aplicar autorización en cada operación y no solo en la interfaz. El aislamiento de cada corredor es una regla de datos y de acceso.
- Representar el seguimiento con valores estructurados y comentario opcional; no solicitar campos de salud ni ampliar ese ámbito, sin ignorar que el texto libre puede contenerlos y requiere tratamiento en `ADR-0010`.

## Componentes lógicos propuestos

| Componente | Responsabilidad | Requisitos principales | Datos lógicos que gobierna |
| --- | --- | --- | --- |
| Identidad y acceso | Invitación, activación, inicio, restablecimiento y autorización por rol. | `RF-01`, `RF-02`, `RF-16`, `RF-18`, `RF-19` | Usuario, rol, estado de activación y credenciales. |
| Administración y taxonomías | Gestión de usuarios, definiciones de etiquetas y valores permitidos. | `RF-02`, `RF-03`, `RF-04` | Corredor, etiqueta, valor permitido y asignación de etiqueta. |
| Segmentación | Reglas dinámicas y excepciones manuales que producen clasificaciones reutilizables y solapables. | `RF-03`, `RF-05`, `RF-06`, `RF-08` | Segmento, criterio de etiqueta, inclusión manual y exclusión manual. |
| Planificación | Gestión de grupos exclusivos, planes semanales, fases, bloques, catálogo, objetivos y lugar de encuentro. | `RF-04`, `RF-07`, `RF-08`, `RF-11`, `RF-12`, `RF-13`, `RF-14` | Grupo de planificación, excepción de grupo, plan semanal, entrenamiento, fase, bloque, tipo, objetivo y ubicación. |
| Publicación y notificación | Captura de miembros del grupo, publicación y republicación atómicas, versiones y solicitud de correo. | `RF-08`, `RF-09`, `RF-10`, `RF-14`, `RF-15`, `RF-20` | Publicación, versión publicada, destinatario efectivo y notificación. |
| Consulta del corredor | Consulta móvil de planes, entrenamientos, ubicación e historial propio. | `RF-16`, `RF-18` | Vista derivada de publicaciones y seguimiento del corredor autenticado. |
| Seguimiento y revisión | Registro de ejecución y consulta por entrenador. | `RF-17`, `RF-18`, `RF-19` | Registro de seguimiento vinculado a entrenamiento y publicación. |

Estos límites son lógicos. `ADR-0002` aceptado define que se materializan como módulos de una única aplicación desplegable, sin introducir multiclub fuera de alcance. Las decisiones de tecnología, persistencia y plataforma de despliegue siguen pendientes.

## Flujos de alto nivel

### Acceso y administración

1. El administrador invita al corredor por correo.
2. El corredor activa su cuenta, define contraseña y puede iniciar sesión o solicitar restablecimiento.
3. El administrador crea o invita usuarios, asigna su rol inicial inmutable y administra taxonomías cerradas; además puede realizar las operaciones del entrenador.
4. Identidad aporta el rol asignado y cada módulo aplica en el backend la capacidad y el alcance del recurso; la administración de taxonomías no se delega a entrenador ni corredor.

### Segmentación y planificación

1. El entrenador configura un segmento mediante reglas permitidas y excepciones manuales.
2. El entrenador crea un grupo de planificación, le asocia uno o varios segmentos y aplica inclusiones o exclusiones persistentes cuando corresponda.
3. El sistema rechaza cualquier cambio que sitúe a un corredor en dos grupos y muestra los corredores y grupos en conflicto.
4. El entrenador crea como máximo un plan para la pareja grupo-semana y añade como máximo un entrenamiento por día.
5. Cada entrenamiento declara calentamiento por duración, una parte principal con bloques ordenados y enfriamiento por duración, además de ubicación cuando corresponda.
6. Antes de publicar, el sistema valida el plan y resuelve los miembros efectivos actuales del grupo.

### Publicación y republicación

1. Una publicación válida hace visible el plan completo a todos los destinatarios efectivos en una única operación lógica.
2. La publicación conserva una versión y su conjunto de destinatarios.
3. Cualquier cambio en contenido visible crea una nueva publicación del plan completo para los mismos destinatarios; cambios internos o de auditoría no obligan a republicar.
4. La publicación o republicación crea transaccionalmente una solicitud individual para cada destinatario congelado. La entrega asíncrona, los reintentos automáticos y la observabilidad técnica quedan pendientes de `ADR-0011`.

### Consulta, seguimiento y revisión

1. El corredor autenticado consulta únicamente sus publicaciones visibles, adaptadas a móvil.
2. Desde la fecha del entrenamiento y durante siete días naturales, el corredor registra `realizado` con esfuerzo y sensación obligatorios o `no-realizado` sin ellos; el comentario es opcional.
3. El historial incluye todos los entrenamientos que llegaron a publicarse, diferencia `sin-seguimiento`, `no-realizado` y `retirado`, y conserva la versión de referencia fijada al responder.
4. El entrenador revisa seguimiento y ausencias por corredor, plan semanal o entrenamiento, sin modificar, responder ni marcar como revisado; no se crea una relación de titularidad de entrenador en el PMV.

## Modelo lógico preliminar

| Concepto | Relación o invariante de diseño |
| --- | --- |
| Usuario y corredor | Un usuario recibe un único rol inmutable al crear la cuenta. El corredor se asocia a sus etiquetas, publicaciones visibles e información de seguimiento. |
| Etiqueta y valor permitido | Una etiqueta posee un conjunto cerrado de valores. La modalidad es una de estas etiquetas; no existe un sistema paralelo de modalidad. |
| Segmento | Evalúa criterios sobre etiquetas y aplica inclusiones o exclusiones manuales. Su resultado es dinámico y puede solaparse con otros segmentos. |
| Grupo de planificación | Combina por unión uno o varios segmentos, inclusiones y exclusiones persistentes. Un corredor puede quedar sin grupo, pero no pertenecer a dos grupos efectivos. |
| Plan semanal y entrenamiento | Un grupo tiene como máximo un plan por semana. El plan agrupa como máximo un entrenamiento por día; cada entrenamiento contiene calentamiento, bloques principales y enfriamiento, además de lugar libre cuando aplique. |
| Grupo y publicación | El plan no recibe asignaciones directas. Una publicación captura una versión y la instantánea de miembros efectivos del grupo, con un único plan por corredor y semana. |
| Seguimiento | Un corredor registra una única respuesta estructurada por entrenamiento publicado durante su ventana de siete días; ausencia, no realización y retirada son estados diferenciados. |
| Notificación | Se origina exclusivamente por publicación o republicación; referencia la versión, destinatario, contenido requerido y resultado de entrega cuando ese dato se incorpore. |

El modelo es conceptual y no define tablas, identificadores, índices, consistencia transaccional ni retención. Esos detalles dependen de ADRs y diseño posterior.

## Trazabilidad de requisitos

Los criterios de validación citados son los de [Criterios de aceptación — Fase 1](phase-1-acceptance-criteria.md). El estado `Pendiente` indica diseño técnico no cerrado, no ausencia de tratamiento funcional.

| Requisito | Flujo y componente | Modelo lógico o regla principal | Decisiones de Fase 1 | ADR relacionado o candidato | Validación prevista | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| `RF-01` | Acceso; Identidad y acceso | Invitación, activación, credencial y restablecimiento sin revelar cuentas existentes. | — | `ADR-0003` | Criterios de `RF-01`; pruebas de token, caducidad y enumeración de cuentas. | Pendiente |
| `RF-02` | Administración; Identidad y acceso | Rol inicial inmutable y taxonomías cerradas administrados solo por administrador; modificar roles queda descartado en Fase 2. | `D-01`, `D-08` | `ADR-0003`, `ADR-0004`, `ADR-0005` | Criterios de `RF-02`, ajustados para probar asignación inicial y rechazo de cambios; pruebas de autorización. | Pendiente |
| `RF-03` | Segmentación; Administración y taxonomías | Etiquetas controladas alimentan segmentos dinámicos y solapables. | `D-01`, `D-02` | `ADR-0005` | Criterios de `RF-03`; pruebas de evaluación dinámica y solapamiento permitido. | Pendiente |
| `RF-04` | Planificación; Administración y taxonomías | Modalidad como etiqueta y ubicación libre solo cuando corresponda. | `D-02`, `D-04` | `ADR-0005`, `ADR-0006` (Aceptado) | Criterios de `RF-04`; pruebas de valores permitidos y ubicación. | Pendiente |
| `RF-05` | Segmentación | Semántica limitada a Y, varios valores por etiqueta y sin expresiones libres. | `D-05` | `ADR-0005` | Criterios de `RF-05`; pruebas de reglas aceptadas y rechazadas. | Pendiente |
| `RF-06` | Segmentación | Excepciones manuales se aplican sobre el resultado dinámico antes de resolver destinatarios. | `D-01`, `D-05` | `ADR-0005` | Criterios de `RF-06`; pruebas de inclusión y exclusión. | Pendiente |
| `RF-07` | Planificación | Cada grupo tiene como máximo un plan por semana; el plan admite como máximo un entrenamiento por día de lunes a domingo. | — | `ADR-0006` (Aceptado) | Criterios de `RF-07`; pruebas de ciclo de vida, unicidad grupo-semana y unicidad diaria. | Pendiente |
| `RF-08` | Segmentación y Planificación | Un grupo combina segmentos e inclusiones o exclusiones persistentes; la primera publicación congela sus miembros para todas las versiones del plan. | `D-01` | `ADR-0005` (Aceptado), `ADR-0006` (Aceptado), `ADR-0007` (Aceptado) | Criterios de `RF-08`; pruebas de fórmula del grupo, referencias, exclusividad y captura al publicar. | Pendiente |
| `RF-09` | Publicación y notificación | Validar, versionar y activar plan y destinatarios dentro de una única transacción. | `D-01`, `D-06` | `ADR-0007` (Aceptado) | Criterios de `RF-09`; pruebas de fallo sin visibilidad parcial. | Pendiente |
| `RF-10` | Publicación y notificación | Cada publicación conserva contenido inmutable y el conjunto de destinatarios congelado en la primera versión. | `D-01`, `D-06` | `ADR-0007` (Aceptado) | Criterios de `RF-10`; pruebas ante cambios posteriores del borrador o grupo. | Pendiente |
| `RF-11` | Planificación | El tipo de la parte principal usa el catálogo cerrado; calentamiento y enfriamiento son siempre `rodaje`. | — | `ADR-0006` (Aceptado) | Criterios de `RF-11`; pruebas de catálogo y fases fijas. | Pendiente |
| `RF-12` | Planificación | Los bloques principales tienen repeticiones, duración o distancia, objetivo y recuperación estructurada; calentamiento y enfriamiento solo tienen duración. | — | `ADR-0006` (Aceptado) | Criterios de `RF-12`; pruebas de bloques, objetivos y modalidades de recuperación. | Pendiente |
| `RF-13` | Planificación y Consulta del corredor | Ubicación libre se conserva y se muestra para entrenamiento presencial cuando exista. | `D-04` | `ADR-0006` (Aceptado) | Criterios de `RF-13`; pruebas de captura y consulta. | Pendiente |
| `RF-14` | Planificación y Publicación | Estados visibles: borrador y publicado; editar el borrador no altera la versión activa ni crea un tercer estado. | `D-06` | `ADR-0006` (Aceptado), `ADR-0007` (Aceptado) | Criterios de `RF-14`; pruebas de transiciones y cambios pendientes. | Pendiente |
| `RF-15` | Publicación y notificación | Todo cambio visible exige una republicación completa y solicitudes individuales para todos los destinatarios congelados del plan. | `D-06` | `ADR-0007` (Aceptado), `ADR-0008` (Aceptado) | Criterios de `RF-15`; pruebas de versión, cambios relevantes, atomicidad y notificación. | Pendiente |
| `RF-16` | Consulta del corredor; Identidad y acceso | Vista móvil del único plan semanal propio, sus fases, bloques y ubicación, sin exponer datos ajenos. | `D-04`, `D-08` | `ADR-0002`, `ADR-0004`, `ADR-0006` (Aceptado) | Criterios de `RF-16`; pruebas adaptables, de estructura y aislamiento. | Pendiente |
| `RF-17` | Seguimiento y revisión | Registro único por corredor y entrenamiento, editable durante siete días desde su fecha; `realizado` exige esfuerzo y sensación, `no-realizado` los omite y el comentario admite hasta `1.000` caracteres. | `D-07` | `ADR-0004` (Aceptado), `ADR-0009` (Aceptado) | Criterios de `RF-17`; pruebas de valores, comentario, ventana, pertenencia, actualización y concurrencia. | Pendiente |
| `RF-18` | Consulta del corredor y Seguimiento | Historial propio de todo entrenamiento publicado, incluidos `sin-seguimiento` y `retirado`, con versión de respuesta e aislamiento. | `D-07`, `D-08` | `ADR-0004` (Aceptado), `ADR-0009` (Aceptado) | Criterios de `RF-18`; pruebas de conjunto histórico, versiones, retirados y acceso indebido. | Pendiente |
| `RF-19` | Seguimiento y revisión | Administrador y entrenador consultan globalmente seguimiento y ausencias por corredor, plan o entrenamiento, sin modificar ni revisar. | `D-07`, `D-08` | `ADR-0004` (Aceptado), `ADR-0009` (Aceptado) | Criterios de `RF-19`; pruebas de filtros, ausencias y permisos. | Pendiente |
| `RF-20` | Publicación y notificación | Cada publicación confirmada genera una solicitud individual por destinatario, con semana, día, fecha y tipo de cada entrenamiento y enlace; las versiones se procesan en orden sin sustitución. | `D-06` | `ADR-0007` (Aceptado), `ADR-0008` (Aceptado), `ADR-0011` | Criterios de `RF-20`; pruebas de contenido, destinatario, atomicidad, orden y no emisión. | Pendiente |

## Trazabilidad de decisiones de Fase 1

| Decisión | Tratamiento en este diseño | ADR relacionado o candidato |
| --- | --- | --- |
| `D-01` | Taxonomías, segmentos solapables, grupos exclusivos, excepciones e instantáneas de versión y destinatarios efectivos. | `ADR-0005` (Aceptado), `ADR-0006` (Aceptado), `ADR-0007` (Aceptado) |
| `D-02` | Modalidad dentro de la taxonomía controlada. | `ADR-0005` (Aceptado) |
| `D-03` | Límite de un único club en todos los componentes lógicos, materializado como una aplicación única modular. | `ADR-0002` (Aceptado) |
| `D-04` | Ubicación libre por entrenamiento presencial. | `ADR-0006` (Aceptado) |
| `D-05` | Gramática limitada de reglas de segmentos. | `ADR-0005` (Aceptado) |
| `D-06` | Republicación atómica, versiones completas, destinatarios congelados y solicitud transaccional de correo. | `ADR-0007` (Aceptado), `ADR-0008` (Aceptado), `ADR-0011` |
| `D-07` | Seguimiento estructurado, historial y revisión global de solo lectura. | `ADR-0009` (Aceptado) |
| `D-08` | Permisos globales de entrenador y aislamiento del corredor. | `ADR-0004` (Aceptado) |

## Preguntas bloqueantes y ADRs pendientes

| ADR o pregunta | Impacto | Bloquea | Responsable | Tratamiento |
| --- | --- | --- | --- | --- |
| `ADR-0003`: identidad, autenticación e invitación | Seguridad de acceso y flujo de activación. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con línea base de seguridad de acceso. |
| `ADR-0004`: autorización y aislamiento | Permisos, consultas y datos visibles. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con jerarquía explícita e inmutabilidad del rol. |
| `ADR-0005`: taxonomías y segmentación | Modelo de datos y semántica de segmentos. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con un único valor por definición y corredor, modalidad protegida y segmentos dinámicos solapables. |
| `ADR-0006`: grupos, planes y entrenamientos | Grupos exclusivos, modelo semanal, fases, bloques, objetivos y ubicación. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con grupos estables, un plan por grupo-semana, un entrenamiento por día y estructura obligatoria de tres fases. |
| `ADR-0007`: publicación, versiones y destinatarios | Consistencia, historial, captura de contenido y miembros del grupo y garantía de un plan por corredor y semana. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con visibilidad inmediata, grupo no vacío, contenido completo inmutable y destinatarios congelados desde la primera publicación. |
| `ADR-0008`: notificaciones de publicación | Solicitudes transaccionales, destinatarios, contenido, idempotencia lógica y orden. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con solicitud individual para todos los destinatarios, sin estado visible ni reintento manual. |
| `ADR-0009`: seguimiento e historial | Identidad del registro, ventana de actualización, versiones, conjunto histórico y revisión global. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con siete días, versión fijada al responder, retirados históricos y lectura global sin flujo de revisión. |
| `ADR-0010`: privacidad, retención y derechos | Datos personales y seguimiento declarado. | Salida a producción; no el diseño funcional actual salvo cambio de alcance. | Responsable de privacidad o DPO | Resolver antes de producción. |
| `ADR-0011`: correo transaccional | Proveedor, entrega, reintentos y observabilidad de los correos de acceso y publicación. | Implementar cualquier correo del PMV. | Revisor de arquitectura | Proponer y aceptar antes de implementar correo. |

## Riesgos y mitigaciones

- Una publicación parcialmente visible o sin destinatarios congelados rompería la trazabilidad. Mitigación: tratar publicación, versión y destinatarios como una misma decisión en `ADR-0007`.
- Un control de acceso solo de interfaz expondría datos de corredores. Mitigación: `ADR-0004` debe definir reglas de autorización aplicadas en las operaciones de datos.
- Convertir reglas de segmentos en un lenguaje genérico ampliaría el alcance. Mitigación: conservar la gramática de `D-05` y rechazar expresiones libres.
- Un cambio de etiquetas, segmentos o excepciones podría situar a un corredor en dos grupos. Mitigación: validar todos los grupos afectados y rechazar la operación completa mostrando los conflictos.
- Un cambio de grupo posterior a una publicación podría intentar incluir al corredor en otro plan de la misma semana. Mitigación: los planes ya publicados conservan destinatarios y la primera publicación de otro plan comprueba unicidad transaccional por corredor y semana.
- Implementar parcialmente la semántica de republicación puede producir correos duplicados, omitidos o fuera de orden. Mitigación: aplicar `ADR-0007` y `ADR-0008` y resolver `ADR-0011` antes de implementar notificaciones.
- El comentario libre puede contener datos de salud aunque el PMV no los solicite. Mitigación: mantener los campos acotados, no presentar el seguimiento como clínico y resolver base jurídica, información, retención y derechos en `ADR-0010` antes de producción.

## Criterios para avanzar

Este documento deja lista la trazabilidad funcional de Fase 2, pero no cierra la fase ni autoriza implementación. Se puede continuar abriendo ADRs en estado `Propuesto` por orden de dependencia. Antes de implementar un área deben estar aceptados los ADRs que la bloquean y debe existir diseño detallado enlazado a sus requisitos y criterios de aceptación.
