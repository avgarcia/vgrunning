# Entorno local de Running Coach

**Estado:** Activo — preparación técnica del PMV

Esta guía permite preparar y reconstruir el entorno local desde un checkout limpio. Solo admite servicios, credenciales y datos sintéticos; no autoriza datos de corredores reales, proveedores externos, Azure, staging ni producción.

## Requisitos

- Git y Docker Desktop con Docker Compose v2 en ejecución.
- Una JVM 17 o superior para iniciar Gradle Wrapper. El build descarga Java 25 Temurin automáticamente.
- Node.js `24.19.0` y npm `11.17.0` para la SPA y sus pruebas.
- Acceso de red en la primera ejecución para descargar dependencias, imágenes y toolchains.

## Configuración local

`.env.example` contiene valores copiables exclusivamente para local. Si se necesita una configuración persistente, cópialo a `.env`; este archivo está ignorado por Git y Spring Boot lo importa como propiedades locales. Compose usa el mismo archivo para publicar PostgreSQL, de modo que el puerto JDBC y el puerto publicado se mantienen coordinados.

```powershell
Copy-Item .env.example .env
```

| Variable | Finalidad | Obligatoria | Secreta | Ejemplo sintético |
| --- | --- | --- | --- | --- |
| `RUNNING_COACH_DB_PORT` | Puerto de PostgreSQL publicado por Compose. | No; por defecto `5432`. | No. | `5432` |
| `RUNNING_COACH_DB_URL` | URL JDBC que usa el backend. | No; por defecto coincide con Compose. | No. | `jdbc:postgresql://localhost:5432/running_coach` |
| `RUNNING_COACH_DB_USERNAME` | Usuario JDBC local. | No; por defecto coincide con Compose. | No. | `running_coach` |
| `RUNNING_COACH_DB_PASSWORD` | Contraseña JDBC local. | No; por defecto coincide con Compose. | Sí, aunque el valor de ejemplo es deliberadamente sintético. | `running_coach` |

Si cambia `RUNNING_COACH_DB_PORT`, actualiza también `RUNNING_COACH_DB_URL`. La contraseña de ejemplo no puede reutilizarse en ningún entorno compartido o real.

## Arranque desde cero

En Windows:

```powershell
Copy-Item .env.example .env
npm ci --prefix frontend
npm --prefix frontend run playwright:install
docker compose up -d --wait postgres
.\gradlew.bat verifyJavaToolchain
.\gradlew.bat generateJooqFromPostgres
.\gradlew.bat bootRun
```

En macOS, Linux o Git Bash, sustituye `.\gradlew.bat` por `./gradlew` y `Copy-Item` por el comando equivalente de tu shell.

Flyway aplica las migraciones automáticamente al iniciar el backend. `generateJooqFromPostgres` usa otro PostgreSQL efímero: no modifica el volumen local y genera fuentes exclusivamente bajo `build/generated`.

## Comprobación operativa

Con `bootRun` en ejecución, verifica los probes técnicos:

```powershell
Invoke-WebRequest http://localhost:8081/actuator/health/liveness
Invoke-WebRequest http://localhost:8081/actuator/health/readiness
```

Los dos deben devolver `200`. El shell técnico se sirve en `http://localhost:8080/`; `/api` sigue denegada hasta que exista autenticación. El puerto `8081` no expone `/actuator/health` ni otros endpoints de Actuator.

## Desarrollo, validación y artefactos

| Objetivo | Comando Windows |
| --- | --- |
| Pruebas, formato, contrato y frontend | `.\gradlew.bat check` |
| Validar solo la documentación local | `.\gradlew.bat verifyDocumentationLinks` |
| Gate de PR | `.\gradlew.bat fastGate` |
| Autopruebas del tooling | `.\gradlew.bat toolingGate` |
| Evidencia integral final | `.\gradlew.bat qualityGate` |
| Comprobar contrato | `.\gradlew.bat apiCheck` |
| Comprobar frontend | `.\gradlew.bat frontendCheck` |
| Construir imagen OCI | `.\gradlew.bat buildOciImage` |
| Generar SBOM | `.\gradlew.bat generateSbom` |
| Analizar vulnerabilidades CRITICAL | `.\gradlew.bat trivy` |

Durante el desarrollo ejecuta solo el control afectado. Ejecuta `qualityGate` una única vez cuando todos los controles dirigidos hayan pasado; si falla, identifica la causa, corrige y valida solo el control correspondiente antes de repetirlo.

## Parada y recreación

Para detener la aplicación iniciada con `bootRun`, usa `Ctrl+C` y espera a que termine. Spring Boot usa apagado graceful y permite hasta 30 segundos por fase para cerrar trabajo en curso.

Para detener PostgreSQL sin borrar sus datos locales:

```powershell
docker compose down
```

Para eliminar deliberadamente el volumen y reconstruir la base desde las migraciones:

```powershell
docker compose down --volumes
docker compose up -d --wait postgres
.\gradlew.bat bootRun
```

`down --volumes` elimina solo el volumen local `running-coach-postgres-data`; no debe ejecutarse si se necesita conservar su estado técnico.

## Fixtures y límites de datos

Los únicos fixtures actuales son técnicos: la base `running_coach`, el usuario y contraseña sintéticos `running_coach`, fixtures mínimos de calidad y contrato, y el shell técnico de la SPA. No existen seeds de corredores, correos, teléfonos, planes, sesiones ni tablas de negocio.

No copies datos desde sistemas reales, no uses nombres o correos de personas y no añadas secretos a `.env.example`, documentación, fixtures, logs o comandos. Cuando se implemente una funcionalidad vertical, sus fixtures sintéticos se definirán junto con su contrato, migración y pruebas.

## Incidencias frecuentes

| Síntoma | Acción |
| --- | --- |
| Docker no inicia o Testcontainers falla | Inicia Docker Desktop y confirma `docker version`; el build completo requiere Docker. |
| El puerto `5432` está ocupado | Cambia `RUNNING_COACH_DB_PORT` y `RUNNING_COACH_DB_URL` de forma coordinada en `.env`. |
| El backend no conecta | Ejecuta `docker compose up -d --wait postgres` y comprueba que las variables JDBC coinciden con Compose. |
| Cambios de migración no se reflejan localmente | Recrea el volumen con `docker compose down --volumes` y vuelve a iniciar Compose. |
| Falla el primer build | Comprueba red y deja que Gradle, npm, Docker, Playwright y Trivy reutilicen sus cachés en intentos posteriores. |
