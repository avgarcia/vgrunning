# ADR-0027: Consumo de todos los intentos de inicio de sesión

**Estado:** Aceptado
**Fecha:** 2026-09-03
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0025](0025-spring-session-jdbc-local-login-rate-limit.md)

## Contexto

El límite local de ADR-0025 estaba expresado como fallos de acceso y permitía separar la comprobación de capacidad del consumo posterior. Esa separación permite carreras concurrentes y hace que los inicios correctos no tengan la misma protección frente al abuso.

## Decisión

Cada solicitud de creación de sesión que alcanza el controlador tras la validación HTTP y CSRF consume un intento de ambos buckets locales antes de verificar Argon2. Los límites son cinco intentos por correo canónico y veinte por dirección IP en quince minutos. Se intenta consumir ambos buckets, aunque el primero rechace; no se devuelve capacidad. Si cualquiera rechaza, se responde `429` sin ejecutar autenticación ni crear sesión.

Cada consumo de Bucket4j es atómico por bucket. No se introduce una transacción distribuida entre ambos buckets ni un backend compartido para el nodo único.

## Alternativas consideradas

### Alternativa A: Contar únicamente fallos

Se descarta porque separa admisión y consumo y deja fuera los intentos correctos.

### Alternativa B: Un backend distribuido con consumo conjunto

Se aplaza porque requiere una topología de varias réplicas no aprobada para el PMV.

## Consecuencias

- Los inicios correctos consumen capacidad y pueden recibir `429` cuando exista abuso previo.
- Un rechazo puede consumir la capacidad disponible del otro bucket; el límite es deliberadamente conservador.
- `Retry-After` representa la espera máxima conocida de ambos buckets, no reserva capacidad futura.

## Requisitos relacionados

- `RF-01`
- `RF-02`
- `RF-16`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-03`: el PMV opera inicialmente en un único nodo.

## Validación prevista

- Probar límites exactos, consumo de ambos buckets, inicios correctos y acceso concurrente.
- Comprobar que no se registran correo ni IP como etiquetas de métricas.
- Ejecutar los gates de identidad, arquitectura y calidad aplicables.

## Decisiones pendientes

- **Aplazada deliberadamente, no bloquea el nodo único:** backend compartido de Bucket4j al aprobar varias réplicas. Responsable: Revisor de arquitectura. Tratamiento: ADR previo al cambio de topología.
