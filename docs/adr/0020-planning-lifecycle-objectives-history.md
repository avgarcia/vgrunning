# ADR-0020: Ciclo de vida, objetivos e historial de planificación

**Estado:** Aceptado
**Fecha:** 2026-08-18
**Responsable de revisión:** Revisor de arquitectura
**Validación documental:** Decisiones de planificación aceptadas explícitamente por el responsable el 2026-08-19

## Contexto

`ADR-0006` define grupos exclusivos, planes semanales, entrenamientos con tres fases, bloques, recuperaciones y objetivos relativos. `ADR-0007` separa el borrador mutable de las versiones publicadas y congela destinatarios desde la primera publicación. `ADR-0012` prescribe una fila global de coordinación para cualquier mutación que pueda alterar la pertenencia efectiva a grupos.

El diseño detallado todavía no había decidido el ciclo de vida de los grupos, cómo preparar un grupo fuera de operación, cómo trasladar corredores sin estados intermedios, qué puede cambiar después de publicar, ni el formato y la escala verificables de los objetivos de `RF-12`. Tampoco existía una política para el historial de borradores y grupos o para purgar planes nunca publicados.

Hay además dos precisiones necesarias sobre `ADR-0006`:

- exigir siempre uno o varios segmentos impide usar el estado inactivo como espacio de preparación incremental;
- deducir la presencialidad desde la ubicación o desde la modalidad dinámica de los corredores no distingue correctamente el contenido propio del entrenamiento.

Resolver estas reglas durante la implementación ocultaría decisiones que afectan modelo de datos, permisos, transacciones, API, privacidad y pruebas. Este ADR mantiene la propiedad modular aceptada: `planning` gobierna grupos y borradores; `publication` consume su API y gobierna versiones, destinatarios y visibilidad.

> **Evolución propuesta:** `ADR-0021` reemplaza únicamente, para planes publicados, la mutabilidad del nombre, el borrador persistente, los cambios pendientes, la restauración y la consulta de historial del plan como capacidad de producto. Las demás decisiones de planificación de este ADR continúan vigentes.

## Decisión

### Ciclo de vida de grupos

Un grupo de planificación tendrá estado `active` o `inactive`, identificador estable y revisión monotónica. No se eliminará físicamente y podrá reactivarse. Administrador y entrenador elegirán el estado inicial al crearlo.

Un grupo `active` deberá tener al menos un segmento y contribuirá a la pertenencia efectiva y a las reservas `pending_reactivation`. Crearlo activo, reactivarlo o modificar su composición adquirirá la coordinación global, evaluará el estado final completo y rechazará cualquier solapamiento.

Un grupo `inactive` podrá no tener segmentos mientras se prepara. Conservará su composición, excepciones, borradores e historial, pero no producirá pertenencias efectivas ni reservará corredores. Administrador y entrenador podrán modificar su composición, crear planes y editar borradores. Los cambios serán hipotéticos y la reactivación recalculará todo contra el estado vigente; si aparece un conflicto, no se aplicará ninguna parte de la reactivación.

No podrán asociarse por primera vez segmentos inactivos, aunque una referencia creada cuando el segmento estaba activo continuará conservada y evaluable conforme a `ADR-0019`. Para activar un grupo seguirá siendo obligatorio que exista al menos una referencia de segmento; las inclusiones manuales no podrán convertirlo en una lista exclusivamente manual.

Un grupo inactivo no permitirá la primera publicación de un plan porque no tiene miembros efectivos. Sí permitirá republicar un plan ya publicado: `publication` reutilizará exactamente sus destinatarios congelados y no reactivará ni recalculará el grupo.

El nombre del grupo seguirá siendo obligatorio, único y normalizado según `ADR-0006`, con un máximo de `120` caracteres. La primera publicación de cada plan congelará el nombre visible del grupo para todas las versiones de ese plan. Renombrar después el grupo no marcará retrospectivamente esos planes como pendientes ni cambiará sus publicaciones.

### Reconfiguración multigrupo

Las modificaciones que puedan trasladar corredores entre grupos activos se expresarán mediante una reconfiguración multigrupo atómica. La entrada contendrá el estado final completo de todos los grupos que el actor pretende cambiar y la revisión esperada de cada uno. La operación:

1. autorizará al actor;
2. adquirirá la coordinación global de planificación;
3. bloqueará los grupos afectados en orden estable;
4. aplicará provisionalmente todos los estados finales;
5. evaluará corredores activos y reservas `pending_reactivation` contra todos los grupos activos;
6. registrará historial e impacto anterior y posterior;
7. confirmará todo o revertirá todo.

No existirán confirmación posterior, previsualización persistente ni resultados parciales. Una operación válida se aplicará inmediatamente y devolverá el impacto confirmado. Una revisión obsoleta, referencia inválida o conflicto rechazará el conjunto completo.

La reconfiguración permitirá activar y desactivar varios grupos dentro de la misma transacción, de modo que un traslado no deje un intervalo confirmado sin grupo ni una doble pertenencia. Los cambios de un único grupo usarán las mismas garantías.

### Reactivación de corredores y excepciones de grupo

Dar de baja a un corredor no restaurará posteriormente su pertenencia anterior. El último grupo se conservará solo como referencia administrativa conforme a `ADR-0018`.

Cada excepción de grupo quedará ligada al ciclo operativo revisado del corredor. Una excepción usada en un ciclo anterior será histórica y nunca recuperará efectividad de forma automática. Durante la revisión de reactivación, el administrador podrá crear o confirmar una inclusión o exclusión dormida para el nuevo ciclo. Mientras el corredor siga `inactive` no tendrá efecto. Antes de iniciar `pending_reactivation`, `planning` evaluará la configuración completa como si el corredor estuviera activo; si es válida, la excepción contribuirá a su reserva hipotética hasta aceptación, cancelación o caducidad. El entrenador no podrá localizar ni modificar corredores no activos.

### Ciclo de vida del plan y del entrenamiento

Solo podrán crearse planes para la semana local actual o una futura. La semana seguirá identificada por su lunes en la única zona IANA configurada para el club, inicialmente `Europe/Madrid`. Cambiar esa zona cuando existan planes requerirá una migración explícita y una decisión compatible con sus fechas históricas.

Un plan nunca publicado podrá cambiar de grupo o semana mientras respete las unicidades aceptadas. Después de su primera publicación, grupo y semana quedarán inmutables. El nombre podrá seguir cambiando en el borrador y será un cambio visible que exigirá republicación.

Un plan nunca publicado podrá eliminarse físicamente de forma manual. Un plan que haya tenido alguna versión publicada nunca se eliminará mediante la operación ordinaria de planificación y seguirá la retención de `ADR-0010`.

Un entrenamiento podrá moverse atómicamente entre dos planes solo si ninguno se publicó nunca. La operación bloqueará ambos planes, exigirá sus revisiones vigentes y rechazará un día ya ocupado. Después de publicar, la identidad del entrenamiento quedará fijada a su plan: retirarlo del borrador y crearlo en otro generará una identidad nueva y, cuando corresponda, exigirá republicación.

El plan podrá estar vacío o contener menos de siete días durante su preparación, pero cada entrenamiento confirmado será estructuralmente válido. La interfaz podrá mantener datos incompletos localmente; el backend no persistirá fases o bloques parciales. Cada plan tendrá una única revisión optimista: cualquier cambio del plan, entrenamiento o bloque la incrementará y una precondición obsoleta se rechazará sin sobrescritura.

Un plan publicado conservará su borrador de trabajo. Administrador o entrenador podrán descartar atómicamente todos los cambios pendientes y restaurar el contenido de la versión activa. `publication`, que posee esa versión y depende de `planning`, coordinará la restauración sin introducir una dependencia inversa. La restauración incrementará la revisión y quedará en el historial.

La duplicación de planes y las plantillas reutilizables quedan fuera del PMV y se registrarán como mejora futura.

### Modalidad, texto y magnitudes del entrenamiento

Cada entrenamiento tendrá modalidad explícita `presencial` o `en-linea`. El lugar de encuentro solo se admitirá para `presencial`, será opcional, de una sola línea y con un máximo de `300` caracteres. Su ausencia no generará un valor ficticio. La aclaración será texto plano opcional y multilínea de hasta `1.000` caracteres. Ambos campos usarán Unicode NFC; un valor compuesto solo por espacios será ausencia y no se admitirá HTML.

Las duraciones se introducirán en minutos y segundos y se normalizarán a segundos enteros. Cada duración válida estará entre `1` segundo y `360` minutos. Las distancias se introducirán en metros enteros o kilómetros con un máximo de tres decimales, se normalizarán exactamente a metros enteros y conservarán la unidad de presentación elegida. Cada distancia válida estará entre `1` metro y `100` kilómetros.

Un bloque tendrá de `1` a `100` repeticiones y un entrenamiento de `1` a `20` bloques principales. Cuando exista recuperación, se ejecutará después de cada repetición, incluida la última; el bloque repetirá la unidad completa `trabajo + recuperación` antes de continuar al siguiente bloque o al enfriamiento.

### Objetivos estructurados

Los objetivos no almacenarán frecuencia cardiaca máxima, zonas personales, marcas ni ritmos personales del corredor. La aplicación mostrará una instrucción relativa común para todos los destinatarios y no calculará un valor individual.

Un objetivo de `frecuencia-cardiaca` contendrá exactamente una clave de catálogo entre `Z1`, `Z2`, `Z3`, `Z4` y `Z5`. La aplicación no asociará porcentajes ni descripciones fisiológicas fijas; entrenador y corredor interpretarán la zona mediante sus referencias externas.

Un objetivo de `ritmo-relativo` contendrá:

- una distancia de referencia cerrada: `1-km`, `3-km`, `5-km`, `10-km`, `media-maraton` o `maraton`;
- una desviación mínima y máxima, ambas enteras entre `-60` y `+180` segundos por kilómetro;
- la regla `desviacionMinima <= desviacionMaxima`.

Una desviación positiva significa un ritmo más lento que la referencia y una negativa, más rápido. Extremos iguales expresan un objetivo exacto. La aplicación mostrará la fórmula y no pedirá ni derivará la marca real del corredor.

Todos los bloques de trabajo de una misma parte principal usarán una única familia de objetivo, aunque cada bloque podrá elegir su valor concreto. Cada recuperación `rodaje` elegirá independientemente su familia y objetivo. Las recuperaciones `parado` y `andando`, el calentamiento y el enfriamiento seguirán sin objetivo.

### Consulta e historial operativo

La consulta de un grupo mostrará, con paginación, sus miembros efectivos cuando esté activo o su proyección hipotética cuando esté inactivo. Cada fila explicará segmentos coincidentes, inclusión, exclusión y resultado. Los conflictos con reservas aplicarán la redacción de `ADR-0019`: el entrenador no recibirá la identidad de un corredor no activo y el administrador podrá verla solo en el flujo auditado.

La búsqueda paginada de planes admitirá prefijo de nombre, intervalo de semanas, grupo, estado y existencia de cambios pendientes de republicar. Esta consulta no habilitará la búsqueda global de corredores por etiquetas, segmentos o grupos aplazada en `MF-004`.

Cada mutación confirmada de grupos, planes, entrenamientos y bloques registrará de forma inmutable actor, instante, correlación, recurso, operación, revisiones y representaciones estructuradas completas anterior y posterior. Los intentos rechazados pertenecerán a observabilidad o seguridad y no crearán historial de negocio.

Administrador y entrenador podrán consultar este historial. El entrenador solo verá referencias personales de corredores actualmente activos; el administrador podrá consultar inactivos mediante acceso auditado. El historial no ofrecerá restauración arbitraria ni edición.

El historial operativo y los recursos confirmados de reconfiguración se conservarán `12` meses desde cada evento. La supresión anticipada o el vencimiento de la retención del corredor anonimizarán antes cualquier referencia personal que ya no pueda conservarse, incluidas excepciones de ciclos anteriores. Borradores y planes nunca tratarán datos reales mientras siga pendiente la revisión especializada de `ADR-0010`.

Un plan nunca publicado, junto con todo su historial de contenido, se purgará automáticamente `90` días después de terminar su semana. El job conservará durante `12` meses solo evidencia técnica mínima de la purga —identificador opaco, instante y resultado— sin nombre, ubicación, aclaraciones, estructura ni referencias personales.

## Alternativas consideradas

### Alternativa A: Borrado físico o reservas permanentes de grupos inactivos

Se descarta. El borrado pierde referencias e historia; mantener reservas convierte un grupo supuestamente inactivo en un bloqueo invisible para la operación activa.

### Alternativa B: Reconfigurar grupos mediante llamadas secuenciales

Se descarta porque un traslado tendría un intervalo confirmado sin grupo o requeriría tolerar doble pertenencia. Una primera publicación concurrente podría congelar destinatarios en ese estado intermedio.

### Alternativa C: Crear siempre grupos activos o siempre inactivos

Se descarta porque el responsable necesita ambos flujos. La elección explícita mantiene una única validación fuerte para el alta activa y un espacio seguro de preparación para el alta inactiva.

### Alternativa D: Permitir planes y entrenamientos parcialmente inválidos

Se descarta porque trasladaría combinaciones nulas a dominio, persistencia, publicación e historial. La construcción incremental se cubre con un plan vacío y formularios locales sin persistir recursos rotos.

### Alternativa E: Concurrencia por entrenamiento o última escritura gana

Se descarta para el PMV. Un plan tiene como máximo siete entrenamientos y se publica como unidad; una revisión global evita mezclas y pérdida silenciosa con menor complejidad.

### Alternativa F: Guardar referencias deportivas personales

Se descarta porque amplía perfil, privacidad y exactitud sin ser necesario para expresar instrucciones relativas. También contradice la minimización aceptada en `ADR-0018`.

### Alternativa G: Objetivos cardiacos por porcentaje o pulsaciones absolutas

Se descarta. Los porcentajes seguirían necesitando una máxima personal que la aplicación no posee; un intervalo absoluto no sería relativo ni adecuado como contenido común del grupo.

### Alternativa H: Ritmo libre, subjetivo o sin escala

Se descarta porque `RF-12` exige un formato estructurado y una escala verificable. Las distancias cerradas y desviaciones acotadas permiten validar y reproducir la instrucción sin almacenar la marca.

### Alternativa I: Historial parcial o retención uniforme del borrador caducado

Se descarta porque un diff obliga a reconstruir cadenas y conservar instantáneas después de purgar el borrador haría la eliminación meramente cosmética. La purga de los nunca publicados elimina conjuntamente contenido e historial.

### Alternativa J: Duplicación o plantillas en el PMV

Se descarta del alcance actual porque Fase 1 las clasifica como opcionales y exigirían decidir origen, destino, adaptación de semana, grupo y ciclo de vida de plantillas.

## Consecuencias

- Los grupos inactivos permiten preparar composición y planes sin afectar la operación, pero su reactivación puede ser rechazada por conflictos aparecidos mientras estaban dormidos.
- La reconfiguración multigrupo evita estados intermedios a costa de una operación y un bloqueo global más amplios.
- Elegir el estado inicial añade una rama visible de creación, pero evita altas activas accidentales.
- Publicar por primera vez exige grupo activo; republicar un grupo inactivo sigue siendo seguro porque no recalcula destinatarios.
- La revisión global del plan puede rechazar ediciones simultáneas de días distintos; se acepta para evitar una combinación no revisada antes de publicar.
- El objetivo cardiaco y el ritmo son verificables, pero la aplicación no personaliza valores ni garantiza que el corredor conozca sus referencias.
- Conservar la unidad de distancia mantiene la intención visible del entrenador y exige almacenar además un valor canónico exacto.
- El historial completo facilita investigación y responsabilidad, pero aumenta volumen y exposición; se limita a `12` meses y se excluye de logs y métricas.
- La purga a `90` días reduce borradores abandonados y elimina también su trazabilidad de contenido; solo queda evidencia técnica mínima.
- La modalidad explícita del entrenamiento evita depender de una clasificación dinámica, pero añade un campo obligatorio y una validación cruzada con la ubicación.

## Requisitos relacionados

- `RF-04`
- `RF-07`
- `RF-08`
- `RF-09`
- `RF-10`
- `RF-11`
- `RF-12`
- `RF-13`
- `RF-14`
- `RF-15`
- `RF-16`

## Decisiones de Fase 1 relacionadas

- `D-01`: los grupos materializan segmentos y excepciones antes de congelar destinatarios.
- `D-02`: la modalidad del corredor sigue siendo etiqueta; la modalidad del entrenamiento describe el contenido y no la sustituye.
- `D-04`: el lugar de encuentro permanece como texto libre y no se restringe al Retiro.
- `D-06`: cambios visibles de un plan publicado solo llegan al corredor mediante republicación.
- `D-08`: administrador y entrenador gestionan globalmente planificación; el corredor no accede a borradores.

## Validación prevista

- Probar creación activa e inactiva, grupo inactivo vacío, activación sin segmentos y reactivación con conflicto.
- Probar que un inactivo no genera miembros ni reservas y que una reconfiguración multigrupo traslada corredores sin estado intermedio confirmado.
- Ejecutar carreras entre reconfiguración, clasificación, reservas y primera publicación bajo la misma coordinación global.
- Probar primera publicación prohibida y republicación permitida cuando el grupo está inactivo.
- Probar configuración administrativa de excepciones dormidas antes de reactivar, aislamiento entre ciclos y redacción de conflictos para entrenador.
- Probar semana actual o futura, mutabilidad de grupo y semana antes de publicar e inmutabilidad posterior.
- Probar borrado de un plan nunca publicado, prohibición tras publicar y traslado de entrenamientos solo entre planes inéditos.
- Probar revisión única del plan, `If-Match`, restauración de la versión activa y ausencia de pérdida silenciosa.
- Probar entrenamientos siempre completos, tres fases, bloques, recuperación tras cada repetición y todos los límites numéricos.
- Probar modalidad explícita, rechazo de ubicación en `en-linea`, ausencia permitida en `presencial` y límites de texto.
- Probar `Z1` a `Z5`, distancias de referencia, desviaciones `-60..+180`, orden del intervalo y familias de trabajo y recuperación.
- Probar normalización exacta de minutos/segundos, metros/kilómetros y conservación de la unidad de presentación.
- Probar consulta explicada de miembros, búsqueda de planes, permisos e historial completo anterior/posterior.
- Probar retención de `12` meses, anonimización anticipada y purga conjunta de borrador e historial a los `90` días.
- Verificar con Spring Modulith y ArchUnit que `publication` depende de `planning` y nunca al revés.
- Revisar OpenAPI, Problem Details, idempotencia, cursores y recursos HTTP antes de implementar.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes dentro del diseño detallado de `planning`.

- **Tratado por `ADR-0021` propuesto:** el diseño de `publication` concreta la regla temporal y reemplaza la restauración por una sustitución atómica sin borrador persistente. La propuesta requiere validación humana antes de considerarse resuelta normativamente.
- Antes de implementar deberán producirse OpenAPI, migraciones Flyway, tipos jOOQ, catálogo de Problem Details, límites de página y reconfiguración medidos, y pruebas transaccionales con PostgreSQL.
- El tratamiento de datos personales reales y la producción continúan bloqueados hasta completar las evidencias de privacidad aplicables.
