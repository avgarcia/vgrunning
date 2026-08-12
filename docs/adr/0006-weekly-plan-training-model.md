# ADR-0006: Modelo de grupos de planificación, planes semanales y entrenamientos

**Estado:** Aceptado
**Fecha:** 2026-08-12
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV necesita que administrador y entrenador gestionen grupos de planificación estables y creen para ellos planes semanales con entrenamientos, tipos del catálogo cerrado, objetivos por frecuencia cardiaca o ritmo relativo, aclaraciones y lugar de encuentro cuando aplique. El corredor solo consulta el contenido publicado que le corresponde y puede quedarse sin plan en una semana, pero no recibir dos.

Fase 1 define el plan semanal como agrupación de entrenamientos fechados, con estado borrador o publicado. También fija que el catálogo mínimo incluye rodaje, tirada larga, series, cambios de ritmo/fartlek, cuestas y carrera/test. El lugar de encuentro se usa en entrenamientos presenciales y es texto libre; no se restringe a El Retiro.

`ADR-0002` sitúa planificación como módulo de una aplicación única. `ADR-0004` permite a administrador y entrenador gestionar grupos, planes y entrenamientos, y restringe al corredor a la lectura de publicaciones propias. `ADR-0005` decide la evaluación de segmentos dinámicos, permite que sus resultados se solapen y mantiene la modalidad del corredor como etiqueta controlada, pero no convierte la ubicación del entrenamiento en taxonomía.

Este ADR decide el modelo editable de planificación antes de publicar. Refina `RF-08`: la asignación por segmentos y corredores se configura una vez en el grupo, en lugar de repetirse en cada plan, pero conserva el mismo resultado observable. `ADR-0007` decidirá la transacción de publicación, versionado, destinatarios efectivos, definición de cambio relevante y relación entre borrador y versiones publicadas.

## Decisión

El modelo distinguirá ocho conceptos:

- **Grupo de planificación:** cohorte operativa estable que reúne segmentos y excepciones para recibir un plan común.
- **Plan semanal:** contenedor editable de una semana de entrenamiento perteneciente a un grupo.
- **Entrenamiento:** sesión de un día de la semana perteneciente a un único plan.
- **Fase de entrenamiento:** calentamiento, parte principal o enfriamiento.
- **Bloque principal:** unidad ordenada y repetible de trabajo y recuperación dentro de la parte principal.
- **Tipo de entrenamiento:** valor cerrado del catálogo funcional del PMV.
- **Objetivo:** instrucción estructurada por frecuencia cardiaca o ritmo relativo al corredor.
- **Lugar de encuentro:** texto libre opcional asociado al entrenamiento cuando sea presencial.

Cada grupo de planificación tendrá un identificador estable y un nombre obligatorio y único sin distinguir mayúsculas, minúsculas ni espacios exteriores. La unicidad usará normalización Unicode NFC y una representación canónica, como en `ADR-0005`. El grupo referenciará uno o varios segmentos existentes y podrá declarar inclusiones y exclusiones manuales de corredores. No funcionará como una lista exclusivamente manual.

Los miembros efectivos del grupo se calcularán como:

`(unión de resultados efectivos de sus segmentos ∪ inclusiones del grupo) − exclusiones del grupo`

Las excepciones del grupo serán persistentes hasta que administrador o entrenador las modifiquen; no existirán excepciones limitadas a una semana. Una única relación por pareja grupo-corredor tendrá modo `inclusion` o `exclusion`, sin permitir ambos simultáneamente. Referencias a segmentos o corredores inexistentes se rechazarán sin alterar la configuración guardada.

Un corredor podrá no pertenecer a ningún grupo de planificación, pero no podrá pertenecer a más de uno. Crear o modificar un grupo, un segmento, una regla, una excepción o una asignación de etiqueta que provoque solapamiento entre miembros efectivos de grupos se rechazará completo y mostrará los corredores y grupos en conflicto. Los segmentos sí podrán solaparse entre sí; la exclusividad se aplica únicamente a los grupos de planificación.

Un plan semanal tendrá un identificador estable, un nombre obligatorio, un único grupo de planificación, una semana identificada de forma explícita, estado `borrador` o `publicado`, y metadatos de auditoría mínimos. La semana se representará mediante la fecha del lunes que inicia la semana en el calendario local del club. El PMV no almacenará una semana como texto libre.

El PMV permitirá crear varios planes para una misma semana, pero cada grupo tendrá como máximo uno. El nombre será único dentro de la semana después de eliminar espacios exteriores y comparar sin distinguir mayúsculas de minúsculas. Se rechazará un segundo plan para la misma pareja grupo-semana y un nombre normalizado duplicado en la misma semana; el nombre podrá reutilizarse en semanas diferentes.

Los planes no se asignarán directamente a segmentos ni corredores. Sus destinatarios candidatos serán los miembros efectivos del grupo en el momento de publicar. El corredor podrá quedarse sin plan si no pertenece a un grupo o si su grupo no tiene plan esa semana. `ADR-0007` impedirá además que cambios de pertenencia posteriores permitan publicar para un corredor un segundo plan de la misma semana y conservará la instantánea histórica de destinatarios.

Un plan en `borrador` podrá modificarse por administrador o entrenador. Un plan en `publicado` conservará ese estado tras la primera publicación válida. La forma exacta de modificar contenido ya publicado, crear una nueva versión y calcular destinatarios afectados pertenece a `ADR-0007`; este ADR no permite ediciones silenciosas sobre contenido visible para corredores.

Un plan semanal válido tendrá al menos un entrenamiento para poder publicarse. Podrá existir temporalmente como borrador sin entrenamientos para permitir creación incremental, pero no será publicable en ese estado.

Cada entrenamiento pertenecerá a un único plan semanal y declarará un día obligatorio de lunes a domingo. La fecha concreta se derivará del lunes que identifica la semana del plan; el día, no una fecha absoluta, será el dato canónico del entrenamiento. Un plan tendrá como máximo un entrenamiento por día.

Todo entrenamiento publicable tendrá exactamente tres fases y en este orden:

1. **Calentamiento:** tipo fijo `rodaje`, duración temporal positiva obligatoria y sin objetivo.
2. **Parte principal:** tipo obligatorio del catálogo y uno o varios bloques principales ordenados.
3. **Enfriamiento:** tipo fijo `rodaje`, duración temporal positiva obligatoria y sin objetivo.

Cada bloque principal tendrá un orden estable, un número positivo de repeticiones, una carga de trabajo expresada exclusivamente como duración o distancia positiva y un objetivo estructurado. Cuando haya varios bloques, su orden formará una única secuencia de ejecución.

Un bloque con más de una repetición requerirá recuperación; con una repetición podrá omitirla. La recuperación, cuando exista, tendrá duración o distancia positiva y una modalidad cerrada: `parado`, `andando` o `rodaje`. Solo la recuperación `rodaje` tendrá objetivo estructurado obligatorio; las modalidades `parado` y `andando` no admitirán objetivo.

La aclaración opcional de texto libre y el lugar de encuentro opcional pertenecerán al entrenamiento completo. La aclaración complementará la estructura, pero no sustituirá fases, bloques, tipo, carga, objetivo o recuperación obligatorios.

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

Los objetivos de los bloques principales y de las recuperaciones `rodaje` usarán una de estas familias:

- `frecuencia-cardiaca`: objetivo expresado mediante zonas relativas del corredor.
- `ritmo-relativo`: objetivo expresado mediante referencias relativas al tiempo actual del corredor en una distancia de referencia, como 5 km, 10 km u otra distancia definida por el diseño detallado.

Todos los tipos del catálogo permitirán cualquiera de las dos familias de objetivo. Calentamiento, enfriamiento y recuperaciones `parado` o `andando` no tendrán objetivo. Un objetivo no almacenará una instrucción arbitraria como único dato estructurado.

El lugar de encuentro será texto libre del entrenamiento, no una taxonomía ni una sede administrada. Podrá estar vacío. Si está vacío, la consulta del corredor debe distinguir ausencia de lugar informado y no inventar una ubicación por defecto ni reutilizar la de otro entrenamiento.

La presencialidad de un entrenamiento no se decidirá en este ADR como campo independiente. En el PMV, la modalidad del corredor es una etiqueta controlada decidida por `ADR-0005`; la necesidad de mostrar lugar de encuentro depende de que el entrenamiento tenga lugar informado y de las reglas de consulta que defina el diseño de experiencia del corredor.

## Alternativas consideradas

### Alternativa A: Plan como lista libre de texto

Se descarta porque impediría validar días, fases, bloques, tipos, objetivos, ubicación y consulta móvil. También haría impracticable el seguimiento posterior por entrenamiento de `RF-17` a `RF-19`.

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

### Alternativa G: Asignar cada plan directamente a segmentos y corredores

Se descarta porque repetiría cada semana la resolución de solapamientos y obligaría al entrenador a reconstruir cohortes operativas estables. El grupo de planificación centraliza segmentos y excepciones antes de crear el plan.

### Alternativa H: Prohibir cualquier solapamiento entre segmentos

Se descarta porque un corredor puede cumplir varias clasificaciones reutilizables al mismo tiempo. La exclusividad pertenece a los grupos de planificación, no al sistema general de segmentación.

### Alternativa I: Excepciones de grupo limitadas a una semana

Se descarta para el PMV porque no existe un caso operativo que justifique cambiar temporalmente de grupo al corredor. Cualquier cambio de pertenencia será explícito y persistirá hasta una modificación posterior.

## Consecuencias

- El grupo de planificación evita reconstruir destinatarios cada semana, pero introduce una validación transversal ante cambios de etiquetas, segmentos, excepciones o grupos.
- Los segmentos conservan solapamiento y reutilización, mientras los grupos garantizan una única pertenencia operativa por corredor.
- Varios planes pueden compartir semana, pero cada grupo tiene como máximo uno y el corredor puede recibir como máximo el correspondiente a su grupo.
- La estructura de tres fases y bloques ordenados permite representar rodajes continuos, series, fartlek y sesiones mixtas sin reducirlas a texto libre.
- El catálogo cerrado simplifica interfaz, pruebas y consulta móvil, pero limita nuevos tipos hasta una decisión posterior.
- El entrenamiento queda ligado a un día de una semana concreta; moverlo exige cambiar el día o moverlo a otro plan.
- El objetivo estructurado reduce ambigüedad para el corredor, pero exige que el corredor tenga referencias personales suficientes para interpretar zonas o ritmos relativos.
- Calentamiento y enfriamiento quedan deliberadamente simplificados a rodaje por duración, sin objetivo estructurado.
- Recuperaciones estructuradas permiten distinguir pausa, marcha y rodaje, aunque aumentan las validaciones del editor de entrenamientos.
- La ubicación libre evita administrar sedes, pero no permite búsquedas o estadísticas por lugar sin una evolución posterior.
- El cálculo histórico de miembros del grupo y la garantía frente a cambios posteriores siguen dependiendo de `ADR-0007`.

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

- `D-01`: los segmentos dinámicos alimentan grupos de planificación y las publicaciones conservan destinatarios efectivos.
- `D-04`: el lugar de encuentro es texto libre y no se restringe al Retiro.
- `D-06`: un cambio relevante en un plan publicado exige republicación atómica y correo.

## Validación prevista

- Probar la fórmula de miembros efectivos del grupo con unión de segmentos, inclusiones y exclusiones persistentes.
- Probar que segmentos solapados son válidos y que cualquier operación que produzca un corredor en dos grupos se rechaza completa mostrando el conflicto.
- Probar que un corredor puede no pertenecer a ningún grupo y que no existen excepciones de grupo limitadas a una semana.
- Probar que un plan no puede publicarse sin grupo, semana identificable ni entrenamientos.
- Probar que pueden crearse planes para varios grupos en la misma semana y que se rechaza un segundo plan para la misma pareja grupo-semana.
- Probar la unicidad normalizada del nombre dentro de la semana y su reutilización en semanas diferentes.
- Probar que un entrenamiento exige un día de lunes a domingo, deriva su fecha desde el lunes del plan y no puede duplicar otro entrenamiento del mismo día en ese plan.
- Probar que un entrenamiento publicable contiene exactamente calentamiento, parte principal y enfriamiento en ese orden.
- Probar que calentamiento y enfriamiento tienen tipo `rodaje`, duración temporal positiva y ningún objetivo.
- Probar que la parte principal admite varios bloques ordenados con repeticiones positivas y carga exclusiva por duración o distancia.
- Probar que un bloque con varias repeticiones requiere recuperación y que uno con una repetición puede omitirla.
- Probar las recuperaciones `parado`, `andando` y `rodaje`, incluida la obligación de objetivo solo para `rodaje`.
- Probar que el catálogo acepta únicamente las seis claves definidas y rechaza tipos libres.
- Probar que todos los tipos del catálogo aceptan en sus bloques objetivos por `frecuencia-cardiaca` y por `ritmo-relativo`.
- Probar que un objetivo por `frecuencia-cardiaca` usa zonas relativas del corredor.
- Probar que un objetivo por `ritmo-relativo` referencia el tiempo actual del corredor en una distancia de referencia.
- Probar que la aclaración no sustituye ninguna estructura obligatoria.
- Probar que el lugar de encuentro conserva texto libre, puede estar ausente y no se reemplaza por valores ficticios.
- Probar que el corredor no accede a borradores ni a entrenamientos de otros corredores, según `ADR-0004`.
- Probar que `ADR-0007` captura los miembros efectivos del grupo y evita un segundo plan para el mismo corredor y semana tras cambios posteriores de pertenencia.

## Decisiones pendientes

- **Bloqueante para implementar publicación:** `ADR-0007` debe definir publicación, versionado, edición de planes publicados, captura de miembros efectivos del grupo y garantía transaccional de un único plan publicado por corredor y semana aunque cambie después de grupo. Responsable: revisor de arquitectura. Tratamiento: aceptar antes de implementar publicación o republicación.
- **Pendiente, sin bloquear este ADR:** elegir persistencia, índices y restricciones físicas. Responsable: revisor de arquitectura. Tratamiento: documentarlo al seleccionar stack y conservar las reglas canónicas de este ADR.
