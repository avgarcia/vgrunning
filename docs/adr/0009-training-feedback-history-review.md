# ADR-0009: Seguimiento por entrenamiento, historial y revisión

**Estado:** Aceptado
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura
**Refinado parcialmente por:** [ADR-0022](0022-five-point-perceived-effort-scale.md)

> **Refinamiento aceptado:** `ADR-0022` reemplaza exclusivamente el intervalo de esfuerzo `1..10` por `1..5`. Las demás decisiones de este ADR continúan vigentes.

## Contexto

`RF-17` exige que el corredor registre para un entrenamiento publicado si lo realizó, su esfuerzo percibido de `1` a `10`, una sensación `bien`, `normal` o `mal` y un comentario opcional. `RF-18` exige historial propio y `RF-19` una vista global para que entrenador y administrador revisen esa información por corredor, plan semanal y entrenamiento.

`ADR-0004` limita la escritura al corredor propietario y concede lectura global a entrenador y administrador. `ADR-0018` precisa que el entrenador solo consulta corredores actualmente `active`, mientras el administrador conserva acceso auditado a inactivos. `ADR-0006` modela cada entrenamiento dentro de un plan semanal y `ADR-0007` conserva versiones publicadas inmutables. Falta decidir la identidad del registro, su relación con las versiones, las reglas de actualización y qué constituye un historial y una revisión mínimos.

La información de seguimiento es dato personal declarado. El comentario libre puede contener información de salud aunque el producto no la solicite. `ADR-0010` define consentimiento explícito y separado, transparencia, retención, bloqueo, acceso, rectificación y supresión; este ADR no puede afirmar que el texto libre carece de datos sensibles.

## Decisión

El modelo distinguirá:

- **Entrenamiento publicado para el corredor:** entrenamiento incluido en una versión cuyo conjunto de destinatarios contiene al corredor.
- **Registro de seguimiento:** respuesta estructurada del corredor sobre un entrenamiento propio.
- **Sin seguimiento:** ausencia derivada de registro; no es equivalente a `no-realizado` ni se persistirá como respuesta del corredor.

Cada registro pertenecerá de forma inmutable a un corredor y a un entrenamiento lógico dentro de un plan. La combinación corredor-entrenamiento será única. El registro conservará además la versión publicada contra la que se creó por primera vez, para poder reproducir el contenido que el corredor tenía disponible al responder. Una actualización válida modificará el mismo registro y no cambiará corredor, entrenamiento ni versión de referencia.

El contenido estructurado usará valores cerrados:

- `realizado`: booleano explícito;
- `esfuerzo`: entero obligatorio entre `1` y `10` cuando `realizado` sea verdadero y ausente cuando sea falso;
- `sensacion`: valor obligatorio `bien`, `normal` o `mal` cuando `realizado` sea verdadero y ausente cuando sea falso;
- `comentario`: texto plano opcional de hasta `1.000` caracteres, conservando saltos de línea y sin formato enriquecido;
- fechas de creación y última actualización.

Antes de validar el comentario se eliminarán sus espacios exteriores, conservando los saltos de línea y espacios interiores. Si supera `1.000` caracteres después de esa normalización, se rechazará la operación completa y nunca se truncará silenciosamente.

Guardar o actualizar será una única operación. Una entrada inválida, una modificación concurrente o una referencia a un entrenamiento no autorizado se rechazará completa y no sustituirá el último registro válido. La implementación usará revisión optimista o mecanismo equivalente para no perder cambios por dos envíos simultáneos.

El corredor podrá crear el registro desde el inicio de la fecha del entrenamiento y nunca antes. La ventana permanecerá abierta durante siete días naturales contando esa fecha como día `1`, hasta el final del sexto día posterior. Dentro de esa misma ventana podrá modificar el registro; después quedará cerrado para nuevas respuestas y ediciones. El cálculo usará una única zona horaria operativa configurada para el club.

Solo el corredor vinculado a la identidad autenticada podrá crear o actualizar su registro, y únicamente si fue destinatario del entrenamiento publicado. El entrenador tendrá lectura global únicamente sobre corredores `active`; el administrador podrá consultar inactivos mediante acceso auditado. Ninguno podrá crear, corregir ni completar seguimiento en nombre del corredor. No habrá asignación a entrenador, respuestas, notas internas, prioridad, SLA, aprobación, prueba de lectura ni estado de revisión en el PMV.

El historial del corredor y la revisión global se construirán desde entrenamientos publicados y registros de seguimiento, no desde borradores. Incluirán todos los entrenamientos que hayan llegado a publicarse para el corredor, aunque no exista seguimiento. La ausencia de registro se mostrará como estado derivado `sin-seguimiento`, distinto de una declaración explícita `no-realizado`.

Antes del primer registro, la versión activa determina el contenido, la fecha y la ventana aplicables. Si una republicación mueve el entrenamiento conservando su identidad lógica, se aplicarán los datos de la nueva versión activa. Al crear el registro quedarán fijadas su versión de referencia, fecha y ventana; ninguna republicación posterior las modificará.

Si una republicación retira el entrenamiento antes de que el corredor registre seguimiento, el entrenamiento permanecerá en el historial como `retirado` y ya no admitirá respuesta. Si ya existía seguimiento, se conservará junto con la versión contra la que se registró y el entrenamiento también se marcará como `retirado`.

Las consultas de entrenador y administrador admitirán como mínimo los ejes exigidos por `RF-19`: corredor, plan semanal y entrenamiento. Incluirán filas `sin-seguimiento` y `retirado`, aplicarán autorización antes de recuperar datos y devolverán los mismos campos estructurados y comentario que ve el corredor, sin información de otros módulos que no sea necesaria para identificar plan y entrenamiento.

El PMV conservará únicamente el último contenido válido del registro, junto con sus fechas de creación y actualización. No mantendrá historial de ediciones. Tampoco habrá notas, respuestas, asignación a entrenador, aprobación ni estado `revisado`.

`ADR-0012` define PostgreSQL, índices justificados por consulta y paginación por cursor para los históricos cronológicos. Deberán soportar más de 500 corredores, consulta del historial propio y revisión global sin convertir proyecciones o cachés en fuente de verdad.

## Alternativas consideradas

### Alternativa A: Comentarios libres sin campos estructurados

Se descarta porque contradice `RF-17` y `D-07`, impide distinguir ausencia de respuesta de no realización y produce una bandeja difícil de revisar.

### Alternativa B: Un registro nuevo en cada edición

Se descarta para el PMV. Mantener todas las revisiones aumenta datos personales, consultas y obligaciones de retención sin un requisito explícito; se conserva solo el último contenido válido y sus fechas.

### Alternativa C: Permitir al entrenador editar el seguimiento

Se descarta porque convertiría una declaración del corredor en un dato corregible por terceros, contradice `ADR-0004` y exigiría auditoría, motivo de corrección y reglas de conflicto.

### Alternativa D: Persistir registros `sin-seguimiento`

Se descarta porque confundiría ausencia con una declaración y obligaría a crear o actualizar registros al publicar, republicar o cambiar el paso del tiempo. `sin seguimiento` será una proyección de la ausencia.

### Alternativa E: Asociar el registro solo a la versión activa

Se descarta porque una republicación podría cambiar retrospectivamente el entrenamiento contra el que respondió el corredor. La primera respuesta fija la versión de referencia, la fecha y la ventana aplicables.

### Alternativa F: Añadir flujo de revisión del entrenador

Se descarta para el PMV porque `RF-19` exige consulta, no asignación, aprobación, respuesta ni cierre. Añadirlo cambiaría alcance, permisos y modelo de datos.

## Consecuencias

- Se distingue de forma verificable `sin seguimiento`, `realizado` y `no realizado`.
- Un único registro por corredor y entrenamiento simplifica consulta y evita respuestas simultáneas divergentes.
- Conservar la versión inicial permite reproducir el contexto, pero requiere mantener el vínculo con instantáneas publicadas.
- La ventana de siete días impide respuestas futuras o cambios indefinidos, pero también bloquea correcciones tardías aunque el entrenador las solicite por otro canal.
- Los entrenamientos retirados permanecen visibles para conservar la historia de lo que llegó a publicarse, sin permitir nuevas respuestas.
- El límite de `1.000` caracteres mantiene el comentario acotado y verificable; los textos mayores se rechazan completos para evitar pérdida silenciosa.
- Entrenadores y administrador pueden revisar globalmente sin alterar declaraciones del corredor.
- No existe flujo operativo para marcar registros como revisados ni responder al corredor; esa limitación es deliberada.
- El comentario libre introduce riesgo real de recibir datos de salud y bloquea producción hasta completar la revisión especializada y la EIPD exigidas por `ADR-0010`.
- La consulta global requerirá índices y paginación, aunque su tecnología concreta no se decide aquí.

## Requisitos relacionados

- `RF-17`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-07`: seguimiento estructurado y vista mínima de revisión.
- `D-08`: lectura global para entrenador y aislamiento entre corredores.

## Validación prevista

- Probar unicidad por corredor y entrenamiento y rechazo de referencias a borradores o publicaciones ajenas.
- Probar los valores permitidos y que una entrada inválida no sustituye el registro válido existente.
- Probar que `sin seguimiento` se deriva de la ausencia y se diferencia de `no-realizado`.
- Probar que `realizado` exige esfuerzo y sensación y que `no-realizado` los rechaza, manteniendo comentario opcional en ambos casos.
- Probar que el comentario está deshabilitado sin el consentimiento explícito de `ADR-0010`, que rechazarlo no impide registrar seguimiento estructurado y que retirarlo impide nuevos comentarios sin afectar al resto del servicio.
- Probar que el comentario acepta exactamente `1.000` caracteres, rechaza `1.001` sin truncar ni sustituir un registro válido y conserva saltos de línea tras eliminar espacios exteriores.
- Probar que no se responde antes de la fecha, que la fecha cuenta como primer día y que creación y edición se cierran al terminar el sexto día posterior en la zona horaria configurada.
- Probar que solo el corredor propietario crea o actualiza y que entrenador y administrador solo leen.
- Probar acceso directo, listas y filtros para impedir exposición entre corredores.
- Probar consultas por corredor, plan semanal y entrenamiento, incluida ausencia de respuesta.
- Probar conservación del vínculo con la versión publicada ante republicaciones.
- Probar conflicto de actualización concurrente sin pérdida silenciosa de cambios.
- Probar que una republicación anterior al primer registro actualiza fecha y ventana y que una posterior no cambia su versión de referencia ni contexto.
- Probar que un entrenamiento retirado permanece en el historial, conserva su seguimiento si existía y no admite nuevas respuestas si no existía.
- Probar que se conserva solo el último contenido válido y las fechas, sin historial de ediciones.
- Verificar que no existen notas, respuestas, asignación ni estados de revisión del entrenador.
- Validar con `ADR-0010` la preparación para privacidad antes de producción.

## Relaciones con decisiones posteriores

- **Refinado por `ADR-0022`:** el esfuerzo percibido usa un entero de `1` a `5` con etiquetas canónicas; las referencias `1..10` anteriores conservan únicamente el contexto histórico de esta decisión.

## Decisiones pendientes

- **Tratado por `ADR-0010` (Aceptado), bloqueante para producción:** el comentario requiere consentimiento explícito y separado, retirada sin pérdida del servicio, retención y ejercicio de derechos. Responsable: responsable del tratamiento con asesoramiento de privacidad. Tratamiento: revisar la base y completar la EIPD antes de datos reales.
- **Resuelto por `ADR-0012`:** PostgreSQL será la persistencia, los recorridos cronológicos usarán cursor estable y los índices se validarán con planes de consulta.
