# Directrices de Agentes y Desarrollo (AGENTS.md)

## 1. Rol, Comportamiento y Scope Guard (¡Importante!)
- **Pragmatismo ante todo:** Busca siempre la solución más sencilla, directa y mantenible. Evita la sobreingeniería, la introducción de abstracciones innecesarias, adaptadores o capas de configuración que no respondan a un requisito real e inmediato.
- **Pensamiento crítico y Trade-offs:** Actúa como un par técnico riguroso. Cuestiona las decisiones de diseño, evalúa trade-offs y sugiere alternativas estándar. No des nada por sentado.
- **Pregunta siempre que dudes:** Si los requisitos son ambiguos, falta contexto o hay múltiples caminos, **detente y pregunta**. No asumas la ruta a seguir.
- **Plan Mínimo antes de editar:** Define un plan ultra-reducido con:
  - **Resultado:** Comportamiento exacto esperado.
  - **No-objetivos:** Lo que explícitamente *no* hará esta tarea.
  - **Ficheros:** El conjunto mínimo indispensable a modificar.
  - **Prueba:** La validación exacta que demostrará el éxito.
- **Reglas durante la edición:**
  - Reutiliza código, helpers y patrones existentes antes de crear nada nuevo.
  - Arregla los errores desde su **causa raíz**; no pongas parches sobre premisas incorrectas.
  - No diseñes para casos futuros hipotéticos que nadie ha pedido. Elimina el código que reemplaces salvo exigencia explícita de compatibilidad.

## 2. Pausa y Confirmación Obligatoria
Obtén aprobación antes de:
- Expandir materialmente el alcance o tocar ficheros no relacionados.
- Añadir dependencias, frameworks o nueva infraestructura de pruebas.
- Modificar contratos públicos (OpenAPI), esquemas de base de datos o formatos de almacenamiento.
- Destruir o sobrescribir datos, descartar trabajo no comiteado o reescribir historial.

## 3. Estructura del Proyecto
- **Backend:** Spring Boot (MVC) estructurado mediante Spring Modulith con Java 25.
- **Persistencia:** PostgreSQL 18 como único motor. jOOQ efímero por módulo y migraciones con Flyway. Cero bases de datos en memoria para tests.
- **API y Frontend:** Contratos OpenAPI 3.1 como única fuente de verdad (Contract-first). SPA en React en `frontend/`.
- **Documentación:** Centralizada en `docs/` (ADRs y definiciones de problema) en español.

## 4. Arquitectura y Diseño
- **Fronteras estrictas:** Mantén la separación modular dictada por Modulith y protegida por ArchUnit (puertos y adaptadores). La infraestructura (controladores, tipos jOOQ) jamás debe filtrarse al dominio o aplicación.
- **API HTTP:** Diseño orientado a recursos estricto. Sin verbos ni acciones en rutas; respeta la semántica HTTP y la idempotencia.

## 5. Flujo de Trabajo y Pruebas
- **Ramas y Commits:** Ramas con prefijo `feature/`. Commits cortos, imperativos y en inglés (ej. `Add user authentication`). Todo entra por PR hacia `main`.
- **Pruebas (Backend y Frontend):**
  - Ejecuta la prueba más estrecha y relevante posible antes de crear archivos nuevos.
  - No añadas infraestructura de pruebas ni cobertura descontextualizada solo para cumplir métricas de un cambio puntual.
  - Stack: JUnit 6, AssertJ, Testcontainers (Backend); Vitest, Testing Library, Playwright (Frontend).

## 6. Comandos Esenciales (Ciclo corto)
- **Desarrollo Backend:** `./gradlew build`, `./gradlew test`, `./gradlew bootRun`
- **Generación de Código:** `./gradlew generateJooqFromPostgres`, `./gradlew generateOpenApiServer`, `./gradlew apiCheck`
- **Desarrollo Frontend (`/frontend`):** `npm run typecheck`, `npm run test:unit`, `npm run build`
- **Validación Final Pre-PR:** `./gradlew qualityGate` (Reservado estrictamente para la evidencia final, no para depuración iterativa).

## Autoridad y límites de la IA
Los gates deterministas son la única autoridad automática para compilación, pruebas,
análisis, contratos y seguridad. La Skill local `$implementar-slice` prepara y ejecuta
el trabajo autorizado, pero no puede aprobar, omitir ni declarar innecesario un gate
aplicable. No puede decidir producto, alcance o arquitectura; esas decisiones requieren
confirmación humana explícita.
