---
name: arquitectura-hexagonal-y-modulith
description: Clasifica, crea, mueve o revisa clases Java de Running Coach conforme a su arquitectura hexagonal y Spring Modulith. Úsala obligatoriamente antes de crear, renombrar o mover cualquier clase Java y al revisar paquetería, dependencias entre capas o límites de módulos.
---

# Arquitectura hexagonal y Spring Modulith

Aplica esta Skill antes de crear, renombrar o mover una clase Java. Su objetivo es decidir primero la responsabilidad y el paquete, impedir contaminación del núcleo y exigir una revisión independiente del resultado.

## Contexto obligatorio

Lee antes de proponer código:

1. `AGENTS.md`.
2. `docs/adr/0014-modular-hexagonal-ddd-architecture.md` y sus refinamientos.
3. El diseño detallado del módulo afectado.
4. El árbol y los imports reales del código relacionado.

Si estas fuentes se contradicen, detente antes de crear la clase y señala el conflicto. Una Skill no puede resolver ni aceptar una decisión arquitectónica.

## Clasificar antes de crear

Presenta o registra esta ficha para cada clase nueva o movida:

| Clase | Responsabilidad única | Módulo propietario | Paquete | Dependencias permitidas | Prueba mínima |
| --- | --- | --- | --- | --- | --- |

No escribas la clase hasta poder completar la fila sin mezclar responsabilidades. Usa la primera categoría que encaje:

1. Interfaz o tipo de contrato consumido por otro módulo: `api.<concepto>`.
2. Invariante o lenguaje de negocio: `domain.<concepto>.<tipo>`, donde el tipo es `aggregate`, `entity`, `valueobject`, `policy`, `service`, `event` o `exception`.
3. Interfaz de caso de uso consumido solo dentro del módulo: `application.port.in`.
4. Interfaz requerida por aplicación: `application.port.out`.
5. Implementación y coordinación del caso de uso: `application.service`.
6. Entrada web, programada u operativa: `infrastructure.input`.
7. Persistencia o proveedor externo: `infrastructure.output`.
8. Spring, seguridad, properties o composición: `infrastructure.security` o `infrastructure.configuration`.

Lee [reglas de paquetería](references/reglas-de-paqueteria.md) para aplicar la estructura exacta y resolver comandos, resultados, excepciones y dependencias.

## Reglas no negociables

- `application.port.in` y `application.port.out` contienen solo interfaces y sus `package-info.java`.
- No existe un paquete genérico `application.model`.
- Los adaptadores son infraestructura; no existe una carpeta física `adapter`.
- Solo `api` es contrato entre módulos. Ningún módulo importa internos de otro.
- Cada paquete publicado `api.<concepto>` declara explícitamente `@NamedInterface("api")` en su `package-info.java`.
- `domain` y `application` no dependen de infraestructura, OpenAPI, jOOQ, Servlet ni Spring Security.
- `@Transactional` está permitido exclusivamente en servicios de aplicación que delimitan un caso de uso.
- Los controladores están en `infrastructure.input.web`, consumen tipos OpenAPI y pueden depender directamente de componentes técnicos de infraestructura.
- Los tipos jOOQ solo aparecen en `infrastructure.output.persistence.jooq` del módulo propietario.
- Una entrada de negocio solo invoca `application.port.in`; nunca usa `application.port.out`, repositorios o jOOQ directamente.
- Una salida implementa `application.port.out`; nunca invoca puertos de entrada ni coordina servicios de aplicación.
- No crees interfaces para componentes técnicos con una única implementación si el núcleo no necesita ese puerto.
- No crees agregados, entidades, value objects, políticas, comandos o resultados por plantilla; deben proteger una invariante o un contrato real.

## Revisión obligatoria por otro agente

Después de implementar y antes de declarar el cambio terminado, solicita a un subagente independiente que revise exclusivamente:

- ubicación y responsabilidad de cada clase creada o movida;
- imports prohibidos y dirección de dependencias;
- acceso entre módulos solo mediante `api`;
- fugas de tipos OpenAPI o jOOQ;
- interfaces, modelos o capas ceremoniales;
- tipo de prueba elegido.

Créalo sin historial heredado y entrégale mediante un prompt neutral únicamente `AGENTS.md`, esta Skill, el ADR aplicable, el diseño del módulo y el diff, sin anticipar una conclusión. El revisor no modifica archivos. Corrige los hallazgos válidos antes de continuar. Si no hay subagentes disponibles, declara que falta la revisión; no la presentes como independiente.

## Entrada en vigor de una estructura nueva

Si el árbol existente o los gates ArchUnit todavía codifican una paquetería anterior, no crees clases con ninguna de las dos estructuras. Declara la migración bloqueante y exige que paquetes, imports, tests de arquitectura y documentación cambien juntos. No desactives ni eludas un gate para hacer encajar la clase.

## Validación

Ejecuta primero las pruebas dirigidas. Después ejecuta las reglas ArchUnit y `ApplicationModules.verify()` incluidas en `check`. La revisión de un agente no reemplaza esos gates ni autoriza omitirlos.

En la entrega informa:

- clases y paquetes decididos;
- hallazgos del agente revisor;
- gates ejecutados y resultados;
- contradicciones o decisiones pendientes.
