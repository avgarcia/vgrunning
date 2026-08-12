# ADR-0005: Taxonomías controladas y segmentos dinámicos

**Estado:** Aceptado
**Fecha:** 2026-08-12
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV clasifica corredores mediante etiquetas controladas y usa esas etiquetas para construir segmentos dinámicos. Fase 1 limita las reglas a condiciones con operador Y entre criterios, permite seleccionar uno o varios valores dentro de cada etiqueta y añade inclusiones o exclusiones manuales. Las etiquetas y sus valores no son texto libre.

`ADR-0002` sitúa administración y taxonomías y segmentación en módulos explícitos de una única aplicación. `ADR-0004` establece que el administrador gestiona definiciones y valores permitidos, mientras que administrador y entrenador gestionan corredores, asignaciones de etiquetas, segmentos y excepciones manuales.

La publicación debe conservar los destinatarios efectivos aunque después cambien etiquetas, segmentos o asignaciones. Este ADR decide la semántica dinámica del segmento. `ADR-0006` usa los resultados de varios segmentos para formar grupos de planificación exclusivos y `ADR-0007` decidirá cuándo y cómo se captura la instantánea de destinatarios.

## Decisión

El modelo distinguirá cuatro conceptos:

- **Definición de etiqueta:** clasificación administrada, identificada de forma estable y con nombre visible.
- **Valor permitido:** opción perteneciente a una única definición de etiqueta.
- **Asignación de etiqueta:** relación entre un corredor y un valor permitido.
- **Segmento:** regla dinámica compuesta por criterios de etiquetas y excepciones manuales.

Los nombres de definiciones serán obligatorios y únicos sin distinguir mayúsculas, minúsculas ni espacios exteriores. Los nombres de valores cumplirán la misma regla dentro de su definición. La comparación de nombres usará normalización Unicode NFC y una representación canónica para unicidad; los identificadores estables, no los nombres visibles, sostendrán relaciones y reglas.

Cada corredor tendrá como máximo un valor asignado por definición de etiqueta. Cambiar la clasificación sustituirá el valor anterior dentro de esa definición en una única operación. No existirán valores libres ni asignaciones a valores que no pertenezcan a la definición indicada.

La modalidad será una definición de etiqueta protegida e identificada mediante una clave estable del sistema. Tendrá los valores protegidos `en-linea` y `presencial`, también identificados mediante claves estables. Mantendrá la misma mecánica de asignación y segmentación que cualquier etiqueta, pero no podrá eliminarse, desactivarse ni cambiar su cardinalidad. Los nombres visibles podrán adaptarse sin cambiar las claves ni el significado.

Las definiciones y valores tendrán estado activo o inactivo. El PMV no permitirá su eliminación física. Un elemento inactivo:

- no podrá usarse en nuevas asignaciones ni en nuevos criterios;
- conservará asignaciones y criterios existentes para no alterar segmentos de forma silenciosa;
- podrá retirarse de asignaciones o criterios existentes;
- podrá reactivarse por el administrador.

Una definición no podrá desactivarse mientras alguno de sus valores permanezca activo. Desactivar un valor no desactiva ni elimina automáticamente asignaciones, criterios, segmentos o publicaciones.

Cada segmento tendrá un nombre obligatorio y único sin distinguir mayúsculas, minúsculas ni espacios exteriores. Un segmento válido tendrá al menos un criterio y no funcionará como una lista exclusivamente manual.

Cada criterio referenciará una definición activa y un conjunto no vacío de uno o varios valores activos de esa definición. Un segmento tendrá como máximo un criterio por definición. Su evaluación dinámica seguirá esta semántica:

- dentro de un criterio, el corredor cumple si su valor asignado pertenece al conjunto seleccionado; esto equivale a O entre valores, pero no expone un operador O configurable;
- entre criterios, el corredor debe cumplir todos; esto equivale a Y;
- un corredor sin valor para una definición requerida no cumple ese criterio.

Formalmente, el resultado base contiene los corredores que cumplen todos los criterios. Las excepciones manuales se modelarán como una única relación por pareja segmento-corredor con modo `inclusion` o `exclusion`; no podrán coexistir ambos modos. Cambiar el modo sustituirá la excepción anterior.

El resultado efectivo se calculará como:

`(resultado base ∪ inclusiones manuales) − exclusiones manuales`

Una inclusión o exclusión podrá registrarse aunque en ese momento no cambie el resultado base. Así conservará su intención si posteriormente cambian las etiquetas del corredor o los criterios del segmento. Una excepción que referencia un corredor inexistente se rechazará sin modificar las excepciones existentes.

La evaluación será dinámica al consultar el segmento y al calcular los miembros efectivos de los grupos de planificación de `ADR-0006`. No se mantendrá una lista persistente de miembros como fuente de verdad del segmento. Una implementación podrá usar índices o caché derivada, pero deberá invalidarla ante cambios de definiciones, valores, asignaciones, criterios o excepciones y producir el mismo resultado que la regla canónica.

Los cambios posteriores a una publicación nunca modificarán sus destinatarios históricos. `ADR-0007` deberá capturar el resultado efectivo usado al publicar como una instantánea inmutable.

## Alternativas consideradas

### Alternativa A: Etiquetas y reglas de texto libre

Se descarta porque permitiría valores inconsistentes, expresiones no validables y reglas difíciles de explicar. Contradice `RF-03`, `RF-05` y la gramática limitada de `D-05`.

### Alternativa B: Listas manuales como fuente de verdad del segmento

Se descarta porque perdería el comportamiento dinámico de `D-01`. Las inclusiones y exclusiones son excepciones a una regla, no un segundo tipo de segmento manual.

### Alternativa C: Varios valores de una misma etiqueta por corredor

Se descarta para el PMV porque amplía la semántica de clasificación y hace menos evidente el significado de modalidad y de cada criterio. Seleccionar varios valores en un criterio ya permite agrupar corredores cuyos valores individuales sean distintos.

### Alternativa D: Eliminar físicamente etiquetas y valores

Se descarta porque rompería asignaciones y criterios o exigiría cascadas que alterarían segmentos de forma silenciosa. El estado inactivo conserva trazabilidad y permite una retirada explícita.

### Alternativa E: Congelar miembros al guardar un segmento

Se descarta porque produciría resultados obsoletos tras cambiar etiquetas o excepciones. La congelación pertenece a la publicación, no al segmento.

## Consecuencias

- La evaluación de segmentos tiene una expresión predecible y comprobable sin motor genérico de reglas.
- Un único valor por definición simplifica asignaciones y consultas, pero no permite clasificaciones multivalor dentro de una misma etiqueta.
- La modalidad comparte el sistema de etiquetas sin depender de nombres visibles, aunque introduce una definición y dos valores protegidos por el producto.
- Desactivar no borra referencias ni cambia automáticamente la pertenencia; retirar un valor exige revisar de forma explícita asignaciones y criterios existentes.
- Las excepciones manuales conservan intención frente a cambios posteriores y no pueden entrar en conflicto porque existe un único modo por segmento y corredor.
- El resultado de un segmento puede cambiar en cualquier momento antes de publicar. Solo la instantánea de destinatarios de `ADR-0007` aporta trazabilidad histórica.
- Consultas e índices deberán soportar más de 500 corredores sin convertir una caché en fuente de verdad.

## Requisitos relacionados

- `RF-02`
- `RF-03`
- `RF-04`
- `RF-05`
- `RF-06`
- `RF-08`
- `RF-09`
- `RF-10`

## Decisiones de Fase 1 relacionadas

- `D-01`: los segmentos son dinámicos y las publicaciones conservan destinatarios efectivos.
- `D-02`: la modalidad usa la misma taxonomía controlada que las demás etiquetas.
- `D-05`: las reglas usan Y entre criterios, varios valores permitidos por criterio y excepciones manuales.

## Validación prevista

- Probar unicidad normalizada de definiciones, valores y segmentos, incluido rechazo de nombres vacíos o equivalentes.
- Probar que un corredor solo conserva un valor por definición y que sustituirlo actualiza inmediatamente los segmentos afectados.
- Probar la modalidad mediante claves estables, sus dos valores protegidos y el rechazo de eliminación o desactivación.
- Probar Y entre criterios, O entre valores, ausencia de asignación y rechazo de criterios vacíos, duplicados o con referencias inválidas.
- Probar que cambiar una excepción entre inclusión y exclusión sustituye el modo anterior y que la fórmula efectiva se cumple aunque la excepción sea redundante en el momento de guardarla.
- Probar que desactivar una definición o valor impide usos nuevos sin borrar asignaciones ni criterios existentes.
- Probar que cambios de etiquetas, criterios y excepciones actualizan el segmento antes de publicar, pero no alteran destinatarios de publicaciones anteriores.
- Medir la evaluación con más de 500 corredores y verificar que cualquier caché produce el mismo resultado que la regla canónica tras invalidación.
- Probar la matriz de permisos de `ADR-0004` para administración de taxonomías, asignaciones, segmentos y excepciones.

## Decisiones pendientes

- **Bloqueante para implementar publicación:** `ADR-0007` debe definir la transacción que captura los miembros efectivos del grupo de planificación como destinatarios inmutables. Responsable: revisor de arquitectura. Tratamiento: aceptar antes de implementar publicación; no bloquea este ADR.
- **Pendiente, sin bloquear este ADR:** elegir estrategia concreta de índices y caché al seleccionar persistencia. Responsable: revisor de arquitectura. Tratamiento: documentarla con el stack y demostrar equivalencia con la evaluación canónica.
