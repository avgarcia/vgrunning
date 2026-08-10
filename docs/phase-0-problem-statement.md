# Problem Statement — Gestión de entrenos del club de running

**Estado:** Validado — Fase 0 cerrada  
**Fecha:** 2026-08-10

## Contexto

El club gestiona más de 500 alumnos en dos modalidades: entrenamiento online y entrenamiento presencial en el Retiro, dirigido por uno de los entrenadores. Hoy los planes se distribuyen como PDF por correo electrónico; la comunicación se realiza por correo o WhatsApp.

El modelo actual crea entrenamientos individualmente para cada alumno. Esto convierte la planificación y distribución en trabajo repetitivo, difícil de escalar y propenso a errores.

## Problema

El club no dispone de una plataforma centralizada para planificar, distribuir, comunicar y recoger el seguimiento básico de los entrenamientos. La operación depende de PDFs, correo y WhatsApp, herramientas que no representan grupos, asignaciones ni el estado de cada entrenamiento.

Como consecuencia:

- Crear y enviar planes individuales a más de 500 alumnos es un cuello de botella operativo.
- Entrenadores y corredores no trabajan sobre una única fuente de verdad.
- La modalidad presencial necesita comunicar, además del entrenamiento, el lugar de encuentro.
- El club no tiene un mecanismo estructurado para recoger y revisar el feedback de los corredores.

## Resultado de producto buscado

Una aplicación que permita a los entrenadores crear y asignar entrenamientos por grupos, aplicar personalizaciones individuales solo cuando sea necesario, y comunicar a cada corredor su plan —incluido el punto de encuentro de las sesiones presenciales—. Los corredores deben poder consultar sus entrenos y enviar feedback estructurado sobre ellos.

## Decisiones tomadas

| Área | Decisión |
| --- | --- |
| Escala inicial | Más de 500 alumnos |
| Modalidades | Online y presencial en el Retiro |
| Planificación objetivo | Por grupos, con personalización individual excepcional |
| Distribución actual | PDF enviado por correo electrónico |
| Comunicación actual | Correo electrónico y WhatsApp |
| Seguimiento inicial | Feedback declarado por el corredor |
| Presencial | Mismo flujo de entrenamiento que online, añadiendo lugar de encuentro |

## Límites conocidos en este punto

- No se ha definido aún si el producto es exclusivamente interno o comercializable a otros clubes.
- No se ha definido el detalle del feedback, sus escalas, ni cómo se convierte en decisiones para el entrenador.
- No se ha decidido integración con Strava, Garmin u otras fuentes de actividad.
- No se han definido reservas, aforo, asistencia, pagos ni cancelaciones para sesiones presenciales; por ahora no forman parte del problema declarado.

## Riesgos y observaciones

- **Riesgo alto:** si cada personalización individual acaba siendo obligatoria, los grupos serán cosméticos y no habrá mejora real de productividad.
- **Riesgo medio:** definir “analizar feedback” de forma vaga puede producir una bandeja de comentarios inútil; será necesario concretar qué información se pide y qué decisiones habilita.
- **Riesgo medio:** con 500 alumnos, mantener correo y WhatsApp como canales operativos principales perpetuaría la fragmentación. Pueden coexistir como notificaciones, no como sistema de gestión.

## Supuestos a validar

- Los grupos podrán definirse con criterios operativos útiles (nivel, objetivo, disponibilidad u otros), no solo como listas manuales.
- El entrenador podrá revisar el feedback de sus corredores sin intervención administrativa adicional.
- El Retiro es, al menos inicialmente, el único lugar relevante para sesiones presenciales.
