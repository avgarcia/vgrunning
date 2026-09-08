# ADR-0031: Delegación de supresión, eventos y operación en el proveedor de correo

**Estado:** Aceptado
**Fecha:** 2026-09-08
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0010](0010-privacy-retention-rights-readiness.md), [ADR-0011](0011-transactional-email-delivery-strategy.md), [ADR-0012](0012-relational-persistence-transaction-strategy.md) y [ADR-0016](0016-deployment-platform-operations.md)

## Contexto

El diseño detallado de `notification-delivery` (`phase-2-detailed-design-notification-delivery.md`) modela 7 tablas (`notification_request`, `notification_attempt`, `delivery_event_inbox`, `delivery_transition`, `suppressed_destination`, `delivery_control`, `delivery_operation_audit`), cifrado AEAD con rotación de claves en Key Vault para destino y payload, supresión propia por huella HMAC versionada, una ventana de reconciliación de resultados inciertos con búsqueda activa, y comandos operativos auditados para reanudar el worker o reactivar un destino.

El módulo no tiene código ni migraciones propias más allá de la tabla `notification_delivery.notification_request` creada en `V004__create_runner_invitation_activation_tables.sql` (solo para el tipo `invitation`, con `status` restringido a `pending`). El worker, el adaptador de Brevo, el webhook y las cinco tablas restantes son papel: simplificar ahora no exige migración de datos.

El proveedor elegido en `ADR-0011` es Brevo, un ESP (email service provider) que ya gestiona de forma nativa buena parte de lo que el diseño reimplementa:

- **Supresión:** Brevo bloquea automáticamente un contacto transaccional tras rebote duro, queja o desuscripción, y expone esa lista mediante `GET /v3/smtp/blockedContacts` y `DELETE /v3/smtp/blockedContacts/{email}`.
- **Idempotencia:** el envío admite un `idempotencyKey` (UUID) para deduplicar solicitudes repetidas.

Manteniendo una implementación propia de ambas capacidades, el propio diseño anterior reconoce una trampa operativa: *"Una versión de clave HMAC no puede retirarse mientras existan supresiones calculadas con ella, porque no se conserva la dirección necesaria para recalcularlas"*.

Además, `identity_access.account_email` (creada en `V003`) guarda el correo del corredor **en claro**. El diseño anterior cifra igualmente el destino de cada solicitud de notificación con AEAD, cifrando una copia del mismo dato que ya está sin cifrar en la misma base de datos.

## Decisión

El esquema `notification_delivery` se reduce a 2 tablas: `notification_request` (outbox, evoluciona la de `V004`) y `notification_attempt` (bitácora única de intentos y eventos recibidos, con `UNIQUE (provider_event_id) WHERE provider_event_id IS NOT NULL` para deduplicar webhooks).

Se retiran, y se sustituyen así:

- **`suppressed_destination` → `GET /v3/smtp/blockedContacts` de Brevo.** No hay comprobación local antes de enviar: si el destino está suprimido, Brevo rechaza el envío y el resultado se clasifica `fallo-definitivo/destino-suprimido` (con red de seguridad en el evento `blocked` del webhook). La reactivación se hace en la consola o API de Brevo, no mediante comando propio.
- **La ventana de reconciliación con búsqueda activa → reintento con la misma clave de idempotencia.** Un resultado incierto (timeout, `5xx`, conexión perdida tras transmitir) se reintenta con el mismo `idempotencyKey` dentro de una ventana de 30 minutos desde el primer envío (`first_transmit_at`). Cerrada la ventana sin respuesta concluyente, se cierra como `fallo-definitivo/resultado-desconocido` sin más reintentos, porque la clave de idempotencia ya habría dejado de proteger frente a un duplicado físico.
- **`delivery_control` (pausa persistida) → circuit breaker en memoria del proceso.** Válido mientras el PMV corra en un único nodo, con la misma cláusula de alcance que `ADR-0025` fijó para Bucket4j. Un fallo global se registra como intento con `outcome='global-error'` sin incrementar `attempt_count`, preservando la garantía de `ADR-0011` de que un fallo global no consume intentos individuales.
- **`delivery_event_inbox` + `delivery_transition` → proyección directa en una transacción corta.** El webhook, tras autenticar, correlaciona por referencia de mensaje o etiqueta opaca, inserta la fila de bitácora con `ON CONFLICT (provider_event_id) DO NOTHING` y proyecta el estado con `UPDATE ... WHERE status_rank < :newRank`, todo en la misma transacción que confirma antes de responder `204`.
- **`delivery_operation_audit` → sin sustituto.** Las dos operaciones que auditaba dejan de ser mutaciones propias de la aplicación.
- **Cuatro calendarios de reintento diferenciados → `expires_at` + `max_attempts` con un backoff exponencial único.** Para publicación, `expires_at = created_at + 120 minutos` unifica el horizonte de reintentos con el máximo absoluto de elegibilidad que ya fijaba `ADR-0021`.
- **Cifrado AEAD del destino → destino en claro.** El payload sigue cifrado: `access_challenge` persiste solo el verificador del secreto, así que el payload de la outbox es el único lugar del sistema con un secreto portador vigente en forma explotable durante horas.

## Alternativas consideradas

### Alternativa A: Mantener las 7 tablas y los mecanismos propios del diseño original

Se descarta. Reimplementa capacidades que el proveedor ya presta, con el coste adicional confesado por el propio diseño (la clave HMAC que no puede retirarse).

### Alternativa B: Delegar supresión pero conservar la inbox de eventos

Se descarta como intermedio innecesario: la inbox no protegía nada que una transacción corta de proyección directa no proteja igual, y mantenerla exige justificar dos tablas para un problema que una resuelve.

### Alternativa C: Mantener AEAD también en el destino

Se descarta. El destino ya está en claro en `identity_access.account_email`, en la misma base de datos y bajo el mismo cifrado en reposo: cifrar una segunda copia no protege frente a ningún adversario realista y solo añade dependencia de ciclo de vida de claves sobre filas de 90 días de vida.

### Alternativa D: Modelo de 2 tablas con delegación en el proveedor (elegida)

Conserva las garantías que son responsabilidad exclusiva de la aplicación (creación transaccional, orden, elegibilidad, prioridad, cifrado del payload) y delega en Brevo lo que ya es su función.

## Consecuencias

- **Impacto positivo:** de 7 a 2 tablas; elimina la rotación de clave HMAC y su trampa operativa; elimina un bloqueante de producción («Huella de supresión»); reduce `domain/suppression/` e `infrastructure/input/command/` del paquete del módulo.
- **Riesgo y coste:** la evidencia de 90 días de eventos huérfanos se pierde (pasan a métrica); una migración de proveedor arrastra la reputación acumulada en Brevo, mitigable exportando `GET /v3/smtp/blockedContacts` antes de migrar; la pausa global no sobrevive a un reinicio de proceso.
- **Deuda aceptada, parcialmente reducida por documentación oficial (2026-09-08):** la guía de idempotencia de Brevo (`developers.brevo.com/docs/heterogenous-versions-batch-emails`, pese a titularse para envío por lotes) documenta el ejemplo con `curl` contra `POST /v3/smtp/email` —el mismo endpoint de envío individual que usa este proyecto— con un header `idempotencyKey` de TTL `30` minutos: repetir la misma clave dentro de ese plazo devuelve un error `duplicate_parameter` en lugar de procesar la solicitud de nuevo. La referencia formal del endpoint (`developers.brevo.com/reference/sendtransacemail`) no documenta ese comportamiento ni el código de estado HTTP exacto del error. La semántica sigue sin confirmarse por prueba propia porque persiste la duda concreta: si el error se devuelve *antes* de encolar el segundo envío físico (deduplicación real) o después (mera detección tardía). Si la prueba sintética muestra que el envío físico duplicado ocurre igualmente, la política pasa a fallo cerrado (ver diseño detallado, sección *Reintentos y resultados inciertos*) sin requerir otro ADR.
- Esta decisión **no** refina `ADR-0008` ni `ADR-0021`: la creación transaccional de una solicitud por versión y destinatario, el orden por plan-destinatario-versión, el puerto de elegibilidad y la ventana de 120 minutos de elegibilidad quedan intactos.

## Requisitos relacionados

- `RF-01`, `RF-15`, `RF-20`

## Decisiones de Fase 1 relacionadas

- `D-06`: republicación atómica y congelación de correo, sin cambio de comportamiento observable.

## Validación prevista

- Prueba sintética contra Brevo (entorno de pruebas) que confirme si `idempotencyKey` suprime el envío físico duplicado en `POST /v3/smtp/email`, antes de habilitar producción.
- Probar que un destino suprimido en Brevo produce `fallo-definitivo/destino-suprimido` sin consulta local previa.
- Probar la proyección monotónica por `status_rank` ante eventos duplicados y desordenados, en una única transacción con la deduplicación por `provider_event_id`.
- Probar que un fallo global no incrementa `attempt_count` y que la pausa se levanta automáticamente al reiniciar el proceso.
- Ejecutar `ApplicationModules.verify()`/ArchUnit y comprobar la ausencia de `domain/suppression/`.

## Decisiones pendientes

- **Bloqueante para producción:** confirmar con prueba sintética contra Brevo real si el header `idempotencyKey` en `POST /v3/smtp/email` suprime el envío físico duplicado (no solo si la segunda llamada devuelve un error `duplicate_parameter`, que ya está documentado) y registrar el código de estado HTTP exacto de ese error para que el adaptador lo distinga de un fallo real. Responsable: Revisor de arquitectura. Tratamiento: si no suprime el duplicado físico, activar la política de fallo cerrado antes de habilitar Brevo con datos reales.
- **Bloqueante para implementar `V005`:** la migración `V005__evolve_notification_request_outbox.sql` y el cambio del puerto `NotificationRequestApi` (4 ficheros en `src/main/java/com/vgrunning/notificationdelivery/`) no se ejecutan como parte de esta revisión documental. Responsable: Revisor de arquitectura. Tratamiento: implementarlos junto con el worker y el adaptador de Brevo, después de resolver la prueba de `idempotencyKey` — varios de los valores nuevos (`expires_at`, `max_attempts`, `correlation_tag`) dependen de esa política y de datos (como el TTL del desafío de invitación) que hoy no llegan a `ProvisionRunnerAccountService`. Escribir el esquema antes de esos dos hechos obligaría a adivinar valores en la única ruta de código ya construida y probada del módulo.
