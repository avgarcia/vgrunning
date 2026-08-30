# UX-01 — Recorrido cognitivo sintético P05

**Estado:** Ejecutado como revisión experta — no es una prueba con participante

**Fecha:** 2026-08-30

**Perfil sintético:** Corredor activo con baja competencia digital

**Superficie:** Referencia móvil y reflow a `320` píxeles

## Objetivo del perfil

Revisar si los controles dependen de convenciones digitales implícitas o si las acciones y estados principales disponen de texto suficiente.

## Tareas recorridas

| Tarea | Criterios | Evidencia objetiva | Pregunta cognitiva |
| --- | --- | --- | --- |
| `T-UX01-01` | `CA-UX01-01`, `CA-UX01-02` | Los campos tienen etiquetas y el botón dice `Iniciar sesión`. | ¿Distinguirá correo, contraseña y acción principal? |
| `T-UX01-02` | `CA-UX01-03`, `CA-UX01-04` | `Hoy`, modalidad y ubicación aparecen como texto. | ¿Sabrá que la tarjeta abre más información? |
| `T-UX01-03` | `CA-UX01-05`, `CA-UX01-06` | Las opciones usan palabras además de valores numéricos. | ¿Comprenderá esfuerzo y sensación sin explicación adicional? |
| `T-UX01-04` | `CA-UX01-07` | `Continuar sin comentario` expresa la alternativa. | ¿Entenderá que rechazar no cancela todo el seguimiento? |
| `T-UX01-05` | `CA-UX01-09` | `Historial`, `Ver detalle` y estados son etiquetas textuales. | ¿Encontrará el destino en la navegación superior? |
| `T-UX01-06` | `CA-UX01-08` | La retirada enumera consecuencias antes de la confirmación. | ¿Resulta comprensible `Privacidad de comentarios`? |
| `T-UX01-07` | `CA-UX01-12` | El menú incluye `Cerrar sesión` con texto. | ¿Lo distinguirá de abandonar el navegador? |

## Estados asignados

- `ES-UX01-05`: la semana futura expresa `Solo lectura` y ausencia temporal de seguimiento.
- `ES-UX01-07`: la tarjeta indica `Sin seguimiento` y el detalle explica que la ventana está cerrada.
- `ES-UX01-08`: el recurso no disponible usa un mensaje genérico sin causa técnica.

## Hipótesis pendientes

- los conceptos esfuerzo, sensación y privacidad podrían necesitar ayuda contextual;
- la posición del botón principal podría pasar desapercibida en recorridos largos;
- los mensajes de error podrían interpretarse como una acción incorrecta de la persona.

No se asigna severidad: ninguna hipótesis se observó en una persona ni produjo un defecto técnico reproducible.

## Resultado

Recorrido técnico completado a `320` píxeles sin desbordamiento ni controles sin etiqueta. La comprensión real es el principal riesgo pendiente.
