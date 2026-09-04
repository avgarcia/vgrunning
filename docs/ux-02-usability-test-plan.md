# UX-02 — Plan de prueba de usabilidad v0.1

**Estado:** Simulación experta prevista — no equivale a investigación con personas usuarias
**Ámbito:** Propuesta UX-02 v0.1, datos y enlaces sintéticos

## Objetivo

Comprobar que los recorridos de invitación y activación son navegables, comprensibles y accesibles antes de la revisión humana de UX-02. No se reclutan participantes ni se recogen datos personales en esta versión.

## Tareas principales

| ID | Actor | Consigna neutral | Éxito observable |
| --- | --- | --- | --- |
| `T-UX02-01` | Administrador | «Invita a una persona al club.» | Completa nombre, apellidos, correo y declaración; recibe confirmación sin secreto. |
| `T-UX02-02` | Administrador | «Corrige los datos necesarios para continuar.» | Encuentra errores, comprende la declaración y recupera el envío. |
| `T-UX02-03` | Persona invitada | «Completa la activación de tu cuenta.» | Confirma mayoría de edad, crea y repite contraseña válida, y llega al acceso. |
| `T-UX02-04` | Persona invitada | «El enlace no funciona. ¿Qué harías?» | Comprende el mensaje genérico y pide una nueva invitación. |
| `T-UX02-05` | Ambos | «Realiza el recorrido solo con teclado o ampliación.» | Conserva foco, orden, etiquetas y acciones sin depender del color. |

## Estados rotatorios

| Estado | Resultado exigido |
| --- | --- |
| Campos inválidos | Errores asociados y resumen. |
| Correo reservado | Error recuperable sin datos de otra cuenta. |
| Error de envío | Datos conservados y reintento explícito. |
| Sesión caducada | Explicación y retorno al acceso. |
| Contraseña rechazada | Explicación segura sin lista interna. |
| Contraseñas distintas | Error textual asociado. |
| Enlace no disponible | Un solo mensaje público para todas las causas. |
| Error de activación | Datos conservados y reintento explícito. |

## Perfiles y cobertura

| Registro | Perfil simulado | Tareas | Estados |
| --- | --- | --- | --- |
| `P01` | Administración digital experimentada, escritorio | 01, 02, 05 | correo reservado, error de envío, sesión caducada |
| `P02` | Administración con competencia digital baja, móvil | 01, 02 | campos inválidos, correo reservado, sesión caducada |
| `P03` | Administración con teclado, zoom y alto contraste | 01, 05 | campos inválidos, error de envío, sesión caducada |
| `P04` | Persona invitada, móvil habitual | 03, 04, 05 | contraseña distinta, enlace no disponible, error de activación |
| `P05` | Persona invitada con competencia digital baja | 03, 04 | contraseña rechazada, enlace no disponible, contraseña distinta |
| `P06` | Persona invitada con ampliación y teclado | 03, 04, 05 | contraseña rechazada, enlace no disponible, error de activación |

Cada estado crítico aparece al menos dos veces. Los perfiles son herramientas de revisión, no personas reales ni datos demográficos.

## Comprobaciones técnicas

- Reflow a `320 CSS px`, zoom `400 %` y texto `200 %`.
- Orden de teclado, foco visible, retorno al resumen o resultado y ausencia de trampas de foco.
- Etiquetas persistentes, ayuda, errores asociados y mensajes de estado textuales.
- Objetivos de puntero mínimos de `24 × 24 CSS px`.
- Sin desplazamiento horizontal no esencial ni errores de consola.
- El fragmento sintético desaparece antes de renderizar; no aparece en DOM visible, almacenamiento ni consola.

## Registro y decisión

Cada recorrido documenta resultado, hallazgos, severidad y estado probado. Un hallazgo `S1` bloquea la revisión; `S2` exige corrección antes de repetir el recorrido; `S3` y `S4` se priorizan por impacto.

La revisión humana debe separar evidencia, interpretación y decisión de producto. La decisión de rutas cliente permanece bloqueante para UX-02 v0.2 y para el cierre de `RC-16`.
