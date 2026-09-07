# ADR-0029: CQRS Básico para Queries de Solo Lectura

**Estado:** Aceptado
**Fecha:** 2026-09-07
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0014](0014-modular-hexagonal-ddd-architecture.md)

## Contexto

Según `ADR-0014` y `ADR-0026`, el proyecto sigue una arquitectura modular, hexagonal y DDD selectivo con adaptadores de infraestructura. Además, los test de arquitectura (ej. `JooqBoundaryTest`) aseguran que los tipos generados por jOOQ no escapen del adaptador de persistencia y que el dominio permanezca puro sin dependencias de infraestructura.

Esta separación estricta protege la lógica de negocio, pero plantea un desafío de rendimiento y complejidad innecesaria para las operaciones de consulta puramente de lectura (ej. listados de la interfaz de usuario, consultas de perfil, etc.). Si forzásemos todas las consultas de solo lectura a través del modelo de dominio, tendríamos que hidratar Agregados completos desde jOOQ solo para volver a mapearlos a DTOs en la capa HTTP. Esto crearía cuellos de botella clásicos (consultas N+1, sobrecarga de memoria, hidratación innecesaria de entidades anidadas y mapeos redundantes).

## Decisión

Se adopta un patrón CQRS (Command Query Responsibility Segregation) simplificado a nivel lógico dentro del adaptador de persistencia:

1. **Escrituras y Lógica de Negocio (Commands):** Toda operación que altere el estado pasará estrictamente por el modelo de dominio puro, hidratando el Agregado necesario, ejecutando las reglas de negocio y persistiendo los cambios mediante el puerto de salida del repositorio.
2. **Consultas de Solo Lectura (Queries):** Para vistas o listados de consulta que no ejecutan lógica de negocio ni mutaciones de estado, el adaptador de persistencia usará jOOQ para proyectar los resultados directamente sobre un DTO o Java Record (inmutable). Este DTO se devolverá desde el puerto secundario a la capa de aplicación/HTTP, evitando la hidratación del Agregado de dominio.

## Alternativas consideradas

### Alternativa A: Hidratar siempre el Dominio

Forzar que todas las consultas pasen por los Repositorios y devuelvan entidades de dominio.
Se rechaza porque genera sobrecarga computacional y código repetitivo de mapeo (Base de datos -> jOOQ -> Entidad Dominio -> DTO HTTP) cuando la operación no ejecuta reglas de negocio.

### Alternativa B: Permitir jOOQ en la Capa HTTP

Que los controladores HTTP ejecuten jOOQ directamente.
Se rechaza porque viola `ADR-0014`, `ADR-0026` y los tests de arquitectura (`JooqBoundaryTest`), destruyendo el aislamiento de los adaptadores de infraestructura y acoplando la tecnología de base de datos a la capa web.

## Consecuencias

- **Impacto Positivo:** Consultas altamente optimizadas, reducción del uso de memoria y menor complejidad en el mapeo de objetos para operaciones de lectura.
- **Riesgo y Coste:** Requiere definir explícitamente en el código cuándo una operación es una "Query" y cuándo es un "Command". Además, introduce la creación de DTOs específicos en los paquetes de aplicación para transportar estos datos sin violar las reglas de módulos.
- **Deuda Aceptada:** Posible duplicación parcial de nombres de campos entre Entidades de Dominio y DTOs de lectura.

## Requisitos relacionados

- Todos los requisitos `RF-01` a `RF-21` que requieran listados de datos o consultas de lectura (especialmente UI y portal del corredor).

## Decisiones de Fase 1 relacionadas

- `D-10`, `D-11`

## Validación prevista

- Revisión de Pull Requests para verificar que las consultas (Queries) no realicen mutaciones de estado ni publiquen eventos de dominio.
- `JooqBoundaryTest` debe seguir pasando, garantizando que los tipos generados de jOOQ permanezcan estrictamente contenidos en los adaptadores de persistencia.

## Decisiones pendientes

- Ninguna estructural. En su momento, se definirá la convención de nombres para estos DTOs de lectura (ej. `[Entidad]ReadModel` o `[Entidad]View`).
