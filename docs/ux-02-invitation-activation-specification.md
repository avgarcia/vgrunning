# UX-02 — Especificación de invitación administrativa y activación inicial

**Estado:** Propuesta v0.1 — requiere revisión humana; no desbloquea F01.2 ni F01.3
**Fecha:** 2026-09-04
**Responsable de decisión:** Revisor de producto

## Propósito y límites

Esta propuesta define la experiencia de un administrador que inicia una invitación sintética y de una persona invitada que completa una activación inicial. Complementa [UX-01](ux-01-runner-portal-specification.md), que excluye activación y vistas administrativas. El [prototipo HTML autónomo](ux-02-invitation-activation-prototype.html) es el artefacto navegable de esta propuesta.

No autoriza implementación de producto, datos reales, un proveedor de correo, un shell administrativo, rutas cliente definitivas, cambios de OpenAPI ni el cierre de `RC-16`.

## Decisiones confirmadas

| Dimensión | Decisión |
| --- | --- |
| Dirección visual | `Rendimiento sereno` con `Atlántico sereno`. |
| Plataforma | Aplicación web responsive, mobile-first. |
| Idioma | Castellano. |
| Datos | Solo sintéticos. |
| Accesibilidad | Objetivo WCAG 2.2 AA. |
| Entradas | Invitación administrativa y activación separadas. |
| Contraseña | Dos campos, mostrar u ocultar cada valor y validación de coincidencia. |
| Sesión tras activación | No se inicia automáticamente; se dirige al acceso. |
| Estado de enlace no válido | Una sola respuesta pública para secreto inválido, invitación usada, caducada o reemplazada. |

Los textos de mayoría de edad son contenido sintético para revisar el flujo. No constituyen información jurídica aprobada.

## Arquitectura de información y flujos

```text
Entrada administrativa de revisión
└── Formulario de invitación
    ├── Validación de campos
    ├── Envío
    ├── Confirmación sintética
    ├── Error recuperable o correo reservado
    └── Sesión caducada

Enlace sintético de activación
└── Activar cuenta
    ├── Confirmación de mayoría de edad
    ├── Contraseña y repetición
    ├── Error de datos o guardado
    ├── Enlace no disponible
    └── Cuenta activa → inicio de sesión
```

El prototipo usa un selector externo de escenarios. No pertenece al producto, no representa navegación y no fija rutas cliente.

### Invitación administrativa

El formulario contiene `Nombre`, `Apellidos`, `Correo electrónico` y la declaración obligatoria «Confirmo que la persona invitada tiene 18 años o más». Mantiene etiquetas persistentes, ayuda visible y errores asociados al control; cuando hay varios errores, presenta además un resumen enfocable.

El envío muestra `Creando invitación…` y deshabilita la acción primaria mientras se procesa. La confirmación indica que la solicitud sintética está preparada y nunca muestra el secreto. Un correo ya reservado o un error recuperable conservan los valores introducidos y explican la siguiente acción sin revelar estado interno de identidad. Una sesión caducada sustituye el formulario por una explicación y la acción de iniciar sesión de nuevo.

### Activación inicial

El enlace entrega un secreto solo en el fragmento. El bootstrap lo retira mediante `history.replaceState` antes del primer render y no lo presenta, persiste, registra ni incluye en la maqueta como valor de contenido.

La pantalla pide la segunda confirmación de mayoría de edad, una contraseña y su repetición. Expone el requisito de `12..128` caracteres y evita revelar listas de contraseñas comprometidas o previsibles. Un error de coincidencia identifica ambos campos mediante texto. El estado genérico de enlace no disponible no diferencia causa y ofrece pedir una nueva invitación al administrador.

Al completar correctamente se informa de que la cuenta está activa y se ofrece ir al inicio de sesión. No se crea sesión ni se promete acceso inmediato a contenido protegido.

## Estados de revisión

| Actor | Estado | Mensaje o resultado visible |
| --- | --- | --- |
| Administrador | Campos inválidos | Errores por campo y resumen. |
| Administrador | Envío | Acción primaria ocupada y texto de progreso. |
| Administrador | Correcto | Invitación sintética creada, sin secreto. |
| Administrador | Correo reservado | Error recuperable, sin exponer datos de otra cuenta. |
| Administrador | Fallo recuperable | Valores conservados y reintento explícito. |
| Administrador | Sesión caducada | Retorno controlado al inicio de sesión. |
| Persona invitada | Datos inválidos | Confirmación, longitud, coincidencia o contraseña rechazada. |
| Persona invitada | Enlace no disponible | Respuesta pública indistinguible y solicitud de nueva invitación. |
| Persona invitada | Fallo recuperable | Valores visibles y reintento explícito. |
| Persona invitada | Correcto | Cuenta activa e ir al inicio de sesión. |

## Sistema visual y accesibilidad

Se reutilizan los tokens de [UX-01](ux-01-runner-portal-specification.md#sistema-visual-confirmado): canvas `#EEF3F3`, superficie `#FCFEFD`, texto `#17313D`, acción primaria `#164E63`, foco `#005FCC`, error `#A33131`, éxito `#2E6659` y borde de control `#6F8585`. La interfaz no comunica nunca un estado solo mediante color o icono.

La composición comienza en una columna y no exige un breakpoint fijo. Debe conservar reflow a `320 CSS px`, zoom al `400 %`, texto al `200 %`, objetivos de puntero de al menos `24 × 24 CSS px`, orden de teclado lógico, foco visible y encabezados/formularios semánticos. El resultado o resumen recibe el foco solo tras una acción que cambia el estado.

## Rutas y contrato futuro

La activación usa la ruta cliente estable `/activar#i=<invitationId>&s=<secret>`. El bootstrap retira el fragmento antes del primer renderizado y conserva sus valores únicamente en memoria. Esta decisión no diseña el shell ni fija una ruta administrativa global.

La propuesta conserva la siguiente trazabilidad con [OpenAPI](../api/openapi/running-coach.yaml):

| Slice futura | Operación prevista | Actor |
| --- | --- | --- |
| `RC-18` / F01.2 | `POST /api/runners` | Administrador autenticado con CSRF e `Idempotency-Key`. |
| `RC-19` / F01.3 | `POST /api/invitation-acceptances` | Anónimo con identificador y secreto solo en el cuerpo HTTPS y CSRF. |

Las operaciones, sus Problem Details y sus modelos se revisan contract-first antes de cada cambio compatible.

## Trazabilidad y criterio de salida

| Evidencia | Cobertura |
| --- | --- |
| `RF-01` y `CA-RF01-01` | Invitación, dos declaraciones de mayoría de edad, activación y acceso posterior. |
| `CA-RF01-02` | Datos inválidos, secreto no disponible y ausencia de activación parcial. |
| `RC-16` | Diseño de formulario, activación, estados, accesibilidad y revisión humana. |
| `RC-18` y `RC-19` | Alta administrativa, aceptación anónima y secreto limitado al fragmento y al cuerpo HTTPS. |

La v0.1 está lista para revisión cuando el prototipo y los seis recorridos cognitivos se puedan inspeccionar sin defectos técnicos bloqueantes. No constituye evidencia con personas usuarias ni autoriza cerrar `RC-16`.

## Revisiones documentales

### Revisión de criterios de aceptación

- Estado: listo para revisión humana
- Evidencia: `RF-01`, `CA-RF01-01`, `CA-RF01-02`, estados de invitación y activación de esta propuesta.
- Hallazgos: la respuesta pública indistinguible y el comportamiento posterior al éxito son observables; la ruta de activación ya está fijada.
- Acción requerida: revisar los textos y estados antes de validar la entrega implementada.
- Revisor humano: Revisores de producto y arquitectura.

### Revisión de preguntas bloqueantes

- Estado: requiere decisión
- Evidencia: ruta global administrativa y navegación posterior, excluidas de F01.2/F01.3.
- Hallazgos: `/activar` y el fragmento se han fijado; la navegación administrativa no condiciona la creación ni aceptación de invitaciones.
- Acción requerida: decidir el shell administrativo antes de incorporarlo a la SPA.
- Revisor humano: Revisor de producto.

### Revisión de decisiones de diseño

- Estado: listo para revisión humana
- Evidencia: entradas separadas, repetición de contraseña, no inicio automático de sesión y respuesta indistinguible.
- Hallazgos: las decisiones respetan los límites de `RF-01` y la seguridad de identidad; el secreto permanece limitado al fragmento eliminado durante el bootstrap y al cuerpo HTTPS.
- Acción requerida: aprobar, corregir o descartar la propuesta v0.1.
- Revisor humano: Revisor de arquitectura.
