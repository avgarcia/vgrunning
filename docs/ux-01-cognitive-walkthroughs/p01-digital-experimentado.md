# UX-01 — Recorrido cognitivo sintético P01

**Estado:** Ejecutado como revisión experta — no es una prueba con participante

**Fecha:** 2026-08-30

**Perfil sintético:** Corredor activo con experiencia en planes y aplicaciones deportivas

**Superficie:** Referencia móvil

## Objetivo del perfil

Explorar si una persona acostumbrada a aplicaciones deportivas encuentra los recorridos principales y dónde podrían entrar en conflicto sus expectativas de sincronización o autoguardado. Las expectativas son hipótesis, no respuestas observadas.

## Tareas recorridas

| Tarea | Criterios | Evidencia objetiva | Pregunta cognitiva |
| --- | --- | --- | --- |
| `T-UX01-01` | `CA-UX01-01`, `CA-UX01-02` | El acceso desemboca en `Mi plan` y muestra la semana actual. | ¿Reconocerá el destino sin buscar un dashboard? |
| `T-UX01-02` | `CA-UX01-03`, `CA-UX01-04` | `Hoy` y el detalle muestran entrenamiento, modalidad y lugar. | ¿Entenderá que la tarjeta completa abre el detalle? |
| `T-UX01-03` | `CA-UX01-05`, `CA-UX01-06` | El seguimiento no preselecciona valores y exige `Guardar`. | ¿Esperará autoguardado o sincronización externa? |
| `T-UX01-04` | `CA-UX01-07` | Rechazar el comentario conserva el guardado estructurado. | ¿Distinguirá comentario opcional y datos estructurados? |
| `T-UX01-05` | `CA-UX01-09` | `Historial` abre el viernes anterior con estado textual. | ¿Buscará filtros o métricas que no pertenecen a UX-01? |
| `T-UX01-06` | `CA-UX01-08` | La retirada explica y aplica tres consecuencias. | ¿Comprenderá que no se recuperan comentarios eliminados? |
| `T-UX01-07` | `CA-UX01-12` | `Cerrar sesión` vuelve al acceso. | ¿Distinguirá salir de cerrar el menú? |

## Estados asignados

- `ES-UX01-02`: el acceso permanece visible y muestra un error genérico sin revelar la cuenta.
- `ES-UX01-04`: la semana vacía explica que no existe un plan publicado y enlaza la siguiente semana disponible.
- `ES-UX01-06`: el conflicto conserva tres selecciones locales y ofrece `Recargar entrenamiento`; no confirma guardado.

## Hipótesis pendientes

- puede esperar integración con reloj o plataforma deportiva;
- puede interpretar `Guardar` como paso innecesario frente al autoguardado;
- puede demandar métricas adicionales.

Estas propuestas permanecen fuera de alcance y no reciben severidad sin evidencia humana o defecto reproducible.

## Resultado

Recorrido técnico completado. No se detectó un defecto experto bloqueante. Comprensión y expectativas reales pendientes de beta.
