# Criterios de aceptación — Fase 1

**Estado:** Validado — Fase 1 cerrada
**Fecha:** 2026-08-10
**Última actualización:** 2026-08-24 — criterios revisados tras las decisiones H-01 a H-20 de la auditoría documental

## Propósito

Definir criterios mínimos y observables de éxito y error para los requisitos imprescindibles de [Fase 1](phase-1-requirements.md). Estos criterios no son una batería de pruebas exhaustiva ni sustituyen el diseño técnico de Fase 2.

Cada escenario tiene un identificador estable para que contratos, pruebas y funcionalidades de Linear puedan referenciarlo sin depender de su posición o redacción literal. El identificador no convierte el criterio en una batería exhaustiva ni impide descomponerlo posteriormente en varios casos de prueba.

## Criterios por requisito

### RF-01 — Invitación y acceso

- **ID:** `CA-RF01-01` — **Éxito:** dado un administrador, un correo válido y su declaración de que la persona tiene al menos `18` años, la transacción crea cuenta, desafío y solicitud de correo. El worker obtiene aceptación del proveedor sin que ello prometa entrega física o lectura; una prueba de extremo a extremo con buzón controlado verifica la recepción del enlace. Durante la activación inicial, la persona confirma la mayoría de edad, define contraseña y puede iniciar sesión. Ambas declaraciones conservan actor, origen, instante y versión del texto.
- **ID:** `CA-RF01-02` — **Error o límite:** un correo inválido, la ausencia de cualquiera de las dos declaraciones o un enlace no válido, reemplazado o caducado impiden crear o activar la cuenta correspondiente. Una solicitud de restablecimiento para un correo no registrado no revela si existe una cuenta. No se almacena fecha de nacimiento, documento ni copia acreditativa.

### RF-02 — Cuentas, rol inicial y taxonomías

- **ID:** `CA-RF02-01` — **Éxito:** dado un administrador, al crear una cuenta asigna exactamente un rol inicial entre los permitidos; también puede crear o modificar una definición de etiqueta o uno de sus valores permitidos, y el cambio válido queda disponible para la gestión operativa.
- **ID:** `CA-RF02-02` — **Error o límite:** ningún actor puede modificar el rol después de crear la cuenta; entrenador y corredor tampoco pueden administrar cuentas, definiciones ni valores. La aplicación rechaza roles, nombres o valores vacíos o fuera de catálogo sin conservar un cambio parcial.

### RF-03 — Etiquetas y segmentos dinámicos

- **ID:** `CA-RF03-01` — **Éxito:** dado un corredor con valores permitidos de etiquetas y un segmento basado en esos valores, el corredor aparece en el segmento cuando cumple la regla y deja de aparecer cuando deja de cumplirla.
- **ID:** `CA-RF03-02` — **Error o límite:** al intentar crear una regla con una etiqueta o valor que no pertenece a la taxonomía administrada, la aplicación la rechaza y no guarda el segmento.

### RF-04 — Modalidad y ubicación sin restricción geográfica

- **ID:** `CA-RF04-01` — **Éxito:** dado un corredor de cualquier modalidad y un entrenamiento presencial, el entrenador puede indicar opcionalmente un lugar de encuentro como texto libre, incluido uno distinto de El Retiro, y el corredor lo consulta. Corredores de ambas modalidades pueden pertenecer al mismo grupo y recibir el mismo plan sin advertencia ni confirmación adicional.
- **ID:** `CA-RF04-02` — **Error o límite:** la modalidad del corredor solo admite valores de la etiqueta controlada y la del entrenamiento su catálogo propio; un valor libre se rechaza. Una diferencia entre ambas no bloquea, excluye ni modifica la publicación. La ausencia de lugar en un entrenamiento presencial tampoco bloquea guardado o publicación.

### RF-05 — Reglas de segmentación del PMV

- **ID:** `CA-RF05-01` — **Éxito:** dado un segmento con dos criterios de etiquetas, el sistema incluye solo corredores que cumplen ambos criterios; dentro de una misma etiqueta permite seleccionar uno o varios valores permitidos.
- **ID:** `CA-RF05-02` — **Error o límite:** al intentar usar el operador O, una expresión libre o un criterio fuera de las etiquetas controladas, la aplicación impide guardar la regla.

### RF-06 — Inclusiones y exclusiones manuales

- **ID:** `CA-RF06-01` — **Éxito:** dado un segmento dinámico, el entrenador puede incluir a un corredor que no cumple la regla o excluir a uno que sí la cumple; el miembro efectivo del segmento refleja ambas excepciones.
- **ID:** `CA-RF06-02` — **Error o límite:** al intentar incluir o excluir un corredor inexistente, la operación se rechaza y no altera las excepciones ya guardadas.

### RF-07 — Plan semanal

- **ID:** `CA-RF07-01` — **Éxito:** dado un entrenador, al crear un plan para una semana y añadir varios entrenamientos fechados, el plan conserva todos los entrenamientos asociados a esa semana.
- **ID:** `CA-RF07-02` — **Error o límite:** no se puede guardar un entrenamiento sin fecha ni un plan sin semana identificable; la validación informa del dato faltante.

### RF-08 — Grupos de planificación y planes

- **ID:** `CA-RF08-01` — **Éxito:** dado un grupo de planificación válido que combina segmentos e inclusiones o exclusiones individuales persistentes, el entrenador crea para él un plan semanal y los destinatarios candidatos de la primera publicación son los miembros efectivos del grupo; todos reciben el mismo contenido.
- **ID:** `CA-RF08-02` — **Error o límite:** la aplicación rechaza segmentos o corredores inexistentes, un estado que sitúe a un corredor en dos grupos efectivos y un segundo plan para la misma pareja grupo-semana; ninguna de esas operaciones altera la configuración válida anterior. La mezcla de modalidades nunca es causa de rechazo y una excepción individual no permite personalizar contenido.

### RF-09 — Publicación atómica

- **ID:** `CA-RF09-01` — **Éxito:** dado un plan en borrador válido con varios entrenamientos, al publicarlo todos se hacen visibles para los destinatarios en la misma publicación.
- **ID:** `CA-RF09-02` — **Error o límite:** si alguno de los elementos necesarios para publicar no es válido, la publicación falla completa y ningún entrenamiento del plan pasa a estar visible.

### RF-10 — Versión y destinatarios efectivos

- **ID:** `CA-RF10-01` — **Éxito:** al publicar un plan, la aplicación guarda una versión identificable y la lista de destinatarios efectivos; cambios posteriores en etiquetas, segmentos o asignaciones no modifican ese registro.
- **ID:** `CA-RF10-02` — **Error o límite:** si no puede guardarse la versión o los destinatarios efectivos, la publicación no se completa y el plan mantiene su estado anterior.

### RF-11 — Catálogo de entrenamientos

- **ID:** `CA-RF11-01` — **Éxito:** al crear un entrenamiento, el entrenador puede seleccionar rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas o carrera/test, y el tipo elegido se conserva al consultar el plan.
- **ID:** `CA-RF11-02` — **Error o límite:** un tipo fuera de ese catálogo no se acepta ni se muestra como tipo válido de entrenamiento.

### RF-12 — Objetivos del entrenamiento

- **ID:** `CA-RF12-01` — **Éxito:** dado un tipo compatible, el entrenador define una zona `Z1..Z5` o una distancia de referencia con su intervalo de desviación y puede añadir una aclaración. El corredor ve, respectivamente, «Zx según las zonas que utilizas con tu entrenador» o la fórmula «ritmo de distancia +/− segundos por km, usando tu marca de referencia acordada con tu entrenador».
- **ID:** `CA-RF12-02` — **Error o límite:** un objetivo con formato o escala inválidos se rechaza. La aplicación no solicita, almacena, deriva ni valida frecuencia cardiaca máxima, zonas, marcas o ritmos personales; desconocer la referencia externa no bloquea la publicación y se resuelve fuera del producto con el entrenador.

### RF-13 — Lugar de encuentro presencial

- **ID:** `CA-RF13-01` — **Éxito:** dado un entrenamiento presencial, el entrenador puede guardar opcionalmente un lugar de encuentro y el corredor destinatario lo ve junto con el entrenamiento.
- **ID:** `CA-RF13-02` — **Error o límite:** si no se indica lugar, guardado y publicación continúan y la aplicación no inventa ni reutiliza otro; la ausencia queda diferenciada. Un entrenamiento `en-linea` rechaza una ubicación porque ese campo no aplica, no porque la ubicación sea obligatoria en presencial.

### RF-14 — Estados del plan

- **ID:** `CA-RF14-01` — **Éxito:** un plan nuevo se guarda como borrador y, tras una publicación válida, pasa a publicado; los corredores solo ven el contenido publicado que les corresponde.
- **ID:** `CA-RF14-02` — **Error o límite:** un corredor no puede acceder a un borrador ni forzar el cambio de estado mediante una URL o acción no autorizada.

### RF-15 — Republicación de cambios relevantes

- **ID:** `CA-RF15-01` — **Éxito:** dado un administrador o entrenador, un plan publicado y una o varias diferencias canónicas visibles para el corredor en días futuros —alta o retirada, modalidad, estructura, carga, objetivo, recuperación, aclaración o ubicación—, al confirmarlas se genera una única publicación atómica y una solicitud por destinatario efectivo congelado; la entrega aplica `RF-20`.
- **ID:** `CA-RF15-02` — **Error o límite:** si la propuesta modifica hoy o un día anterior, solo cambia metadatos técnicos o de auditoría, o es canónicamente idéntica, toda la operación se rechaza y permanecen la versión y solicitudes anteriores. Ningún cambio visible futuro puede hacerse efectivo de forma silenciosa.

### RF-16 — Consulta móvil del corredor

- **ID:** `CA-RF16-01` — **Éxito:** un corredor autenticado consulta todo el contenido esencial del plan conforme a WCAG `2.2 AA`: reflow sin desplazamiento bidimensional a `320 CSS px` salvo excepciones esenciales, zoom del navegador al `400 %`, texto al `200 %`, teclado, foco visible, etiquetas y errores asociados, y objetivos de puntero de al menos `24 × 24 CSS px` o una excepción normativa equivalente.
- **ID:** `CA-RF16-02` — **Error o límite:** un corredor no puede consultar un recurso ajeno; ninguna pérdida de contenido, acción o estado esencial aparece al aplicar los tamaños, zoom o mecanismos de entrada anteriores. La validación combina comprobaciones automáticas y revisión manual.

### RF-17 — Registro de ejecución y seguimiento

- **ID:** `CA-RF17-01` — **Éxito:** dado un entrenamiento publicado para el corredor dentro de la ventana, este registra `realizado` respondiendo «¿Cuánto esfuerzo te supuso este entrenamiento?» con una opción sin preselección entre `1 Muy suave`, `2 Suave`, `3 Moderado`, `4 Intenso` y `5 Muy intenso`, además de sensación `bien`, `normal` o `mal`; o registra `no-realizado` sin esfuerzo ni sensación. Solo se persiste el entero y puede añadir comentario con consentimiento vigente.
- **ID:** `CA-RF17-02` — **Error o límite:** un esfuerzo o sensación inválidos, `realizado` sin ambos campos, `no-realizado` con alguno de ellos o un comentario sin consentimiento vigente se rechazan por completo y no sustituyen un registro válido existente.

### RF-18 — Historial del corredor

- **ID:** `CA-RF18-01` — **Éxito:** un corredor puede consultar su historial básico de entrenamientos y la información de seguimiento asociada a cada uno.
- **ID:** `CA-RF18-02` — **Error o límite:** el historial de un corredor no expone entrenamientos ni información de seguimiento de otros corredores.

### RF-19 — Revisión del entrenador

- **ID:** `CA-RF19-01` — **Éxito:** un entrenador puede consultar solo corredores `active` y, por corredor, plan semanal y entrenamiento, el estado realizado/no realizado, esfuerzo con su etiqueta canónica, sensación y comentario autorizado. Al reactivar, reaparecen únicamente datos aún retenidos y los conteos se recalculan.
- **ID:** `CA-RF19-02` — **Error o límite:** un corredor no accede a revisiones ajenas y el entrenador no localiza pendientes, inactivos o cancelados. La consulta no permite modificar, responder, priorizar, anotar, asignar, marcar como revisado ni demostrar que una persona leyó cada elemento.

### RF-20 — Correo de publicación

- **ID:** `CA-RF20-01` — **Éxito:** al confirmar una publicación o republicación se crea una solicitud por destinatario efectivo sin fijar todavía su correo. Al comenzar el procesamiento, una consulta obtiene conjuntamente `active` y el correo vigente verificado; ese correo se cifra y congela para todos los intentos y reconciliaciones de la solicitud. Un cambio posterior solo afecta solicitudes futuras. El proveedor recibe semana, resumen y enlace, y su aceptación se registra sin prometer entrega física ni lectura.
- **ID:** `CA-RF20-02` — **Error o límite:** un no activo termina `omitido-inactivo`. Si la consulta de elegibilidad no se resuelve antes de `createdAt + 120 minutos`, termina `fallo-definitivo/elegibilidad-no-resuelta`, alerta y libera versiones posteriores; no se reabre, recrea ni envía manualmente. Una llamada al proveedor ya iniciada conserva su ventana de reconciliación aunque cruce ese límite. Una baja posterior al inicio no garantiza cancelar el correo y otra acción distinta de publicar o republicar no crea esta notificación.

### RF-21 — Cobertura semanal de corredores activos

- **ID:** `CA-RF21-01` — **Éxito:** para la semana actual por defecto o una seleccionada, administrador o entrenador ve conteos y una lista paginada de todos los corredores `active`. Cada corredor tiene exactamente un estado: `sin-grupo` si no pertenece a grupo activo; `grupo-sin-plan` si su grupo no tiene plan; `plan-en-borrador` si el plan existe sin publicación; `cubierto` si pertenece a los destinatarios de la publicación vigente; o `fuera-de-publicacion` si el plan está publicado pero no forma parte de sus destinatarios congelados. `sin-modalidad` aparece como indicador informativo independiente.
- **ID:** `CA-RF21-02` — **Error o límite:** pendientes, inactivos y cancelados quedan fuera; una combinación incoherente no se resuelve mediante prioridad arbitraria ni modifica grupos, planes o publicaciones. La consulta se calcula desde las fuentes vigentes, no persiste una proyección y ninguno de sus estados bloquea operación.

## Uso en Fase 2

Fase 2 enlaza cada decisión de diseño y cada validación prevista con los criterios de `RF-01` a `RF-21`. La implementación deberá conservar esa trazabilidad; cualquier contradicción descubierta en OpenAPI, migraciones o pruebas requiere corregir el diseño o registrar una nueva decisión, no resolverla implícitamente durante la codificación.
