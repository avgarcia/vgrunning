# Diseño funcional y técnico de alto nivel — Fase 2

**Estado:** Propuesto
**Fecha:** 2026-08-11

## Propósito

Materializar el contrato de entrada de Fase 1 en un diseño de alto nivel trazable. Este documento delimita los componentes lógicos, los flujos, los datos y las decisiones técnicas que deben resolverse antes de implementar el PMV.

No selecciona framework, base de datos, proveedor de identidad, proveedor de correo ni estrategia de despliegue. Esas decisiones deben registrarse en ADRs antes de cerrar el diseño afectado.

## Alcance y restricciones heredadas

- Aplicación web adaptable para un único club, con más de 500 corredores registrados y picos iniciales inferiores a 100 usuarios concurrentes.
- Roles operativos: administrador, entrenador y corredor. El corredor solo accede a sus propios datos; administrador y entrenador acceden a todos los datos operativos del PMV.
- La modalidad se expresa mediante una etiqueta controlada. El lugar de encuentro es texto libre por entrenamiento presencial y no se limita a El Retiro.
- Las reglas de segmentos se limitan a condiciones de etiquetas con operador Y, varios valores por etiqueta e inclusiones o exclusiones manuales.
- No forman parte del PMV multiclub, aplicaciones nativas, mensajería, integraciones deportivas, reservas, pagos ni datos de salud especiales.

## Principios de diseño

- Mantener una única fuente de verdad para planes, publicaciones y seguimiento. El correo solo notifica disponibilidad o cambios.
- Resolver el destinatario efectivo en cada publicación y conservarlo con su versión. Los cambios posteriores de etiquetas, segmentos o asignaciones no alteran una publicación histórica.
- Separar el borrador de un plan de cada versión publicada. Una republicación no es una edición silenciosa.
- Aplicar autorización en cada operación y no solo en la interfaz. El aislamiento de cada corredor es una regla de datos y de acceso.
- Representar el seguimiento con valores estructurados y comentario opcional; no ampliar el ámbito a datos de salud especiales.

## Componentes lógicos propuestos

| Componente | Responsabilidad | Requisitos principales | Datos lógicos que gobierna |
| --- | --- | --- | --- |
| Identidad y acceso | Invitación, activación, inicio, restablecimiento y autorización por rol. | `RF-01`, `RF-02`, `RF-16`, `RF-18`, `RF-19` | Usuario, rol, estado de activación y credenciales. |
| Administración y taxonomías | Gestión de usuarios, definiciones de etiquetas y valores permitidos. | `RF-02`, `RF-03`, `RF-04` | Corredor, etiqueta, valor permitido y asignación de etiqueta. |
| Segmentación | Reglas dinámicas y excepciones manuales para formar destinatarios. | `RF-03`, `RF-05`, `RF-06`, `RF-08` | Segmento, criterio de etiqueta, inclusión manual y exclusión manual. |
| Planificación | Creación del plan semanal, sus entrenamientos, catálogo, objetivos y lugar de encuentro. | `RF-04`, `RF-07`, `RF-11`, `RF-12`, `RF-13`, `RF-14` | Plan semanal, entrenamiento, tipo de entrenamiento, objetivo y ubicación. |
| Publicación y notificación | Resolución de destinatarios, publicación y republicación atómicas, versiones y solicitud de correo. | `RF-08`, `RF-09`, `RF-10`, `RF-14`, `RF-15`, `RF-20` | Asignación, publicación, versión publicada, destinatario efectivo y notificación. |
| Consulta del corredor | Consulta móvil de planes, entrenamientos, ubicación e historial propio. | `RF-16`, `RF-18` | Vista derivada de publicaciones y seguimiento del corredor autenticado. |
| Seguimiento y revisión | Registro de ejecución y consulta por entrenador. | `RF-17`, `RF-18`, `RF-19` | Registro de seguimiento vinculado a entrenamiento y publicación. |

Estos límites son lógicos. `ADR-0002` aceptado define que se materializan como módulos de una única aplicación desplegable, sin introducir multiclub fuera de alcance. Las decisiones de tecnología, persistencia y plataforma de despliegue siguen pendientes.

## Flujos de alto nivel

### Acceso y administración

1. El administrador invita al corredor por correo.
2. El corredor activa su cuenta, define contraseña y puede iniciar sesión o solicitar restablecimiento.
3. El administrador administra usuarios, roles y taxonomías cerradas.
4. Identidad aporta el rol vigente y cada módulo aplica en el backend la capacidad y el alcance del recurso; la administración de taxonomías no se delega a entrenador ni corredor.

### Segmentación y planificación

1. El entrenador configura un segmento mediante reglas permitidas y excepciones manuales.
2. El entrenador crea un plan semanal en borrador con entrenamientos fechados, tipo, objetivo y ubicación cuando corresponda.
3. El entrenador asigna el plan a segmentos y, excepcionalmente, a corredores individuales.
4. Antes de publicar, el sistema valida el plan y resuelve el conjunto de destinatarios efectivos.

### Publicación y republicación

1. Una publicación válida hace visible el plan completo a todos los destinatarios efectivos en una única operación lógica.
2. La publicación conserva una versión y su conjunto de destinatarios.
3. Un cambio relevante sobre contenido publicado crea una nueva publicación del plan completo; la definición de "cambio relevante" queda pendiente de `ADR-0007`.
4. La publicación o republicación solicita el correo para los destinatarios efectivos afectados. La entrega, reintentos e idempotencia quedan pendientes de `ADR-0008`.

### Consulta, seguimiento y revisión

1. El corredor autenticado consulta únicamente sus publicaciones visibles, adaptadas a móvil.
2. El corredor registra realizado o no realizado, esfuerzo, sensación y comentario opcional para un entrenamiento publicado.
3. El historial conserva la relación entre seguimiento, entrenamiento y publicación.
4. El entrenador revisa seguimiento por corredor, plan semanal o entrenamiento; no se crea una relación de titularidad de entrenador en el PMV.

## Modelo lógico preliminar

| Concepto | Relación o invariante de diseño |
| --- | --- |
| Usuario y corredor | Un usuario tiene un rol. El corredor se asocia a sus etiquetas, publicaciones visibles e información de seguimiento. |
| Etiqueta y valor permitido | Una etiqueta posee un conjunto cerrado de valores. La modalidad es una de estas etiquetas; no existe un sistema paralelo de modalidad. |
| Segmento | Evalúa criterios sobre etiquetas y aplica inclusiones o exclusiones manuales. Su resultado es dinámico hasta que una publicación captura destinatarios efectivos. |
| Plan semanal y entrenamiento | Un plan agrupa entrenamientos fechados y existe como borrador o publicado. Cada entrenamiento declara tipo, objetivos y, solo cuando aplique, lugar de encuentro libre. |
| Asignación y publicación | Un borrador puede dirigirse a segmentos y corredores. Una publicación crea una versión y una instantánea de destinatarios efectivos. |
| Seguimiento | Un corredor registra un único estado estructurado por entrenamiento publicado, con actualización semántica pendiente de `ADR-0009`. |
| Notificación | Se origina exclusivamente por publicación o republicación; referencia la versión, destinatario, contenido requerido y resultado de entrega cuando ese dato se incorpore. |

El modelo es conceptual y no define tablas, identificadores, índices, consistencia transaccional ni retención. Esos detalles dependen de ADRs y diseño posterior.

## Trazabilidad de requisitos

Los criterios de validación citados son los de [Criterios de aceptación — Fase 1](phase-1-acceptance-criteria.md). El estado `Pendiente` indica diseño técnico no cerrado, no ausencia de tratamiento funcional.

| Requisito | Flujo y componente | Modelo lógico o regla principal | Decisiones de Fase 1 | ADR relacionado o candidato | Validación prevista | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| `RF-01` | Acceso; Identidad y acceso | Invitación, activación, credencial y restablecimiento sin revelar cuentas existentes. | — | `ADR-0003` | Criterios de `RF-01`; pruebas de token, caducidad y enumeración de cuentas. | Pendiente |
| `RF-02` | Administración; Identidad y acceso | Roles y taxonomías cerradas administradas solo por administrador. | `D-01`, `D-08` | `ADR-0003`, `ADR-0004`, `ADR-0005` | Criterios de `RF-02`; pruebas de autorización. | Pendiente |
| `RF-03` | Segmentación; Administración y taxonomías | Etiquetas controladas alimentan segmentos dinámicos. | `D-01`, `D-02` | `ADR-0005` | Criterios de `RF-03`; pruebas de evaluación dinámica. | Pendiente |
| `RF-04` | Planificación; Administración y taxonomías | Modalidad como etiqueta y ubicación libre solo cuando corresponda. | `D-02`, `D-04` | `ADR-0005`, `ADR-0006` | Criterios de `RF-04`; pruebas de valores permitidos y ubicación. | Pendiente |
| `RF-05` | Segmentación | Semántica limitada a Y, varios valores por etiqueta y sin expresiones libres. | `D-05` | `ADR-0005` | Criterios de `RF-05`; pruebas de reglas aceptadas y rechazadas. | Pendiente |
| `RF-06` | Segmentación | Excepciones manuales se aplican sobre el resultado dinámico antes de resolver destinatarios. | `D-01`, `D-05` | `ADR-0005` | Criterios de `RF-06`; pruebas de inclusión y exclusión. | Pendiente |
| `RF-07` | Planificación | El plan semanal agrupa entrenamientos fechados y no se guarda sin semana identificable. | — | `ADR-0006` | Criterios de `RF-07`; pruebas de ciclo de vida del borrador. | Pendiente |
| `RF-08` | Segmentación y Publicación | Asignaciones a segmentos y corredores producen un conjunto candidato de destinatarios. | `D-01` | `ADR-0006`, `ADR-0007` | Criterios de `RF-08`; pruebas de combinación de asignaciones. | Pendiente |
| `RF-09` | Publicación y notificación | Validar todo el plan antes de hacerlo visible en una única operación lógica. | `D-01`, `D-06` | `ADR-0007` | Criterios de `RF-09`; pruebas de fallo sin visibilidad parcial. | Pendiente |
| `RF-10` | Publicación y notificación | Cada publicación conserva versión y destinatarios efectivos inmutables. | `D-01`, `D-06` | `ADR-0007` | Criterios de `RF-10`; pruebas ante cambios posteriores de etiquetas. | Pendiente |
| `RF-11` | Planificación | Catálogo cerrado de seis tipos de entrenamiento del PMV. | — | `ADR-0006` | Criterios de `RF-11`; pruebas de tipos permitidos. | Pendiente |
| `RF-12` | Planificación | Objetivos por frecuencia cardiaca o ritmo relativo, según tipo, y aclaración libre. | — | `ADR-0006` | Criterios de `RF-12`; pruebas de compatibilidad entre tipo y objetivo. | Pendiente |
| `RF-13` | Planificación y Consulta del corredor | Ubicación libre se conserva y se muestra para entrenamiento presencial cuando exista. | `D-04` | `ADR-0006` | Criterios de `RF-13`; pruebas de captura y consulta. | Pendiente |
| `RF-14` | Planificación y Publicación | Estados visibles: borrador y publicado; las versiones no crean un tercer estado de plan. | `D-06` | `ADR-0006`, `ADR-0007` | Criterios de `RF-14`; pruebas de transiciones permitidas. | Pendiente |
| `RF-15` | Publicación y notificación | Republicación completa con destinatarios afectados y correo. | `D-06` | `ADR-0007`, `ADR-0008` | Criterios de `RF-15`; pruebas de versión, afectados y notificación. | Pendiente |
| `RF-16` | Consulta del corredor; Identidad y acceso | Vista móvil de publicaciones propias sin exponer datos ajenos. | `D-04`, `D-08` | `ADR-0002`, `ADR-0004` | Criterios de `RF-16`; pruebas adaptables y de aislamiento. | Pendiente |
| `RF-17` | Seguimiento y revisión | Registro estructurado vinculado a un entrenamiento publicado. | `D-07` | `ADR-0009` | Criterios de `RF-17`; pruebas de valores permitidos y pertenencia. | Pendiente |
| `RF-18` | Consulta del corredor y Seguimiento | Historial propio de entrenamientos y seguimiento con aislamiento por corredor. | `D-07`, `D-08` | `ADR-0004`, `ADR-0009` | Criterios de `RF-18`; pruebas de historial y acceso indebido. | Pendiente |
| `RF-19` | Seguimiento y revisión | Entrenador consulta global por corredor, plan o entrenamiento. | `D-07`, `D-08` | `ADR-0004`, `ADR-0009` | Criterios de `RF-19`; pruebas de filtros y permisos. | Pendiente |
| `RF-20` | Publicación y notificación | Solo publicar o republicar genera correo con semana, resumen y enlace. | `D-06` | `ADR-0007`, `ADR-0008` | Criterios de `RF-20`; pruebas de contenido, destinatario y no emisión. | Pendiente |

## Trazabilidad de decisiones de Fase 1

| Decisión | Tratamiento en este diseño | ADR relacionado o candidato |
| --- | --- | --- |
| `D-01` | Taxonomías, segmentos, excepciones, versión y destinatarios efectivos. | `ADR-0005`, `ADR-0007` |
| `D-02` | Modalidad dentro de la taxonomía controlada. | `ADR-0005` |
| `D-03` | Límite de un único club en todos los componentes lógicos, materializado como una aplicación única modular. | `ADR-0002` (Aceptado) |
| `D-04` | Ubicación libre por entrenamiento presencial. | `ADR-0006` |
| `D-05` | Gramática limitada de reglas de segmentos. | `ADR-0005` |
| `D-06` | Republicación atómica, versiones, afectados y correo. | `ADR-0007`, `ADR-0008` |
| `D-07` | Seguimiento estructurado, historial y revisión. | `ADR-0009` |
| `D-08` | Permisos globales de entrenador y aislamiento del corredor. | `ADR-0004` |

## Preguntas bloqueantes y ADRs pendientes

| ADR o pregunta | Impacto | Bloquea | Responsable | Tratamiento |
| --- | --- | --- | --- | --- |
| `ADR-0003`: identidad, autenticación e invitación | Seguridad de acceso y flujo de activación. | No bloquea; decisión aceptada. | Revisor de arquitectura | Aceptado con línea base de seguridad de acceso. |
| `ADR-0004`: autorización y aislamiento | Permisos, consultas y datos visibles. | Implementar cualquier operación autenticada. | Revisor de arquitectura | Propuesto; aceptar antes de implementar operaciones autenticadas. |
| `ADR-0005`: taxonomías y segmentación | Modelo de datos y semántica de destinatarios. | Implementar administración o segmentación. | Revisor de arquitectura | Proponer antes de cerrar segmentación. |
| `ADR-0006`: plan y entrenamiento | Modelo de planificación, objetivos y ubicación. | Implementar planes o entrenamientos. | Revisor de arquitectura | Proponer antes de cerrar planificación semanal. |
| `ADR-0007`: publicación, versiones y destinatarios | Consistencia, historial y cambios publicados. | Implementar publicación o republicación. | Revisor de arquitectura | Proponer antes de cerrar publicación. |
| `ADR-0008`: correo a afectados | Entrega, idempotencia y tratamiento de fallo. | Implementar correo de publicación. | Revisor de arquitectura | Proponer antes de cerrar notificaciones. |
| `ADR-0009`: seguimiento e historial | Actualización de registros, consulta y retención operativa. | Implementar seguimiento o revisión. | Revisor de arquitectura | Proponer antes de cerrar seguimiento. |
| `ADR-0010`: privacidad, retención y derechos | Datos personales y seguimiento declarado. | Salida a producción; no el diseño funcional actual salvo cambio de alcance. | Responsable de privacidad o DPO | Resolver antes de producción. |
| `ADR-0011`: correo transaccional | Proveedor, entrega, reintentos y observabilidad de los correos de acceso y publicación. | Implementar cualquier correo del PMV. | Revisor de arquitectura | Proponer y aceptar antes de implementar correo. |

## Riesgos y mitigaciones

- Una publicación parcialmente visible o sin destinatarios congelados rompería la trazabilidad. Mitigación: tratar publicación, versión y destinatarios como una misma decisión en `ADR-0007`.
- Un control de acceso solo de interfaz expondría datos de corredores. Mitigación: `ADR-0004` debe definir reglas de autorización aplicadas en las operaciones de datos.
- Convertir reglas de segmentos en un lenguaje genérico ampliaría el alcance. Mitigación: conservar la gramática de `D-05` y rechazar expresiones libres.
- Definir tarde la semántica de republicación puede producir correos duplicados o cambios silenciosos. Mitigación: resolver `ADR-0007` y `ADR-0008` antes de implementar publicación.
- Extender seguimiento a salud, lesiones o datos equivalentes cambiaría privacidad y alcance. Mitigación: mantener los campos de `RF-17` y escalar cualquier ampliación a `ADR-0010` y revisión de privacidad.

## Criterios para avanzar

Este documento deja lista la trazabilidad funcional de Fase 2, pero no cierra la fase ni autoriza implementación. Se puede continuar abriendo ADRs en estado `Propuesto` por orden de dependencia. Antes de implementar un área deben estar aceptados los ADRs que la bloquean y debe existir diseño detallado enlazado a sus requisitos y criterios de aceptación.
