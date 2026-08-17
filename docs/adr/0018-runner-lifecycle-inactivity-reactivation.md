# ADR-0018: Ciclo de vida, inactividad y reactivación del corredor

**Estado:** Aceptado
**Fecha:** 2026-08-17
**Responsable de revisión:** Revisor de arquitectura
**Validación documental:** Aceptado explícitamente por el responsable el 2026-08-17; revisión especializada y tratamiento de datos reales pendientes

## Contexto

`runner-management` debe gobernar el perfil operativo del corredor y su vínculo uno a uno con la cuenta de `identity-access`, sin apropiarse de credenciales, correo, rol, taxonomías, grupos, publicaciones ni seguimiento. El diseño detallado de identidad ya exige que el alta de ambos recursos sea atómica y prohíbe cuentas de corredor huérfanas.

Las decisiones aceptadas contienen dos conflictos que impiden cerrar este diseño:

- `ADR-0004` concede al entrenador gestión global de corredores y asignaciones de etiquetas, mientras el diseño de alto nivel atribuye al administrador la gestión del perfil y `ADR-0004` reserva también al administrador la creación de cuentas. Un entrenador no puede crear atómicamente un corredor sin adquirir una capacidad administrativa prohibida.
- `ADR-0010` ordena suprimir cuenta, perfil y clasificación a los `30` días del fin de la relación, pero el responsable de producto necesita recuperar a corredores que regresan después de una o dos temporadas.

Además, una invitación caduca a las `72` horas, pero una cuenta `pending_activation` puede renovarse sin que exista hoy un límite total para el perfil provisional. Dejarla indefinidamente pendiente produciría una retención oculta.

Este ADR propone el ciclo de vida completo y las excepciones concretas que, si se aceptan, reemplazarán únicamente las decisiones incompatibles indicadas de `ADR-0004` y `ADR-0010`. El resto de ambos ADRs seguirá vigente.

## Decisión

### Perfil y propiedad

`runner-management` será propietario de un perfil con identificador estable, `givenName`, `familyName`, vínculo único con una cuenta de rol `corredor`, estado operativo, fechas de ciclo de vida y versión de concurrencia.

`givenName` y `familyName` serán obligatorios, no únicos y se conservarán como texto Unicode normalizado. Los homónimos se distinguirán por identificador estable. El correo seguirá perteneciendo exclusivamente a `identity-access` y no se duplicará en el perfil.

El PMV no incorporará al perfil teléfono, dirección, fecha de nacimiento, documento identificativo, frecuencia cardiaca, zonas personales, marcas ni ritmos de referencia. Los objetivos deportivos relativos se interpretarán fuera del perfil; incorporar referencias personales requerirá revisar alcance, privacidad y el diseño de planificación.

### Estados y elegibilidad

El ciclo de vida observable será:

| Estado | Significado | Elegibilidad operativa | Transiciones permitidas |
| --- | --- | --- | --- |
| `pending_activation` | Perfil y cuenta creados; la invitación todavía no se ha aceptado. | No aparece al entrenador ni participa en clasificación efectiva, grupos, publicaciones o notificaciones de planes. | `active`, `cancelled`. |
| `active` | Relación operativa vigente y cuenta activada. | Puede participar en clasificación, planificación, publicación, portal y seguimiento según permisos. | `inactive`. |
| `inactive` | Relación finalizada; acceso deshabilitado y datos conservados temporalmente. | Queda fuera de consultas ordinarias, segmentos efectivos, grupos y nuevas publicaciones. | `pending_reactivation`, supresión al vencer la retención. |
| `pending_reactivation` | Revisión administrativa completada y nueva activación de acceso pendiente. | Continúa fuera de la operación hasta completar la activación. | `active`, `inactive`. |
| `cancelled` | Alta nunca activada que fue cancelada manual o automáticamente. | Ninguna. | Supresión; una incorporación posterior crea otro perfil y otra cuenta. |

Solo `active` es elegible. La activación de identidad y el perfil deberán alcanzar un resultado coherente sin invertir la dependencia permitida: `runner-management` depende de la API publicada por `identity-access`, mientras identidad no consulta el esquema ni importa código del módulo de corredores. El diseño detallado definirá la coordinación exacta y sus pruebas de fallo.

### Permisos

La gestión del perfil se separará de la clasificación:

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Crear perfil y cuenta de corredor | Sí | No | No |
| Modificar nombre y apellidos | Sí | No | No |
| Dar de baja o iniciar reactivación | Sí | No | No |
| Consultar corredores activos | Sí | Sí | No de forma directa en el PMV |
| Consultar corredores inactivos | Sí, con acceso auditado | No | No |
| Gestionar etiquetas y excepciones operativas | Sí, mediante el módulo propietario | Sí, mediante el módulo propietario | No |

El entrenador conserva alcance global sobre corredores activos para operar clasificación, planificación y revisión, pero no administra identidades, datos identificativos ni el ciclo contractual. Esta precisión reemplazará la fila ambigua de corredores y asignaciones de `ADR-0004` si el ADR se acepta.

### Alta pendiente

El administrador creará perfil y cuenta de corredor dentro de una única transacción. La aceptación de la invitación determinará el inicio de la elegibilidad operativa.

`pending_activation` tendrá un máximo absoluto de `30` días desde el alta inicial. Reenviar la invitación sustituirá el secreto conforme al diseño de identidad, pero no reiniciará ese máximo. La cancelación manual o el vencimiento automático liberarán el correo, cancelarán cuenta y perfil y ejecutarán su supresión; solo las evidencias técnicas conservarán el plazo independiente de `ADR-0010`.

### Baja, conservación y reactivación

La baja administrativa deshabilitará inmediatamente el acceso, revocará sesiones y desafíos y cambiará el perfil a `inactive` dentro de una coordinación transaccional. Los demás módulos excluirán al corredor inactivo de sus conjuntos efectivos sin reescribir publicaciones ni seguimiento históricos.

Cuenta, perfil, vínculo, asignaciones de etiquetas y excepciones manuales de segmentos se conservarán automáticamente durante un máximo de `24` meses desde la baja. Solo el administrador podrá localizar y consultar el perfil inactivo. El corredor podrá solicitar supresión anticipada mediante el canal de privacidad. El plazo no se renovará por consultas, cambios técnicos ni actividad administrativa.

Planes publicados y seguimiento conservarán sus propios plazos desde la fecha de cada entrenamiento. La baja o reactivación no reiniciará esos plazos. El último grupo podrá conservarse únicamente como referencia administrativa durante su retención, nunca como pertenencia operativa restaurable.

La reactivación exigirá que el administrador revise nombre, apellidos, etiquetas, excepciones y compatibilidad con los grupos actuales. Los datos conservados serán un punto de partida, no una afirmación de vigencia. Tras la revisión, el perfil pasará a `pending_reactivation`; solo la activación correcta de la cuenta permitirá recuperar `active`. No se restaurará automáticamente una pertenencia antigua a grupo.

Al vencer los `24` meses, la política ejecutará supresión o anonimización irreversible por categoría y reaplicará las supresiones tras cualquier restauración de copia. Un regreso posterior será un alta nueva. El tratamiento automático de reactivación deberá aparecer en la información de privacidad desde el alta y comunicarse al dar de baja.

La conservación de `24` meses introduce una finalidad posterior al fin de la relación: facilitar una reactivación durante dos temporadas. Antes de tratar datos personales reales, una revisión especializada deberá confirmar su base jurídica, necesidad, proporcionalidad, información, derecho de oposición o supresión y acceso restringido. La conveniencia operativa no constituye por sí sola esa validación.

La aceptación de este ADR valida una decisión de arquitectura, no su conformidad jurídica ni su uso con personas reales. Hasta completar esa revisión, todos los entornos y pruebas usarán exclusivamente datos ficticios, sintéticos o anonimizados de forma irreversible; queda prohibido introducir, importar o copiar datos de corredores reales. Si la revisión no confirma la política, se mantendrán los `30` días vigentes en `ADR-0010` o se reemplazará este ADR antes de cualquier tratamiento real.

## Alternativas consideradas

### Alternativa A: Suprimir perfil y clasificación a los 30 días

Es la decisión vigente en `ADR-0010`. Minimiza retención, pero obliga a reconstruir perfil y clasificación cuando un corredor regresa después de una temporada y no satisface la necesidad confirmada por el responsable de producto.

### Alternativa B: Conservar durante 24 meses solo con confirmación del corredor

Se descarta como decisión de producto. Añade un flujo, comunicación y registro de preferencia que puede no completarse cuando la baja sea administrativa. No queda descartada como medida exigible si la revisión de privacidad concluye que la conservación automática carece de fundamento suficiente.

### Alternativa C: Conservar automáticamente durante 24 meses

Es la alternativa elegida como decisión de arquitectura. Restringe acceso y uso, conserva solo las categorías necesarias para reactivación, mantiene plazos históricos independientes y permite supresión anticipada. Su aplicación a datos personales reales queda condicionada a validación especializada.

### Alternativa D: Conservar indefinidamente a los corredores inactivos

Se descarta. Convertiría una posible vuelta en retención permanente, impediría una política automática verificable y contradiría minimización y limitación del plazo.

### Alternativa E: Permitir al entrenador gestionar el perfil y la baja

Se descarta. La creación necesita una cuenta reservada al administrador y la baja modifica acceso, relación y retención. Mezclar estas capacidades con la gestión deportiva amplía privilegios sin necesidad operativa.

## Consecuencias

- El perfil queda mínimo y separado de identidad, clasificación y datos deportivos personales.
- Solo los corredores con cuenta activada participan en la operación, por lo que no reciben planes que todavía no pueden consultar.
- La vuelta dentro de `24` meses recupera datos administrativos sin restaurar configuraciones potencialmente obsoletas de forma automática.
- La búsqueda ordinaria y los módulos consumidores deben excluir todos los estados distintos de `active` por construcción, no mediante filtrado posterior en memoria.
- La conservación automática incrementa exposición y coste de cumplimiento frente a `30` días; el tratamiento de datos personales reales y la producción quedan bloqueados hasta validar el fundamento y actualizar las evidencias de privacidad.
- El desarrollo puede materializar y probar el diseño únicamente con datos ficticios, sintéticos o anonimizados de forma irreversible mientras permanezca ese bloqueo.
- La precisión de permisos reduce privilegios del entrenador respecto a la lectura literal de `ADR-0004`, aunque conserva sus capacidades deportivas globales sobre corredores activos.
- Las tareas de caducidad deberán coordinar varios módulos sin acceso SQL cruzado y ser idempotentes, auditables y seguras ante reintentos.
- El historial puede haber vencido parcialmente cuando el corredor vuelva; reactivar no recupera datos ya suprimidos ni prolonga retroactivamente su retención.

## Requisitos relacionados

- `RF-01`
- `RF-02`
- `RF-03`
- `RF-16`
- `RF-17`
- `RF-18`
- `RF-19`
- Requisito no funcional de datos y privacidad de Fase 1

## Decisiones de Fase 1 relacionadas

- `D-01`: la clasificación dinámica y sus excepciones necesitan excluir corredores no elegibles sin perder trazabilidad histórica.
- `D-08`: el entrenador conserva alcance deportivo global, mientras el corredor queda aislado y el administrador gobierna perfil y ciclo de vida.

## Validación prevista

- Probar creación atómica de perfil y cuenta, rollback ante fallo de cualquier módulo y rechazo de cuentas o perfiles huérfanos.
- Probar todos los estados, transiciones, permisos y carreras entre activación, baja, publicación y reactivación.
- Probar que solo `active` participa en segmentos efectivos, grupos, destinatarios, notificaciones y portal.
- Probar cancelación manual, caducidad absoluta de `pending_activation` a los `30` días y que los reenvíos no prolongan el máximo.
- Probar baja inmediata de acceso, exclusión operativa y conservación histórica sin reescribir publicaciones.
- Probar acceso exclusivo y auditado del administrador a inactivos, incluidas listas, búsquedas y acceso directo por identificador.
- Probar revisión administrativa obligatoria y ausencia de restauración automática de grupos al reactivar.
- Probar el vencimiento de `24` meses, supresión anticipada, plazos históricos independientes, idempotencia y reaplicación tras restaurar copias.
- Probar con datos sintéticos que ningún entorno de desarrollo o prueba admite importaciones o copias de datos personales reales mientras permanezca el bloqueo.
- Revisar finalidad, base jurídica, proporcionalidad, información y derechos con Revisor de privacidad o DPO antes de tratar datos personales reales.
- Actualizar el inventario, registro de actividades, información de privacidad, EIPD y pruebas de retención antes de tratar datos personales reales o salir a producción.

## Decisiones pendientes

No quedan decisiones pendientes para aceptar este ADR. Permanecen estas condiciones y evidencias obligatorias:

- **Bloqueante antes de tratar datos personales reales y para producción:** confirmar mediante revisión especializada una base jurídica defendible y la necesidad y proporcionalidad de conservar automáticamente cuenta, perfil y clasificación durante `24` meses tras finalizar la relación. Responsable: responsable del tratamiento con Revisor de privacidad o DPO. Tratamiento: documentar la evaluación y aplicar esta política a datos reales solo si la confirma; en caso contrario, mantener los `30` días vigentes o reemplazar este ADR.
- **Restricción vigente mientras permanezca el bloqueo:** usar exclusivamente datos ficticios, sintéticos o anonimizados de forma irreversible en desarrollo y pruebas. Responsable: Revisor de arquitectura. Tratamiento: impedir importaciones y copias de datos de corredores reales y conservar esta restricción como criterio de revisión.
- **Resuelta por el responsable de producto:** la conservación será automática, no exigirá confirmación del corredor, tendrá un máximo no renovable de `24` meses y permitirá supresión anticipada.
- **Resuelta por el responsable de producto:** la reactivación exigirá revisión administrativa y no restaurará automáticamente el grupo anterior.
- **Resuelta por el responsable de producto:** nombre y apellidos estarán separados y no se almacenarán referencias deportivas personales.
