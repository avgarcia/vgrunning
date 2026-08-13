# Línea base de seguridad de acceso — Fase 2

**Estado:** Aceptado
**Fecha:** 2026-08-11

## Propósito

Concretar los parámetros de seguridad que materializan [ADR-0003](adr/0003-identity-authentication-invitation.md). Esta línea base no decide el proveedor de correo, framework, persistencia ni plataforma de despliegue.

## Credenciales

| Control | Decisión verificable |
| --- | --- |
| Contraseña | Entre 12 y 128 caracteres. Se permiten todos los caracteres imprimibles ASCII, incluido el espacio, y caracteres Unicode, incluidos números. Antes de comprobarla y almacenarla se normaliza en NFC. No se exige rotación periódica ni reglas artificiales de composición. |
| Rechazo de contraseñas | Se rechaza una lista versionada de contraseñas comunes o comprometidas y valores contextuales previsibles del PMV. La comprobación no registra ni expone la contraseña introducida. |
| Almacenamiento | Argon2id con al menos 19 MiB de memoria, 2 iteraciones y paralelismo 1. Los parámetros almacenados permiten aumentar el coste y rehash al iniciar sesión. |
| Intentos de inicio | Máximo 5 fallos por cuenta y 20 por IP en 15 minutos; al superar cualquiera se retrasa o rechaza el intento sin revelar cuál fue el límite. |
| CAPTCHA | No se habilita por defecto. Solo podrá añadirse como defensa adicional tras evidencia de abuso que los límites anteriores no contengan; antes de ello se documentarán proveedor o solución, accesibilidad, tratamiento de datos y umbral de activación. |
| Respuesta de acceso | El inicio y la recuperación devuelven mensajes genéricos ante correo, contraseña o estado inválidos. |

## Secretos de activación y recuperación

| Control | Decisión verificable |
| --- | --- |
| Generación | El CSPRNG del sistema operativo genera 32 bytes aleatorios. El secreto se codifica en `base64url` exclusivamente para incluirlo en el enlace. |
| Almacenamiento | Solo se conserva el verificador `SHA-256` del secreto, propósito, caducidad y estado de uso; el secreto en claro aparece únicamente en el enlace enviado. |
| Activación | Caduca a las 72 horas. Solo el último secreto emitido para la cuenta es válido. |
| Recuperación | Caduca a la hora. Solo el último secreto emitido para la cuenta es válido. |
| Solicitudes | Máximo 3 solicitudes por cuenta y 10 por IP en una hora; la respuesta no revela la existencia de la cuenta. |
| Cambio de contraseña | No inicia sesión automáticamente e invalida todas las sesiones activas de la cuenta. |
| Vistas con secreto | Las páginas de activación y recuperación usan `Referrer-Policy: no-referrer` y no cargan recursos ni analítica de terceros. |

## Sesiones y solicitudes de cambio de estado

| Control | Decisión verificable |
| --- | --- |
| Identificador | El CSPRNG del sistema operativo genera 32 bytes aleatorios, codificados en `base64url` para la cookie. El servidor guarda exclusivamente el verificador `SHA-256` de la sesión. |
| Cookie | Nombre `__Host-pmv_session`, `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/` y sin atributo `Domain`. |
| Caducidad | 12 horas de inactividad y 7 días de duración absoluta, aplicadas por el servidor. |
| Rotación y revocación | Rotar el identificador al iniciar sesión y tras reautenticación; cerrar sesión revoca la sesión actual; cambiar contraseña revoca todas las sesiones de la cuenta. |
| Roles | La sesión identifica a la cuenta, pero no congela su rol ni permisos. Cada operación consulta las reglas vigentes definidas por `ADR-0004`. |
| CSRF | Las operaciones que cambian estado exigen un token anti-CSRF vinculado a la sesión y validan el origen de la solicitud cuando el navegador lo envía. |

## Primer administrador

El despliegue provisiona una única cuenta pendiente con rol administrador mediante un comando de bootstrap disponible solo en el entorno de despliegue. El comando recibe únicamente el correo, crea el mismo flujo de activación por invitación y rechaza una segunda ejecución cuando ya existe una cuenta de bootstrap, activa o pendiente. No acepta, muestra ni registra una contraseña inicial.

## Validación de salida para acceso

- Probar todos los límites, caducidades, invalidaciones y mensajes genéricos definidos en esta línea base.
- Medir en el entorno elegido el coste de Argon2id y no habilitar acceso si no cumple los parámetros mínimos.
- Verificar atributos de cookie, protección anti-CSRF, ausencia de secretos en respuestas, URL de redirección, registros o analítica.
- Revisar esta línea base cuando cambie la plataforma, antes de producción y ante un incidente de seguridad.
