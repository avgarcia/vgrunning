# Requisitos funcionales y no funcionales — Fase 1

**Estado:** Validado — Fase 1 cerrada
**Fecha:** 2026-08-10

## Actores y permisos

| Actor | Responsabilidades MVP |
| --- | --- |
| Administrador | Da de alta e invita usuarios; asigna roles; mantiene las definiciones de tags y sus valores permitidos. |
| Entrenador | Gestiona todos los corredores, tags, segmentos, planes semanales, entrenamientos y feedback. El alcance no se restringe por entrenador en el MVP. |
| Corredor | Consulta sus planes y entrenamientos, registra su ejecución y envía feedback sobre ellos. |

El corredor solo accede a sus propios datos. Administradores y entrenadores acceden a todos los datos operativos.

## Requisitos funcionales priorizados (MoSCoW)

### Must have

- Invitación de corredores por el administrador mediante email; activación con email, contraseña y restablecimiento de contraseña.
- Gestión de usuarios, roles, definiciones de tags y sus valores permitidos por el administrador.
- Tags controlados para clasificar corredores y segmentos dinámicos construidos a partir de reglas de tags. No se permite crear tags ni valores libres.
- Modalidad online o presencial representada mediante tags controlados del corredor. El lugar de encuentro se define en el entrenamiento cuando aplique a sesiones presenciales.
- Reglas de segmentos en el MVP limitadas a condiciones sobre tags con operador AND entre criterios, selección de uno o varios valores permitidos dentro de cada tag y exclusión manual de corredores. No se incluyen expresiones avanzadas ni reglas libres.
- Inclusión y exclusión manual de corredores en un segmento.
- Creación de un plan semanal que agrupe varios entrenamientos fechados.
- Asignación de un plan a segmentos y, excepcionalmente, a corredores individuales.
- Publicación atómica del plan semanal: todos sus entrenamientos se hacen visibles a la vez.
- Registro de la versión publicada y de los destinatarios efectivos de cada publicación, para conservar trazabilidad si cambian los tags, segmentos o asignaciones posteriores.
- Catálogo de entrenamientos: rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas y carrera/test.
- Objetivos por frecuencia cardiaca o ritmo relativo al corredor, según el tipo de entrenamiento, y texto libre de aclaraciones.
- Lugar de encuentro en entrenamientos presenciales cuando aplique.
- Estados del plan: borrador y publicado.
- Edición de un plan publicado mediante republicación atómica y email a todos los corredores afectados.
- Consulta responsive de planes, entrenamientos y lugar de encuentro por el corredor.
- Registro por entrenamiento: realizado/no realizado, esfuerzo percibido (1–10), sensación general con valores bien/normal/mal y comentario opcional.
- Historial básico de entrenamientos y feedback del corredor.
- Vista de entrenadores para revisar feedback por corredor, plan semanal y entrenamiento, con visibilidad de estado realizado/no realizado, esfuerzo, sensación y comentario.
- Email al publicar o republicar un plan semanal. El correo debe enviarse a todos los destinatarios efectivos afectados e incluir semana del plan, resumen de entrenamientos y enlace al plan publicado. No se incluyen otros eventos de notificación.

### Should have

- Filtros y búsquedas operativas de corredores, segmentos y planes.
- Registro básico del estado de entrega del email de publicación o republicación.

### Could have

- Duplicación de planes semanales como plantilla operativa.
- Exportación básica de planes o feedback.

### Won't have en MVP

- Chat o mensajería interna.
- Integración con WhatsApp, Strava, Garmin u otras fuentes de actividad.
- Apps móviles nativas.
- Offline-first.
- Reservas, asistencia, aforo, pagos, cancelaciones o asignación de entrenadores para sesiones presenciales.
- Métricas de salud adicionales: dolor/lesiones, fatiga o sueño.

## Requisitos no funcionales

| Área | Requisito acordado |
| --- | --- |
| Canal | Aplicación web responsive, plenamente utilizable desde móvil. |
| Escala | Más de 500 corredores registrados; picos iniciales inferiores a 100 usuarios concurrentes. |
| Disponibilidad | Nivel normal de SaaS; no se ha definido un SLA formal. |
| Conectividad | Sin requisito offline-first. |
| Seguridad | Autenticación por email y contraseña; acceso por rol y aislamiento de los datos del corredor. |
| Notificaciones | Correo electrónico en publicación y republicación de planes. |
| Datos | La aplicación gestiona datos personales y feedback declarado; cumplimiento RGPD, retención y derechos de acceso son precondiciones de salida a producción. |

## Decisiones de diseño con impacto funcional

- Un segmento basado en tags es dinámico. Los tags y sus valores provienen de una taxonomía cerrada administrada por el administrador. La aplicación debe conservar los destinatarios efectivos de cada publicación para que cambios posteriores de tags no alteren retrospectivamente un plan ya publicado.
- La modalidad online o presencial se modela con tags controlados para evitar un segundo sistema paralelo de clasificación.
- Las reglas de segmentos se limitan deliberadamente en el MVP. Si el club necesita lógica compleja, se revisará después de validar que los criterios operativos simples reducen trabajo manual.
- Un cambio relevante en un plan publicado no es una edición silenciosa: republica el plan semanal completo y comunica el cambio por email.
- Recoger feedback sin vista de revisión no aporta valor operativo. Por eso la vista mínima de feedback para entrenadores forma parte del MVP.
- Los entrenadores tienen permisos globales por simplicidad del MVP. Esta decisión no debe confundirse con un modelo de permisos definitivo.

## Supuestos cerrados para Fase 1

- El feedback se asociará al entrenamiento realizado dentro de un plan semanal publicado.
- No se recopilarán datos de salud especiales en el MVP; si se incorporan lesiones o similares, el análisis de privacidad deberá ampliarse.
- La primera versión no necesita reglas avanzadas de segmentación para demostrar mejora operativa frente al envío manual de PDFs.
- El correo electrónico actúa como notificación de disponibilidad del plan, no como canal principal de gestión del entrenamiento.

## Criterios de cierre de Fase 1

- El alcance MVP queda delimitado mediante requisitos Must, Should, Could y Won't.
- Los riesgos principales de Fase 0 quedan tratados: planificación por grupos, feedback revisable, presencial con lugar de encuentro y correo como notificación, no como sistema de gestión.
- Las decisiones que condicionan Fase 2 quedan cerradas: segmentos dinámicos con tags controlados, trazabilidad de publicaciones, republicación atómica, modalidad como tag y feedback mínimo estructurado.
- Las obligaciones de privacidad quedan identificadas como precondición antes de producción, sin bloquear el diseño funcional de Fase 2.
