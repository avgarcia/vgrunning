# ADR-0007: Publicación atómica, versionado y destinatarios efectivos

**Estado:** Aceptado
**Fecha:** 2026-08-12
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV debe publicar un plan semanal completo para los miembros efectivos de su grupo de planificación sin exponer contenido parcial. Cada publicación debe conservar una versión identificable y sus destinatarios aunque después cambien el borrador, las etiquetas, los segmentos, las excepciones o la pertenencia a grupos.

`ADR-0002` establece una única frontera de datos transaccionales. `ADR-0004` permite al corredor leer una publicación solo si aparece en su instantánea de destinatarios. `ADR-0005` define segmentos dinámicos y `ADR-0006` grupos exclusivos, un plan por pareja grupo-semana y contenido editable separado de sus futuras versiones publicadas.

Fase 1 exige publicación y republicación atómicas, impide cambios silenciosos y exige correo para destinatarios afectados. Este ADR decide la consistencia del dominio, las versiones y la visibilidad. `ADR-0008` define la solicitud transaccional de notificación y `ADR-0011` su entrega asíncrona; la entrega externa no forma parte de la transacción que hace visible el plan.

> **Evolución propuesta:** `ADR-0021` reemplaza el borrador persistente y los cambios pendientes después de publicar por una sesión local que sustituye contenido y versión activa en una única transacción. Las instantáneas completas, destinatarios congelados y demás garantías de este ADR permanecen vigentes.

## Decisión

El modelo distinguirá tres conceptos:

- **Borrador de trabajo:** contenido editable actual del plan y referencia a su grupo.
- **Versión publicada:** instantánea inmutable del contenido completo del plan en una publicación concreta.
- **Destinatario efectivo:** corredor incluido de forma inmutable en una versión publicada.

Cada plan tendrá como máximo una versión activa. La primera publicación confirmada creará la versión `1`; cada republicación confirmada incrementará en uno el número de versión del plan. Una operación abortada no creará ni consumirá un número de versión. Las versiones anteriores se conservarán como historial inmutable y no volverán a activarse mediante edición directa.

El plan conservará los estados `borrador` y `publicado` de `ADR-0006`. La primera publicación válida cambiará el estado a `publicado`. Editar después su borrador de trabajo no modificará la versión activa ni creará un tercer estado; el sistema expondrá un indicador derivado de cambios relevantes pendientes de republicar.

La primera publicación resolverá y congelará los destinatarios efectivos del plan. Toda republicación conservará exactamente ese mismo conjunto; no recalculará miembros desde el grupo ni permitirá añadir o retirar corredores. Un cambio posterior de grupo, segmentos, etiquetas o excepciones no alterará el plan ya publicado y solo afectará a planes cuya primera publicación todavía no se haya producido, normalmente los de semanas siguientes.

Publicar o republicar ejecutará una única operación transaccional con estos pasos lógicos:

1. comprobar autorización de administrador o entrenador;
2. comprobar que el borrador no cambió desde que comenzó la operación;
3. validar de nuevo el plan completo según `ADR-0006`;
4. en la primera publicación, resolver los miembros efectivos actuales del grupo y rechazar un conjunto vacío; en una republicación, copiar los destinatarios congelados de la versión activa;
5. comprobar que ningún destinatario figura en la versión activa de otro plan para la misma semana;
6. crear la siguiente versión con una instantánea completa del contenido;
7. insertar la instantánea completa de destinatarios efectivos;
8. desactivar la versión anterior del mismo plan, activar la nueva y conservar el estado `publicado`;
9. registrar actor y fecha de la publicación.

Si falla cualquier paso, no se crea versión visible, no cambia la versión activa, no se modifican destinatarios históricos y el borrador conserva su estado anterior. `ADR-0012` lo materializa en una transacción PostgreSQL con restricciones físicas; no se coordinará esta atomicidad mediante compensaciones entre servicios.

La comprobación de concurrencia comparará una revisión estable del borrador o mecanismo equivalente. En la primera publicación también comprobará una revisión estable del grupo y sus miembros. Si otro actor modifica alguno de esos datos entre la lectura y la confirmación, la publicación se rechazará como obsoleta y deberá reintentarse después de mostrar el estado actualizado. Una republicación no dependerá de la pertenencia actual al grupo porque reutiliza los destinatarios congelados.

La instantánea de contenido contendrá todos los datos necesarios para consultar la versión sin leer el borrador mutable: identidad visible del plan y grupo, semana, entrenamientos por día, fases, bloques, tipos, cargas, objetivos, recuperaciones, aclaraciones y lugares de encuentro. Las referencias de catálogo podrán conservar claves estables, pero el contenido visible necesario para reproducir la publicación no dependerá de nombres modificables posteriores.

La instantánea de destinatarios contendrá identificadores estables de corredor y la versión publicada a la que pertenecen. Cada nueva versión del mismo plan copiará el conjunto congelado en la primera publicación. Cambios posteriores de etiquetas, segmentos, excepciones, grupos o borrador no añadirán, retirarán ni trasladarán destinatarios de ese plan publicado.

La exclusividad se aplicará sobre versiones activas: un corredor tendrá como máximo un plan activo por semana. La restricción se comprobará dentro de la misma transacción de publicación y `ADR-0012` la refuerza mediante una restricción única física. Un conflicto rechazará la publicación completa y mostrará los corredores y planes afectados; no habrá prioridades ni sobrescrituras automáticas.

Se considerará relevante cualquier cambio en los datos visibles incluidos en la instantánea de contenido: identidad visible del plan, semana, entrenamientos, fases, bloques, tipos, cargas, objetivos, recuperaciones, aclaraciones o lugares de encuentro. Los metadatos internos y de auditoría no obligarán a republicar. Si no existe ninguna diferencia relevante respecto a la versión activa, la republicación se rechazará y no creará una versión nueva.

Republicar creará siempre una instantánea completa, no un parche. La nueva versión sustituirá atómicamente a la anterior como versión activa del plan y conservará sus destinatarios. El contrato de correo para esos destinatarios pertenece a `ADR-0008`.

Publicar o republicar hará visible la nueva versión inmediatamente después de confirmar la transacción. El PMV no incluirá programación de fecha u hora de publicación.

## Alternativas consideradas

### Alternativa A: Hacer visibles los entrenamientos uno a uno

Se descarta porque permitiría que el corredor consultase una fracción del plan y contradice `RF-09` y `D-06`.

### Alternativa B: Consultar siempre el borrador actual

Se descarta porque cualquier edición cambiaría silenciosamente lo visible y haría imposible reproducir una publicación histórica.

### Alternativa C: Guardar solo diferencias entre versiones

Se descarta para el PMV porque complica consulta, auditoría, pruebas y recuperación sin una necesidad de escala que justifique reconstruir cadenas de parches.

### Alternativa D: Recalcular destinatarios al consultar

Se descarta porque cambios de etiquetas, segmentos o grupos alterarían retrospectivamente quién recibió una publicación y contradirían `RF-10`.

### Alternativa E: Resolver conflictos por prioridad de grupo o plan

Se descarta porque ocultaría errores operativos y podría asignar un plan diferente por orden incidental. Los conflictos se muestran y se corrigen explícitamente.

### Alternativa F: Incluir la entrega del correo en la transacción

Se descarta porque un sistema externo no puede participar de forma fiable en la transacción de datos del PMV. `ADR-0008` desacopla la entrega sin perder la solicitud asociada a la publicación.

### Alternativa G: Recalcular destinatarios en cada republicación

Se descarta porque un cambio de grupo durante una semana podría retirar el plan actual o trasladar al corredor a otro plan. Los destinatarios quedan congelados en la primera publicación y los cambios de grupo se aplican a planes todavía no publicados.

### Alternativa H: Permitir publicaciones sin destinatarios

Se descarta porque no producen ningún resultado visible y generan versiones y notificaciones sin utilidad operativa. Un grupo vacío debe corregirse antes de publicar.

### Alternativa I: Programar la publicación

Se descarta para el PMV. La acción de publicar produce visibilidad inmediata y evita introducir planificación temporal, cancelación y ejecución diferida sin un requisito que las justifique.

## Consecuencias

- El corredor ve una versión coherente y reproducible del plan completo.
- El historial ocupa más espacio porque cada versión guarda una instantánea completa, coste aceptable para la escala del PMV.
- El borrador puede evolucionar sin alterar contenido publicado, pero la interfaz debe señalar cambios pendientes para evitar confusión operativa.
- Resolver miembros en la primera publicación garantiza trazabilidad y exige detectar modificaciones concurrentes de grupos y segmentación en ese momento.
- Congelar destinatarios mantiene estable la semana actual; un cambio de grupo solo afecta a planes todavía no publicados y no permite traslados entre planes ya publicados.
- Rechazar grupos vacíos y republicaciones sin cambios evita versiones sin efecto observable.
- El correo deja de condicionar la atomicidad del contenido; `ADR-0008` garantiza que la solicitud de notificación no se pierda.
- Persistencia e índices deberán soportar unicidad, orden de versiones y consultas por corredor y semana sin convertir datos derivados en fuente de verdad.

## Requisitos relacionados

- `RF-08`
- `RF-09`
- `RF-10`
- `RF-14`
- `RF-15`
- `RF-16`
- `RF-20`

## Decisiones de Fase 1 relacionadas

- `D-01`: cada publicación conserva versión y destinatarios efectivos.
- `D-06`: los cambios relevantes exigen republicación atómica y notificación a afectados.

## Validación prevista

- Probar que una publicación válida crea simultáneamente versión completa, destinatarios y versión activa.
- Inyectar fallos en cada paso y comprobar que no queda contenido parcial ni cambia la versión activa anterior.
- Probar numeración secuencial por plan y conservación inmutable de versiones anteriores.
- Probar que editar el borrador publicado no altera la consulta del corredor hasta republicar.
- Probar que la primera publicación rechaza un grupo sin miembros efectivos.
- Probar que cambios posteriores de etiquetas, segmentos, excepciones o grupos no cambian los destinatarios de ninguna republicación del plan.
- Probar que una modificación concurrente del borrador rechaza cualquier publicación obsoleta y que una modificación concurrente de miembros rechaza la primera publicación.
- Probar que dos publicaciones concurrentes no pueden dejar dos planes activos para el mismo corredor y semana.
- Probar que un conflicto lista corredores y planes afectados y no aplica prioridades ni cambios parciales.
- Probar que una republicación sustituye la versión activa completa, conserva los destinatarios iniciales y mantiene la anterior como historial.
- Probar que cualquier cambio visible exige republicación y que cambios solo de auditoría no la exigen.
- Probar que una republicación sin diferencias relevantes se rechaza sin consumir versión.
- Probar que publicar hace visible inmediatamente la versión y que no existe programación diferida.
- Probar autorización y aislamiento según `ADR-0004`, incluida consulta solo por destinatarios efectivos.
- Probar que un fallo de entrega de correo no revierte una versión ya publicada; la garantía de solicitud se validará con `ADR-0008`.

## Decisiones pendientes

- **Resuelto por `ADR-0012`:** PostgreSQL con `READ COMMITTED`, bloqueo de coordinación, bloqueo del plan y restricciones únicas materializará estas garantías.
