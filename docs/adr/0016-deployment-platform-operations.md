# ADR-0016: Plataforma de despliegue y operación

**Estado:** Aceptado
**Fecha:** 2026-08-14
**Responsable de revisión:** Revisor de arquitectura

## Contexto

`ADR-0002` define una única aplicación desplegable y `ADR-0013` concreta un ejecutable Spring Boot que contiene API, SPA y worker. `ADR-0012` exige PostgreSQL como única persistencia primaria, migraciones Flyway previas al arranque y garantías relacionales que no pueden degradarse por la plataforma. `ADR-0010` obliga a identificar regiones, encargados, copias, telemetría y transferencias antes de tratar datos reales.

La escala prevista, superior a `500` corredores y con picos iniciales inferiores a `100` usuarios concurrentes, no justifica operar Kubernetes ni construir una plataforma propia. Sí requiere un proceso siempre activo para el worker, PostgreSQL administrado, despliegues repetibles, recuperación probada y observabilidad suficiente para una única persona operadora.

Esta decisión debe minimizar operación sin fingir que una PaaS elimina la responsabilidad sobre migraciones, capacidad, seguridad, copias o incidentes. Un plan gratuito, una base sin recuperación o un despliegue directo no controlado desde `main` no son aceptables para producción.

## Decisión

### Plataforma, región y topología

La plataforma del PMV será **Microsoft Azure** en la región `West Europe`, con todos los recursos persistentes y de aplicación de cada entorno en esa misma región.

- un Azure App Service Linux de pago, inicialmente candidato al plan `B2`, con `Always On`, que ejecuta una imagen OCI `linux/amd64` con backend, SPA y worker;
- una instancia Azure Database for PostgreSQL Flexible Server con PostgreSQL `18`, inicialmente candidata al tamaño burstable `B1ms`, acceso privado e integración con la red virtual;
- Azure Container Registry `Basic` como réplica de despliegue del digest canónico publicado en GHCR;
- Azure Key Vault y managed identities para evitar credenciales permanentes entre App Service, ACR y los secretos de runtime;
- Azure Container Instances como ejecución efímera del comando Flyway previo al despliegue, dentro de la red autorizada para acceder a PostgreSQL;
- dominio propio con DNS administrado fuera de la plataforma de ejecución y TLS administrado por App Service;
- ningún almacenamiento persistente montado en el contenedor de aplicación.

Producción comenzará con una instancia de aplicación y una base sin réplica de lectura. No habrá autoscaling ni alta disponibilidad de PostgreSQL hasta que el objetivo de servicio, el coste aceptado o las métricas lo justifiquen. La outbox ya admite varios workers mediante leases, pero ninguna tarea programada podrá asumir instancia única: deberá ser idempotente o adquirir coordinación en PostgreSQL antes de habilitar escalado horizontal.

La aplicación usará HikariCP contra la conexión PostgreSQL directa de la red privada. No se habilitará PgBouncer inicialmente porque existe un único pool acotado y no hay presión de conexiones medida. Adoptarlo exigirá verificar que no se depende de estado de sesión, tablas temporales, `LISTEN/NOTIFY` o advisory locks de sesión.

### Entornos

Existirán cuatro contextos aislados:

| Entorno | Ejecución | Datos | Servicios externos |
| --- | --- | --- | --- |
| Local | Docker Compose y aplicación local | Sintéticos | Dobles o sandbox |
| CI | Testcontainers efímeros | Generados por prueba | Dobles |
| Staging | Resource group y recursos Azure de pago aprovisionados bajo demanda en la región elegida | Sintéticos o anonimizados irreversiblemente | Sandbox, sin correo real a corredores |
| Producción | Suscripción y resource group protegidos en la región elegida | Reales | Proveedores aprobados |

Staging se aprovisionará para validar una entrega y se retirará después de conservar sus evidencias. Mientras esté activo usará una base de pago independiente y capacidades suficientes para probar Flyway, worker, recuperación y promoción con fidelidad. Staging y producción no compartirán base, Key Vault, managed identities, app settings, dominio, cookies ni destinos de correo. No se clonará producción hacia entornos no productivos. Los preview environments no se crearán automáticamente; solo podrán habilitarse bajo demanda con datos sintéticos y límites de coste.

### Contrato portable de ejecución

La especificación canónica del despliegue será independiente de Azure. La aplicación deberá poder ejecutarse en cualquier PaaS que admita imágenes OCI, un proceso siempre activo, PostgreSQL administrado y configuración externa. El contrato exigirá:

- una única imagen OCI `linux/amd64`, identificada y promocionada por digest desde GHCR;
- comandos documentados y ajenos al proveedor para ejecutar la aplicación y para validar y aplicar Flyway;
- puerto de escucha, URLs, credenciales, timeouts, pools y parámetros del worker recibidos mediante configuración externa;
- sistema de archivos efímero y ausencia de estado local necesario para atender peticiones o recuperar trabajo;
- logs JSON por `stdout` y `stderr`, telemetría OTLP y endpoints separados de `liveness` y `readiness`;
- recepción de `SIGTERM`, drenado de peticiones y detención ordenada del worker;
- compatibilidad con PostgreSQL `18` sin depender de extensiones no verificadas en origen y destino, roles administrativos, tablas internas, backups ni APIs exclusivas del proveedor.

El repositorio mantendrá un inventario versionado de las claves de configuración, su finalidad, obligatoriedad y carácter secreto, pero nunca sus valores. Los nombres serán propiedad de la aplicación y cada plataforma se limitará a proporcionar sus valores. No se invocarán APIs de Azure desde el código de aplicación.

Los manifiestos de plataforma serán adaptadores del contrato. Los módulos Bicep bajo `infra/azure/` declararán cómo Azure satisface recursos, red, región, health checks, comandos, identidades y configuración no secreta; una futura App Spec, definición de Cloud Run u otro manifiesto podrá hacer lo mismo sin modificar el artefacto ni el código de negocio.

### Artefacto, infraestructura y promoción

GitHub Actions ejecutará los gates de `ADR-0013`, construirá una única imagen OCI reproducible desde un Dockerfile multi-stage, generará SBOM, analizará vulnerabilidades y publicará la imagen canónica en GHCR con etiqueta de commit y digest inmutable. Para desplegar, Azure Container Registry importará ese mismo manifiesto por digest y lo expondrá con una etiqueta de release inmutable; el pipeline verificará que el digest resuelto en ACR coincide con el de GHCR. Una imagen con vulnerabilidad crítica explotable no podrá promocionarse sin una excepción temporal documentada, responsable y fecha de caducidad.

El mismo digest se desplegará bajo demanda primero en staging y después en producción. La promoción a producción será manual mediante un GitHub Environment protegido y requerirá:

1. CI completa superada;
2. migraciones verificadas desde una base vacía y desde la versión productiva anterior;
3. despliegue y smoke tests correctos en staging;
4. revisión del diff de migraciones y plan de rollback;
5. aprobación explícita de la persona operadora.

GitHub Actions será independiente de la plataforma hasta el último paso: compilará, verificará y publicará el artefacto, y un job adaptador recibirá el digest y el entorno objetivo para desplegarlo. El adaptador Azure usará federación OIDC sin secretos de larga duración, aplicará Bicep, importará el digest en ACR y actualizará App Service. Sustituir Azure deberá requerir reemplazar ese adaptador y la infraestructura declarativa, no reconstruir la imagen ni alterar la aplicación. Los identificadores de plan podrán variar sin cambiar este ADR si conservan las capacidades y objetivos decididos. Los valores secretos no se incluirán en manifiestos, repositorio, imagen, logs ni artefactos de CI.

Flyway se ejecutará antes de actualizar App Service mediante un Azure Container Instance efímero creado por el adaptador de despliegue desde el mismo digest, con un comando específico de migración, acceso privado a PostgreSQL y logs conservados como evidencia. El pipeline esperará su finalización correcta y eliminará el container group; cualquier fallo bloqueará el despliegue. Las migraciones deberán ser compatibles con la versión anterior durante el solapamiento del despliegue: se aplicará estrategia expandir-migrar-contraer y las eliminaciones incompatibles se harán en una entrega posterior. Solo una ejecución de migración podrá actuar sobre cada base.

Un rollback reutilizará el digest anterior. No habrá migraciones Flyway descendentes ni restauración automática de la base para revertir código. Si una migración no es compatible hacia atrás, el despliegue quedará bloqueado hasta dividirla. Una corrupción o pérdida de datos seguirá el procedimiento de recuperación, no el rollback ordinario de aplicación.

### Arranque, salud y cierre

El contenedor escuchará en el puerto indicado por la plataforma y no conservará estado en su sistema de archivos efímero. Spring Boot Actuator expondrá endpoints diferenciados de `liveness` y `readiness`, mínimos y sin datos sensibles. El adaptador de plataforma comprobará que el proceso está listo para aceptar tráfico, pero no incluirá dependencias externas cuya caída provocaría reinicios en bucle.

PostgreSQL, Brevo y la cola se comprobarán mediante indicadores separados. El despliegue solo se considerará correcto después de smoke tests autenticados y no autenticados sobre rutas críticas.

La aplicación responderá a `SIGTERM`, dejará de aceptar trabajo nuevo, drenará peticiones y detendrá el worker sin perder leases. El tiempo de cierre de la plataforma será superior al timeout máximo de las operaciones normales y nunca mayor que el lease sin una justificación probada.

### Secretos y acceso operativo

Los secretos de runtime se almacenarán en Azure Key Vault, separados por entorno y mapeados al inventario canónico de configuración mediante referencias de App Service. La fuente operativa de cada secreto y el procedimiento para recrearlo y rotarlo no dependerán de conservar acceso a Azure. GitHub Actions usará permisos mínimos, GitHub Environments protegidos y federación OIDC con Microsoft Entra ID. Ningún secreto de producción estará disponible en workflows de pull requests.

La base de producción deshabilitará acceso externo por defecto. Cualquier acceso temporal exigirá TLS, allowlist limitada, credencial individual o rotada, motivo y retirada al terminar. No se usarán cuentas compartidas para el panel cuando la plataforma permita identidades individuales. Se activará MFA para GitHub, Microsoft Azure, registrador, DNS, Brevo y observabilidad.

Los cambios de secretos, recursos, accesos y despliegues relevantes deberán quedar auditados. Las credenciales se rotarán ante cambio de responsable, sospecha de exposición y con la periodicidad que fije el runbook; la rotación se probará antes de producción.

### Copias y recuperación

Azure Database for PostgreSQL Flexible Server proporcionará PITR con una ventana configurada de `7` días. Se verificará la redundancia de backup disponible en la región y el coste del almacenamiento que exceda el incluido antes de contratar y después de cada cambio de plan. Además se realizará un backup lógico diario con `pg_dump` en formato portable, cifrado antes de la subida y almacenado en **Scaleway Object Storage**, región `fr-par`, clase Standard Multi-AZ y API compatible con S3. El bucket tendrá versionado, Object Lock y una regla de ciclo de vida que elimine las copias como máximo a los `35` días conforme a `ADR-0010`.

Los objetivos iniciales propuestos son:

- `RPO` máximo de `15` minutos para fallos recuperables mediante PITR;
- `RTO` máximo de `4` horas, contado desde que la única persona operadora acepte el incidente durante su disponibilidad, para restaurar base, validar integridad, reconfigurar la aplicación y reabrir el servicio;
- disponibilidad mensual objetivo de `99,5 %`, excluyendo mantenimiento anunciado, medido como objetivo interno y no como SLA contractual.

Un backup no se considerará válido hasta restaurarlo. Se ejecutará un simulacro trimestral alternando PITR y backup lógico, verificando migraciones, recuentos, restricciones, acceso, outbox y reaplicación de supresiones pendientes antes de reabrir. El runbook identificará responsables, credenciales de emergencia, orden de operaciones y evidencia del ejercicio.

La alta disponibilidad de PostgreSQL y una segunda instancia de aplicación se activarán antes de elevar el objetivo de disponibilidad o cuando el coste medido de una interrupción supere el coste operativo adicional. HA no sustituye backups: la replicación asíncrona puede perder escrituras recientes y replica también errores lógicos.

### Portabilidad y salida del proveedor

El cambio de plataforma seguirá inicialmente una parada controlada, al ser más simple y verificable que introducir replicación permanente o operación multicloud:

1. aprovisionar el destino con PostgreSQL de la misma versión mayor y restaurar allí un backup reciente para ensayar la compatibilidad;
2. desplegar el mismo digest sin tráfico, recrear la configuración y ejecutar migraciones, comprobaciones de integridad y smoke tests;
3. reducir con antelación el TTL del DNS a un máximo de `300` segundos;
4. activar modo mantenimiento, impedir nuevas escrituras y drenar peticiones y worker en el origen;
5. generar y restaurar un backup lógico final, sin aceptar escrituras concurrentes;
6. validar versión de Flyway, restricciones, recuentos, usuarios, sesiones, outbox y supresiones pendientes;
7. arrancar aplicación y worker en el destino, ejecutar smoke tests y cambiar DNS;
8. conservar el origen sin escrituras entre `24` y `48` horas antes de retirar recursos y rotar credenciales.

La migración deberá completarse dentro de `RTO <= 4 h` y no perder más de `RPO <= 15 min`; la parada de escrituras y el backup final buscarán pérdida cero. Si el volumen medido impide cumplir el RTO mediante backup y restauración, la migración usará replicación lógica de PostgreSQL y un cambio final breve, con un plan específico que pruebe compatibilidad de esquema, secuencias y operaciones no replicadas antes de ejecutarse.

Cada año, y también antes de abandonar la plataforma tras un cambio material del modelo de datos o del pipeline, se realizará un simulacro de salida: desplegar temporalmente el último digest en una PaaS alternativa, restaurar el backup independiente y ejecutar las validaciones críticas. La portabilidad no se considerará demostrada solo por disponer de una imagen o un runbook.

### Observabilidad y alertas

La aplicación emitirá logs JSON por `stdout`, métricas Micrometer y trazas OpenTelemetry con correlación común. Quedan prohibidos cuerpos de correo, cookies, tokens, credenciales, comentarios de seguimiento, direcciones completas y datos personales innecesarios. Los identificadores de corredor no se usarán como etiquetas de métricas.

Azure Monitor proporcionará Resource Health, Activity Log, eventos de despliegue, métricas de App Service y logs operativos inmediatos con límites de ingestión y retención. La telemetría de aplicación se enviará mediante OTLP a **Grafana Cloud Pro** en la región EU Germany sobre AWS `eu-central-1`. Se aplicarán redacción, muestreo, límites de cardinalidad y alertas de consumo; logs y trazas tendrán una retención máxima de `30` días y las métricas podrán conservarse hasta `13` meses. Los eventos canónicos de seguridad y negocio que requieran `12` meses de conservación permanecerán en PostgreSQL; la telemetría no será su fuente de verdad.

Como mínimo se alertará por:

- aplicación no disponible o reinicios repetidos;
- despliegue o migración fallidos;
- tasa sostenida de errores `5xx` y latencia anómala;
- saturación de CPU, memoria, HikariCP o conexiones PostgreSQL;
- espacio de base próximo al límite, bloqueos prolongados y fallos de backup;
- antigüedad o profundidad creciente de outbox, pausa global y fallos de webhooks;
- fallos de autenticación o patrones de denegación anómalos sin cardinalidad personal.

Cada alerta tendrá umbral, ventana, severidad, destino, responsable y runbook. El propietario del servicio será la única persona operadora y no existirá guardia `24x7`. Las alertas críticas se enviarán por correo y un canal móvil; las advertencias no críticas se enviarán por correo y se revisarán diariamente. Fuera de la disponibilidad declarada por la persona operadora, la respuesta comenzará en su siguiente ventana disponible y ese tiempo quedará fuera del `RTO` operativo, aunque la indisponibilidad sí contará para medir el objetivo mensual.

### Coste y capacidad

No se usarán recursos gratuitos en producción. El presupuesto mensual máximo inicial será de **`100 EUR` sin IVA**, incluyendo App Service, PostgreSQL, ACR, Key Vault, Azure Monitor, ejecuciones efímeras de Flyway, uso prorrateado de staging, backups, Grafana Cloud Pro, dominio y transferencia. Queda fuera el consumo variable de correo de Brevo. Azure Cost Management configurará presupuesto y alertas al `50 %`, `80 %` y `100 %`. Los tamaños `B2` y `B1ms` son candidatos iniciales, no garantías de capacidad: deberán superar una prueba de carga representativa para admitir JVM, pool HTTP, HikariCP y worker sin swapping ni saturar PostgreSQL.

Se revisarán mensualmente coste, capacidad, errores y SLO. Aumentar CPU, memoria o almacenamiento dentro de la misma topología no requiere otro ADR. Cambiar de proveedor, región, frontera de despliegue, modelo de datos administrado o garantías de recuperación sí requiere reemplazar esta decisión.

## Alternativas consideradas

### Alternativa A: Render

Es la alternativa más sencilla de operar y proporciona web service, PostgreSQL, red privada, health checks y despliegue previo de migraciones con menos piezas. Se descarta porque, con workspace, capacidades de backup y observabilidad equivalentes, su coste estimado se acerca más al límite de `100 EUR` y ofrece menos margen de capacidad y evolución que Azure. Sigue siendo el primer destino para el simulacro de salida por la sencillez de su adaptador.

### Alternativa B: DigitalOcean App Platform y Managed PostgreSQL

Presenta el menor coste estimado y una operación más simple que Azure. Se descarta porque Azure ofrece mejores controles de identidad, red, auditoría y evolución, y porque el propietario acepta pagar esa complejidad moderada. Esta elección solo se sostiene mientras Azure permanezca dentro del presupuesto; superar de forma continuada `100 EUR` obligará a reevaluar DigitalOcean.

### Alternativa C: Google Cloud Run, Cloud SQL y Cloud Monitoring

Es técnicamente sólida, dispone de región Madrid y controles granulares. Se descarta para el PMV porque el worker embebido obliga a facturación por instancia y al menos una instancia activa, y la combinación de IAM, Artifact Registry, Cloud SQL, conectividad, jobs, secretos y observabilidad añade más operación que Azure sin una ventaja necesaria para la escala prevista. Las instancias compartidas económicas de Cloud SQL tampoco ofrecen SLA.

### Alternativa D: AWS ECS con Fargate y RDS

Ofrece el camino de evolución más amplio y servicios maduros. App Runner no satisface con claridad el worker siempre activo, mientras ECS exige balanceador, red, IAM, registro, RDS, secretos y monitorización. Se descarta porque su coste y carga operativa superan a Azure para el mismo nivel inicial de servicio.

### Alternativa E: VPS administrada por el proyecto

Puede reducir la factura directa, pero traslada parches, hardening, PostgreSQL, copias, certificados, monitorización y recuperación a una única persona. Se descarta porque el ahorro aparente compra un riesgo operativo desproporcionado.

### Alternativa F: Kubernetes y OpenShift

Se descartan de forma tajante. Un servicio y una base para esta escala no justifican clúster, ingress, operadores, gestión de secretos, observabilidad y actualizaciones de plataforma. OpenShift añade además control plane, licencias y un mínimo de nodos cuyo coste es varios órdenes de magnitud superior al presupuesto. Adoptarlos ahora sería sobreingeniería sin beneficio operativo.

### Alternativa G: Planes gratuitos

Se descartan para producción porque no garantizan el proceso siempre activo, capacidad suficiente, el comando de migración previo ni recuperación adecuada de PostgreSQL. Podrán usarse solo para pruebas desechables sin datos reales si sus límites siguen siendo compatibles.

### Alternativa H: Despliegue automático de cada merge a producción

Se descarta. Los gates automáticos son necesarios pero no validan por sí solos migraciones, recuperación, privacidad ni impacto operativo. Producción promoverá manualmente el mismo digest ya probado en staging.

## Consecuencias

- Azure proporciona identidad administrada, red privada, auditoría y una ruta de crecimiento amplia, pero requiere coordinar más servicios que Render o DigitalOcean.
- El contrato portable limita la dependencia al adaptador de infraestructura, secretos y operación; no pretende ocultar diferencias reales entre proveedores.
- La parada controlada simplifica y hace verificable la primera migración, pero introduce mantenimiento anunciado y dejará de ser viable si el volumen no cabe en el `RTO`.
- Una región Azure europea reduce latencia y mantiene los recursos principales en la UE, pero no demuestra por sí sola cumplimiento ni ausencia de transferencias internacionales.
- Una sola instancia y PostgreSQL sin HA contienen coste, pero aceptan interrupciones hasta los objetivos definidos.
- El artefacto único mantiene backend y frontend sincronizados y hace trazable la promoción.
- La réplica en ACR añade coste y un paso de promoción, pero permite autenticación mediante managed identity sin convertir ACR en la fuente canónica del artefacto.
- Ejecutar Flyway en Azure Container Instances evita abrir PostgreSQL a runners públicos, pero añade un adaptador Azure que debe probarse y observarse.
- Las migraciones compatibles hacia atrás aumentan disciplina y pueden requerir varias entregas para cambios destructivos.
- Staging de pago bajo demanda reduce el coste fijo, pero exige aprovisionamiento reproducible y añade tiempo a cada promoción frente a mantenerlo permanentemente activo.
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
- Ejecutar la imagen y sus comandos de aplicación y migración fuera de Azure usando solo el contrato documentado de configuración.
- Hacer fallar CI, análisis de imagen, Flyway, health check y smoke tests para confirmar que bloquean la promoción.
- Probar una migración expandir-migrar-contraer con revisiones antigua y nueva solapadas.
- Probar rollback de aplicación sin revertir la base y documentar el límite ante migraciones incompatibles.
- Medir con carga representativa CPU, memoria, pausas JVM, HikariCP, conexiones, latencia, bloqueos y worker.
- Reiniciar y reemplazar instancias durante envíos para comprobar cierre ordenado, expiración de leases e idempotencia.
- Verificar aislamiento de entornos, ausencia de datos reales en staging y falta de secretos de producción en pull requests.
- Rotar cada tipo de secreto y recuperar el servicio siguiendo el runbook.
- Restaurar trimestralmente desde PITR y backup lógico dentro de `RPO` y `RTO`, incluida la reaplicación de supresiones.
- Completar anualmente un simulacro de salida hacia una PaaS alternativa dentro de `RTO` y `RPO`, incluido despliegue, restauración, smoke tests y cambio de configuración sin modificar la aplicación.
- Simular indisponibilidad de PostgreSQL, Brevo y observabilidad sin provocar reinicios en bucle ni pérdida silenciosa.
- Revisar logs, métricas, trazas y alertas para impedir datos personales o secretos y comprobar cada destino.
- Verificar dominio, HTTPS, cookies seguras, cabeceras y rechazo de hosts distintos del dominio de producción.
- Revisar factura y alertas presupuestarias con la carga objetivo antes de autorizar producción.

## Decisiones pendientes

- **Bloqueante para producción, no para aceptar este ADR:** adquirir dominio, configurar DNS y completar TLS antes de publicar el servicio. Responsable: propietario del servicio. Tratamiento: verificar renovación, acceso al registrador y recuperación de cuenta.
- **Bloqueante para producción, no para aceptar este ADR:** aprobar Microsoft Azure, GHCR, Scaleway y Grafana Cloud como encargados o subencargados conforme a `ADR-0010`. Responsable: responsable del tratamiento. Tratamiento: inventario, DPA, regiones, transferencias, retención y análisis de riesgos antes de datos reales.
- **Bloqueante para producción, no para aceptar este ADR:** escribir y probar runbooks de despliegue, rollback, restauración, rotación, incidentes, saturación, caída y salida de proveedor. Responsable: persona operadora y revisor de arquitectura. Tratamiento: aportar evidencia de simulacros, incluido despliegue del mismo digest y restauración en una PaaS alternativa, antes de producción.

## Referencias oficiales

- [Azure App Service: contenedores personalizados](https://learn.microsoft.com/azure/app-service/quickstart-custom-container), [configuración](https://learn.microsoft.com/azure/app-service/configure-custom-container) y [health checks](https://learn.microsoft.com/azure/app-service/monitor-instances-health-check).
- [Azure App Service: planes](https://learn.microsoft.com/azure/app-service/overview-hosting-plans), [integración con red virtual](https://learn.microsoft.com/azure/app-service/overview-vnet-integration) y [deployment slots](https://learn.microsoft.com/azure/app-service/deploy-staging-slots).
- [Azure Container Registry: importación de imágenes por digest](https://learn.microsoft.com/azure/container-registry/container-registry-import-images).
- [Azure Database for PostgreSQL: red privada](https://learn.microsoft.com/azure/postgresql/flexible-server/concepts-networking-private), [copias y PITR](https://learn.microsoft.com/azure/postgresql/backup-restore/concepts-backup-restore) y [recuperación](https://learn.microsoft.com/azure/reliability/reliability-database-postgresql).
- [Azure Key Vault: referencias desde App Service](https://learn.microsoft.com/azure/app-service/app-service-key-vault-references) y [GitHub Actions: federación OIDC con Azure](https://learn.microsoft.com/azure/developer/github/connect-from-azure-openid-connect).
- [Azure Container Instances: despliegue en red virtual](https://learn.microsoft.com/azure/container-instances/container-instances-vnet) y [managed identity](https://learn.microsoft.com/azure/container-instances/container-instances-managed-identity).
- [Azure Monitor: monitorización de App Service](https://learn.microsoft.com/azure/app-service/overview-monitoring) y [Azure Cost Management: presupuestos](https://learn.microsoft.com/azure/cost-management-billing/costs/tutorial-acm-create-budgets).
- [Render: regiones](https://render.com/docs/regions), [Docker](https://render.com/docs/docker), [despliegues](https://render.com/docs/deploys) y [PostgreSQL](https://render.com/docs/postgresql-creating-connecting).
- [Google Cloud Run: instancias mínimas](https://cloud.google.com/run/docs/configuring/min-instances) y [facturación por instancia](https://cloud.google.com/run/docs/configuring/billing-settings).
- [Scaleway Object Storage: conceptos, regiones, clases, versionado y Object Lock](https://www.scaleway.com/en/docs/object-storage/concepts/).
- [Grafana Cloud: telemetría](https://grafana.com/docs/grafana-cloud/telemetry-signals/get-started/quick-start/), [regiones](https://grafana.com/docs/grafana-cloud/security-and-account-management/regional-availability/), [precios](https://grafana.com/pricing/) y [subencargados](https://grafana.com/legal/list-of-subprocessors/).
- [PostgreSQL: `pg_dump`](https://www.postgresql.org/docs/18/app-pgdump.html), [`pg_restore`](https://www.postgresql.org/docs/18/app-pgrestore.html) y [replicación lógica](https://www.postgresql.org/docs/18/logical-replication.html).
