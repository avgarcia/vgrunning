# ADR-0004: Autorización por roles y aislamiento de datos del corredor

**Estado:** Propuesto
**Fecha:** 2026-08-11
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV tiene tres roles operativos: administrador, entrenador y corredor. Fase 1 establece que el entrenador opera sobre todos los corredores, que el administrador accede a los datos operativos y administra usuarios, roles y taxonomías, y que cada corredor solo accede a sus propios planes, entrenamientos e información de seguimiento.

`ADR-0002` limita la aplicación a un único club y descarta relaciones de titularidad entre entrenadores y corredores. `ADR-0003` establece una única identidad local con un rol operativo y sesiones gestionadas por el servidor, pero separa expresamente autenticación de autorización.

Sin una política verificable, ocultar acciones en la interfaz o filtrar datos después de recuperarlos permitiría accesos directos por identificador y exposición entre corredores. La autorización debe aplicarse en el backend y preservar los límites de cada módulo sin introducir un motor genérico de permisos ni capacidades multiclub fuera de alcance.

## Decisión

El PMV aplicará control de acceso basado en roles con un conjunto cerrado de roles: `administrador`, `entrenador` y `corredor`. Cada cuenta activa tendrá exactamente un rol. No habrá permisos personalizados, acumulación de roles, jerarquías implícitas ni asignaciones de entrenador a corredores, segmentos o planes.

La política partirá de denegación por defecto. Cada caso de uso protegido declarará la acción permitida y el alcance de los recursos afectados. La autorización se comprobará en el backend antes de leer o modificar datos; ocultar controles en la interfaz será solo una consecuencia de la política y nunca su mecanismo de seguridad.

La matriz mínima de capacidades será la siguiente:

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Cuentas, invitaciones y roles | Gestión global | Sin acceso | Sin acceso |
| Definiciones de etiquetas y valores permitidos | Gestión global | Lectura necesaria para operar | Sin acceso administrativo |
| Corredores y asignaciones de etiquetas | Lectura global | Gestión global | Sin acceso directo en el PMV |
| Segmentos y excepciones manuales | Lectura global | Gestión global | Sin acceso |
| Planes y entrenamientos en borrador | Lectura global | Gestión global | Sin acceso |
| Publicación y republicación | Lectura global | Gestión global | Lectura solo de publicaciones propias |
| Seguimiento e historial | Lectura global | Lectura global para revisión | Lectura y escritura solo propias |

El rol administrador no heredará las capacidades de escritura del entrenador. La lectura global satisface el acceso operativo establecido en Fase 1, mientras que la separación de mutaciones conserva responsabilidades explícitas. Una persona que deba realizar trabajo de entrenador usará una cuenta con rol entrenador; ampliar una cuenta a varios roles requerirá reemplazar este ADR.

Identidad y acceso será responsable de resolver la cuenta autenticada, su rol vigente y su estado de activación. El rol no se copiará como autoridad permanente dentro de la sesión: cada operación usará el rol vigente, de modo que un cambio de rol tenga efecto en la siguiente solicitud sin esperar a que caduque la sesión.

Cada módulo será responsable de autorizar las acciones sobre los recursos que gobierna. Podrá reutilizar políticas comunes de rol, pero deberá comprobar localmente el alcance que depende de sus datos. Una llamada interna entre módulos no se considerará autorizada solo por proceder de otro módulo.

Una cuenta con rol corredor se vinculará a un único corredor. Esa relación se obtendrá de la identidad autenticada y nunca de un identificador de corredor elegido por el cliente. El aislamiento se aplicará con estas reglas:

- una lista para corredor se filtra en la consulta por el corredor autenticado;
- una publicación es visible si su instantánea de destinatarios efectivos incluye al corredor autenticado;
- un seguimiento solo puede leerse o modificarse por el corredor al que pertenece y debe referirse a un entrenamiento publicado para ese corredor;
- un corredor no puede consultar borradores, resultados actuales de segmentos ni publicaciones ajenas aunque conozca sus identificadores;
- una consulta individual a un recurso inexistente o ajeno devuelve el mismo resultado de no encontrado; la falta de una capacidad de rol independiente de un recurso concreto devuelve acceso denegado.

Los identificadores recibidos del cliente localizan candidatos, pero nunca conceden acceso. Las consultas globales del administrador y entrenador son intencionadas y no requieren una relación de titularidad por entrenador.

Solo un administrador puede asignar o cambiar roles. Se rechazará cualquier cambio que deje al sistema sin al menos una cuenta administradora activa. Cada cambio de rol registrará actor, cuenta afectada, valor anterior, valor nuevo y fecha, sin almacenar credenciales ni secretos.

La política se implementará en la capa de aplicación de cada módulo. Una futura base de datos podrá añadir controles como seguridad por fila como defensa adicional, pero estos no sustituirán la autorización de casos de uso ni serán requisito para aceptar este ADR.

## Alternativas consideradas

### Alternativa A: Autorización solo en rutas, controladores o interfaz

Se descarta porque las mismas operaciones pueden invocarse desde distintos puntos de entrada y porque ocultar una acción no impide acceder directamente a un recurso. Duplicaría condiciones y facilitaría omisiones al añadir nuevos casos de uso.

### Alternativa B: Permisos por usuario y asignación de entrenadores

Se descarta porque contradice `D-08`. El PMV no necesita asignar corredores, segmentos o planes a entrenadores concretos y añadir listas de control de acceso por recurso ampliaría modelo, administración y pruebas sin requisito funcional.

### Alternativa C: Administrador como superrol que hereda todas las escrituras

Se descarta porque mezclar administración de identidades con operación diaria oculta responsabilidades y amplía innecesariamente el impacto de una cuenta administradora comprometida. Fase 1 exige acceso global del administrador, pero no que publique planes ni modifique seguimiento.

### Alternativa D: Seguridad por fila en base de datos como mecanismo principal

Se descarta como mecanismo principal porque todavía no se ha elegido persistencia y porque no cubre por sí sola acciones, transiciones ni reglas entre módulos. Puede adoptarse después como control complementario.

## Consecuencias

- Las capacidades quedan acotadas y verificables sin introducir permisos personalizados ni relaciones de titularidad de entrenador.
- El aislamiento del corredor se aplica al construir consultas y validar acciones, no mediante filtrado posterior ni confianza en identificadores del cliente.
- El administrador conserva lectura global, pero necesita una cuenta con rol entrenador para realizar mutaciones operativas. Esta separación reduce privilegios y añade una restricción operativa deliberada.
- El permiso global del entrenador simplifica el PMV, pero aumenta el impacto de una cuenta de entrenador comprometida; autenticación segura, revocación y registro de cambios de rol son controles necesarios.
- Cada módulo debe mantener pruebas de autorización de sus casos de uso. Una política aplicada solo en el componente de identidad sería insuficiente.
- Una futura asignación de entrenadores, multiclub, permisos personalizados o cuentas con varios roles requerirá reemplazar este ADR y revisar el modelo de datos.

## Requisitos relacionados

- `RF-02`
- `RF-03`
- `RF-05`
- `RF-06`
- `RF-07`
- `RF-08`
- `RF-09`
- `RF-14`
- `RF-16`
- `RF-17`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-01`: la administración de taxonomías y la resolución de destinatarios requieren permisos explícitos.
- `D-03`: existe un único club y no se introduce aislamiento por organización.
- `D-06`: publicar y republicar son mutaciones operativas reservadas al entrenador.
- `D-07`: el corredor escribe su seguimiento y entrenador y administrador disponen de lectura global.
- `D-08`: el entrenador tiene permisos globales y el corredor queda aislado de los datos de otros corredores.

## Validación prevista

- Ejecutar pruebas de matriz que cubran cada combinación de rol, capacidad y resultado esperado, incluidas denegaciones por defecto.
- Probar acceso directo por identificador, listas, búsquedas y relaciones anidadas para confirmar que un corredor nunca observa datos, borradores ni seguimiento ajenos.
- Probar que administrador y entrenador pueden leer globalmente, pero solo el rol autorizado ejecuta cada mutación.
- Probar que los cambios de rol se aplican en la siguiente solicitud y que no es posible dejar el sistema sin una cuenta administradora activa.
- Verificar que una denegación por alcance de recurso no revela si un identificador ajeno existe.
- Revisar cada módulo para confirmar que sus casos de uso autorizan antes de leer o modificar y que las llamadas internas no eluden la política.
- Verificar el registro de cambios de rol sin credenciales, secretos ni datos operativos innecesarios.

## Decisiones pendientes

- **Pendiente, sin bloquear este ADR:** elegir el mecanismo concreto de políticas del framework. Responsable: revisor de arquitectura. Tratamiento: documentarlo al seleccionar el stack y conservar denegación por defecto, matriz y alcance por recurso.
- **Pendiente, sin bloquear este ADR:** decidir si la persistencia elegida añade seguridad por fila como defensa adicional. Responsable: revisor de arquitectura. Tratamiento: evaluarlo con la elección de base de datos sin sustituir controles de aplicación.
- **Bloqueante para producción, no para aceptar este ADR:** `ADR-0010` debe fijar retención y acceso al registro de cambios de rol. Responsable: responsable de privacidad o DPO. Tratamiento: resolverlo antes de producción.
