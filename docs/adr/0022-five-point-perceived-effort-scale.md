# ADR-0022: Escala de cinco puntos para esfuerzo percibido

**Estado:** Aceptado
**Fecha:** 2026-08-24
**Responsable de revisión:** Revisor de producto y Revisor de arquitectura

## Contexto

`RF-17` y `ADR-0009` definían el esfuerzo percibido del corredor mediante un entero de `1` a `10` cuando el entrenamiento se declarase `realizado`. Durante la revisión del cierre de Fase 2, el responsable de producto confirmó que prefiere una escala más breve de `1` a `5`.

El intervalo forma parte del contrato funcional: afecta a la representación HTTP, las restricciones de persistencia, los controles de interfaz, la consulta de seguimiento y las pruebas. Cambiar únicamente un ejemplo dejaría fuentes normativas contradictorias y permitiría que la implementación eligiese la escala de forma implícita.

Todavía no existen runtime, contrato OpenAPI, migraciones ni datos reales. Por tanto, la decisión puede cambiarse sin migrar producción ni mantener compatibilidad con registros persistidos. `ADR-0009` continúa gobernando identidad, campos, ventana, historial, autorización y revisión; este ADR reemplaza exclusivamente su intervalo de esfuerzo.

## Decisión

El campo `effort` será un entero de `1` a `5`, ambos extremos incluidos, cuando `performed=true`. Será obligatorio en ese estado y deberá estar ausente cuando `performed=false`.

La interfaz presentará las cinco opciones numéricas sin atribuirles nombres clínicos ni afirmar que implementan una escala médica estandarizada. Añadir etiquetas interpretativas requerirá una decisión de producto posterior para evitar significados inconsistentes entre corredores.

OpenAPI declarará `type: integer`, `minimum: 1` y `maximum: 5`. La persistencia aplicará una restricción equivalente y las validaciones de aplicación no corregirán, redondearán ni truncarán valores fuera del intervalo.

No se define una conversión de `1..10` a `1..5`. Si un entorno no productivo contiene datos sintéticos anteriores, deberán descartarse o regenerarse; dividir o redondear alteraría una declaración subjetiva sin una regla validada.

Las sensaciones `bien`, `normal` y `mal`, el comentario opcional, el consentimiento, la ventana temporal, la unicidad del registro y las reglas de acceso permanecen sin cambios.

## Alternativas consideradas

### Alternativa A: Mantener `1..10`

Se descarta por decisión explícita del responsable de producto. Ofrece mayor granularidad, pero conserva diez opciones cuando el PMV prefiere una captura más breve y simple.

### Alternativa B: Usar `1..5`

Se acepta. Reduce opciones y carga de interacción, mantiene orden y extremos verificables y sigue permitiendo comparar la declaración vigente sin añadir otra dimensión de seguimiento.

### Alternativa C: Sustituir el número por etiquetas cualitativas

Se descarta porque duplicaría la sensación general o exigiría decidir otro catálogo y su orden. También perdería la restricción numérica ya esperada por `RF-17` y las consultas de `RF-19`.

## Consecuencias

- El corredor elige entre cinco valores y la interfaz puede usar un control discreto más breve.
- Se reduce la granularidad respecto a `1..10`; dos percepciones antes diferenciables pueden compartir valor.
- Requisito, criterios, diseño, OpenAPI, restricción PostgreSQL y pruebas deben usar exactamente el mismo intervalo.
- No se añade una nueva categoría de datos, actor, permiso, flujo ni capacidad del PMV.
- No existe coste de migración productiva porque la decisión precede a implementación y datos reales.
- Cualquier futura ampliación o interpretación clínica deberá revisar producto, compatibilidad, privacidad y este ADR.

## Requisitos relacionados

- `RF-17`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-07`: seguimiento estructurado y vista mínima de revisión.

## Validación prevista

- Probar que `performed=true` acepta exactamente los enteros `1` a `5` y exige `effort`.
- Probar que `0`, `6`, números no enteros y valores no numéricos se rechazan sin sustituir el registro válido anterior.
- Probar que `performed=false` rechaza cualquier `effort`, incluido uno dentro del intervalo.
- Verificar que OpenAPI y la restricción física de PostgreSQL usan los mismos extremos.
- Verificar que portal, historial y revisión muestran el valor conservado sin convertirlo ni añadir etiquetas no aprobadas.
- Buscar referencias activas a `1..10`, `1–10`, límites `10` u `11` y justificar únicamente las que permanezcan como historia de `ADR-0009`.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro de este refinamiento. OpenAPI, migraciones y pruebas siguen siendo artefactos obligatorios antes de implementar, no alternativas abiertas sobre la escala.
