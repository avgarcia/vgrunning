# ADR-0006: Modelo de plan semanal, entrenamientos y objetivos

**Estado:** Aceptado
**Fecha:** 2026-08-12
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV necesita que administrador y entrenador gestionen planes semanales con entrenamientos fechados, tipos de entrenamiento del catálogo cerrado, objetivos por frecuencia cardiaca o ritmo relativo, aclaraciones y lugar de encuentro cuando aplique. El corredor solo consulta el contenido publicado que le corresponde.

Fase 1 define el plan semanal como agrupación de entrenamientos fechados, con estado borrador o publicado. También fija que el catálogo mínimo incluye rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas y carrera/test. El lugar de encuentro se usa en entrenamientos presenciales y es texto libre; no se restringe a El Retiro.

`ADR-0002` sitúa planificación como módulo de una aplicación única. `ADR-0004` permite a administrador y entrenador gestionar planes y entrenamientos, y restringe al corredor a la lectura de publicaciones propias. `ADR-0005` decide que la modalidad del corredor es una etiqueta controlada, pero no convierte la ubicación del entrenamiento en taxonomía.

Este ADR decide el modelo editable de planificación antes de publicar. `ADR-0007` decidirá la transacción de publicación, versionado, destinatarios efectivos, definición de cambio relevante y relación entre borrador y versiones publicadas.

## Decisión

El modelo distinguirá cinco conceptos:

- **Plan semanal:** contenedor editable de una semana de entrenamiento.
- **Entrenamiento:** sesión fechada perteneciente a un único plan semanal.
- **Tipo de entrenamiento:** valor cerrado del catálogo funcional del PMV.
- **Objetivo de entrenamiento:** instrucción estructurada por frecuencia cardiaca o ritmo relativo al corredor.
- **Lugar de encuentro:** texto libre opcional asociado al entrenamiento cuando sea presencial.

Un plan semanal tendrá un identificador estable, un nombre obligatorio, una semana identificada de forma explícita, estado `borrador` o `publicado`, y metadatos de auditoría mínimos. La semana se representará mediante una fecha de inicio de semana en calendario local del club. El PMV no almacenará una semana como texto libre.

El PMV permitirá crear varios planes para una misma semana. El nombre distinguirá operativamente esos planes y será único dentro de la semana después de eliminar espacios exteriores y comparar sin distinguir mayúsculas de minúsculas. Se rechazará un segundo plan con el mismo nombre normalizado en la misma semana, pero el nombre podrá reutilizarse en semanas diferentes.

Un plan en `borrador` podrá modificarse por administrador o entrenador. Un plan en `publicado` conservará ese estado tras la primera publicación válida. La forma exacta de modificar contenido ya publicado, crear una nueva versión y calcular destinatarios afectados pertenece a `ADR-0007`; este ADR no permite ediciones silenciosas sobre contenido visible para corredores.

Un plan semanal válido tendrá al menos un entrenamiento para poder publicarse. Podrá existir temporalmente como borrador sin entrenamientos para permitir creación incremental, pero no será publicable en ese estado.

Cada entrenamiento pertenecerá a un único plan semanal y tendrá:

- fecha obligatoria dentro de la semana del plan;
- tipo obligatorio del catálogo cerrado;
- objetivo principal estructurado obligatorio antes de publicar;
- aclaración opcional de texto libre;
- lugar de encuentro opcional;
- orden estable dentro del plan cuando dos entrenamientos compartan fecha.

No se permitirá guardar un entrenamiento sin fecha ni con fecha fuera de la semana del plan. No se permitirá publicar un entrenamiento sin tipo u objetivo principal. La aclaración de texto libre complementa el objetivo, pero no sustituye el tipo ni convierte el entrenamiento en una instrucción libre sin estructura.

El catálogo de tipos será cerrado y estará identificado mediante claves estables del sistema:

| Clave | Nombre visible inicial |
| --- | --- |
| `rodaje` | Rodaje |
| `tirada-larga` | Tirada larga |
| `series` | Series |
| `cambios-ritmo-fartlek` | Cambios de ritmo/fartlek |
| `cuestas` | Cuestas |
| `carrera-test` | Carrera/test |

Los nombres visibles podrán adaptarse sin cambiar las claves ni el significado. El PMV no permitirá crear tipos de entrenamiento libres ni eliminar tipos del catálogo mínimo. Añadir tipos nuevos requerirá una decisión posterior porque cambia validaciones, consulta del corredor y pruebas.

El objetivo principal usará una de estas familias:

- `frecuencia-cardiaca`: objetivo expresado mediante zonas relativas del corredor.
- `ritmo-relativo`: objetivo expresado mediante referencias relativas al tiempo actual del corredor en una distancia de referencia, como 5 km, 10 km u otra distancia definida por el diseño detallado.

Todos los tipos del catálogo permitirán cualquiera de las dos familias de objetivo. El objetivo no almacenará una instrucción arbitraria como único dato estructurado. La aclaración de texto libre será opcional y nunca sustituirá al objetivo principal estructurado.

El lugar de encuentro será texto libre del entrenamiento, no una taxonomía ni una sede administrada. Podrá estar vacío. Si está vacío, la consulta del corredor debe distinguir ausencia de lugar informado y no inventar una ubicación por defecto ni reutilizar la de otro entrenamiento.

La presencialidad de un entrenamiento no se decidirá en este ADR como campo independiente. En el PMV, la modalidad del corredor es una etiqueta controlada decidida por `ADR-0005`; la necesidad de mostrar lugar de encuentro depende de que el entrenamiento tenga lugar informado y de las reglas de consulta que defina el diseño de experiencia del corredor.

Las asignaciones de un plan a segmentos y corredores individuales se modelarán como referencias editables del borrador hacia segmentos y corredores existentes. Una asignación a una referencia inexistente se rechazará sin modificar asignaciones ya guardadas. Resolver el conjunto final de destinatarios, congelarlo y versionarlo pertenece a `ADR-0007`.

## Alternativas consideradas

### Alternativa A: Plan como lista libre de texto

Se descarta porque impediría validar fechas, tipos, objetivos, ubicación y consulta móvil. También haría impracticable el seguimiento posterior por entrenamiento de `RF-17` a `RF-19`.

### Alternativa B: Catálogo de tipos administrable por el usuario

Se descarta para el PMV. Fase 1 cerró un catálogo mínimo de seis tipos. Convertirlo en catálogo administrable ampliaría permisos, pruebas, compatibilidad de objetivos y experiencia del corredor sin requisito imprescindible.

### Alternativa C: Un entrenamiento puede pertenecer a varios planes

Se descarta porque complicaría publicación, republicación, seguimiento e historial. Para el PMV, un entrenamiento publicado se entiende dentro de un plan semanal concreto.

### Alternativa D: Ubicación como catálogo de sedes

Se descarta porque `D-04` decide que el lugar de encuentro es texto libre y que El Retiro es contexto inicial, no restricción de producto. Gestionar sedes introduciría alcance no requerido.

### Alternativa E: Objetivos solo como texto libre

Se descarta porque contradice `RF-12`, que exige objetivos por frecuencia cardiaca o ritmo relativo. El texto libre queda como aclaración, no como fuente principal del objetivo.

### Alternativa F: Un único plan global por semana

Se descarta porque impediría preparar en paralelo planes distintos para segmentos, niveles u objetivos diferentes durante la misma semana. Obligar a combinar todo el contenido en un único plan también haría más complejas las asignaciones y la consulta del corredor.

## Consecuencias

- El plan semanal tiene una estructura verificable y apta para publicación atómica posterior.
- Varios planes pueden compartir semana, lo que permite separar propuestas de entrenamiento, pero exige nombres únicos dentro de esa semana y una selección explícita del plan en asignación, publicación y consulta.
- El catálogo cerrado simplifica interfaz, pruebas y consulta móvil, pero limita nuevos tipos hasta una decisión posterior.
- El entrenamiento queda ligado a una semana concreta; moverlo fuera de semana exige cambiar su fecha o moverlo a otro plan.
- El objetivo estructurado reduce ambigüedad para el corredor, pero exige que el corredor tenga referencias personales suficientes para interpretar zonas o ritmos relativos.
- Permitir ambas familias de objetivo en todos los tipos simplifica la operación inicial, aunque deja al entrenador la responsabilidad de elegir una referencia adecuada para cada sesión.
- La ubicación libre evita administrar sedes, pero no permite búsquedas o estadísticas por lugar sin una evolución posterior.
- La asignación a segmentos y corredores queda preparada, pero el cálculo histórico de destinatarios sigue dependiendo de `ADR-0007`.

## Requisitos relacionados

- `RF-04`
- `RF-07`
- `RF-08`
- `RF-11`
- `RF-12`
- `RF-13`
- `RF-14`
- `RF-16`

## Decisiones de Fase 1 relacionadas

- `D-01`: los planes se asignan a segmentos y publicaciones conservan destinatarios efectivos.
- `D-04`: el lugar de encuentro es texto libre y no se restringe al Retiro.
- `D-06`: un cambio relevante en un plan publicado exige republicación atómica y correo.

## Validación prevista

- Probar que un plan no puede publicarse sin semana identificable ni sin entrenamientos.
- Probar que pueden crearse varios planes con nombres distintos para la misma semana.
- Probar que dos planes de la misma semana no pueden compartir nombre después de normalizar espacios exteriores y mayúsculas/minúsculas, y que el mismo nombre sí puede reutilizarse en semanas diferentes.
- Probar que un entrenamiento no puede guardarse sin fecha, con fecha fuera de la semana del plan o sin tipo válido.
- Probar que el catálogo acepta únicamente las seis claves definidas y rechaza tipos libres.
- Probar que un entrenamiento publicable requiere objetivo principal estructurado y que la aclaración no sustituye ese objetivo.
- Probar que todos los tipos del catálogo aceptan objetivos por `frecuencia-cardiaca` y por `ritmo-relativo`.
- Probar que un objetivo por `frecuencia-cardiaca` usa zonas relativas del corredor.
- Probar que un objetivo por `ritmo-relativo` referencia el tiempo actual del corredor en una distancia de referencia.
- Probar que el lugar de encuentro conserva texto libre, puede estar ausente y no se reemplaza por valores ficticios.
- Probar que el corredor no accede a borradores ni a entrenamientos de otros corredores, según `ADR-0004`.
- Probar que asignaciones a segmentos o corredores inexistentes se rechazan sin alterar asignaciones existentes.
- Probar que `ADR-0007` captura una versión publicada sin depender de cambios posteriores del borrador.

## Decisiones pendientes

- **Bloqueante para implementar publicación:** `ADR-0007` debe definir publicación, versionado, edición de planes publicados, destinatarios efectivos y qué ocurre si un corredor resulta destinatario de varios planes en la misma semana. Responsable: revisor de arquitectura. Tratamiento: aceptar antes de implementar publicación o republicación.
- **Pendiente, sin bloquear este ADR:** elegir persistencia, índices y restricciones físicas. Responsable: revisor de arquitectura. Tratamiento: documentarlo al seleccionar stack y conservar las reglas canónicas de este ADR.
