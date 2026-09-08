# ADR-0030: Instantáneas de publicación en JSONB y modelo relacional mínimo

**Estado:** Aceptado
**Fecha:** 2026-09-08
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0007](0007-atomic-publication-versioning-recipients.md), [ADR-0012](0012-relational-persistence-transaction-strategy.md) y [ADR-0021](0021-publication-editing-notification-eligibility.md)

## Contexto

El diseño detallado de `publication` (`phase-2-detailed-design-publication.md`) modela cada versión publicada con 9 tablas relacionales: `published_plan`, `published_plan_recipient`, `published_plan_version`, `published_version_recipient`, `published_workout`, `published_phase_duration`, `published_workout_block`, `published_workout_recovery` y `published_version_changed_day`. Cuatro de ellas replican, columna a columna, el esquema vivo de `planning` (`workout`, `workout_phase_duration`, `workout_block`, `workout_recovery`), incluidos sus `CHECK` de carga exclusiva.

El módulo no tiene código ni migraciones Flyway todavía: las cuatro migraciones existentes (`V001`–`V004`) cubren `platform`, `identity_access`, `runner_management` y una tabla de `notification_delivery`. Simplificar ahora no exige migración de datos.

Tres hechos, verificados en el propio corpus, hacen que la réplica relacional sea una duplicación sin beneficio:

1. **El contenido llega ya validado.** El paso 7 de *Editar y actualizar una publicación* dice: *"valida la estructura completa mediante la API de `planning`"*. La invariante estructural de `ADR-0006`/`ADR-0020` (un entrenamiento por día, orden de bloques único, carga exclusiva) se declara y se aplica una vez, en `planning`, su dueño.
2. **`ADR-0021` eliminó la única razón para que ambas copias pudieran divergir.** Al retirar el borrador persistente post-publicación, *"su contenido mutable en `planning` coincidirá siempre con la versión activa de `publication`"*. Para la versión activa, la instantánea relacional es una copia bit a bit sin variación posible.
3. **No existe consulta relacional por fila sobre ese contenido.** El propio diseño reconoce: *"El plan contiene como máximo siete entrenamientos completos, por lo que sustituir su representación completa es razonable"*. Las lecturas reales son de plan completo (candidatura, publicación activa, republicación), no de bloques o fases sueltas.

Dos accesos, ambos en `tracking-review`, sí exigen leer el contenido congelado: un lookup puntual («¿fue publicado el entrenamiento X para el corredor R?») y un recorrido paginado de asignaciones publicadas por corredor y semana que incluye entrenamientos **retirados** en una versión posterior — este último obliga a leer contenido histórico, no solo el activo.

## Decisión

El esquema `publication` se reduce a 3 tablas:

- **`published_plan`**: identidad de la publicación por `weekly_plan_id`, con `active_version_number` como escalar entero. Sustituye a la columna de revisión monotónica: como ninguna representación puede mutar sin incrementar la versión (`ADR-0021`), el par `(id, active_version_number)` es el `ETag`.
- **`published_plan_recipient`**: destinatarios congelados en la primera publicación, con `PRIMARY KEY (published_plan_id, runner_id)` y `UNIQUE (runner_id, week_start)` **no parcial** — al no existir retirada ni despublicación, todo plan publicado permanece activo indefinidamente.
- **`published_plan_version`**: una fila por versión, con `content JSONB` (instantánea completa: nombre, grupo, semana y entrenamientos con su estructura visible) y `changed_days JSONB` (solo si `version_number > 1`), más `content_fingerprint` para rechazar la republicación sin diferencias.

`published_workout`, `published_phase_duration`, `published_workout_block`, `published_workout_recovery` y `published_version_changed_day` se eliminan; su contenido pasa al documento `content`/`changed_days`. **`published_version_recipient` se elimina sin sustituto**: por `ADR-0007`, toda versión conserva exactamente el mismo conjunto de destinatarios que `published_plan_recipient`, así que una copia por versión sería, por invariante, siempre idéntica al original.

La sustitución de contenido usa una sentencia CAS de una sola operación sobre `active_version_number`, no una comparación de revisión leída previamente:

```sql
UPDATE publication.published_plan
   SET active_version_number = :expected + 1, last_modified_by = :actor, last_modified_at = :now
 WHERE id = :planId AND active_version_number = :expected;
-- 0 filas => 412 Precondition Failed
```

Los dos accesos que no cubre un índice relacional se resuelven sobre el `JSONB`: el lookup puntual con un índice `GIN (content) jsonb_ops` y el predicado de contención; el recorrido paginado con `LATERAL jsonb_to_recordset(content -> 'workouts')` sobre las versiones de los planes donde el corredor es destinatario, deduplicando por `id` con `max(version_number)`.

Esta decisión **no** refina `ADR-0006` ni `ADR-0020`: `planning` conserva su modelo relacional completo y su validador canónico. `publication` no reintroduce DDD táctico ni estructura relacional para un contenido que ya fue validado antes de llegar.

## Alternativas consideradas

### Alternativa A: Mantener las 9 tablas del diseño original

Se descarta. Declara dos veces la misma invariante estructural, en dos esquemas, para un dato que un único camino de código ya validó. No aporta ninguna consulta que el modelo mínimo no resuelva.

### Alternativa B: 4 tablas, conservando `published_version_recipient`

Es la simplificación apuntada por la auditoría documental inicial. Se descarta por ir menos lejos de lo que las propias invariantes permiten: `published_version_recipient` no puede divergir nunca de `published_plan_recipient`, así que su existencia no es una instantánea distinta, es una copia redundante.

### Alternativa C: Añadir `published_workout_index` desde el inicio

Se descarta como decisión inicial. Sin datos reales ni medición de `EXPLAIN (ANALYZE, BUFFERS)` con volumen representativo, crear una tabla de índice por adelantado es la misma sobreingeniería preventiva que este ADR corrige en sentido contrario. Se documenta como escotilla condicionada, no como parte del modelo base.

### Alternativa D: Modelo de 3 tablas con `JSONB` (elegida)

Conserva en DDL exactamente las invariantes que son restricciones físicas reales (unicidad de plan, de versión, de destinatario, exclusividad corredor-semana) y traslada a un documento único lo que es, por naturaleza, una instantánea de lectura íntegra y escritura única.

## Consecuencias

- **Impacto positivo:** de 9 a 3 tablas; el `ETag` se deriva sin columna propia; el CAS de una sentencia es más fuerte que la comparación de revisión anterior (detecta la escritura perdida en la propia sentencia); la transacción de publicación pasa de ~9 inserciones/actualizaciones a 3, con menos superficie de fallo parcial.
- **Riesgo y coste:** el recorrido paginado de asignaciones publicadas (incluidas las retiradas) no tiene recorrido ordenado por índice; su coste es O(total) por página. Para el historial de un corredor (~1.500 filas) es irrelevante; para la revisión semanal del entrenador (~3.500 filas por página) es el caso a medir antes de escalar.
- **Deuda aceptada:** las unicidades `(versión, día)` y `(versión, día cambiado)` dejan de ser restricción física y pasan a validación de código.
- Un futuro caso de uso que exija consultar bloques o fases individuales de una versión histórica (no solo el plan completo) obligaría a revisar esta decisión.

## Requisitos relacionados

- `RF-08`, `RF-09`, `RF-10`, `RF-14`, `RF-15`, `RF-16`, `RF-20`, `RF-21`

## Decisiones de Fase 1 relacionadas

- `D-01`: cada concepto y regla conserva un propietario inequívoco — la validación estructural sigue siendo propiedad exclusiva de `planning`.
- `D-06`: republicación atómica y congelación de correo, preservadas sin cambio de comportamiento observable.

## Validación prevista

- Validar `content` de la versión 1 contra el esquema declarado en este ADR y comprobar que conserva valores visibles de catálogo, no solo claves.
- Ejecutar el CAS de `active_version_number` bajo concurrencia real y comprobar que la segunda escritura recibe `412` sin mutar la fila.
- Medir con `EXPLAIN (ANALYZE, BUFFERS)` el recorrido paginado de asignaciones publicadas con más de 500 corredores destinatarios.
- Probar que ninguna ruta de código permite crear `published_plan_recipient` con destino de correo o dato ajeno a la congelación (el módulo no conoce el correo, ver `ADR-0011`).
- Verificar mediante `ApplicationModules.verify()`/ArchUnit que `publication` no declara SQL cruzado hacia el esquema de `planning`.

## Decisiones pendientes

- **No bloqueante:** activar `published_workout_index (version_id, workout_id, workout_date)` si la medición de volumen real supera lo aceptable en el recorrido paginado. Responsable: Revisor de arquitectura. Tratamiento: revisión de rendimiento antes de superar 500 destinatarios por semana.
