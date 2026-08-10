# Requisitos funcionales y no funcionales — Fase 1

**Estado:** Borrador para validación de Fase 1  
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
- Inclusión y exclusión manual de corredores en un segmento.
- Creación de un plan semanal que agrupe varios entrenamientos fechados.
- Asignación de un plan a segmentos y, excepcionalmente, a corredores individuales.
- Publicación atómica del plan semanal: todos sus entrenamientos se hacen visibles a la vez.
- Catálogo de entrenamientos: rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas y carrera/test.
- Objetivos por frecuencia cardiaca o ritmo relativo al corredor, según el tipo de entrenamiento, y texto libre de aclaraciones.
- Lugar de encuentro en entrenamientos presenciales cuando aplique.
- Estados del plan: borrador y publicado.
- Edición de un plan publicado mediante republicación atómica y email a todos los corredores afectados.
- Consulta responsive de planes, entrenamientos y lugar de encuentro por el corredor.
- Registro por entrenamiento: realizado/no realizado, esfuerzo percibido (1–10), sensación general y comentario opcional.
- Historial básico de entrenamientos y feedback del corredor.
- Email al publicar o republicar un plan semanal; no se incluyen otros eventos de notificación.

### Should have

- Vista de entrenadores para revisar el feedback de los corredores.
- Filtros y búsquedas operativas de corredores, segmentos y planes.
- Registro de la versión o destinatarios efectivos de cada publicación, para conservar trazabilidad si cambian los tags.

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
| Datos | La aplicación gestiona datos personales y feedback declarado; se requerirá definir cumplimiento RGPD, retención y derechos de acceso antes de producción. |

## Decisiones de diseño con impacto funcional

- Un segmento basado en tags es dinámico. Los tags y sus valores provienen de una taxonomía cerrada administrada por el administrador. La aplicación debe conservar los destinatarios efectivos de cada publicación para que cambios posteriores de tags no alteren retrospectivamente un plan ya publicado.
- Un cambio relevante en un plan publicado no es una edición silenciosa: republica el plan semanal completo y comunica el cambio por email.
- Los entrenadores tienen permisos globales por simplicidad del MVP. Esta decisión no debe confundirse con un modelo de permisos definitivo.

## Supuestos pendientes de validar

- El feedback se asociará al entrenamiento realizado dentro de un plan semanal publicado.
- No se recopilarán datos de salud especiales en el MVP; si se incorporan lesiones o similares, el análisis de privacidad deberá ampliarse.
