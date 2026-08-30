# UX-01 — Recorrido cognitivo sintético P04

**Estado:** Ejecutado como revisión experta — no es una prueba con participante

**Fecha:** 2026-08-30

**Perfil sintético:** Corredor activo acostumbrado a registrar información en papel o libreta

**Superficie:** Referencia de escritorio a `736` píxeles

## Objetivo del perfil

Explorar si la secuencia seleccionar, revisar y guardar tiene señales suficientes para alguien sin hábitos de formularios deportivos digitales.

## Tareas recorridas

| Tarea | Criterios | Evidencia objetiva | Pregunta cognitiva |
| --- | --- | --- | --- |
| `T-UX01-01` | `CA-UX01-01`, `CA-UX01-02` | El acceso abre `Mi plan` con semana y días visibles. | ¿Interpretará la semana sin buscar una agenda de papel? |
| `T-UX01-02` | `CA-UX01-03`, `CA-UX01-04` | El día de hoy está etiquetado con texto y abre el detalle. | ¿Entenderá que la tarjeta es interactiva? |
| `T-UX01-03` | `CA-UX01-05`, `CA-UX01-06` | Ningún valor está preseleccionado y existe confirmación tras `Guardar`. | ¿Reconocerá que seleccionar no equivale a guardar? |
| `T-UX01-04` | `CA-UX01-07` | Rechazar comentario mantiene los valores estructurados. | ¿Distinguirá nota opcional y registro principal? |
| `T-UX01-05` | `CA-UX01-09` | El historial muestra fecha, modalidad y estado textual. | ¿Comprenderá la agrupación semanal? |
| `T-UX01-06` | `CA-UX01-08` | Las tres consecuencias aparecen antes de confirmar. | ¿Podrá explicarlas con lenguaje propio? |
| `T-UX01-07` | `CA-UX01-12` | `Cerrar sesión` está separado de los destinos principales. | ¿Distinguirá sesión y cierre de ventana? |

## Estados asignados

- `ES-UX01-04`: el estado vacío contiene explicación y acceso explícito a la semana siguiente publicada.
- `ES-UX01-06`: el conflicto rechaza el guardado, conserva valores y ofrece recarga.
- `ES-UX01-03`: la sesión caducada vuelve al acceso con mensaje textual.

## Hipótesis pendientes

- puede esperar que marcar una opción guarde automáticamente;
- puede necesitar mayor persistencia visual del botón `Guardar` en recorridos largos;
- puede preferir una representación más parecida a una agenda.

Calendario y autoguardado permanecen fuera de la decisión confirmada.

## Resultado

Recorrido técnico completado a `736` píxeles sin desbordamiento. La comprensión del guardado explícito sigue pendiente de beta.
