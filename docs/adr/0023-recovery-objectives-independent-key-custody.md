# ADR-0023: Objetivos de recuperación y custodia independiente de claves

**Estado:** Aceptado
**Fecha:** 2026-08-24
**Responsable de revisión:** Revisor de arquitectura y persona operadora
**Refina parcialmente:** [ADR-0016](0016-deployment-platform-operations.md)
**Validación documental:** Escenarios, objetivos y custodia aceptados explícitamente por el responsable durante la auditoría H-17; evidencias y simulacros pendientes antes de producción

## Contexto

`ADR-0016` decidió Azure Database for PostgreSQL con PITR, una copia lógica diaria cifrada en Scaleway Object Storage y una salida portable. Sin embargo, asignó un único `RPO <= 15 min` y `RTO <= 4 h` sin distinguir una avería recuperable dentro de Azure de la pérdida total de la suscripción, de la plataforma o del acceso a sus secretos.

La distinción es necesaria. El PITR nativo puede satisfacer una recuperación ordinaria dentro de Azure, pero no es una copia exportable fuera del proveedor. Una copia lógica diaria externa limita la dependencia de Azure, aunque por definición no puede prometer quince minutos de pérdida máxima. Además, cifrarla con una clave privada recuperable únicamente desde Azure convertiría la independencia del almacenamiento en una ficción.

La escala inicial —un único club y alrededor de `500` corredores— no justifica replicación continua de WAL fuera de Azure, operación multicloud activa ni una segunda base siempre disponible. Sí justifica objetivos honestos por escenario, separación de credenciales, custodia física independiente y simulacros que demuestren restauración real.

Este ADR reemplaza únicamente los objetivos indiferenciados, la custodia de claves y la cadencia de simulacros de recuperación de `ADR-0016`. Mantiene su plataforma, región, copia diaria, retención máxima de `35` días, Object Lock, contrato portable, parada controlada y demás decisiones no contradichas.

## Decisión

### Escenarios y objetivos

Los objetivos se medirán desde que la persona operadora acepta el incidente dentro de su disponibilidad declarada. El tiempo anterior sigue contando como indisponibilidad del servicio, pero queda fuera del `RTO` operativo porque no existe guardia `24x7`.

| Escenario | Fuente de recuperación | Objetivo de pérdida | Objetivo de recuperación |
| --- | --- | --- | --- |
| Fallo lógico u operativo recuperable dentro de Azure | PITR de Azure Database for PostgreSQL | `RPO <= 15 min` | `RTO <= 4 h` |
| Pérdida total o inaccesibilidad prolongada de Azure, de la suscripción o del proveedor | Último `pg_dump` diario cifrado y bloqueado en Scaleway | `RPO <= 24 h` | `RTO <= 24 h` |
| Migración planificada de proveedor | Parada de escrituras, drenado y copia lógica final | Objetivo de pérdida `0`; no se garantiza sin verificar el corte | `RTO <= 4 h` |

No se presentará el objetivo de `15` minutos como aplicable a la pérdida total de Azure. Tampoco se afirmará pérdida cero en una migración hasta haber bloqueado escrituras, drenado peticiones y worker, restaurado la copia final y verificado recuentos, restricciones, Flyway, sesiones, outbox y supresiones.

Si el volumen medido impide cumplir la migración dentro de cuatro horas mediante parada y copia final, se preparará una decisión específica para replicación lógica. No se introduce ahora archivo continuo de WAL externo, base secundaria activa ni operación multicloud.

### Cifrado y custodia

Cada copia lógica se cifrará antes de abandonar Azure mediante un formato estándar de cifrado híbrido autenticado. La implementación podrá elegir una herramienta mantenida que proporcione cifrado simétrico autenticado del contenido y encapsulado asimétrico de su clave de datos; quedan prohibidos algoritmos, formatos o gestión criptográfica propios del proyecto.

Azure solo dispondrá de la clave pública necesaria para cifrar. La clave privada de recuperación y sus copias nunca estarán en:

- Azure, incluido Key Vault, App Service, Container Instances o PostgreSQL;
- GitHub, Actions, GHCR, repositorio, artefactos, variables o secretos de CI;
- Scaleway Object Storage o sus metadatos;
- imagen OCI, equipo de desarrollo ordinario, logs o documentación pública.

Existirán dos copias protegidas de la clave privada y del procedimiento mínimo de recuperación, en ubicaciones físicas separadas. Una quedará bajo custodia del propietario del servicio y otra bajo una persona custodio de recuperación designada. Acceso, sustitución y prueba quedarán registrados fuera del repositorio. Una clave privada antigua no se destruirá mientras exista una copia retenida que dependa de ella.

La pérdida conjunta de las dos copias privadas hace irrecuperables los backups cifrados y se acepta como riesgo que debe reducirse mediante custodia, inventario y simulacros, no mediante una copia oculta en Azure.

### Separación de permisos y credenciales

La identidad de Azure que genera copias podrá crear nuevos objetos, pero no eliminar versiones existentes, acortar su retención, desactivar Object Lock ni modificar la política del bucket. La administración de retención usará otra identidad protegida y solo operará mediante el runbook aprobado.

Las credenciales de administración y recuperación de Scaleway se custodiarán fuera de Azure, con MFA independiente y procedimiento de acceso de emergencia. La pérdida de la suscripción Azure no podrá impedir autenticarse en Scaleway, obtener la copia, descifrarla o aprovisionar un destino alternativo.

La automatización comprobará tras cada copia: finalización de `pg_dump`, cifrado, subida, tamaño no vacío, checksum, retención y capacidad de listar el objeto con la identidad de recuperación. Esas comprobaciones no sustituyen una restauración.

### Simulacros y gate de producción

Antes de producción se realizará una restauración completa desde la copia externa en un PostgreSQL situado fuera de la suscripción y de los recursos productivos de Azure. Deberá usar las credenciales y una copia privada custodiadas fuera de Azure y demostrar integridad, migraciones, acceso, outbox y reaplicación de supresiones antes de abrir el servicio restaurado.

Cadencia mínima:

- PITR dentro de Azure: trimestral;
- restauración completa desde copia externa fuera de Azure: semestral;
- salida de proveedor con el mismo digest y copia independiente: anual y después de cambios materiales del pipeline o modelo de datos.

Cada ejercicio conservará fecha, escenario, backup utilizado, custodios participantes, tiempos reales, pérdida observada, verificaciones, incidencias y acciones correctivas. Un objetivo no demostrado por un simulacro vigente seguirá siendo una intención y bloqueará producción cuando corresponda.

## Alternativas consideradas

### Alternativa A: Aplicar `RPO <= 15 min` a todos los fallos

Se descarta porque una copia diaria externa no lo satisface y el PITR permanece dentro de Azure. Mantener esa promesa sería documentación falsa, no resiliencia.

### Alternativa B: Replicación continua externa o multicloud activo

Se descarta para la escala inicial. Reduciría el `RPO` de pérdida total, pero añade red, compatibilidad, credenciales, monitorización, pruebas de failover, consistencia y coste permanentes sin evidencia de negocio que lo justifique.

### Alternativa C: Guardar la clave privada en Azure Key Vault

Se descarta para la recuperación externa porque una pérdida de suscripción, identidad o proveedor podría hacer inaccesibles a la vez datos y clave. Key Vault sigue gobernando secretos ordinarios de runtime, no la única clave privada de recuperación independiente.

### Alternativa D: Guardar una clave privada junto al backup o en CI

Se descarta de forma tajante. El compromiso del almacenamiento, repositorio o pipeline expondría simultáneamente copia y capacidad de descifrado.

### Alternativa E: Copias privadas bajo una única persona y ubicación

Se descarta porque incendio, pérdida, indisponibilidad o error de una sola persona convertirían el backup externo en irrecuperable. La doble custodia añade coordinación, pero elimina ese punto único.

## Consecuencias

- Los objetivos son más modestos ante pérdida total de Azure, pero ahora son compatibles con la arquitectura realmente decidida.
- La copia externa sigue siendo útil aunque Azure o la suscripción no estén disponibles.
- La separación de permisos reduce el riesgo de que una credencial de aplicación borre todas las copias, pero añade identidades y un runbook que deben probarse.
- La doble custodia reduce el punto único de fallo y exige mantener una persona designada, dos ubicaciones e inventario actualizado.
- Rotar claves obliga a conservar material privado antiguo hasta que caduque la última copia cifrada con él.
- Los simulacros semestrales externos tienen coste y tiempo operativo; sin ellos no existe evidencia de recuperación independiente.
- La recuperación total puede perder hasta veinticuatro horas de datos. El proyecto acepta ese riesgo para la escala inicial y deberá reabrir la decisión si el impacto medido deja de ser tolerable.
- No se introduce infraestructura continua adicional ni una falsa alta disponibilidad multicloud.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-21`.
- Requisitos no funcionales de disponibilidad, seguridad, datos y privacidad.

## Decisiones de Fase 1 relacionadas

- `D-03`: el PMV mantiene una única unidad de despliegue y no incorpora aislamiento multiclub.
- `D-06`: versiones, solicitudes y supresiones deben recuperarse coherentemente.
- `D-07`: seguimiento e historial requieren restauración íntegra y reaplicación de retención.

## Validación prevista

- Restaurar mediante PITR y medir `RPO` y `RTO` del escenario Azure.
- Restaurar un `pg_dump` externo en infraestructura ajena a la suscripción productiva de Azure y medir `RPO <= 24 h` y `RTO <= 24 h`.
- Ejecutar una migración planificada con bloqueo de escrituras, copia final y objetivo de pérdida cero dentro de cuatro horas.
- Demostrar que la identidad escritora de Azure no puede borrar objetos, versiones, Object Lock ni reducir retención.
- Perder deliberadamente el acceso operativo a Azure en el simulacro y recuperar Scaleway y la clave privada mediante las credenciales independientes.
- Probar cada copia privada y el procedimiento con propietario y custodio sin copiar el material a CI, repositorio o servicios cloud prohibidos.
- Rotar la clave pública, cifrar nuevas copias y conservar la privada anterior hasta el vencimiento de la última copia dependiente.
- Verificar checksum, descifrado, `pg_restore`, Flyway, restricciones, recuentos, cuentas, sesiones, outbox, eventos incompletos y supresiones.
- Registrar tiempos, pérdida real, fallos y acciones correctivas de cada ejercicio.

## Decisiones pendientes

No quedan decisiones de producto o arquitectura pendientes para aceptar este ADR. Permanecen estos artefactos y evidencias bloqueantes para producción:

- designar nominalmente a la persona custodio y documentar aceptación, sustitución y acceso de emergencia fuera del repositorio;
- seleccionar y versionar la herramienta estándar de cifrado híbrido, formato de sobre y comandos de recuperación sin cambiar las propiedades decididas;
- crear las identidades separadas de Azure y Scaleway, MFA, Object Lock, retención y runbooks;
- ejecutar con éxito la primera restauración externa completa y corregir cualquier desviación de objetivos.

## Referencias oficiales

- [Azure Database for PostgreSQL: backup y restauración](https://learn.microsoft.com/azure/postgresql/backup-restore/concepts-backup-restore).
- [Azure Database for PostgreSQL: continuidad de negocio](https://learn.microsoft.com/azure/postgresql/backup-restore/concepts-business-continuity).
- [Azure Key Vault: backup](https://learn.microsoft.com/azure/key-vault/general/backup).
- [PostgreSQL: `pg_dump`](https://www.postgresql.org/docs/18/app-pgdump.html) y [`pg_restore`](https://www.postgresql.org/docs/18/app-pgrestore.html).
- [Scaleway Object Storage: versionado y Object Lock](https://www.scaleway.com/en/docs/object-storage/concepts/).
