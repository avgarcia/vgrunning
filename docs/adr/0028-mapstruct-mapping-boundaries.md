# ADR-0028: MapStruct en las fronteras de representaciones

**Estado:** Aceptado
**Fecha:** 2026-09-03
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0026](0026-hexagonal-packaging-under-infrastructure.md)

## Contexto

Las conversiones manuales entre resultados de puertos, representaciones de sesión, OpenAPI y registros jOOQ duplicaban campos y podían perder propiedades al evolucionar los tipos.

## Decisión

MapStruct generará todos los mapeos entre representaciones de clases de datos. Los mappers puros entre contratos internos viven en `application.mapper`, no usan Spring y se componen desde infraestructura. Los mappers HTTP y jOOQ viven en sus fronteras de infraestructura y usan el modelo Spring correspondiente. La compilación fallará ante propiedades destino sin mapear.

Un mapper no consulta, autentica, cifra, calcula tiempo, genera identificadores ni decide permisos. La construcción de objetos propios del framework no requiere representaciones intermedias ni mappers ceremoniales.

## Alternativas consideradas

### Alternativa A: Copias manuales junto a cada consumidor

Se descarta porque duplica conversiones y no detecta en compilación destinos incompletos.

### Alternativa B: Un mapper genérico compartido

Se descarta porque mezclará fronteras y añadirá una abstracción sin responsabilidad de negocio.

## Consecuencias

- Los contratos internos conservan su independencia de infraestructura.
- `application.mapper` es una excepción explícita y limitada a MapStruct puro.
- Los mappers generados no se versionan y sus warnings no sustituyen la revisión del código fuente.

## Requisitos relacionados

- `RF-01`
- `RF-02`
- `RF-16`
- `RF-18`
- `RF-19`

## Decisiones de Fase 1 relacionadas

- `D-01`: cada concepto y regla conserva un propietario inequívoco.
- `D-03`: la aplicación continúa siendo un único monolito modular.

## Validación prevista

- Compilar con destino no mapeado como error.
- Verificar mapeos de identidad, HTTP y jOOQ mediante pruebas dirigidas.
- Aplicar ArchUnit para impedir imports técnicos en `application.mapper`.

## Decisiones pendientes

- Mantener el inventario de mappers al añadir nuevas fronteras. Responsable: Revisor de arquitectura. Tratamiento: revisión de cada slice mediante la skill de arquitectura.
