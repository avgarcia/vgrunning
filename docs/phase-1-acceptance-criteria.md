# Criterios de aceptación — Fase 1

**Estado:** Validado — Fase 1 cerrada
**Fecha:** 2026-08-10
**Última actualización:** 2026-08-21 — refinamiento de `RF-15` y `RF-20` conforme a `ADR-0021`

## Propósito

Definir criterios mínimos y observables de éxito y error para los requisitos imprescindibles de [Fase 1](phase-1-requirements.md). Estos criterios no son una batería de pruebas exhaustiva ni sustituyen el diseño técnico de Fase 2.

## Criterios por requisito

### RF-01 — Invitación y acceso

- **Éxito:** dado un administrador y un correo de corredor válido, al enviar una invitación el corredor recibe el mecanismo de activación, define una contraseña y puede iniciar sesión con ese correo y contraseña.
- **Error o límite:** al intentar invitar un correo con formato inválido o activar con un enlace no válido o caducado, la operación se rechaza y no crea ni activa una cuenta. Una solicitud de restablecimiento para un correo no registrado no revela si existe una cuenta.

### RF-02 — Usuarios, roles y taxonomías

- **Éxito:** dado un administrador, al crear o modificar un usuario, su rol, una etiqueta controlada o uno de sus valores permitidos, el cambio queda disponible para la gestión operativa.
- **Error o límite:** un entrenador o corredor no puede modificar roles, definiciones de etiquetas ni valores permitidos; la aplicación rechaza también nombres o valores vacíos.

### RF-03 — Etiquetas y segmentos dinámicos

- **Éxito:** dado un corredor con valores permitidos de etiquetas y un segmento basado en esos valores, el corredor aparece en el segmento cuando cumple la regla y deja de aparecer cuando deja de cumplirla.
- **Error o límite:** al intentar crear una regla con una etiqueta o valor que no pertenece a la taxonomía administrada, la aplicación la rechaza y no guarda el segmento.

### RF-04 — Modalidad y ubicación sin restricción geográfica

- **Éxito:** dado un corredor con modalidad presencial y un entrenamiento presencial, el entrenador puede indicar un lugar de encuentro como texto libre, incluido uno distinto de El Retiro, y el corredor lo consulta en el entrenamiento.
- **Error o límite:** la modalidad solo puede tomar valores permitidos de la etiqueta controlada; un valor libre o no permitido se rechaza y no modifica la clasificación del corredor.

### RF-05 — Reglas de segmentación del PMV

- **Éxito:** dado un segmento con dos criterios de etiquetas, el sistema incluye solo corredores que cumplen ambos criterios; dentro de una misma etiqueta permite seleccionar uno o varios valores permitidos.
- **Error o límite:** al intentar usar el operador O, una expresión libre o un criterio fuera de las etiquetas controladas, la aplicación impide guardar la regla.

### RF-06 — Inclusiones y exclusiones manuales

- **Éxito:** dado un segmento dinámico, el entrenador puede incluir a un corredor que no cumple la regla o excluir a uno que sí la cumple; el destinatario efectivo del segmento refleja ambas excepciones.
- **Error o límite:** al intentar incluir o excluir un corredor inexistente, la operación se rechaza y no altera las excepciones ya guardadas.

### RF-07 — Plan semanal

- **Éxito:** dado un entrenador, al crear un plan para una semana y añadir varios entrenamientos fechados, el plan conserva todos los entrenamientos asociados a esa semana.
- **Error o límite:** no se puede guardar un entrenamiento sin fecha ni un plan sin semana identificable; la validación informa del dato faltante.

### RF-08 — Asignación de planes

- **Éxito:** dado un plan semanal, el entrenador puede asignarlo a uno o varios segmentos y añadir excepcionalmente corredores individuales; los destinatarios resultantes incluyen ambos tipos de asignación.
- **Error o límite:** una asignación a un segmento o corredor inexistente se rechaza y no cambia los destinatarios ya configurados.

### RF-09 — Publicación atómica

- **Éxito:** dado un plan en borrador válido con varios entrenamientos, al publicarlo todos se hacen visibles para los destinatarios en la misma publicación.
- **Error o límite:** si alguno de los elementos necesarios para publicar no es válido, la publicación falla completa y ningún entrenamiento del plan pasa a estar visible.

### RF-10 — Versión y destinatarios efectivos

- **Éxito:** al publicar un plan, la aplicación guarda una versión identificable y la lista de destinatarios efectivos; cambios posteriores en etiquetas, segmentos o asignaciones no modifican ese registro.
- **Error o límite:** si no puede guardarse la versión o los destinatarios efectivos, la publicación no se completa y el plan mantiene su estado anterior.

### RF-11 — Catálogo de entrenamientos

- **Éxito:** al crear un entrenamiento, el entrenador puede seleccionar rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas o carrera/test, y el tipo elegido se conserva al consultar el plan.
- **Error o límite:** un tipo fuera de ese catálogo no se acepta ni se muestra como tipo válido de entrenamiento.

### RF-12 — Objetivos del entrenamiento

- **Éxito:** dado un tipo de entrenamiento compatible, el entrenador puede definir un objetivo por frecuencia cardiaca o ritmo relativo al corredor y añadir una aclaración en texto libre; el corredor ve esos datos en su entrenamiento.
- **Error o límite:** un valor de objetivo con formato no válido o fuera de la escala definida para el tipo de objetivo se rechaza y no se publica como instrucción del entrenamiento.

### RF-13 — Lugar de encuentro presencial

- **Éxito:** dado un entrenamiento presencial que requiere lugar de encuentro, el entrenador puede guardarlo y el corredor destinatario lo ve junto con el entrenamiento.
- **Error o límite:** si no se ha indicado un lugar de encuentro, la aplicación no muestra un lugar ficticio ni reutiliza el de otro entrenamiento; la ausencia queda diferenciada de un lugar informado.

### RF-14 — Estados del plan

- **Éxito:** un plan nuevo se guarda como borrador y, tras una publicación válida, pasa a publicado; los corredores solo ven el contenido publicado que les corresponde.
- **Error o límite:** un corredor no puede acceder a un borrador ni forzar el cambio de estado mediante una URL o acción no autorizada.

### RF-15 — Republicación de cambios relevantes

- **Éxito:** dado un administrador o entrenador, un plan publicado y cambios relevantes en uno o varios días futuros, al confirmarlos se genera una única publicación atómica y una solicitud de notificación para cada destinatario efectivo congelado; los intentos de entrega aplican la elegibilidad vigente de `RF-20`.
- **Error o límite:** si la propuesta modifica hoy o un día anterior, toda la operación se rechaza, permanece visible la versión publicada anterior y no se crean versión ni solicitudes. Ningún cambio relevante puede hacerse visible de forma silenciosa.

### RF-16 — Consulta móvil del corredor

- **Éxito:** desde un dispositivo móvil, un corredor autenticado puede consultar su plan, sus entrenamientos y el lugar de encuentro cuando exista sin perder información esencial ni requerir una aplicación nativa.
- **Error o límite:** un corredor no puede consultar mediante la interfaz móvil un plan o entrenamiento asignado a otro corredor.

### RF-17 — Registro de ejecución y seguimiento

- **Éxito:** dado un entrenamiento publicado para el corredor, este puede registrar realizado o no realizado, un esfuerzo de 1 a 10, una sensación de bien, normal o mal y un comentario opcional; el registro queda asociado al entrenamiento.
- **Error o límite:** valores de esfuerzo fuera de 1 a 10 o una sensación distinta de los valores permitidos se rechazan y no sustituyen un registro válido existente.

### RF-18 — Historial del corredor

- **Éxito:** un corredor puede consultar su historial básico de entrenamientos y la información de seguimiento asociada a cada uno.
- **Error o límite:** el historial de un corredor no expone entrenamientos ni información de seguimiento de otros corredores.

### RF-19 — Revisión del entrenador

- **Éxito:** un entrenador puede consultar, por corredor, plan semanal y entrenamiento, el estado realizado/no realizado, esfuerzo, sensación y comentario registrados.
- **Error o límite:** un corredor no puede acceder a la vista de revisión de otros corredores ni a los datos operativos reservados a entrenadores.

### RF-20 — Correo de publicación

- **Éxito:** cuando administrador o entrenador confirma la publicación o republicación de un plan semanal, se crea una solicitud por cada destinatario efectivo congelado; si el corredor continúa `active` justo antes del intento, se contacta con el proveedor para enviar un correo con la semana del plan, un resumen de entrenamientos y un enlace al plan publicado. Un corredor reactivado vuelve a ser elegible para solicitudes de versiones futuras.
- **Error o límite:** si el corredor no está `active` antes del intento, no se contacta con el proveedor y la solicitud termina como `omitido-inactivo`, sin reintento ni reapertura al reactivarlo. Una baja posterior al inicio de la llamada no garantiza cancelar el correo en curso. Acciones distintas de publicar o republicar no crean solicitudes de esta notificación.

## Uso en Fase 2

Fase 2 debe enlazar cada decisión de diseño y cada prueba de implementación con los criterios de su requisito `RF-01` a `RF-20`. Cualquier criterio que requiera precisar una escala, formato, política de reintentos o definición de cambio relevante debe convertir esa precisión en una decisión documentada, no resolverla implícitamente durante implementación.
