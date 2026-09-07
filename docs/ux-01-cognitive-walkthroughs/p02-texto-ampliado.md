# UX-01 — Recorrido cognitivo sintético P02

**Estado:** Ejecutado como revisión experta — no es una prueba con participante

**Fecha:** 2026-08-30

**Perfil sintético:** Corredor activo que utiliza ampliación y navegación por teclado

**Superficie:** Reflow a `320` píxeles y teclado

## Objetivo del perfil

Comprobar condiciones técnicas básicas que afectan a la ampliación y al teclado. No se atribuye uso real de lector de pantalla ni alto contraste a una persona sintética.

## Tareas recorridas

| Tarea | Criterios | Evidencia objetiva | Límite pendiente |
| --- | --- | --- | --- |
| `T-UX01-01` | `CA-UX01-01`, `CA-UX01-02` | El foco avanza de correo a contraseña y después a `Iniciar sesión`. | Falta revisión con lector de pantalla real. |
| `T-UX01-02` | `CA-UX01-03`, `CA-UX01-04` | A `320` píxeles no aparece desbordamiento horizontal. | Falta comprobar zoom `400 %` y texto `200 %`. |
| `T-UX01-03` | `CA-UX01-05`, `CA-UX01-06` | Controles visibles etiquetados y objetivos de al menos `24` píxeles. | Falta validar nombres accesibles con tecnología real. |
| `T-UX01-04` | `CA-UX01-07` | Información, rechazo y guardado permanecen operativos en columna. | Falta verificar lectura y anuncios en lector de pantalla. |
| `T-UX01-05` | `CA-UX01-09` | Historial y detalle conservan el orden de lectura. | Falta validar navegación extensa con ampliación real. |
| `T-UX01-06` | `CA-UX01-08` | Consecuencias y controles son texto visible, no solo color o icono. | Falta comprobar alto contraste del sistema. |
| `T-UX01-07` | `CA-UX01-12` | Menú y cierre de sesión son controles nativos. | Falta probar combinación completa solo con teclado. |

## Estados asignados

- `ES-UX01-03`: la sesión caducada vuelve al acceso y explica que hay que iniciar sesión de nuevo.
- `ES-UX01-05`: la semana futura aparece en lectura; no existe acción de seguimiento antes de la fecha.
- `ES-UX01-07`: `Sin seguimiento` y `Solo lectura` expresan la ventana cerrada mediante texto.

## Hipótesis pendientes

- la ampliación real podría aumentar el recorrido vertical;
- el alto contraste del sistema podría alterar tokens o bordes;
- los mensajes dinámicos podrían necesitar ajustes tras probar un lector de pantalla.

## Resultado

Recorrido técnico completado sin desbordamiento ni controles sin etiqueta. La compatibilidad con configuraciones y tecnologías de asistencia reales sigue sin validar.
