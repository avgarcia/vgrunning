# Mejoras futuras

**Estado:** Backlog
**Fecha:** 2026-08-14

## Propósito

Registrar mejoras deliberadamente excluidas del PMV para que no reaparezcan como decisiones implícitas durante la implementación. Este documento no autoriza desarrollo, no altera los requisitos `RF-01` a `RF-20` y no sustituye un ADR cuando una mejora futura cambie la arquitectura aceptada.

## MF-001: MFA para cuentas privilegiadas

**Estado:** Aplazada fuera del PMV

### Problema

Las cuentas con rol `administrador` o `entrenador` tienen acceso global a los datos operativos. Mantener únicamente correo y contraseña aumenta el impacto de una credencial comprometida, especialmente porque los comentarios de seguimiento pueden contener datos de salud aunque el producto no los solicite.

### Decisión actual

El PMV no incorporará MFA. La autenticación seguirá la identidad local, contraseña, sesión opaca, límites de intentos y controles definidos por `ADR-0003`, `ADR-0013` y la línea base de seguridad de acceso.

Esta exclusión reduce alcance, pero no demuestra que el riesgo sea irrelevante. La cuenta privilegiada comprometida continúa siendo un riesgo aceptado del PMV y deberá incluirse en el análisis de riesgos y la EIPD de `ADR-0010`.

### Alternativas para una revisión futura

- TOTP mediante aplicación autenticadora, con secretos cifrados y códigos de recuperación de un solo uso.
- WebAuthn o passkeys para obtener mayor resistencia al phishing, con un flujo de recuperación y compatibilidad de dispositivos definidos.
- Segundo factor obligatorio solo para roles privilegiados o extensible de forma opcional al corredor.

### Criterios para reabrir la decisión

- La EIPD o una revisión especializada exige una protección adicional para cuentas privilegiadas.
- Se detecta abuso, credential stuffing, phishing o compromiso de una cuenta.
- Aumentan de forma relevante los usuarios privilegiados, el volumen de corredores o la sensibilidad de los datos tratados.
- Se introduce multiclub, una API pública, aplicaciones nativas, SSO o acceso de terceros.
- Aparece una exigencia contractual, aseguradora, regulatoria o de un cliente.

### Condiciones para incorporarla

Antes de implementar MFA deberá aprobarse un ADR que defina factor, enrolamiento, recuperación, revocación, dispositivos perdidos, bootstrap, soporte, almacenamiento de secretos, auditoría, UX y compatibilidad con sesiones existentes. También deberán actualizarse OpenAPI, modelo de datos, línea base de seguridad, privacidad y pruebas.

### Trazabilidad

- `RF-01`, `RF-02`
- `ADR-0003`, `ADR-0004`, `ADR-0010`, `ADR-0013`, `ADR-0015`
- [Línea base de seguridad de acceso](phase-2-access-security-baseline.md)

## MF-002: Reautenticación para operaciones sensibles

**Estado:** Aplazada fuera del PMV

### Problema

Una sesión privilegiada comprometida podría iniciar operaciones de alto impacto, como crear cuentas privilegiadas, cambiar correos o desactivar cuentas. Solicitar nuevamente la contraseña limita parte de ese riesgo, pero añade pasos y estados temporales a flujos administrativos poco frecuentes.

### Decisión actual

El PMV no exigirá reautenticación adicional ni una ventana de autenticación reciente. Las operaciones usarán la sesión normal, autorización de aplicación, auditoría y las confirmaciones por correo ya decididas. Se acepta el riesgo residual para evitar complejidad y fricción que no aportan valor suficiente en el alcance inicial.

### Criterios para reabrir la decisión

- Se detecta secuestro o uso indebido de una sesión privilegiada.
- Aumenta el número de administradores o entrenadores.
- Se incorporan operaciones administrativas más sensibles, MFA, cambios de rol o gestión multiclub.
- La EIPD, una auditoría o una revisión de seguridad exige autenticación reforzada.

### Condiciones para incorporarla

La revisión deberá definir operaciones afectadas, duración de la ventana, rotación de sesión, tratamiento de errores, recuperación y pruebas. La solución deberá integrarse con el mecanismo de autenticación vigente sin convertir la contraseña en un dato transportado o registrado fuera del adaptador de seguridad.

### Trazabilidad

- `RF-01`, `RF-02`
- `ADR-0003`, `ADR-0004`, `ADR-0010`, `ADR-0015`

## MF-003: Límite de sesiones simultáneas por cuenta

**Estado:** Aplazada fuera del PMV

### Problema

Permitir un número no acotado de sesiones activas por cuenta aumenta el volumen de sesiones persistidas y puede ampliar la superficie de una cuenta comprometida. Imponer un límite obliga a definir recuento, orden de expulsión, experiencia multidispositivo y comunicación al usuario.

### Decisión actual

El PMV no contará ni limitará las sesiones simultáneas. Cada inicio de sesión creará una sesión independiente. Cada sesión caducará tras `12` horas de inactividad o al alcanzar `7` días de duración absoluta, y los flujos de contraseña, correo y desactivación revocarán todas las sesiones cuando corresponda.

### Criterios para reabrir la decisión

- El volumen de sesiones persistidas produce un coste o degradación medible.
- Se detecta abuso mediante creación masiva de sesiones o reutilización de sesiones comprometidas.
- Se incorpora una pantalla de dispositivos, cierre remoto o alertas de nuevos accesos.
- Una revisión de seguridad exige limitar la exposición por cuenta.

### Condiciones para incorporarla

La revisión deberá definir límite por cuenta, tratamiento de empates y concurrencia, criterio de revocación, respuesta al usuario, auditoría y compatibilidad con sesiones existentes. El valor deberá ser configurable y sustentarse en métricas o riesgo, no en un número arbitrario.

### Trazabilidad

- `RF-01`
- `ADR-0003`, `ADR-0010`, `ADR-0012`, `ADR-0013`

## MF-004: Búsqueda de corredores por etiquetas y grupos

**Estado:** Aplazada fuera del PMV

### Problema

La búsqueda mínima de corredores del PMV usa nombre, apellidos y estado visible. Administrador y entrenador podrían necesitar posteriormente localizar corredores por valores de etiquetas, pertenencia efectiva a segmentos o grupo de planificación.

### Decisión actual

`runner-management` no incorporará filtros por etiquetas, segmentos o grupos. Esos datos pertenecen respectivamente a `classification-segmentation` y `planning`; añadir filtros mediante joins cruzados o duplicar su estado rompería los límites definidos por `ADR-0014`.

### Criterios para reabrir la decisión

- La búsqueda nominal no permite completar una tarea operativa frecuente o medible.
- El volumen de corredores, etiquetas o grupos hace inviable la selección actual.
- Existe el diseño detallado de los módulos propietarios y puede definirse una composición sin ciclos ni acceso SQL cruzado.

### Condiciones para incorporarla

La revisión deberá definir actor, filtros, semántica de pertenencia actual o histórica, paginación, consistencia, autorización y rendimiento. La solución compondrá APIs publicadas o una proyección explícita con propietario y actualización documentados; no trasladará taxonomías ni grupos a `runner-management`.

### Trazabilidad

- `RF-02`, `RF-03`, `RF-05`, `RF-06`, `RF-08`
- `ADR-0005`, `ADR-0006`, `ADR-0014`, `ADR-0015`
- [Diseño detallado de gestión de corredores](phase-2-detailed-design-runner-management.md)

## MF-005: Duplicación de planes y plantillas reutilizables

**Estado:** Aplazada fuera del PMV

### Problema

Administrador y entrenador podrían querer reutilizar una semana anterior como punto de partida o mantener plantillas independientes del calendario. Fase 1 clasifica la duplicación de planes como opcional y no existe un requisito imprescindible que obligue a incorporarla ahora.

### Decisión actual

El PMV no duplicará planes ni administrará plantillas. Cada plan se crea para un grupo y una semana concretos y se edita como borrador independiente. Esta exclusión evita decidir silenciosamente qué grupo, semana, modalidad, ubicación, objetivos o identidad de entrenamientos debe copiarse.

### Criterios para reabrir la decisión

- Crear manualmente semanas similares se convierte en una tarea frecuente y medible.
- El tiempo de preparación semanal justifica una operación adicional.
- Se necesita una biblioteca independiente de planes para estandarizar trabajo entre entrenadores.

### Condiciones para incorporarla

La revisión deberá decidir si se copia desde un borrador o publicación, grupo y semana de destino, tratamiento de fechas y ubicaciones, identidad nueva de planes y entrenamientos, adaptación de objetivos, concurrencia, permisos, historial, retención e idempotencia. Una plantilla con identidad y ciclo de vida propios requerirá un ADR y no se representará como un plan sin semana.

### Trazabilidad

- `RF-07`, `RF-11`, `RF-12`, `RF-14`
- `ADR-0006`, `ADR-0017`, `ADR-0020`
- [Diseño detallado de planificación](phase-2-detailed-design-planning.md)
