# ADR-0003: Identidad, autenticación, invitación y recuperación de acceso

**Estado:** Propuesto
**Fecha:** 2026-08-11
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV necesita que el administrador invite corredores por correo, que estos activen una cuenta con correo y contraseña, puedan iniciar sesión y recuperen su contraseña. No existe registro público, aplicaciones nativas, SSO, inicio de sesión social ni requisitos de multiclub.

`ADR-0002` establece una aplicación web única con backend modular y una frontera transaccional común. La identidad debe aportar una persona autenticada y una sesión segura sin asumir qué operaciones permite cada rol; la autorización por rol y el aislamiento del corredor pertenecen a `ADR-0004`.

El diseño no ha elegido framework, persistencia, proveedor de correo ni plataforma. La elección debe permitir gestionar credenciales y secretos de forma segura sin exponerlos al navegador ni almacenar contraseñas o tokens en texto claro.

## Decisión

El PMV usará identidad local basada en correo electrónico y contraseña. El administrador crea o invita la cuenta; no habrá autorregistro, SSO ni inicio de sesión social en el PMV.

Cada cuenta tendrá un correo comparado sin distinción de mayúsculas, único dentro del único club, un estado de activación y un rol operativo. La asignación y comprobación de permisos del rol no se decide aquí y queda en `ADR-0004`.

Una invitación crea o reutiliza una cuenta pendiente de activación y envía un enlace con un secreto aleatorio de un solo uso. La activación válida el secreto, permite fijar la contraseña y activa la cuenta. Reenviar una invitación invalida el secreto de activación anterior. No se crearán cuentas duplicadas para un correo ya activo.

La recuperación de contraseña usa un secreto separado, aleatorio y de un solo uso. La solicitud responde de forma indistinguible para correos existentes o inexistentes y no revela si una cuenta está registrada. Una solicitud nueva invalida el secreto de recuperación anterior de esa cuenta.

Los secretos de activación y recuperación solo se enviarán al correo del usuario y se almacenarán como valores verificables no reversibles, con propósito, fecha de expiración y estado de uso. Una contraseña se almacenará mediante un algoritmo de derivación de contraseña adaptativo; se preferirá Argon2id cuando la plataforma elegida lo soporte. Los plazos de expiración, la política de contraseñas, límites de intentos y parámetros de coste se configurarán y revisarán antes de implementar acceso.

El inicio de sesión creará una sesión opaca gestionada por el servidor. El navegador la recibirá exclusivamente en una cookie `Secure`, `HttpOnly` y `SameSite=Lax`; no se expondrá un token de acceso al código del navegador. Las operaciones que cambien estado deberán protegerse contra solicitudes forjadas. El modelo de almacenamiento, la duración y revocación de sesiones se concretarán con la plataforma antes de implementar acceso.

## Alternativas consideradas

### Alternativa A: Proveedor externo de identidad, SSO o inicio de sesión social

Se descarta para el PMV. No existe una necesidad funcional de federación y añadiría elección de proveedor, costes, dependencias de disponibilidad y flujos de vinculación de cuentas antes de validar el funcionamiento interno del club.

### Alternativa B: Registro público con correo y contraseña

Se descarta porque contradice el alta controlada por administrador de `RF-01` y `RF-02`. Permitirá cuentas ajenas al club y obligaría a resolver validación y administración de acceso fuera de alcance.

### Alternativa C: Tokens de acceso portadores expuestos al navegador

Se descarta para la primera aplicación web. Complica revocación, tratamiento ante cierre de sesión y protección frente a exposición en código cliente sin aportar una necesidad de API pública o aplicaciones nativas.

## Consecuencias

- El PMV controla el ciclo de vida de las cuentas y puede vincularlo a la administración de usuarios sin depender de un proveedor externo.
- Las invitaciones y recuperaciones son verificables, de un solo uso y caducan; requieren persistir estado y solicitar envío de correo.
- La sesión gestionada por el servidor simplifica la revocación y evita exponer credenciales de acceso al código del navegador, pero exige proteger cookies y solicitudes que cambian estado.
- No se implementará autorregistro, SSO, inicio de sesión social ni una API de autenticación para clientes nativos como parte del PMV.
- Los controles de autorización no se sustituyen por autenticar una cuenta. Cada operación protegida deberá aplicar `ADR-0004`.

## Requisitos relacionados

- `RF-01`
- `RF-02`
- `RF-16`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-03`: existe una única organización operativa y no se necesita aislamiento por club.
- `D-08`: los roles y el permiso global del entrenador son premisas de Fase 1; su comprobación técnica y el aislamiento del corredor se decidirán en `ADR-0004`.

## Validación prevista

- Probar que una invitación válida permite activar una cuenta, fijar contraseña e iniciar sesión; una invitación caducada, usada o reemplazada se rechaza sin activar la cuenta.
- Probar que una recuperación válida permite cambiar la contraseña una vez y que la respuesta a correos inexistentes no revela la existencia de una cuenta.
- Verificar que no se almacenan ni devuelven contraseñas, secretos de activación, secretos de recuperación ni identificadores de sesión en texto claro.
- Verificar que las cookies de sesión aplican los atributos acordados y que las operaciones que cambian estado rechazan solicitudes sin protección contra falsificación.
- Probar que autenticar una cuenta no concede por sí mismo acceso a datos u operaciones fuera de las reglas que defina `ADR-0004`.

## Decisiones pendientes

- **Bloqueante para implementar acceso:** `ADR-0004` debe definir autorización por rol, comprobación de permisos y aislamiento del corredor. Responsable: revisor de arquitectura. Tratamiento: aceptarlo antes de habilitar cualquier operación autenticada.
- **Bloqueante para implementar correo de acceso:** seleccionar proveedor, configuración de entrega y observabilidad para invitación y recuperación. Responsable: revisor de arquitectura. Tratamiento: documentarlo con la plataforma elegida antes de implementar el envío; no se reutiliza implícitamente `ADR-0008`, que cubre correo de publicación.
- **Bloqueante para desplegar acceso:** provisionar el primer administrador sin usar registro público. Responsable: revisor de arquitectura. Tratamiento: definir el mecanismo seguro con la plataforma de despliegue y exigir activación o cambio de credencial inicial antes de uso operativo.
- **Bloqueante para implementar acceso:** fijar política de contraseñas, expiración de secretos, límites de intentos, duración y revocación de sesiones conforme a la plataforma. Responsable: revisor de arquitectura. Tratamiento: registrarlo en el diseño técnico de acceso antes de implementar; si cambia alcance o varios módulos, abrir un ADR específico.
- **Pendiente, sin bloquear este ADR:** elegir framework, persistencia y plataforma de despliegue. Responsable: revisor de arquitectura. Tratamiento: mantener la decisión compatible con sesiones opacas, secretos verificables y cookies seguras.
