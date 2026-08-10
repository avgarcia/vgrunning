# Requisitos funcionales y no funcionales — Fase 1

**Estado:** Validado — Fase 1 cerrada
**Fecha:** 2026-08-10

## Actores y permisos

| Actor | Responsabilidades PMV |
| --- | --- |
| Administrador | Da de alta e invita usuarios; asigna roles; mantiene las definiciones de etiquetas y sus valores permitidos. |
| Entrenador | Gestiona todos los corredores, etiquetas, segmentos, planes semanales, entrenamientos e información de seguimiento. El alcance no se restringe por entrenador en el PMV. |
| Corredor | Consulta sus planes y entrenamientos, registra su ejecución y envía información de seguimiento sobre ellos. |

El corredor solo accede a sus propios datos. Administradores y entrenadores acceden a todos los datos operativos.

## Requisitos funcionales priorizados (MoSCoW)

### Imprescindible

- **RF-01.** Invitación de corredores por el administrador mediante correo electrónico; activación con correo electrónico, contraseña y restablecimiento de contraseña.
- **RF-02.** Gestión de usuarios, roles, definiciones de etiquetas y sus valores permitidos por el administrador.
- **RF-03.** Etiquetas controladas para clasificar corredores y segmentos dinámicos construidos a partir de reglas de etiquetas. No se permite crear etiquetas ni valores libres.
- **RF-04.** Modalidad en línea o presencial representada mediante etiquetas controladas del corredor. El lugar de encuentro se define como texto libre en el entrenamiento cuando aplique a sesiones presenciales; no se limita al Retiro.
- **RF-05.** Reglas de segmentos en el PMV limitadas a condiciones sobre etiquetas con operador Y entre criterios, selección de uno o varios valores permitidos dentro de cada etiqueta y exclusión manual de corredores. No se incluyen expresiones avanzadas ni reglas libres.
- **RF-06.** Inclusión y exclusión manual de corredores en un segmento.
- **RF-07.** Creación de un plan semanal que agrupe varios entrenamientos fechados.
- **RF-08.** Asignación de un plan a segmentos y, excepcionalmente, a corredores individuales.
- **RF-09.** Publicación atómica del plan semanal: todos sus entrenamientos se hacen visibles a la vez.
- **RF-10.** Registro de la versión publicada y de los destinatarios efectivos de cada publicación, para conservar trazabilidad si cambian las etiquetas, segmentos o asignaciones posteriores.
- **RF-11.** Catálogo de entrenamientos: rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas y carrera/test.
- **RF-12.** Objetivos por frecuencia cardiaca o ritmo relativo al corredor, según el tipo de entrenamiento, y texto libre de aclaraciones.
- **RF-13.** Lugar de encuentro en entrenamientos presenciales cuando aplique.
- **RF-14.** Estados del plan: borrador y publicado.
- **RF-15.** Edición de un plan publicado mediante republicación atómica y correo electrónico a todos los corredores afectados.
- **RF-16.** Consulta adaptable a dispositivos móviles de planes, entrenamientos y lugar de encuentro por el corredor.
- **RF-17.** Registro por entrenamiento: realizado/no realizado, esfuerzo percibido (1–10), sensación general con valores bien/normal/mal y comentario opcional.
- **RF-18.** Historial básico de entrenamientos e información de seguimiento del corredor.
- **RF-19.** Vista de entrenadores para revisar información de seguimiento por corredor, plan semanal y entrenamiento, con visibilidad de estado realizado/no realizado, esfuerzo, sensación y comentario.
- **RF-20.** Correo electrónico al publicar o republicar un plan semanal. El correo debe enviarse a todos los destinatarios efectivos afectados e incluir semana del plan, resumen de entrenamientos y enlace al plan publicado. No se incluyen otros eventos de notificación.

### Deseable

- Filtros y búsquedas operativas de corredores, segmentos y planes.
- Registro básico del estado de entrega del correo electrónico de publicación o republicación.

### Opcional

- Duplicación de planes semanales como plantilla operativa.
- Exportación básica de planes o información de seguimiento.

### Fuera del PMV

- Chat o mensajería interna.
- Integración con WhatsApp, Strava, Garmin u otras fuentes de actividad.
- Aplicaciones móviles nativas.
- Uso sin conexión como prioridad.
- Reservas, asistencia, aforo, pagos, cancelaciones o asignación de entrenadores para sesiones presenciales.
- Métricas de salud adicionales: dolor/lesiones, fatiga o sueño.
- Comercialización del producto a otros clubes y soporte multiclub.

## Matriz de trazabilidad de Fase 1 a Fase 2

Esta matriz es el contrato de entrada para Fase 2, no una declaración de diseño ya realizado. Cada fila debe enlazarse desde el diseño de Fase 2 que la materialice y completar sus criterios de aceptación antes de implementación.

| Requisito de Fase 1 | Elemento de diseño requerido en Fase 2 | Estado |
| --- | --- | --- |
| RF-01 | Flujo de invitación, activación, inicio y restablecimiento de contraseña | Pendiente de diseño en Fase 2 |
| RF-02 | Modelo de usuarios, roles y administración de taxonomías | Pendiente de diseño en Fase 2 |
| RF-03 | Modelo de etiquetas y evaluación de segmentos dinámicos | Pendiente de diseño en Fase 2 |
| RF-04 | Modelo de modalidad y ubicación de entrenamiento | Pendiente de diseño en Fase 2 |
| RF-05 | Semántica y límites de reglas de segmentación | Pendiente de diseño en Fase 2 |
| RF-06 | Interacción y persistencia de inclusiones y exclusiones manuales | Pendiente de diseño en Fase 2 |
| RF-07 | Modelo y ciclo de vida del plan semanal | Pendiente de diseño en Fase 2 |
| RF-08 | Modelo de asignación a segmentos y corredores | Pendiente de diseño en Fase 2 |
| RF-09 | Flujo y consistencia de publicación atómica | Pendiente de diseño en Fase 2 |
| RF-10 | Versionado y registro de destinatarios efectivos | Pendiente de diseño en Fase 2 |
| RF-11 | Modelo de catálogo y tipos de entrenamiento | Pendiente de diseño en Fase 2 |
| RF-12 | Modelo de objetivos por frecuencia cardiaca, ritmo y aclaraciones | Pendiente de diseño en Fase 2 |
| RF-13 | Captura y consulta del lugar de encuentro presencial | Pendiente de diseño en Fase 2 |
| RF-14 | Máquina de estados de borrador y publicado | Pendiente de diseño en Fase 2 |
| RF-15 | Flujo de republicación y destinatarios afectados | Pendiente de diseño en Fase 2 |
| RF-16 | Experiencia móvil de consulta para corredores | Pendiente de diseño en Fase 2 |
| RF-17 | Modelo y captura de información de seguimiento | Pendiente de diseño en Fase 2 |
| RF-18 | Modelo de historial de entrenamientos y seguimiento | Pendiente de diseño en Fase 2 |
| RF-19 | Consulta y permisos de revisión para entrenadores | Pendiente de diseño en Fase 2 |
| RF-20 | Contrato de contenido y entrega de correo electrónico | Pendiente de diseño en Fase 2 |

El revisor de arquitectura debe comprobar, al cerrar Fase 2, que cada requisito `RF-01` a `RF-20` enlaza con su diseño, criterios de aceptación y decisión técnica correspondiente.

## Requisitos no funcionales

| Área | Requisito acordado |
| --- | --- |
| Canal | Aplicación web adaptable, plenamente utilizable desde móvil. |
| Escala | Más de 500 corredores registrados; picos iniciales inferiores a 100 usuarios concurrentes. |
| Disponibilidad | Nivel normal de SaaS; no se ha definido un SLA formal. |
| Conectividad | Sin requisito de uso sin conexión como prioridad. |
| Seguridad | Autenticación por correo electrónico y contraseña; acceso por rol y aislamiento de los datos del corredor. |
| Notificaciones | Correo electrónico en publicación y republicación de planes. |
| Datos | La aplicación gestiona datos personales e información de seguimiento declarada; cumplimiento RGPD, retención y derechos de acceso son precondiciones de salida a producción. |

## Decisiones de diseño con impacto funcional

- Un segmento basado en etiquetas es dinámico. Las etiquetas y sus valores provienen de una taxonomía cerrada administrada por el administrador. La aplicación debe conservar los destinatarios efectivos de cada publicación para que cambios posteriores de etiquetas no alteren retrospectivamente un plan ya publicado.
- La modalidad en línea o presencial se modela con etiquetas controladas para evitar un segundo sistema paralelo de clasificación.
- El PMV se limita a la operación interna de un único club. La comercialización a otros clubes y cualquier requisito de multiclub quedan fuera de alcance; no se diseñará aislamiento organizativo en esta fase.
- El Retiro es el contexto operativo inicial, no una restricción de producto. Cada entrenamiento presencial puede indicar cualquier lugar de encuentro mediante texto libre.
- Las reglas de segmentos se limitan deliberadamente en el PMV. Si el club necesita lógica compleja, se revisará después de validar que los criterios operativos simples reducen trabajo manual.
- Un cambio relevante en un plan publicado no es una edición silenciosa: republica el plan semanal completo y comunica el cambio por correo electrónico.
- Recoger información de seguimiento sin vista de revisión no aporta valor operativo. Por eso la vista mínima de seguimiento para entrenadores forma parte del PMV.
- Los entrenadores tienen permisos globales por simplicidad del PMV. Esta decisión no debe confundirse con un modelo de permisos definitivo.

## Supuestos cerrados para Fase 1

- La información de seguimiento se asociará al entrenamiento realizado dentro de un plan semanal publicado.
- No se recopilarán datos de salud especiales en el PMV; si se incorporan lesiones o similares, el análisis de privacidad deberá ampliarse.
- La primera versión no necesita reglas avanzadas de segmentación para demostrar mejora operativa frente al envío manual de archivos PDF.
- El correo electrónico actúa como notificación de disponibilidad del plan, no como canal principal de gestión del entrenamiento.

## Criterios de cierre de Fase 1

- El alcance del PMV queda delimitado mediante requisitos imprescindibles, deseables, opcionales y fuera de alcance.
- Los riesgos principales de Fase 0 quedan tratados: planificación por grupos, información de seguimiento revisable, presencial con lugar de encuentro y correo como notificación, no como sistema de gestión.
- Las decisiones que condicionan Fase 2 quedan cerradas: segmentos dinámicos con etiquetas controladas, trazabilidad de publicaciones, republicación atómica, modalidad como etiqueta, información de seguimiento mínima estructurada, operación de un único club y lugar de encuentro no restringido al Retiro.
- Los requisitos imprescindibles `RF-01` a `RF-20` tienen un elemento de diseño identificado y declarado pendiente en la matriz de trazabilidad hacia Fase 2.
- Las obligaciones de privacidad quedan identificadas como precondición antes de producción, sin bloquear el diseño funcional de Fase 2.
