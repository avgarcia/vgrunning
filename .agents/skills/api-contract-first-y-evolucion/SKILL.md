---
name: api-contract-first-y-evolucion
description: Diseña o revisa operaciones HTTP públicas contract-first conforme a OpenAPI, ADR-0017 y la guía de API antes de implementarlas. Úsala al crear o cambiar recursos, operaciones, seguridad o compatibilidad HTTP; no para rutas técnicas ni recursos estáticos.
---

# API contract-first y evolución

Diseña el contrato antes del código. Un lint valida sintaxis; no decide si un supuesto recurso oculta una acción ni si la autorización o la compatibilidad son correctas.

## Contexto obligatorio

Antes de proponer o revisar una operación, lee:

- `AGENTS.md`.
- `docs/adr/0017-resource-oriented-http-api.md` y `docs/api-design-guidelines.md`.
- El requisito y criterio de aceptación afectados en `docs/phase-1-requirements.md` y `docs/phase-1-acceptance-criteria.md`.
- El diseño detallado del módulo propietario y los ADRs de autorización, privacidad o transacciones que afecten a la operación.
- `api/openapi/running-coach.yaml` cuando exista o cambie el contrato.

Si falta una decisión que pueda alterar el recurso, el modelo de datos, la seguridad o la compatibilidad, detente y declárala bloqueante. No inventes la operación ni aceptes una excepción arquitectónica.

## Revisión de una operación

Determina y documenta, como mínimo:

1. Recurso con identidad, representación y módulo propietario. No conviertas un caso de uso en una ruta RPC ni uses un sustantivo que sólo oculte una acción.
2. Ruta bajo `/api`, método, respuesta de éxito, Problem Details, parámetros, filtros y paginación. No uses prefijos de rol, nombres de módulos, secretos en URL ni `/v1` preventivo.
3. Actor autorizado, alcance del recurso, sesión y CSRF. La ruta, un UUID o un `accountId` enviado por el cliente nunca conceden acceso.
4. Idempotencia, concurrencia, precondiciones y efectos externos. Una repetición no debe volver a producir notificaciones u otros efectos; cuando exista pérdida de actualizaciones, exige una estrategia explícita.
5. Compatibilidad con consumidores existentes. Un cambio incompatible necesita transición explícita o ADR; no se disfraza como una corrección menor.

Actualiza OpenAPI antes de escribir el adaptador HTTP. Los tipos generados permanecen en los adaptadores HTTP; el contrato no expone entidades de dominio ni tipos de persistencia.

## Evidencia y límites

- Para cambios de contrato, ejecuta la validación dirigida (`validateOpenApi` o `apiCheck`) y registra el resultado. No declares que una prueba ha pasado sin salida y código de salida completos.
- Mantén la revisión humana obligatoria para semántica del recurso, autorización, idempotencia y compatibilidad.
- No crees un endpoint funcional, no modifiques un ADR aceptado y no uses datos reales salvo que la petición lo autorice expresamente.

## Entregable

Devuelve este informe, incluso si el resultado es bloquear la propuesta:

```markdown
## Revisión de API HTTP
- Estado: listo para revisión humana | requiere decisión | bloqueado
- Recurso y módulo propietario: <...>
- Contrato: <método, ruta, representaciones y estados HTTP>
- Seguridad y concurrencia: <actor, alcance, sesión/CSRF, idempotencia/ETag>
- Compatibilidad: <sin ruptura | transición o ADR requerido>
- Evidencia: <OpenAPI y validaciones ejecutadas>
- Hallazgos y acción requerida: <...>
```
