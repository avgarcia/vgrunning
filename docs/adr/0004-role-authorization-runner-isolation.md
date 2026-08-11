# ADR-0004: Autorización por roles y aislamiento de datos del corredor

**Estado:** Propuesto
**Fecha:** 2026-08-11
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV tiene tres roles operativos: administrador, entrenador y corredor. Fase 1 establece que el entrenador opera sobre todos los corredores, que el administrador accede a los datos operativos y administra usuarios, roles y taxonomías, y que cada corredor solo accede a sus propios planes, entrenamientos e información de seguimiento.

`ADR-0002` limita la aplicación a un único club y descarta relaciones de titularidad entre entrenadores y corredores. `ADR-0003` establece una única identidad local con un rol operativo y sesiones gestionadas por el servidor, pero separa expresamente autenticación de autorización.

Sin una política verificable, ocultar acciones en la interfaz o filtrar datos después de recuperarlos permitiría accesos directos por identificador y exposición entre corredores. La autorización debe aplicarse en el backend y preservar los límites de cada módulo sin introducir un motor genérico de permisos ni capacidades multiclub fuera de alcance.

## Decisión

El PMV aplicará control de acceso basado en roles con un conjunto cerrado de roles: `administrador`, `entrenador` y `corredor`. Cada cuenta tendrá exactamente un rol, asignado al crearla e inmutable desde ese momento. No habrá permisos personalizados, acumulación de roles ni asignaciones de entrenador a corredores, segmentos o planes.

La política partirá de denegación por defecto. Cada caso de uso protegido declarará la acción permitida y el alcance de los recursos afectados. La autorización se comprobará en el backend antes de leer o modificar datos; ocultar controles en la interfaz será solo una consecuencia de la política y nunca su mecanismo de seguridad.

La matriz mínima de capacidades será la siguiente:

| Capacidad | Administrador | Entrenador | Corredor |
| --- | --- | --- | --- |
| Cuentas, invitaciones y rol inicial | Gestión global | Sin acceso | Sin acceso |
| Definiciones de etiquetas y valores permitidos | Gestión global | Lectura necesaria para operar | Sin acceso administrativo |
| Corredores y asignaciones de etiquetas | Gestión global | Gestión global | Sin acceso directo en el PMV |
| Segmentos y excepciones manuales | Gestión global | Gestión global | Sin acceso |
| Planes y entrenamientos en borrador | Gestión global | Gestión global | Sin acceso |
| Publicación y republicación | Gestión global | Gestión global | Lectura solo de publicaciones propias |
| Seguimiento e historial | Lectura global | Lectura global para revisión | Lectura y escritura solo propias |

El rol administrador heredará todas las capacidades del entrenador y añadirá la gestión de cuentas, invitaciones, roles iniciales, definiciones de etiquetas y valores permitidos. Esta jerarquía evita que una misma persona necesite dos cuentas para administrar y entrenar. El entrenador no hereda capacidades administrativas y el corredor no hereda capacidades de los otros roles.

Identidad y acceso será responsable de resolver la cuenta autenticada, su rol inmutable y su estado de activación. La sesión identificará la cuenta y cada operación obtendrá de ella el rol asignado; no se aceptará un rol enviado por el cliente ni una operación para modificarlo.

Cada módulo será responsable de autorizar las acciones sobre los recursos que gobierna. Podrá reutilizar políticas comunes de rol, pero deberá comprobar localmente el alcance que depende de sus datos. Una llamada interna entre módulos no se considerará autorizada solo por proceder de otro módulo.

Una cuenta con rol corredor se vinculará a un único corredor. Esa relación se obtendrá de la identidad autenticada y nunca de un identificador de corredor elegido por el cliente. El aislamiento se aplicará con estas reglas:

- una lista para corredor se filtra en la consulta por el corredor autenticado;
- una publicación es visible si su instantánea de destinatarios efectivos incluye al corredor autenticado;
- un seguimiento solo puede leerse o modificarse por el corredor al que pertenece y debe referirse a un entrenamiento publicado para ese corredor;
- un corredor no puede consultar borradores, resultados actuales de segmentos ni publicaciones ajenas aunque conozca sus identificadores;
- una consulta individual a un recurso inexistente o ajeno devuelve el mismo resultado de no encontrado; la falta de una capacidad de rol independiente de un recurso concreto devuelve acceso denegado.

Los identificadores recibidos del cliente localizan candidatos, pero nunca conceden acceso. Las consultas globales del administrador y entrenador son intencionadas y no requieren una relación de titularidad por entrenador.

Solo un administrador puede crear o invitar cuentas y asignar su rol inicial. Una invitación pendiente con un rol incorrecto debe cancelarse y sustituirse por una cuenta nueva antes de la activación. El rol de una cuenta activada no se puede cambiar ni migrar en el PMV; si una persona asume otro rol necesitará otra cuenta y otro correo. Cualquier operación futura de cancelación o desactivación deberá impedir que el sistema quede sin al menos una cuenta administradora activa.

La asignación inicial de rol registrará actor, cuenta afectada, rol y fecha, sin almacenar credenciales ni secretos. Esta inmutabilidad restringe deliberadamente el criterio de aceptación original de `RF-02`, que contemplaba modificar el rol de un usuario. El cambio queda registrado en Fase 2 y deberá reflejarse en sus pruebas: se acepta asignar el rol al crear la cuenta y se rechaza modificarlo después.

La política se implementará en la capa de aplicación de cada módulo. Una futura base de datos podrá añadir controles como seguridad por fila como defensa adicional, pero estos no sustituirán la autorización de casos de uso ni serán requisito para aceptar este ADR.

## Alternativas consideradas

### Alternativa A: Autorización solo en rutas, controladores o interfaz

Se descarta porque las mismas operaciones pueden invocarse desde distintos puntos de entrada y porque ocultar una acción no impide acceder directamente a un recurso. Duplicaría condiciones y facilitaría omisiones al añadir nuevos casos de uso.

### Alternativa B: Permisos por usuario y asignación de entrenadores

Se descarta porque contradice `D-08`. El PMV no necesita asignar corredores, segmentos o planes a entrenadores concretos y añadir listas de control de acceso por recurso ampliaría modelo, administración y pruebas sin requisito funcional.

### Alternativa C: Separar completamente administrador y entrenador

Se descarta porque una misma persona necesitaría dos cuentas y, por la unicidad del correo, dos direcciones diferentes para administrar y entrenar. El administrador ya puede acceder globalmente a los datos y gestionar identidades, por lo que impedirle las mutaciones del entrenador añade fricción sin reducir de forma relevante el impacto de una cuenta comprometida.

### Alternativa D: Seguridad por fila en base de datos como mecanismo principal

Se descarta como mecanismo principal porque todavía no se ha elegido persistencia y porque no cubre por sí sola acciones, transiciones ni reglas entre módulos. Puede adoptarse después como control complementario.

### Alternativa E: Permitir cambios de rol

Se descarta para el PMV. Aunque facilitaría promociones, correcciones y cambios de responsabilidad, obligaría a definir migración de relaciones, efecto sobre sesiones y conservación del historial bajo una identidad que cambia de capacidades. El PMV fija el rol al crear la cuenta y trata cualquier evolución posterior como una identidad distinta.

## Consecuencias

- Las capacidades quedan acotadas y verificables sin introducir permisos personalizados ni relaciones de titularidad de entrenador.
- El aislamiento del corredor se aplica al construir consultas y validar acciones, no mediante filtrado posterior ni confianza en identificadores del cliente.
- El administrador puede realizar todas las operaciones del entrenador. Esto simplifica la operación, pero amplía el impacto de una cuenta administradora comprometida y exige aplicar con rigor los controles de `ADR-0003`.
- El permiso global del entrenador simplifica el PMV, pero aumenta el impacto de una cuenta de entrenador comprometida; autenticación segura y revocación de sesiones son controles necesarios.
- Un rol incorrecto solo puede corregirse cancelando una invitación antes de activarla. Una cuenta activada no puede promocionarse, degradarse ni reutilizar su correo para otro rol; esta es una restricción funcional deliberada.
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
- Probar que el administrador puede ejecutar todas las operaciones del entrenador y que entrenador y corredor no pueden ejecutar capacidades administrativas.
- Probar que el rol solo se asigna al crear una cuenta, que cualquier intento posterior de modificarlo se rechaza y que una invitación pendiente incorrecta se puede cancelar sin activar la cuenta.
- Probar que no es posible dejar el sistema sin una cuenta administradora activa mediante operaciones de ciclo de vida disponibles.
- Verificar que una denegación por alcance de recurso no revela si un identificador ajeno existe.
- Revisar cada módulo para confirmar que sus casos de uso autorizan antes de leer o modificar y que las llamadas internas no eluden la política.
- Verificar el registro de la asignación inicial de rol sin credenciales, secretos ni datos operativos innecesarios.

## Decisiones pendientes

- **Pendiente, sin bloquear este ADR:** elegir el mecanismo concreto de políticas del framework. Responsable: revisor de arquitectura. Tratamiento: documentarlo al seleccionar el stack y conservar denegación por defecto, matriz y alcance por recurso.
- **Pendiente, sin bloquear este ADR:** decidir si la persistencia elegida añade seguridad por fila como defensa adicional. Responsable: revisor de arquitectura. Tratamiento: evaluarlo con la elección de base de datos sin sustituir controles de aplicación.
- **Bloqueante para producción, no para aceptar este ADR:** `ADR-0010` debe fijar retención y acceso al registro de asignaciones iniciales de rol. Responsable: responsable de privacidad o DPO. Tratamiento: resolverlo antes de producción.
