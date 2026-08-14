# ADR-0016: Plataforma de despliegue y operación

**Estado:** Propuesto
**Fecha:** 2026-08-13
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0002` define una única aplicación desplegable y `ADR-0013` concreta un ejecutable Spring Boot que contiene API, SPA y worker. `ADR-0012` exige PostgreSQL como única persistencia primaria, migraciones Flyway previas al arranque y garantías relacionales que no pueden degradarse por la plataforma. `ADR-0010` obliga a identificar regiones, encargados, copias, telemetría y transferencias antes de tratar datos reales.

La escala prevista, superior a `500` corredores y con picos iniciales inferiores a `100` usuarios concurrentes, no justifica operar Kubernetes ni construir una plataforma propia. Sí requiere un proceso siempre activo para el worker, PostgreSQL administrado, despliegues repetibles, recuperación probada y observabilidad suficiente para una única persona operadora.

Esta decisión debe minimizar operación sin fingir que una PaaS elimina la responsabilidad sobre migraciones, capacidad, seguridad, copias o incidentes. Un plan gratuito, una base sin recuperación o un despliegue directo no controlado desde `main` no son aceptables para producción.

## Decisión

### Plataforma, región y topología

La propuesta para el PMV es **Render** en la región `Frankfurt`, con todos los recursos persistentes y de aplicación del mismo entorno en esa región:

- un Render Web Service de pago que ejecuta una imagen Docker `linux/amd64` con backend, SPA y worker;
- una instancia Render Postgres de pago con PostgreSQL `18`;
- red privada entre aplicación y base mediante la URL interna;
- dominio propio y TLS administrado por Render, deshabilitando el subdominio `onrender.com` en producción;
- ningún disco persistente adjunto al servicio de aplicación.

Producción comenzará con una instancia de aplicación y una base sin réplica de lectura. No habrá autoscaling ni alta disponibilidad de PostgreSQL hasta que el objetivo de servicio, el coste aceptado o las métricas lo justifiquen. La outbox ya admite varios workers mediante leases, pero ninguna tarea programada podrá asumir instancia única: deberá ser idempotente o adquirir coordinación en PostgreSQL antes de habilitar escalado horizontal.

La aplicación usará HikariCP contra la conexión PostgreSQL directa de la red privada. No se habilitará PgBouncer inicialmente porque existe un único pool acotado y no hay presión de conexiones medida. Adoptarlo exigirá verificar que no se depende de estado de sesión, tablas temporales, `LISTEN/NOTIFY` o advisory locks de sesión.

### Entornos

Existirán cuatro contextos aislados:

| Entorno | Ejecución | Datos | Servicios externos |
| --- | --- | --- | --- |
| Local | Docker Compose y aplicación local | Sintéticos | Dobles o sandbox |
| CI | Testcontainers efímeros | Generados por prueba | Dobles |
| Staging | Proyecto y recursos propios en Render Frankfurt | Sintéticos o anonimizados irreversiblemente | Sandbox, sin correo real a corredores |
| Producción | Proyecto protegido en Render Frankfurt | Reales | Proveedores aprobados |

Staging y producción no compartirán base, credenciales, environment groups, dominio, cookies ni destinos de correo. No se clonará producción hacia entornos no productivos. Los preview environments no se crearán automáticamente; solo podrán habilitarse bajo demanda con datos sintéticos y límites de coste.

### Artefacto, infraestructura y promoción

GitHub Actions ejecutará los gates de `ADR-0013`, construirá una única imagen OCI reproducible desde un Dockerfile multi-stage, generará SBOM, analizará vulnerabilidades y publicará la imagen en GHCR con etiqueta de commit y digest inmutable. Una imagen con vulnerabilidad crítica explotable no podrá promocionarse sin una excepción temporal documentada, responsable y fecha de caducidad.

El mismo digest se desplegará primero en staging y después en producción. La promoción a producción será manual mediante un GitHub Environment protegido y requerirá:

1. CI completa superada;
2. migraciones verificadas desde una base vacía y desde la versión productiva anterior;
3. despliegue y smoke tests correctos en staging;
4. revisión del diff de migraciones y plan de rollback;
5. aprobación explícita de la persona operadora.

`render.yaml` declarará recursos, región, health check, comandos, configuración no secreta y cierre ordenado. Los identificadores de plan podrán variar sin cambiar este ADR si conservan las capacidades y objetivos decididos. Los valores secretos no se incluirán en el Blueprint, repositorio, imagen, logs ni artefactos de CI.

Flyway se ejecutará como comando `pre-deploy` desde la misma imagen antes de arrancar la nueva revisión. Las migraciones deberán ser compatibles con la versión anterior durante el solapamiento del despliegue: se aplicará estrategia expandir-migrar-contraer y las eliminaciones incompatibles se harán en una entrega posterior. Solo una ejecución de migración podrá actuar sobre cada base.

Un rollback reutilizará el digest anterior. No habrá migraciones Flyway descendentes ni restauración automática de la base para revertir código. Si una migración no es compatible hacia atrás, el despliegue quedará bloqueado hasta dividirla. Una corrupción o pérdida de datos seguirá el procedimiento de recuperación, no el rollback ordinario de aplicación.

### Arranque, salud y cierre

El contenedor escuchará en el puerto indicado por la plataforma y no conservará estado en su sistema de archivos efímero. Spring Boot Actuator expondrá un endpoint de salud mínimo sin datos sensibles. El health check de Render comprobará que el proceso está listo para aceptar tráfico, pero no incluirá dependencias externas cuya caída provocaría reinicios en bucle.

PostgreSQL, Brevo y la cola se comprobarán mediante indicadores separados. El despliegue solo se considerará correcto después de smoke tests autenticados y no autenticados sobre rutas críticas.

La aplicación responderá a `SIGTERM`, dejará de aceptar trabajo nuevo, drenará peticiones y detendrá el worker sin perder leases. El tiempo de cierre de la plataforma será superior al timeout máximo de las operaciones normales y nunca mayor que el lease sin una justificación probada.

### Secretos y acceso operativo

Los secretos de runtime se almacenarán en variables o secret files de Render, separados por entorno. GitHub Actions usará permisos mínimos, entornos protegidos y credenciales de despliegue rotables; se preferirá autenticación de corta duración cuando Render y GHCR la soporten. Ningún secreto de producción estará disponible en workflows de pull requests.

La base de producción deshabilitará acceso externo por defecto. Cualquier acceso temporal exigirá TLS, allowlist limitada, credencial individual o rotada, motivo y retirada al terminar. No se usarán cuentas compartidas para el panel cuando la plataforma permita identidades individuales. Se activará MFA para GitHub, Render, registrador, DNS, Brevo y observabilidad.

Los cambios de secretos, recursos, accesos y despliegues relevantes deberán quedar auditados. Las credenciales se rotarán ante cambio de responsable, sospecha de exposición y con la periodicidad que fije el runbook; la rotación se probará antes de producción.

### Copias y recuperación

Render Postgres de pago proporcionará PITR con una ventana mínima de `7` días. Además se realizará un backup lógico diario, cifrado y almacenado en un proveedor de objetos independiente de Render dentro de una región europea, con rotación máxima de `35` días conforme a `ADR-0010`.

Los objetivos iniciales propuestos son:

- `RPO` máximo de `15` minutos para fallos recuperables mediante PITR;
- `RTO` máximo de `4` horas para restaurar base, validar integridad, reconfigurar la aplicación y reabrir el servicio;
- disponibilidad mensual objetivo de `99,5 %`, excluyendo mantenimiento anunciado.

Un backup no se considerará válido hasta restaurarlo. Se ejecutará un simulacro trimestral alternando PITR y backup lógico, verificando migraciones, recuentos, restricciones, acceso, outbox y reaplicación de supresiones pendientes antes de reabrir. El runbook identificará responsables, credenciales de emergencia, orden de operaciones y evidencia del ejercicio.

La alta disponibilidad de PostgreSQL y una segunda instancia de aplicación se activarán antes de elevar el objetivo de disponibilidad o cuando el coste medido de una interrupción supere el coste operativo adicional. HA no sustituye backups: la replicación asíncrona puede perder escrituras recientes y replica también errores lógicos.

### Observabilidad y alertas

La aplicación emitirá logs JSON por `stdout`, métricas Micrometer y trazas OpenTelemetry con correlación común. Quedan prohibidos cuerpos de correo, cookies, tokens, credenciales, comentarios de seguimiento, direcciones completas y datos personales innecesarios. Los identificadores de corredor no se usarán como etiquetas de métricas.

Render proporcionará salud, eventos de despliegue, métricas de infraestructura y logs inmediatos. La propuesta para telemetría de aplicación es Grafana Cloud en una región europea mediante OTLP, sujeto a revisión de privacidad y contrato. Los eventos canónicos de seguridad y negocio que requieran `12` meses de conservación permanecerán en PostgreSQL; la telemetría no será su fuente de verdad.

Como mínimo se alertará por:

- aplicación no disponible o reinicios repetidos;
- despliegue o migración fallidos;
- tasa sostenida de errores `5xx` y latencia anómala;
- saturación de CPU, memoria, HikariCP o conexiones PostgreSQL;
- espacio de base próximo al límite, bloqueos prolongados y fallos de backup;
- antigüedad o profundidad creciente de outbox, pausa global y fallos de webhooks;
- fallos de autenticación o patrones de denegación anómalos sin cardinalidad personal.

Cada alerta tendrá umbral, ventana, severidad, destino, responsable y runbook. No se declarará una guardia `24x7` inexistente: el objetivo de disponibilidad y el `RTO` deberán ser compatibles con la cobertura operativa realmente financiada.

### Coste y capacidad

No se usarán recursos gratuitos en producción. Antes de contratar se documentará el coste mensual de aplicación, PostgreSQL, workspace, backups, observabilidad, dominio y transferencia, junto con alertas presupuestarias. Los tamaños iniciales se fijarán después de una prueba de carga representativa y deberán admitir el JVM, el pool HTTP, HikariCP y el worker sin swapping ni saturar PostgreSQL.

Se revisarán mensualmente coste, capacidad, errores y SLO. Aumentar CPU, memoria o almacenamiento dentro de la misma topología no requiere otro ADR. Cambiar de proveedor, región, frontera de despliegue, modelo de datos administrado o garantías de recuperación sí requiere reemplazar esta decisión.

## Alternativas consideradas

### Alternativa A: Google Cloud Run, Cloud SQL y Cloud Monitoring

Es técnicamente sólida, dispone de región Madrid y controles más granulares. Se descarta para el PMV porque el worker embebido obliga a CPU asignada fuera de peticiones y al menos una instancia activa, y la combinación de IAM, Artifact Registry, Cloud SQL, conectividad, jobs, secretos y observabilidad añade operación sin una necesidad de escala que la compense.

### Alternativa B: AWS ECS o App Runner con RDS

Ofrece un camino amplio de evolución y servicios maduros, pero exige más decisiones de red, IAM, registro, balanceo, despliegue, RDS y monitorización. Se descarta mientras no exista un requisito de integración AWS, disponibilidad o escala que justifique ese coste cognitivo y económico.

### Alternativa C: VPS administrada por el proyecto

Puede reducir la factura directa, pero traslada parches, hardening, PostgreSQL, copias, certificados, monitorización y recuperación a una única persona. Se descarta porque el ahorro aparente compra un riesgo operativo desproporcionado.

### Alternativa D: Kubernetes

Se descarta de forma tajante. Un servicio y una base para esta escala no justifican clúster, ingress, operadores, gestión de secretos, observabilidad y actualizaciones de plataforma. Añadir Kubernetes ahora sería sobreingeniería sin beneficio operativo.

### Alternativa E: Planes gratuitos de Render

Se descartan para producción porque no garantizan el proceso siempre activo, el comando de migración previo ni recuperación adecuada de PostgreSQL. Podrán usarse solo para pruebas desechables sin datos reales si sus límites siguen siendo compatibles.

### Alternativa F: Despliegue automático de cada merge a producción

Se descarta. Los gates automáticos son necesarios pero no validan por sí solos migraciones, recuperación, privacidad ni impacto operativo. Producción promoverá manualmente el mismo digest ya probado en staging.

## Consecuencias

- Render reduce la superficie operada por el proyecto, pero crea dependencia de proveedor y no elimina la necesidad de runbooks ni simulacros.
- Frankfurt reduce latencia y mantiene los recursos principales en una región europea, pero no demuestra por sí sola cumplimiento ni ausencia de transferencias internacionales.
- Una sola instancia y PostgreSQL sin HA contienen coste, pero aceptan interrupciones hasta los objetivos definidos.
- El artefacto único mantiene backend y frontend sincronizados y hace trazable la promoción.
- Las migraciones compatibles hacia atrás aumentan disciplina y pueden requerir varias entregas para cambios destructivos.
- Staging permanente y backups independientes elevan el coste, pero eliminarlos haría ficticias la promoción y la recuperación.
- Grafana Cloud añadiría otro encargado y superficie de datos; deberá aplicar minimización y revisión de `ADR-0010`.
- El límite de `35` días para copias obliga a rotación verificable y evita convertir recuperación en conservación indefinida.
- El objetivo `99,5 %` es deliberadamente modesto; prometer más con una instancia, base sin HA y una sola persona operadora sería falso.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-20`.
- Requisito no funcional de datos y privacidad de Fase 1.

## Decisiones de Fase 1 relacionadas

- `D-03`: una única unidad de despliegue single-club.
- `D-06`: publicación y solicitud de notificación conservan una frontera transaccional común.
- `D-07`: seguimiento e historial requieren persistencia y recuperación coherentes.
- `D-08`: el aislamiento del corredor exige secretos, red, logs y accesos operativos controlados.

## Validación prevista

- Desplegar el mismo digest en staging y producción y verificar su trazabilidad hasta commit, SBOM y resultados de CI.
- Hacer fallar CI, análisis de imagen, Flyway, health check y smoke tests para confirmar que bloquean la promoción.
- Probar una migración expandir-migrar-contraer con revisiones antigua y nueva solapadas.
- Probar rollback de aplicación sin revertir la base y documentar el límite ante migraciones incompatibles.
- Medir con carga representativa CPU, memoria, pausas JVM, HikariCP, conexiones, latencia, bloqueos y worker.
- Reiniciar y reemplazar instancias durante envíos para comprobar cierre ordenado, expiración de leases e idempotencia.
- Verificar aislamiento de entornos, ausencia de datos reales en staging y falta de secretos de producción en pull requests.
- Rotar cada tipo de secreto y recuperar el servicio siguiendo el runbook.
- Restaurar trimestralmente desde PITR y backup lógico dentro de `RPO` y `RTO`, incluida la reaplicación de supresiones.
- Simular indisponibilidad de PostgreSQL, Brevo y observabilidad sin provocar reinicios en bucle ni pérdida silenciosa.
- Revisar logs, métricas, trazas y alertas para impedir datos personales o secretos y comprobar cada destino.
- Verificar dominio, HTTPS, cookies seguras, cabeceras y desactivación del subdominio de plataforma.
- Revisar factura y alertas presupuestarias con la carga objetivo antes de autorizar producción.

## Decisiones pendientes

- **Bloqueante para aceptar este ADR:** confirmar Render y la región `Frankfurt` como plataforma y ubicación principal del PMV. Responsable: revisor de arquitectura. Tratamiento: aceptar la dependencia de proveedor frente a las alternativas consideradas.
- **Bloqueante para aceptar este ADR:** confirmar staging y producción persistentes, ambos con recursos de pago y bases independientes. Responsable: revisor de arquitectura y propietario del servicio. Tratamiento: aceptar el coste mínimo necesario para migraciones, recuperación y promoción real.
- **Bloqueante para aceptar este ADR:** fijar presupuesto mensual máximo y comprobar que workspace, aplicación, PostgreSQL, staging, backups y observabilidad caben en él. Responsable: propietario del servicio. Tratamiento: elaborar estimación con precios vigentes antes de contratar.
- **Bloqueante para aceptar este ADR:** confirmar el inicio con una instancia de aplicación y PostgreSQL sin HA, objetivo de disponibilidad `99,5 %`, `RPO` de `15` minutos y `RTO` de `4` horas. Responsable: propietario del servicio y revisor de arquitectura. Tratamiento: aceptar explícitamente indisponibilidad y recuperación manual dentro de esos límites.
- **Bloqueante para aceptar este ADR:** confirmar construcción en GitHub Actions, imagen inmutable en GHCR, staging automático tras CI y promoción manual del mismo digest a producción. Responsable: revisor de arquitectura y persona operadora. Tratamiento: validar permisos y flujo de promoción.
- **Bloqueante para aceptar este ADR:** seleccionar proveedor europeo de object storage para backups diarios cifrados con rotación de `35` días. Responsable: revisor de arquitectura y responsable del tratamiento. Tratamiento: comparar coste, región, DPA, subencargados, restauración y borrado antes de producción.
- **Bloqueante para aceptar este ADR:** confirmar Grafana Cloud en región europea o elegir una alternativa para logs, métricas, trazas y alertas. Responsable: revisor de arquitectura y responsable del tratamiento. Tratamiento: validar retención, minimización, DPA, subencargados, transferencias y coste.
- **Bloqueante para aceptar este ADR:** identificar a la persona operadora, cobertura real y destino de alertas. Responsable: propietario del servicio. Tratamiento: alinear alertas, `RTO` y runbooks con una capacidad de respuesta real.
- **Bloqueante para producción, no para aceptar este ADR:** adquirir dominio, configurar DNS y completar TLS antes de publicar el servicio. Responsable: propietario del servicio. Tratamiento: verificar renovación, acceso al registrador y recuperación de cuenta.
- **Bloqueante para producción, no para aceptar este ADR:** aprobar Render, GHCR, proveedor de backups y observabilidad como encargados o subencargados conforme a `ADR-0010`. Responsable: responsable del tratamiento. Tratamiento: inventario, DPA, regiones, transferencias, retención y análisis de riesgos antes de datos reales.
- **Bloqueante para producción, no para aceptar este ADR:** escribir y probar runbooks de despliegue, rollback, restauración, rotación, incidentes, saturación y caída de proveedores. Responsable: persona operadora y revisor de arquitectura. Tratamiento: aportar evidencia de simulacros antes de producción.

## Referencias oficiales

- [Render: regiones](https://render.com/docs/regions), [Docker](https://render.com/docs/docker) y [despliegues](https://render.com/docs/deploys).
- [Render: health checks](https://render.com/docs/health-checks), [rollbacks](https://render.com/docs/rollbacks) y [Blueprint](https://render.com/docs/blueprint-spec).
- [Render Postgres: conexión](https://render.com/docs/postgresql-creating-connecting), [copias y PITR](https://render.com/docs/postgresql-backups) y [alta disponibilidad](https://render.com/docs/postgresql-high-availability).
- [Render: variables y secretos](https://render.com/docs/configure-environment-variables), [logs](https://render.com/docs/logging), [métricas](https://render.com/docs/service-metrics) y [notificaciones](https://render.com/docs/notifications).
- [Render: dominios](https://render.com/docs/custom-domains), [TLS](https://render.com/docs/tls) y [cumplimiento](https://render.com/docs/certifications-compliance).
- [Google Cloud Run: instancias mínimas](https://cloud.google.com/run/docs/configuring/min-instances) y [facturación por instancia](https://cloud.google.com/run/docs/configuring/billing-settings).
- [Grafana Cloud: telemetría](https://grafana.com/docs/grafana-cloud/telemetry-signals/get-started/quick-start/) y [subencargados](https://grafana.com/legal/list-of-subprocessors/).
